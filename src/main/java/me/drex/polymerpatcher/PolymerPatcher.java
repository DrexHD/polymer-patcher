package me.drex.polymerpatcher;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.MapColorTintSource;
import me.drex.polymerpatcher.block.color.ColorMapHelper;
import me.drex.polymerpatcher.command.PolymerPatcherCommand;
import me.drex.polymerpatcher.config.ConfigManager;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry;
import me.drex.polymerpatcher.dump.data.ClientDataStorage;
import me.drex.polymerpatcher.entity.AnimatedEntities;
import me.drex.polymerpatcher.resources.ResourceHelper;
import me.drex.polymerpatcher.resources.ResourcePackGenerator;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolymerPatcher implements DedicatedServerModInitializer {
    public static final String MOD_ID = "polymer-patcher";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path POLYMER_PATCHER_PATH = FabricLoader.getInstance().getGameDir().resolve(".polymer-patcher");

    public static final Set<String> PATCHED_MODS = new HashSet<>();

    private static ClientDataRegistry clientDataRegistry = new ClientDataRegistry();

    public static ClientDataRegistry getRenderRegistry() {
        return clientDataRegistry;
    }

    @Override
    public void onInitializeServer() {
        ConfigManager.load();
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(ResourceHelper::init);
        ResourcePackGenerator.setup();
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        ColorMapHelper.init();

        clientDataRegistry = ClientDataStorage.load();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            AnimatedEntities.registerEntities(clientDataRegistry);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            PolymerPatcherCommand.register(dispatcher)
        );
    }

    public static void setupModAssets(String modId) {
        ResourcePackExtras.forDefault().addBridgedModelsFolder(
            Identifier.fromNamespaceAndPath(modId, "block_sign")
        );
        ResourcePackExtras.forDefault().addBridgedModelsFolder(
            Identifier.fromNamespaceAndPath(modId, "block"),
            (id, resourcePackBuilder) -> {
                return new ItemAsset(new BasicItemModel(id, List.of(new MapColorTintSource(0xFFFFFF))), ItemAsset.Properties.DEFAULT);
            }
        );
        ResourcePackExtras.forDefault().addBridgedModelsFolder(Identifier.fromNamespaceAndPath(modId, "entity"), (id, b) -> {
            return new ItemAsset(new BasicItemModel(id, List.of(new MapColorTintSource(0xFFFFFF))), new ItemAsset.Properties(true, true));
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
