package me.drex.polymerpatcher.mixin.sync;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.attribute.EnvironmentAttribute;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnvironmentAttribute.Builder.class)
public class EnvironmentAttribute$BuilderMixin {
    @WrapOperation(
        method = "syncable",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttribute$Builder;isSyncable:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    public void weDontSupportSync(EnvironmentAttribute.Builder instance, boolean value, Operation<Void> original) {
    }
}
