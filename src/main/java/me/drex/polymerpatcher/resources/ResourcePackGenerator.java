package me.drex.polymerpatcher.resources;


import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.resourcepack.ModelModifiers;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateMultiPartDefinition;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelElement;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.entity.AnimatedEntities;
import net.minecraft.IdentifierException;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.function.Predicate;

public class ResourcePackGenerator {
    private static final Vec3 EXPANSION = new Vec3(0.08, 0.08, 0.08);
    public static final Set<Identifier> SIGNS = new HashSet<>();
    public static final Set<Identifier> EXPANDABLE_MODELS = new HashSet<>();
    private static final Set<Identifier> EXPANDED_BLOCK_IDS = new HashSet<>();

    public static void setup() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(ResourcePackGenerator::build);
    }

    public static void expandBlockModel(Identifier id) {
        if (EXPANDED_BLOCK_IDS.add(id)) {
            expandBlockModel(id, x -> true);
        }
    }

    public static void expandBlockModel(Identifier id, Predicate<String> variantPredicate) {
        try {
            BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);

            Map<String, List<StateModelVariant>> variants = blockStateAsset.variants().orElse(Collections.emptyMap());
            variants.entrySet().forEach(entry -> {
                if (variantPredicate.test(entry.getKey())) {
                    entry.getValue().forEach(variant -> expandModel(variant.model()));
                }
            });

            List<StateMultiPartDefinition> multiParts = blockStateAsset.multipart().orElse(Collections.emptyList());
            multiParts.forEach(stateMultiPartDefinition -> stateMultiPartDefinition.apply().forEach(stateModelVariant -> expandModel(stateModelVariant.model())));
        } catch (Throwable e) {
            PolymerPatcher.LOGGER.error("Failed to read blockstate {}: {}", id, e);
        }
    }

    private static void expandModel(Identifier id) {
        try {
            EXPANDABLE_MODELS.add(id.withSuffix(".json"));
            ModelAsset modelAsset = ResourceHelper.decodeModel(id);
            modelAsset.parent().ifPresent(ResourcePackGenerator::expandModel);
        } catch (Throwable e) {
            PolymerPatcher.LOGGER.error("Failed to read model {}: {}", id, e);
        }
    }

    private static void build(ResourcePackBuilder builder) {
        PolymerPatcher.LOGGER.info("Generating resource pack for {}...", Arrays.toString(PolymerPatcher.PATCHED_MODS.toArray()));
        var atlas = AtlasAsset.builder();
        modifyAcaciaBoatTextures(builder);
        builder.forEachResource((path, packResource) -> {
            String[] parts = path.split("/", 4);
            if (parts.length < 4) return;
            try {
                Identifier id = Identifier.fromNamespaceAndPath(parts[1], parts[3]);
                if (!parts[0].equals("assets") || !parts[2].equals("models")) return;
                if (!EXPANDABLE_MODELS.contains(id)) return;
                var asset = ModelAsset.fromJson(new String(packResource.readAllBytes(), StandardCharsets.UTF_8));
                expandModelAsset(asset, path, builder);
            } catch (IdentifierException ignored) {
            }
        });

        for (var model : AnimatedEntities.POLY_MODELS) {
            model.generateAssets(builder::addData, atlas);
        }

        PolymerPatcher.PATCHED_MODS.forEach(modid -> {
            for (var entry : BlockStateModelManager.UV_LOCKED_MODELS.getOrDefault(modid, Collections.emptyMap()).entrySet()) {
                String path = entry.getKey();
                Identifier id = Identifier.fromNamespaceAndPath(modid, path).withSuffix(".json");

                var expand = EXPANDABLE_MODELS.contains(id) ? EXPANSION : Vec3.ZERO;

                for (var v : entry.getValue()) {
                    var suffix = "_uvlock_" + v.x() + "_" + v.y();
                    var modelId = v.model().withSuffix(suffix);
                    var asset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getData(AssetPaths.model(v.model()) + ".json")), StandardCharsets.UTF_8));

                    if (asset.parent().isPresent()) {
                        var parentId = asset.parent().get();
                        var parentAsset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getDataOrSource(AssetPaths.model(parentId) + ".json")), StandardCharsets.UTF_8));
                        builder.addData(AssetPaths.model(PolymerPatcher.MOD_ID, parentId.getPath() + suffix) + ".json",
                            ModelModifiers.expandModelAndRotateUVLocked(parentAsset, expand, v.x(), v.y()));
                        builder.addData(AssetPaths.model(modelId) + ".json",
                            new ModelAsset(Optional.of(Identifier.fromNamespaceAndPath(PolymerPatcher.MOD_ID, parentId.getPath() + suffix)), asset.elements(),
                                asset.textures(), asset.display(), asset.guiLight(), asset.ambientOcclusion()).toBytes());
                    }
                }
            }
        });

        SIGNS.forEach(id -> ModelModifiers.createSignModel(builder, id.getNamespace(), id.getPath(), atlas));

        builder.addData("assets/minecraft/atlases/blocks.json", atlas.build());
    }

    private static void modifyAcaciaBoatTextures(ResourcePackBuilder builder) {
        String path = "textures/" + ResourceHelper.ACACIA_BOAT_TEXTURE.getPath() + ".png";
        var source = ResourceHelper.getVanillaAsset(Identifier.DEFAULT_NAMESPACE, path);
        try (var stream = source.get()) {
            byte[] original = stream.readAllBytes();
            var transparent = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB), "png", transparent);
            builder.addData("assets/" + ResourceHelper.ORIGINAL_ACACIA_BOAT_TEXTURE.getNamespace() + "/textures/"
                + ResourceHelper.ORIGINAL_ACACIA_BOAT_TEXTURE.getPath() + ".png", original);
            builder.addData("assets/minecraft/" + path, transparent.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare acacia boat textures", e);
        }
    }

    private static void expandModelAsset(ModelAsset asset, String path, ResourcePackBuilder builder) {
        builder.addData(path, new ModelAsset(asset.parent().map(identifier -> identifier.withSuffix("_expanded")), asset.elements().map(x -> x.stream()
            .map(element -> new ModelElement(element.from().subtract(EXPANSION), element.to().add(EXPANSION),
                element.faces(), element.rotation(), element.shade(), element.lightEmission())
            ).toList()), asset.textures(), asset.display(), asset.guiLight(), asset.ambientOcclusion()).toBytes());

        if (asset.parent().isPresent()) {
            var parentId = asset.parent().get();
            var parentAsset = ModelAsset.fromJson(new String(Objects.requireNonNull(builder.getDataOrSource(AssetPaths.model(parentId) + ".json")), StandardCharsets.UTF_8));

            expandModelAsset(parentAsset, AssetPaths.model(parentId.withSuffix("_expanded")) + ".json", builder);
        }
    }

}