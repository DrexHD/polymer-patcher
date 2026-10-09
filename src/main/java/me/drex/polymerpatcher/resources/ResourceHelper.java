package me.drex.polymerpatcher.resources;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import me.drex.polymerpatcher.PolymerPatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import nl.theepicblock.resourcelocatorapi.ResourceLocatorApi;
import nl.theepicblock.resourcelocatorapi.api.AssetContainer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class ResourceHelper {
    public static final Identifier ACACIA_BOAT_TEXTURE = Identifier.withDefaultNamespace("entity/boat/acacia");
    public static final Identifier ORIGINAL_ACACIA_BOAT_TEXTURE = PolymerPatcher.id("entity/boat/acacia_original");
    public static final AssetContainer GLOBAL_ASSETS = ResourceLocatorApi.createGlobalAssetContainer();
    private static final FileSystem vanillaFilesystem;

    static {
        try {
            vanillaFilesystem = FileSystems.newFileSystem(PolymerCommonUtils.getClientJar());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init(ResourcePackBuilder packBuilder) {
        PolymerPatcher.LOGGER.info("Loading global assets");
        GLOBAL_ASSETS.locateFiles("").forEach(tuple -> {
            Identifier id = tuple.getA();
            IoSupplier<InputStream> ioSupplier = tuple.getB();

            try {
                byte[] data = IOUtils.toByteArray(ioSupplier.get());
                packBuilder.addData("assets/" + id.getNamespace() + "/" + id.getPath(), data);
            } catch (IOException e) {
                PolymerPatcher.LOGGER.error("Failed to read resource {}: {}", id, e);
            }
        });
    }

    public static IoSupplier<InputStream> getAsset(String namespace, String path) {
        IoSupplier<InputStream> supplier = ResourceHelper.GLOBAL_ASSETS.getAsset(namespace, path);
        if (supplier != null) {
            return supplier;
        }
        return getVanillaAsset(namespace, path);
    }

    public static IoSupplier<InputStream> getVanillaAsset(String namespace, String path) {
        var vanillaPath = vanillaFilesystem.getPath("/assets/" + namespace + "/" + path);
        if (Files.exists(vanillaPath)) {
            return IoSupplier.create(vanillaPath);
        } else {
            return null;
        }
    }

    public static <T> T decodeAsset(Codec<T> codec, Identifier id, String type, String extension) throws IOException {
        IoSupplier<InputStream> supplier = getAsset(id.getNamespace(), type + "/" + id.getPath() + extension);
        return codec.decode(JsonOps.INSTANCE, JsonParser.parseReader(new JsonReader(new InputStreamReader(supplier.get())))).getOrThrow().getFirst();
    }

    public static BlockStateAsset decodeBlockState(Identifier id) throws IOException {
        return decodeAsset(BlockStateAsset.CODEC, id, "blockstates", ".json");
    }

    public static ModelAsset decodeModel(Identifier id) throws IOException {
        return decodeAsset(ModelAsset.CODEC, id, "models", ".json");
    }
}
