package me.drex.polymerpatcher.entity.render;

import me.drex.polymerpatcher.duck.IItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ServerItemModelResolver extends ItemModelResolver {
    public ServerItemModelResolver() {
        super(null);
    }

    @Override
    public float swapAnimationScale(ItemStack itemStack) {
        return 1.0f;
    }

    @Override
    public boolean shouldPlaySwapAnimation(ItemStack itemStack) {
        return false;
    }

    @Override
    public void appendItemLayers(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, @Nullable Level level, @Nullable ItemOwner itemOwner, int i) {
        ((IItemStackRenderState)itemStackRenderState).polymer_patcher$updateData(itemStack, itemDisplayContext);
    }

    @Override
    public void updateForTopItem(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, @Nullable Level level, @Nullable ItemOwner itemOwner, int i) {
        ((IItemStackRenderState)itemStackRenderState).polymer_patcher$updateData(itemStack, itemDisplayContext);
    }

    @Override
    public void updateForNonLiving(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, Entity entity) {
        ((IItemStackRenderState)itemStackRenderState).polymer_patcher$updateData(itemStack, itemDisplayContext);
    }

    @Override
    public void updateForLiving(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, LivingEntity livingEntity) {
        ((IItemStackRenderState)itemStackRenderState).polymer_patcher$updateData(itemStack, itemDisplayContext);
    }
}
