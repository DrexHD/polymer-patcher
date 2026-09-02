package me.drex.polymerpatcher.dump.data;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClientDataRegistry {
    public final List<EntityInfo> entityData = new LinkedList<>();
    public final Map<ModelLayerLocation, LayerDefinition> modelLayers = new HashMap<>();
    public final List<BlockInfo> blockData = new LinkedList<>();
    private final Map<String, String> modVersions = new HashMap<>();

    public transient final Map<Block, BlockInfo> blockInfoByBlock = new HashMap<>();

    public record EntityInfo(
        EntityType type,
        Class<? extends EntityRenderer> entityRenderer,
        Set<ModelLayerLocation> modelLayers,
        Set<Identifier> textures
    ) {
    }

    public record BlockInfo(Block block, ColorResolver biomeColor, boolean transparent) {
    }

    public void updateModVersions() {
        for (String requiredMod : contentMods()) {
            FabricLoader.getInstance().getModContainer(requiredMod)
                .map(ModContainer::getMetadata)
                .ifPresent(metadata ->
                    modVersions.put(metadata.getId(), metadata.getVersion().getFriendlyString())
                );
        }
    }

    public boolean requiresUpdate() {
        // Make sure any mods that had data generated still use the same version (or have been removed)
        for (Map.Entry<String, String> entry : modVersions.entrySet()) {
            boolean versionChanged = FabricLoader.getInstance().getModContainer(entry.getKey())
                .map(ModContainer::getMetadata)
                .map(modMetadata -> !modMetadata.getVersion().getFriendlyString().equals(entry.getValue()))
                .orElse(false);
            if (versionChanged) {
                return true;
            }
        }

        // Ensure any content mods have data generated for them
        Set<String> contentMods = contentMods();

        for (String contentMod : contentMods) {
            if (!modVersions.containsKey(contentMod)) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> contentMods() {
        Set<String> contentMods = new HashSet<>();
        collectNamespaces(BuiltInRegistries.ENTITY_TYPE, contentMods::add);
        collectNamespaces(BuiltInRegistries.BLOCK, contentMods::add);
        return contentMods;
    }

    private static <T> void collectNamespaces(DefaultedRegistry<T> registry, Consumer<String> namespaceConsumer) {
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            Identifier identifier = entry.getKey().identifier();
            String namespace = identifier.getNamespace();
            if (namespace.equals(Identifier.DEFAULT_NAMESPACE)) continue;
            namespaceConsumer.accept(namespace);
        }
    }

    public void rebuildIndexes() {
        blockInfoByBlock.clear();
        for (BlockInfo blockDatum : blockData) {
            blockInfoByBlock.put(blockDatum.block, blockDatum);
        }
    }

}
