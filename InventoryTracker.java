package com.ybg.mod.events;

import com.ybg.mod.YBGClient;
import com.ybg.mod.tracker.OreTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class InventoryTracker {

    private static final Map<String, Integer> previousCounts = new HashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Check if idle session should end
            YBGClient.oreTracker.checkIdleTimeout();

            // Poll for session-end summary and print to chat
            String summary = YBGClient.oreTracker.pollEndSummary();
            if (summary != null) {
                client.player.sendMessage(Text.literal(summary), false);
            }

            scan(client);
        });
    }

    private static void scan(MinecraftClient client) {
        PlayerInventory inv = client.player.getInventory();
        Map<String, Integer> currentCounts = new HashMap<>();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            String displayName = stripFormatting(stack.getName().getString());
            if (OreTracker.DISPLAY_TO_KEY.containsKey(displayName)) {
                currentCounts.merge(displayName, stack.getCount(), Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            String name    = entry.getKey();
            int current    = entry.getValue();
            int previous   = previousCounts.getOrDefault(name, 0);
            if (current > previous) {
                int gained = current - previous;
                YBGClient.oreTracker.onMiningActivity();
                YBGClient.oreTracker.addOre(name, gained);
            }
        }

        previousCounts.clear();
        previousCounts.putAll(currentCounts);
    }

    private static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").trim();
    }
}