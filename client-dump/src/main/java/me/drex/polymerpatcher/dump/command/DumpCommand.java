package me.drex.polymerpatcher.dump.command;

import com.mojang.brigadier.CommandDispatcher;
import me.drex.polymerpatcher.dump.PolymerPatcherDumper;
import me.drex.polymerpatcher.dump.data.RenderRegistryStorage;
import me.drex.polymerpatcher.dump.generation.RenderRegistryGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;

public class DumpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pp-dump")
                .executes(commandContext -> {
                    ServerLevel level = commandContext.getSource().getLevel();
                    PolymerPatcherDumper.LOGGER.info("Dumping render registry...");
                    try {
                        RenderRegistryStorage.save(RenderRegistryGenerator.generate(level));
                        PolymerPatcherDumper.LOGGER.info("Render registry dumped...");
                    } catch (IOException e) {
                        PolymerPatcherDumper.LOGGER.error("Failed to save render registry", e);
                        return 0;
                    }
                    return 1;
                })
        );
    }
}
