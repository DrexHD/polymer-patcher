package me.drex.polymerpatcher.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.drex.polymerpatcher.entity.AutomaticPolymerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReturnValue(method = "isClientAuthoritative", at = @At("RETURN"))
    private boolean serverControlled(boolean original) {
        if (AutomaticPolymerEntity.isServerControlled((Entity) (Object) this)) {
            return false;
        }
        return original;
    }
}
