package me.drex.polymerpatcher.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.drex.polymerpatcher.PolymerPatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientClassLoader {
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static void init() throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        String version = FabricLoader.getInstance().getRawGameVersion();
        Path cache = PolymerPatcher.POLYMER_PATCHER_PATH.resolve("client-cache");

        Files.createDirectories(cache);

        String officialJarFilename = "client-" + version + "-official.jar";
        Path inputFile = cache.resolve(officialJarFilename);

        if (!Files.exists(inputFile)) {
            PolymerPatcher.LOGGER.info("Downloading client jar for Minecraft {}...", version);
            String clientUrl = findClientUrl(version);
            Path temporaryFile = Files.createTempFile(cache, "client-", ".tmp");
            try {
                try (InputStream stream = openConnection(clientUrl).getInputStream()) {
                    Files.copy(stream, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(temporaryFile, inputFile);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }

        FabricLauncher launcher = FabricLauncherBase.getLauncher();

        launcher.addToClassPath(inputFile);
    }

    private static String findClientUrl(String version) throws IOException {
        try {
            for (var entry : readJson(VERSION_MANIFEST).getAsJsonArray("versions")) {
                JsonObject metadata = entry.getAsJsonObject();
                if (version.equals(metadata.get("id").getAsString())) {
                    return readJson(metadata.get("url").getAsString()).getAsJsonObject("downloads")
                        .getAsJsonObject("client").get("url").getAsString();
                }
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid Minecraft version metadata for " + version, e);
        }
        throw new IOException("No client download found for Minecraft " + version);
    }

    private static JsonObject readJson(String url) throws IOException {
        try (var reader = new InputStreamReader(openConnection(url).getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static URLConnection openConnection(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        return connection;
    }
}
