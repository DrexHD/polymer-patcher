package me.drex.polymerpatcher.command;

import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.impl.HolderHolder;
import me.drex.polymerpatcher.mixin.debug.EntityAttachmentAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PolymerPatcherCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pp")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("count-displays")
                    .then(Commands.literal("block").executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        var map = new HashMap<Block, DisplayCountResult>();
                        for (var holder : ((HolderHolder) player.connection).polymer$getHolders()) {
                            if (holder.getAttachment() instanceof BlockAwareAttachment attachment) {
                                map.compute(attachment.getBlockState().getBlock(), (entityType, displayCountResult) -> {
                                    if (displayCountResult == null) displayCountResult = DisplayCountResult.DEFAULT;
                                    return new DisplayCountResult(displayCountResult.attachments + 1, displayCountResult.elements + holder.getElements().size());
                                });
                            }
                        }
                        DisplayCountResult global = map.values().stream().reduce(DisplayCountResult.DEFAULT, (displayCountResult, displayCountResult2) -> new DisplayCountResult(displayCountResult.attachments + displayCountResult2.attachments, displayCountResult.elements + displayCountResult2.elements));
                        var text = Component.literal("Global: " + global.attachments + " attachments, " + global.elements + " elements\n\n");
                        map.entrySet().stream().sorted(Map.Entry.<Block, DisplayCountResult>comparingByValue().reversed()).forEach(entry -> text.append(Component.literal(BuiltInRegistries.BLOCK.getKey(entry.getKey()) + " -> " + entry.getValue().elements + " elements on " + entry.getValue().attachments + " attachments\n\n")));
                        player.openDialog(Holder.direct(new NoticeDialog(new CommonDialogData(Component.literal("Entity attachment counts from blocks"), Optional.empty(), true, true, DialogAction.CLOSE, List.of(new PlainMessage(text, 1000)), List.of()), new ActionButton(new CommonButtonData(CommonComponents.GUI_OK, CommonButtonData.DEFAULT_WIDTH), Optional.empty()))));
                        return 1;
                    })).then(Commands.literal("entity").executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        var map = new HashMap<EntityType<?>, DisplayCountResult>();
                        for (var holder : ((HolderHolder) player.connection).polymer$getHolders()) {
                            if (holder.getAttachment() instanceof EntityAttachmentAccessor accessor) {
                                map.compute(accessor.getEntity().getType(), (entityType, displayCountResult) -> {
                                    if (displayCountResult == null) displayCountResult = DisplayCountResult.DEFAULT;
                                    return new DisplayCountResult(displayCountResult.attachments + 1, displayCountResult.elements + holder.getElements().size());
                                });
                            }
                        }
                        DisplayCountResult global = map.values().stream().reduce(DisplayCountResult.DEFAULT, (displayCountResult, displayCountResult2) -> new DisplayCountResult(displayCountResult.attachments + displayCountResult2.attachments, displayCountResult.elements + displayCountResult2.elements));
                        var text = Component.literal("Global: " + global.attachments + " attachments, " + global.elements + " elements\n\n");

                        map.entrySet().stream().sorted(Map.Entry.<EntityType<?>, DisplayCountResult>comparingByValue().reversed()).forEach(entry -> text.append(Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()) + " -> " + entry.getValue().elements + " elements on " + entry.getValue().attachments + " attachments\n\n")));

                        player.openDialog(Holder.direct(new NoticeDialog(new CommonDialogData(Component.literal("Entity attachment counts from entities"), Optional.empty(), true, true, DialogAction.CLOSE, List.of(new PlainMessage(text, 1000)), List.of()), new ActionButton(new CommonButtonData(CommonComponents.GUI_OK, CommonButtonData.DEFAULT_WIDTH), Optional.empty()))));
                        return 1;
                    })))
        );
    }

    public record DisplayCountResult(int attachments, int elements) implements Comparable<DisplayCountResult> {
        public static DisplayCountResult DEFAULT = new DisplayCountResult(0, 0);

        @Override
        public int compareTo(@NotNull DisplayCountResult displayCountResult) {
            if (this.elements == displayCountResult.elements) {
                return Integer.compare(this.attachments, displayCountResult.attachments);
            } else {
                return Integer.compare(this.elements, displayCountResult.elements);
            }
        }
    }
}
