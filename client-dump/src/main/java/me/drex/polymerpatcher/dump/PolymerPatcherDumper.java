package me.drex.polymerpatcher.dump;

import me.drex.polymerpatcher.dump.launch.ClientDataDump;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.ColorResolver;
import nl.theepicblock.resourcelocatorapi.ResourceLocatorApi;
import nl.theepicblock.resourcelocatorapi.api.AssetContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PolymerPatcherDumper implements ClientModInitializer {
    public static final String MOD_ID = "polymer-patcher-client-dump";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final AssetContainer GLOBAL_ASSETS = ResourceLocatorApi.createGlobalAssetContainer();
    public static final ThreadLocal<ColorResolver> COLOR_RESOLVER = new ThreadLocal<>();
    public static final Path POLYMER_PATCHER_PATH = FabricLoader.getInstance().getGameDir().resolve(".polymer-patcher");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientDataDump::tick);
    }
}
