package me.drex.polymerpatcher.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.item.ItemModelResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    public <T> Object dontCheck(T o, Operation<T> original) {
        return o;
    }
}
