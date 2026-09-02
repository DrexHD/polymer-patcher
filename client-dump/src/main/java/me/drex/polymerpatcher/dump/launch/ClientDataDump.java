package me.drex.polymerpatcher.dump.launch;

import me.drex.polymerpatcher.dump.data.ClientDataStorage;
import me.drex.polymerpatcher.dump.generation.ClientDataGenerator;
import me.drex.polymerpatcher.dump.mixin.CreateWorldScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientDataDump {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDataDump.class);

    private static boolean creationRequested;
    private static boolean createPressed;

    public static void tick(Minecraft minecraft) {
        try {
            if (!creationRequested) {
                if (!(minecraft.screen instanceof TitleScreen)) {
                    return;
                }
                creationRequested = true;
                LOGGER.info("Preparing temporary render-data world");
                CreateWorldScreen.testWorld(minecraft, () -> {});
            } else if (!createPressed && minecraft.screen instanceof CreateWorldScreen screen) {
                createPressed = true;
                LOGGER.info("Creating temporary render-data world");
                ((CreateWorldScreenAccessor) screen).invokeOnCreate();
            } else {
                MinecraftServer server = minecraft.getSingleplayerServer();
                if (server == null) {
                    return;
                }
                LOGGER.info("Generating render data from temporary client world");
                ClientDataStorage.save(ClientDataGenerator.generate(server.overworld()));
                LOGGER.info("Render-data generation finished");
                System.exit(0);
            }
        } catch (Throwable throwable) {
            LOGGER.error("Automatic render-data generation failed", throwable);
            System.exit(1);
        }
    }

}
