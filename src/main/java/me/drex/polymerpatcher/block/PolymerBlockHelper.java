package me.drex.polymerpatcher.block;

import eu.pb4.factorytools.api.block.model.SignModel;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.config.BlockConfig;
import me.drex.polymerpatcher.config.ConfigManager;
import me.drex.polymerpatcher.resources.ResourcePackGenerator;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;

import java.util.Map;

public class PolymerBlockHelper {
    public static void registerPolymerBlock(Identifier id, Block block) {
        BlockStateModelManager.addBlock(id, block);

        PolymerBlock polymerBlock = requestPolymerBlock(id, block);
        PolymerBlock.registerOverlay(block, polymerBlock);
        if (polymerBlock instanceof BlockWithElementHolder blockWithElementHolder) {
            BlockWithElementHolder.registerOverlay(block, blockWithElementHolder);
        }

        if (block instanceof SignBlock) {
            SignModel.setModel(block, id.withPrefix("block_sign/"));
        }
        if (block.getClass().equals(StandingSignBlock.class)) {
            ResourcePackGenerator.SIGNS.add(id.withPath(id.getPath().replace("_sign", "")));
        }
    }

    public static int getPriority(Holder.Reference<Block> reference) {
        Identifier id = reference.key().identifier();
        BlockConfig blocks = ConfigManager.config().blocks;

        Integer weight = blocks.weights.get(id);
        if (weight != null) {
            return weight;
        }

        for (BlockConfig.RegexWeight regexWeight : blocks.regexWeights) {
            if (id.toString().matches(regexWeight.regex())) {
                return regexWeight.weight();
            }
        }
        return blocks.defaultWeight;
    }

    public static PolymerBlock requestPolymerBlock(Identifier id, Block block) {
        PolymerPatcher.LOGGER.debug("Registering polymer block {}...", id);
        return AutomaticFactoryBlock.create(id, block);
    }
}
