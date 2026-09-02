package me.drex.polymerpatcher.dump.launch;

import me.drex.polymerpatcher.dump.PolymerPatcherDumper;
import me.drex.polymerpatcher.dump.data.ClientDataStorage;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class HeadLessmcLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadLessmcLauncher.class);
    private static final String HEADLESS_MC_VERSION = "2.10.0";
    private static final URI HEADLESS_MC_URI = URI.create(
        "https://github.com/headlesshq/headlessmc/releases/download/" + HEADLESS_MC_VERSION
            + "/headlessmc-launcher-" + HEADLESS_MC_VERSION + ".jar"
    );
    private static final String HEADLESS_MC_SHA256 = "52bd5006f478377b3893011d458562977d38c65ead6d2b31089beb4d614f13cd";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration LAUNCH_TIMEOUT = Duration.ofMinutes(15);

    private HeadLessmcLauncher() {
    }

    public static void launch() throws IOException, InterruptedException {
        Path directory = PolymerPatcherDumper.POLYMER_PATCHER_PATH.resolve("headlessmc").toAbsolutePath();
        Path minecraftDirectory = directory.resolve(".minecraft");
        Path launcher = ensureLauncher(directory);
        Files.createDirectories(minecraftDirectory);
        Files.writeString(minecraftDirectory.resolve("options.txt"), "onboardAccessibility:false\npauseOnLostFocus:false\n");

        String gameVersion = FabricLoader.getInstance().getRawGameVersion();
        String loaderVersion = FabricLoader.getInstance().getModContainer("fabricloader")
            .orElseThrow(() -> new IOException("Fabric Loader is not present"))
            .getMetadata().getVersion().getFriendlyString();
        String profile = "fabric-loader-" + loaderVersion + "-" + gameVersion;

        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-Dhmc.assets.dummy=true");
        command.add("-Dhmc.mcdir=" + minecraftDirectory);
        command.add("-Dhmc.gamedir=" + minecraftDirectory);
        command.add("-Dhmc.exit.on.failed.command=true");
        command.add("-jar");
        command.add(launcher.toString());

        LOGGER.info("Launching headlessmc client");
        Process process = new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start();

        Map<String, String> properties = Map.of(
            "fabric.modsFolder", FabricLoader.getInstance().getGameDir().resolve("mods").toAbsolutePath().toString(),
            SystemProperties.POLYMER_PATCHER_DUMP_FILE, PolymerPatcherDumper.POLYMER_PATCHER_PATH.resolve(ClientDataStorage.DUMP_FILE_NAME).toAbsolutePath().toString()
        );
        String jvmArguments = String.join(" ", properties.entrySet().stream()
            .map(entry -> "-D" + entry.getKey() + "=" + entry.getValue())
            .toList());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            if (!Files.isRegularFile(minecraftDirectory.resolve("versions").resolve(profile).resolve(profile + ".json"))) {
                writer.write("fabric " + gameVersion + " --uid " + loaderVersion);
                writer.newLine();
            }
            String launch = "launch " + profile + " -offline --jvm \"" + StringEscapeUtils.escapeJava(jvmArguments) + "\"";
            LOGGER.info("Writing launch command to headlessmc client \"{}\"", launch);
            writer.write(launch);
            writer.newLine();
            writer.flush();
        }

        long deadline = System.nanoTime() + LAUNCH_TIMEOUT.toNanos();
        while (!process.waitFor(5, TimeUnit.SECONDS)) {
            if (System.nanoTime() >= deadline) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException("Timed out waiting for the headlessmc client");
            }
        }
        if (process.exitValue() != 0) {
            throw new IOException("headlessmc exited with code " + process.exitValue());
        }
    }

    private static Path ensureLauncher(Path directory) throws IOException, InterruptedException {
        Files.createDirectories(directory);
        Path launcher = directory.resolve("headlessmc-launcher-" + HEADLESS_MC_VERSION + ".jar");
        if (hasExpectedHash(launcher)) {
            return launcher;
        }

        Path download = launcher.resolveSibling(launcher.getFileName() + ".download");
        Files.deleteIfExists(download);
        LOGGER.info("Downloading headlessmc {}", HEADLESS_MC_VERSION);
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        HttpRequest request = HttpRequest.newBuilder(HEADLESS_MC_URI).timeout(DOWNLOAD_TIMEOUT).GET().build();
        HttpResponse<Path> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(download));
        } catch (IOException | InterruptedException e) {
            Files.deleteIfExists(download);
            throw e;
        }
        if (response.statusCode() / 100 != 2) {
            Files.deleteIfExists(download);
            throw new IOException("headlessmc download failed with HTTP " + response.statusCode());
        }
        if (!hasExpectedHash(download)) {
            Files.deleteIfExists(download);
            throw new IOException("Downloaded headlessmc jar failed SHA-256 verification");
        }
        Files.move(download, launcher, StandardCopyOption.REPLACE_EXISTING);
        return launcher;
    }

    private static boolean hasExpectedHash(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HEADLESS_MC_SHA256.equals(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the Java runtime", e);
        }
    }

    private static Path javaExecutable() throws IOException {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        Path path = Path.of(System.getProperty("java.home"), "bin", executable).toAbsolutePath();
        if (!Files.isRegularFile(path)) {
            throw new IOException("Unable to find the current Java executable at " + path);
        }
        return path;
    }

}
