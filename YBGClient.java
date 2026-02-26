package com.ybg.mod;

import com.ybg.mod.config.YBGConfig;
import com.ybg.mod.events.ChatTracker;
import com.ybg.mod.events.InventoryTracker;
import com.ybg.mod.gui.YBGHud;
import com.ybg.mod.gui.YBGScreen;
import com.ybg.mod.tracker.OreTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

public class YBGClient implements ClientModInitializer {

    public static YBGConfig config;
    public static OreTracker oreTracker;

    @Override
    public void onInitializeClient() {
        // Load saved config (falls back to defaults if no file exists)
        config = YBGConfig.load();
        oreTracker = new OreTracker();

        // Register inventory tick scanner
        InventoryTracker.register();

        // Register chat listener for Compact enchant messages
        ChatTracker.register();

        // Register HUD renderer
        YBGHud.register();

        // Register /ybg command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("ybg")
                    .executes(ctx -> {
                        MinecraftClient.getInstance().send(() ->
                            MinecraftClient.getInstance().setScreen(new YBGScreen())
                        );
                        return 1;
                    })
            )
        );
    }
}
