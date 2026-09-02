package me.drex.polymerpatcher.dump.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.drex.polymerpatcher.dump.generation.ClientDataGenerator;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin {
    @Inject(
        method = "bakeLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/builders/LayerDefinition;bakeRoot()Lnet/minecraft/client/model/geom/ModelPart;"
        )
    )
    public void captureContext(ModelLayerLocation modelLayerLocation, CallbackInfoReturnable<ModelPart> cir, @Local LayerDefinition layerDefinition) {
        ClientDataGenerator.inBuilderContext(builder -> builder.modelLayer(modelLayerLocation, layerDefinition));
    }
}
