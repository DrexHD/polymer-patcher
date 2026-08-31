package me.drex.polymerpatcher.dump.data;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RenderRegistry {
    public final List<RenderInfo> entityData = new LinkedList<>();
    public final Map<ModelLayerLocation, LayerDefinition> modelLayers = new HashMap<>();
    public final List<BlockInfo> blockData = new LinkedList<>();

    public transient final Map<Block, BlockInfo> blockInfoByBlock = new HashMap<>();

    public record RenderInfo(
        EntityType type,
        Class<? extends EntityRenderer> entityRenderer,
        Set<ModelLayerLocation> modelLayers,
        Set<Identifier> textures
    ) {
    }

    public record BlockInfo(Block block, ColorResolver biomeColor, boolean transparent) {
    }

    public void rebuildIndexes() {
        blockInfoByBlock.clear();
        for (BlockInfo blockDatum : blockData) {
            blockInfoByBlock.put(blockDatum.block, blockDatum);
        }
    }

}
