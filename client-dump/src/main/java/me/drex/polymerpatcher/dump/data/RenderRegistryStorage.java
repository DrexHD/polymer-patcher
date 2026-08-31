package me.drex.polymerpatcher.dump.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.drex.polymerpatcher.dump.data.adapter.ClassAdapter;
import me.drex.polymerpatcher.dump.data.adapter.CodecSerializer;
import me.drex.polymerpatcher.dump.data.adapter.ColorResolverAdapter;
import me.drex.polymerpatcher.dump.data.adapter.ModelLayerLocationAdapter;
import me.drex.polymerpatcher.dump.data.adapter.RegistrySerializer;
import me.drex.polymerpatcher.dump.data.adapter.Vector3fAdapter;
import me.drex.polymerpatcher.dump.data.codec.LayerDefinitionCodecs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3fc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RenderRegistryStorage {
    private static final String DUMP_FILE_NAME = "polymer-patcher-dump.json";

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(ModelLayerLocation.class, new ModelLayerLocationAdapter())
        .registerTypeAdapter(LayerDefinition.class, new CodecSerializer<>(LayerDefinitionCodecs.LAYER_DEFINITION))
        .registerTypeAdapter(Class.class, new ClassAdapter())
        .registerTypeAdapter(ColorResolver.class, new ColorResolverAdapter())
        .registerTypeAdapter(Vector3fc.class, new Vector3fAdapter())
        .registerTypeHierarchyAdapter(EntityType.class, new RegistrySerializer<>(BuiltInRegistries.ENTITY_TYPE))
        .registerTypeHierarchyAdapter(Block.class, new RegistrySerializer<>(BuiltInRegistries.BLOCK))
        .registerTypeHierarchyAdapter(Identifier.class, new CodecSerializer<>(Identifier.CODEC))
        .setPrettyPrinting()
        .create();

    private RenderRegistryStorage() {
    }

    public static RenderRegistry load() {
        try {
            return load(dumpFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + DUMP_FILE_NAME, e);
        }
    }

    public static RenderRegistry load(Path path) throws IOException {
        RenderRegistry registry = Files.exists(path)
            ? GSON.fromJson(Files.readString(path), RenderRegistry.class)
            : new RenderRegistry();
        registry.rebuildIndexes();
        return registry;
    }

    public static void save(RenderRegistry registry) throws IOException {
        save(registry, dumpFile());
    }

    public static void save(RenderRegistry registry, Path path) throws IOException {
        Files.writeString(path, GSON.toJson(registry, RenderRegistry.class));
    }

    private static Path dumpFile() {
        return FabricLoader.getInstance().getGameDir().resolve(DUMP_FILE_NAME);
    }
}
