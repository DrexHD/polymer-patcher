package me.drex.polymerpatcher.dump.generation;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drex.polymerpatcher.dump.PolymerPatcherDumper;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry.BlockInfo;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry.EntityInfo;
import me.drex.polymerpatcher.dump.mixin.RenderSetupAccessor;
import me.drex.polymerpatcher.dump.mixin.RenderTypeAccessor;
import me.drex.polymerpatcher.dump.render.DumpSubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ClientDataGenerator {
    private ClientDataGenerator() {
    }

    public static class EntityInfoBuilder {
        private final EntityType<?> type;
        private final Map<ModelLayerLocation, LayerDefinition> modelLayers = new HashMap<>();
        private final Set<Identifier> textures = new HashSet<>();
        private Class<? extends EntityRenderer> entityRenderer;


        public EntityInfoBuilder(EntityType<?> type) {
            this.type = type;
        }

        public void entityRenderer(Class<? extends EntityRenderer> entityRenderer) {
            this.entityRenderer = entityRenderer;
        }

        public void modelLayer(ModelLayerLocation modelLayerLocation, LayerDefinition modelLayer) {
            this.modelLayers.put(modelLayerLocation, modelLayer);
        }

        public void texture(Identifier texture) {
            if (this.textures.add(texture)) {
                PolymerPatcherDumper.LOGGER.info("{} found for {}", texture, this.type);
            }
        }

        public Map<ModelLayerLocation, LayerDefinition> modelLayers() {
            return this.modelLayers;
        }

        public EntityInfo build() {
            return new EntityInfo(type, entityRenderer, new LinkedHashSet<>(modelLayers.keySet()), expandEntityTextures(textures));
        }
    }

    private static final Map<EntityType<?>, EntityInfoBuilder> BUILDERS_BY_TYPE = new ConcurrentHashMap<>();
    private static final ThreadLocal<EntityInfoBuilder> INFO_BUILDER = new ThreadLocal<>();

    public static <T extends EntityRenderer<?, ?>> T captureEntityRenderer(EntityType<?> entityType, Supplier<T> factory) {
        EntityInfoBuilder builder = new EntityInfoBuilder(entityType);
        return withBuilderContext(builder, builder1 -> {
            T renderer = factory.get();

            builder.entityRenderer(renderer.getClass());
            BUILDERS_BY_TYPE.put(entityType, builder);
            return renderer;
        });
    }

    public static void captureTexture(RenderType renderType) {
        RenderSetup renderSetup = ((RenderTypeAccessor) renderType).getState();
        Map<String, RenderSetup.TextureBinding> textures = ((RenderSetupAccessor) (Object) renderSetup).polymer_patcher$textures();
        inBuilderContext(builder -> {
            textures.values().forEach(texture -> {
                builder.texture(texture.location());
            });
        });
    }

    public static <T> T withBuilderContext(EntityInfoBuilder builder, Function<EntityInfoBuilder, T> action) {
        EntityInfoBuilder previousBuilder = INFO_BUILDER.get();
        try {
            INFO_BUILDER.set(builder);
            return action.apply(builder);
        } finally {
            INFO_BUILDER.set(previousBuilder);
        }
    }

    public static void inBuilderContext(Consumer<EntityInfoBuilder> action) {
        EntityInfoBuilder builder = INFO_BUILDER.get();
        if (builder != null) {
            action.accept(builder);
        }
    }

    public static ClientDataRegistry generate(ServerLevel level) {
        ClientDataRegistry registry = new ClientDataRegistry();
        collectEntityInfo(registry, level);
        collectBlockData(registry);
        registry.updateModVersions();
        registry.rebuildIndexes();
        return registry;
    }

    private static void collectEntityInfo(ClientDataRegistry registry, ServerLevel level) {
        CameraRenderState cameraRenderState = new CameraRenderState();

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) continue;
            try {
                Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
                if (entity == null) {
                    continue;
                }

                EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                EntityRenderState renderState = entityRenderDispatcher.extractEntity(entity, 0.0F);
                EntityRenderer<?, ? super EntityRenderState> renderer = entityRenderDispatcher.getRenderer(renderState);
                withBuilderContext(BUILDERS_BY_TYPE.get(entityType), (builder) -> {
                        renderer.submit(renderState, new PoseStack(), DumpSubmitNodeCollector.INSTANCE, cameraRenderState);

                        registry.modelLayers.putAll(builder.modelLayers());
                        registry.entityData.add(builder.build());
                        return null;
                    }
                );
            } catch (Throwable e) {
                PolymerPatcherDumper.LOGGER.warn("Failed to render entity: {}", entityType);
            }
        }
    }

    private static void collectBlockData(ClientDataRegistry registry) {
        PolymerPatcherDumper.LOGGER.info("Collecting block data...");

        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) continue;
            BlockState blockState = block.defaultBlockState();

            PolymerPatcherDumper.COLOR_RESOLVER.remove();
            BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
            if (tintSource != null) {
                tintSource.colorInWorld(blockState, BlockAndTintGetter.EMPTY, BlockPos.ZERO);
            }

            var colorResolver = PolymerPatcherDumper.COLOR_RESOLVER.get();
            // Only store non default values
            if (colorResolver != null) {
                BlockInfo blockInfo = new BlockInfo(block, colorResolver);
                registry.blockData.add(blockInfo);
            }
        }
    }

    private static Set<Identifier> expandEntityTextures(Set<Identifier> textures) {
        Set<Identifier> expandedTextures = new LinkedHashSet<>();
        for (Identifier texture : new HashSet<>(textures)) {
            expandedTextures.add(toModelTextureIdentifier(texture));
            if (shouldExpandSiblingTextures(texture)) {
                addSiblingTextures(texture, expandedTextures);
            }
        }
        return expandedTextures;
    }

    private static boolean shouldExpandSiblingTextures(Identifier defaultTexture) {
        Path path = Path.of(defaultTexture.getPath());
        Path parent = path.getParent();
        if (parent == null) {
            return false;
        }

        return !parent.equals(Path.of("textures/entity"));
    }

    private static void addSiblingTextures(Identifier defaultTexture, Set<Identifier> textures) {
        Path parent = Path.of(defaultTexture.getPath()).getParent();
        if (parent == null) {
            return;
        }

        Set<Tuple<Identifier, IoSupplier<InputStream>>> files = PolymerPatcherDumper.GLOBAL_ASSETS.locateFiles(parent.toString());
        for (Tuple<Identifier, IoSupplier<InputStream>> file : files) {
            Identifier texture = file.getA();
            if (!texture.getNamespace().equals(defaultTexture.getNamespace())) {
                continue;
            }

            Identifier modelTexture = toModelTextureIdentifier(texture);

            PolymerPatcherDumper.LOGGER.info("{} sibling of {}", modelTexture.getPath(), texture);
            textures.add(modelTexture);
        }
    }

    private static Identifier toModelTextureIdentifier(Identifier texture) {
        String path = texture.getPath()
            .replace("textures/", "")
            .replace(".png", "");

        return Identifier.fromNamespaceAndPath(texture.getNamespace(), path);
    }

}
