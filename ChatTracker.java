package com.ybg.mod.events;

import com.ybg.mod.YBGClient;
import com.ybg.mod.tracker.OreTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to game chat messages for the Compact enchantment output.
 *
 * Compact messages appear as game (system) messages, e.g.:
 *   "COMPACT! You received +1 Enchanted Diamond."
 *   "+2 Enchanted Coal"
 *
 * We register for both CHAT and GAME events to catch all variants.
 * We NEVER cancel or modify messages.
 */
public class ChatTracker {

    // Matches "+N Enchanted <Ore>" anywhere in a message
    private static final Pattern COMPACT_PATTERN = Pattern.compile(
        "\\+(\\d+)\\s+(Enchanted\\s+(?:Coal|Iron(?:\\s+Ingot)?|Gold(?:\\s+Ingot)?|Lapis(?:\\s+Lazuli)?|Redstone|Emerald|Diamond))",
        Pattern.CASE_INSENSITIVE
    );

    public static void register() {
        // Game/system messages (what Compact uses)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleMessage(message);
        });

        // Also catch chat messages just in case
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            handleMessage(message)
        );
    }

    private static void handleMessage(Text message) {
        // Strip formatting codes from the plain text
        String raw = stripFormatting(message.getString());

        Matcher matcher = COMPACT_PATTERN.matcher(raw);
        while (matcher.find()) {
            int count;
            try {
                count = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                count = 1;
            }

            // Normalize the ore name to title case
            String enchantedName = toTitleCase(matcher.group(2).trim());

            YBGClient.oreTracker.onMiningActivity();
            YBGClient.oreTracker.addEnchantedOreFromChat(enchantedName, count);
        }
    }

    private static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private static String toTitleCase(String input) {
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
