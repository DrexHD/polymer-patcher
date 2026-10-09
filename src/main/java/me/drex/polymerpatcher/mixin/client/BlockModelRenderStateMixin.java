package me.drex.polymerpatcher.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drex.polymerpatcher.duck.IBlockModelRenderState;
import me.drex.polymerpatcher.entity.render.ServerSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelRenderState.class)
public class BlockModelRenderStateMixin implements IBlockModelRenderState {
    @Unique
    private BlockState polymer_patcher$blockState;

    @Override
    public void polymer_patcher$updateData(BlockState state) {
        polymer_patcher$blockState = state;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clearBlock(CallbackInfo ci) {
        polymer_patcher$blockState = null;
    }

    @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true)
    private void submitBlock(
        RenderType renderType, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci
    ) {
        if (!ServerSubmitNodeCollector.ACTIVE_ENTITY.isBound() || polymer_patcher$blockState == null) return;
        var model = ServerSubmitNodeCollector.ACTIVE_ENTITY.get();
        model.updateBlock(polymer_patcher$blockState, poseStack.last().pose(), 42);
        ci.cancel();
    }
}
