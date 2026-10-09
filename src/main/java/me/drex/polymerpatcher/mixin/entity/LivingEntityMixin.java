package me.drex.polymerpatcher.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import me.drex.polymerpatcher.entity.AutomaticPolymerEntity;
import me.drex.polymerpatcher.entity.SimpleEntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private boolean polymer_patcher$jumping;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @WrapMethod(method = "travelRidden")
    private void applyRiderInput(Player controller, Vec3 input, Operation<Void> original) {
        if (AutomaticPolymerEntity.isServerControlled(this) && controller instanceof ServerPlayer player) {
            var keys = player.getLastClientInput();
            float sideways = (keys.left() ? 1 : 0) - (keys.right() ? 1 : 0);
            float forward = (keys.forward() ? 1 : 0) - (keys.backward() ? 1 : 0);
            float length = Math.max(1, (float) Math.sqrt(sideways * sideways + forward * forward));
            controller.xxa = sideways / length;
            controller.zza = forward / length;
            input = new Vec3(controller.xxa, 0, controller.zza);
            if (this instanceof PlayerRideableJumping jumping && jumping.canJump()) {
                if (keys.jump() && !polymer_patcher$jumping) {
                    jumping.handleStartJump(90);
                    jumping.onPlayerJump(90);
                } else if (!keys.jump() && polymer_patcher$jumping) {
                    jumping.handleStopJump();
                }
            }
            polymer_patcher$jumping = keys.jump();
        } else {
            polymer_patcher$jumping = false;
        }
        original.call(controller, input);
    }

    @ModifyExpressionValue(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
            ordinal = 2
        )
    )
    public boolean serverSideWalkAnimation(boolean original) {
        return original || PolymerEntity.get(this) instanceof AutomaticPolymerEntity;
    }

    @ModifyArg(
        method = "take",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundTakeItemEntityPacket;<init>(III)V"
        ),
        index = 1
    )
    public int fixTakeItemEntityId(int original) {
        UniqueIdentifiableAttachment attachment = UniqueIdentifiableAttachment.get(this, AutomaticPolymerEntity.MODEL);
        if (attachment == null) return original;
        var model = (SimpleEntityModel) attachment.holder();

        return model.leadAttachment.getEntityId();
    }
}
