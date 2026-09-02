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
        // https://piston-meta.mojang.com/v1/packages/30bb79802dcf36de95322ef6a055960c88131d2b/1.21.11.json
        // https://piston-data.mojang.com/v1/objects/ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd/client.jar
        Path cache = PolymerPatcher.POLYMER_PATCHER_PATH.resolve("client-cache");

        Files.createDirectories(cache);

        String sourceNamespace = "official";
        String targetNamespace = "intermediary";

        String officialJarFilename = String.format("%s-%s.jar", "client", sourceNamespace);
        Path inputFile = cache.resolve(officialJarFilename);

        if (!Files.exists(inputFile)) {
            PolymerPatcher.LOGGER.info("Downloading client jar...");
            URL url = new URL("https://piston-data.mojang.com/v1/objects/ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd/client.jar");
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            Files.copy(is, inputFile);
        }


        String deobfJarFilename = String.format("%s-%s.jar", "client", targetNamespace);
        Path outputFile = cache.resolve(deobfJarFilename);
        Path tmpFile = cache.resolve(deobfJarFilename + ".tmp");

        FabricLauncher launcher = FabricLauncherBase.getLauncher();
        MappingTree mappings = launcher.getMappingConfiguration().getMappings();

        if (!Files.exists(outputFile)) {
            PolymerPatcher.LOGGER.info("Deobfuscating client jar...");
            JarDeobfuscationHelper.deobfuscate0(List.of(inputFile), List.of(outputFile), List.of(tmpFile), mappings, sourceNamespace, targetNamespace);
        }

        launcher.addToClassPath(outputFile);
    }
}
