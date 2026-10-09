package me.drex.polymerpatcher.dump.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.drex.polymerpatcher.dump.PolymerPatcherDumper;
import me.drex.polymerpatcher.dump.data.adapter.ClassAdapter;
import me.drex.polymerpatcher.dump.data.adapter.CodecSerializer;
import me.drex.polymerpatcher.dump.data.adapter.ColorResolverAdapter;
import me.drex.polymerpatcher.dump.data.adapter.ModelLayerLocationAdapter;
import me.drex.polymerpatcher.dump.data.adapter.RegistrySerializer;
import me.drex.polymerpatcher.dump.data.adapter.Vector3fAdapter;
import me.drex.polymerpatcher.dump.data.codec.LayerDefinitionCodecs;
import me.drex.polymerpatcher.dump.launch.HeadLessmcLauncher;
import me.drex.polymerpatcher.dump.launch.SystemProperties;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ClientDataStorage {

    public static final String DUMP_FILE_NAME = "client-dump.json";
    private static final Path DUMP_FILE = Optional.ofNullable(System.getProperty(SystemProperties.POLYMER_PATCHER_DUMP_FILE)).map(Path::of)
        .orElse(PolymerPatcherDumper.POLYMER_PATCHER_PATH.resolve(DUMP_FILE_NAME));

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(ModelLayerLocation.class, new ModelLayerLocationAdapter())
        .registerTypeAdapter(LayerDefinition.class, new CodecSerializer<>(LayerDefinitionCodecs.LAYER_DEFINITION))
        .registerTypeAdapter(Class.class, new ClassAdapter())
        .registerTypeAdapter(ColorResolver.class, new ColorResolverAdapter())
        .registerTypeAdapter(Vector3fc.class, new Vector3fAdapter())
        .registerTypeHierarchyAdapter(EntityType.class, new RegistrySerializer<>(BuiltInRegistries.ENTITY_TYPE))
        .registerTypeHierarchyAdapter(Block.class, new RegistrySerializer<>(BuiltInRegistries.BLOCK))
        .registerTypeHierarchyAdapter(Identifier.class, new CodecSerializer<>(Identifier.CODEC))
        .create();

    private ClientDataStorage() {
    }

    public static ClientDataRegistry load() {
        try {
            Files.createDirectories(PolymerPatcherDumper.POLYMER_PATCHER_PATH);
            PolymerPatcherDumper.LOGGER.info("Loading client data...");
            ClientDataRegistry registry = Files.exists(DUMP_FILE)
                ? GSON.fromJson(Files.readString(DUMP_FILE), ClientDataRegistry.class)
                : new ClientDataRegistry();

            // TODO config option to skip
            if (registry.requiresUpdate() && !FabricLoader.getInstance().isDevelopmentEnvironment()) {
                PolymerPatcherDumper.LOGGER.info("Client data requires an updating, launching HeadlessMC client...");
                HeadLessmcLauncher.launch();
                registry = Files.exists(DUMP_FILE)
                    ? GSON.fromJson(Files.readString(DUMP_FILE), ClientDataRegistry.class)
                    : new ClientDataRegistry();
            }

            registry.rebuildIndexes();
            return registry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + DUMP_FILE, e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for the HeadlessMC dump client", e);
        }
    }

    public static void save(ClientDataRegistry registry) throws IOException {
        Files.createDirectories(DUMP_FILE.getParent());
        Files.writeString(DUMP_FILE, GSON.toJson(registry, ClientDataRegistry.class));
    }

}
