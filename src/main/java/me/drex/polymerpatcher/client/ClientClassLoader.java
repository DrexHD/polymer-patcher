package me.drex.polymerpatcher.client;

import me.drex.polymerpatcher.PolymerPatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientClassLoader {
    public static void init() throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        // https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
        // https://piston-meta.mojang.com/v1/packages/78941de799d2675be5bddca699b245d7cbd567ae/26.1.2.json
        // https://piston-data.mojang.com/v1/objects/4e618f09a0c649dde3fdf829df443ce0b8831e65/client.jar
        Path cache = PolymerPatcher.POLYMER_PATCHER_PATH.resolve("client-cache");

        Files.createDirectories(cache);

        String sourceNamespace = "official";

        String officialJarFilename = String.format("%s-%s.jar", "client", sourceNamespace);
        Path inputFile = cache.resolve(officialJarFilename);

        if (!Files.exists(inputFile)) {
            PolymerPatcher.LOGGER.info("Downloading client jar...");
            URL url = new URL("https://piston-data.mojang.com/v1/objects/4e618f09a0c649dde3fdf829df443ce0b8831e65/client.jar");
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            Files.copy(is, inputFile);
        }

        FabricLauncher launcher = FabricLauncherBase.getLauncher();

        launcher.addToClassPath(inputFile);
    }
}
