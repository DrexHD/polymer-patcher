package me.drex.polymerpatcher.mixin.entity;

import me.drex.polymerpatcher.entity.AutomaticPolymerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
    @Shadow
    public abstract void setInput(boolean left, boolean right, boolean up, boolean down);


    @Shadow
    protected abstract void controlBoat();

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;floatBoat()V",
            shift = At.Shift.AFTER
        )
    )
    private void steerOnServer(CallbackInfo ci) {
        var boat = (AbstractBoat) (Object) this;
        if (AutomaticPolymerEntity.isServerControlled(boat)) {
            var input = ((ServerPlayer) boat.getControllingPassenger()).getLastClientInput();
            setInput(input.left(), input.right(), input.forward(), input.backward());
            controlBoat();
        }
    }
}
