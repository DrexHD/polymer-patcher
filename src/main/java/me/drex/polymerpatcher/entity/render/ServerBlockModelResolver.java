package me.drex.polymerpatcher.entity.render;

import me.drex.polymerpatcher.duck.IBlockModelRenderState;
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
        renderState.clear();
        ((IBlockModelRenderState) renderState).polymer_patcher$updateData(blockState);
    }

    @Override
    public void updateForItemFrame(BlockModelRenderState renderState, boolean isGlowing, boolean map) {
    }
}
