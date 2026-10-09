package me.drex.polymerpatcher.mixin.polymer.fix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

@Mixin(ElementHolder.class)
public abstract class ElementHolderMixin {
    // Fixed in https://github.com/Patbox/polymer/commit/9b7a085facc9fb94841636c89800527de32d20cb
    @WrapOperation(
        method = "removeElement",
        at = @At(
            value = "FIELD",
            target = "Leu/pb4/polymer/virtualentity/api/ElementHolder;elements:Ljava/util/List;",
            opcode = Opcodes.GETFIELD
        )
    )
    private List<VirtualElement> dontRemoveAllElements(
        ElementHolder instance, Operation<List<VirtualElement>> original,
        @Local(argsOnly = true, name = "element") VirtualElement element,
        @Local(name = "player") ServerGamePacketListenerImpl player
        ) {
        element.stopWatching(player.getPlayer(), player::send);
        return Collections.emptyList();
    }
}
