package me.drex.polymerpatcher.entity.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

public class ServerBlockModelResolver extends BlockModelResolver {
    public ServerBlockModelResolver() {
        super(null);
    }

    @Override
    public void update(BlockModelRenderState renderState, BlockState blockState, BlockDisplayContext displayContext) {
    }

    @Override
    public void updateForItemFrame(BlockModelRenderState renderState, boolean isGlowing, boolean map) {
    }
}
