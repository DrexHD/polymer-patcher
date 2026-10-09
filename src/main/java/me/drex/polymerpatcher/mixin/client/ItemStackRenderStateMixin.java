package me.drex.polymerpatcher.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drex.polymerpatcher.duck.IItemStackRenderState;
import me.drex.polymerpatcher.entity.SimpleEntityModel;
import me.drex.polymerpatcher.entity.render.ServerSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements IItemStackRenderState {
    @Shadow
    ItemDisplayContext displayContext;
    @Unique
    private ItemStack polymer_patcher$Item = null;

    @Override
    public void polymer_patcher$updateData(ItemStack item, ItemDisplayContext context) {
        polymer_patcher$Item = item;
        this.displayContext = context;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    public void updateItem(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, int k, CallbackInfo ci) {
        if (!ServerSubmitNodeCollector.ACTIVE_ENTITY.isBound() || polymer_patcher$Item == null) return;
        SimpleEntityModel model = ServerSubmitNodeCollector.ACTIVE_ENTITY.get();
        model.updateItemDisplayElement(polymer_patcher$Item, displayContext, poseStack.last().pose());
    }
}
