package me.drex.polymerpatcher.entity;

import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.resources.ResourceHelper;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry;
import me.drex.polymerpatcher.entity.render.ServerBlockModelResolver;
import me.drex.polymerpatcher.entity.render.ServerEntityModelSet;
import me.drex.polymerpatcher.entity.render.ServerEntityRenderDispatcher;
import me.drex.polymerpatcher.entity.render.ServerItemModelResolver;
import me.drex.polymerpatcher.entity.render.ServerRendererContext;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AnimatedEntities {

    public static final List<PolyModelInstance<?, ?, ?>> POLY_MODELS = new ArrayList<>();
    protected static final IdentityHashMap<EntityType<?>, Function<? extends Entity, ? extends SimpleEntityModel<?, ?, ?>>> ENTITY_FACTORIES = new IdentityHashMap<>();

    public static <Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>> void registerEntity(
        EntityType<? extends Entity> type,
        EntityRenderer<Entity, RenderState> renderer, List<ModelPart> allParts, @Nullable ModelLayerLocation modelLayer, @Nullable Identifier texture
    ) {
        PolyModelInstance<Entity, RenderState, Model> defaultModel = PolyModelInstance.create(renderer, allParts, modelLayer, texture);
        POLY_MODELS.add(defaultModel);
        ENTITY_FACTORIES.put(type, entity -> new SimpleEntityModel<>((Entity) entity, defaultModel));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerEntities(ClientDataRegistry registry) {
        Map<ModelLayerLocation, ModelPart> bakedModels = new LinkedHashMap<>();

        registry.modelLayers.forEach((modelLayerLocation, layerDefinition) -> {
            bakedModels.put(modelLayerLocation, layerDefinition.bakeRoot());
        });
        bakedModels.put(ModelLayers.ACACIA_BOAT, BoatModel.createBoatModel().bakeRoot());
        bakedModels.computeIfAbsent(ModelLayers.BOAT_WATER_PATCH, layer -> BoatModel.createWaterPatch().bakeRoot());
        var entityModelSet = new ServerEntityModelSet(bakedModels);
        var blockModelResolver = new ServerBlockModelResolver();
        var itemModelResolver = new ServerItemModelResolver();
        var renderDispatcher = new ServerEntityRenderDispatcher(blockModelResolver, itemModelResolver);

        ServerRendererContext context = new ServerRendererContext(renderDispatcher, blockModelResolver, itemModelResolver, entityModelSet);

        registry.entityData.forEach((renderInfo) -> {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(renderInfo.type());
            Class<? extends EntityRenderer> entityRendererClass = renderInfo.entityRenderer();
            if (entityRendererClass == null) {
                PolymerPatcher.LOGGER.warn("Failed to find entity renderer for {}", renderInfo.type());
                return;
            }
            if (renderInfo.modelLayers().isEmpty() || renderInfo.textures().isEmpty()) {
                EntityRenderer entityRenderer = createEntityRenderer(entityId, entityRendererClass, context, null);
                AnimatedEntities.registerEntity(renderInfo.type(), entityRenderer, Collections.emptyList(), null, null);
            }
            for (ModelLayerLocation modelLayer : renderInfo.modelLayers()) {
                EntityRenderer entityRenderer = createEntityRenderer(entityId, entityRendererClass, context, modelLayer);

                if (entityRenderer != null) {
                    for (Identifier texture : renderInfo.textures()) {
                        ModelPart modelPart = bakedModels.get(modelLayer);
                        AnimatedEntities.registerEntity(renderInfo.type(), entityRenderer, modelPart.getAllParts(), modelLayer, texture);
                    }
                }
            }
        });
        registerEntity(EntityType.ACACIA_BOAT, new BoatRenderer(context, ModelLayers.ACACIA_BOAT),
            bakedModels.get(ModelLayers.ACACIA_BOAT).getAllParts(), ModelLayers.ACACIA_BOAT, ResourceHelper.ACACIA_BOAT_TEXTURE);
    }

    @SuppressWarnings("rawtypes")
    private static EntityRenderer createEntityRenderer(Identifier entityId, Class<? extends EntityRenderer> entityRendererClass, EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
        Constructor<? extends EntityRenderer> declaredConstructor;
        try {
            declaredConstructor = entityRendererClass.getDeclaredConstructor(EntityRendererProvider.Context.class);
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance(context);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            try {
                declaredConstructor = entityRendererClass.getDeclaredConstructor(EntityRendererProvider.Context.class, ModelLayerLocation.class);
                declaredConstructor.setAccessible(true);
                return declaredConstructor.newInstance(context, modelLayer);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException ex) {
                PolymerPatcher.LOGGER.warn("Failed to create entity renderer constructor for {} using {}", entityId, modelLayer);
                return null;
            }
        }
    }
}
