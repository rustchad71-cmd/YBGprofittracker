package com.ybg.mod.tracker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

public class OreTracker {

    public static final Map<String, Integer> NPC_PRICES = new LinkedHashMap<>();
    public static final Map<String, Integer> ENCHANTED_NPC_PRICES = new LinkedHashMap<>();
    public static final Map<String, String> DISPLAY_TO_KEY = new LinkedHashMap<>();
    public static final Map<String, String> ENCHANTED_DISPLAY_TO_KEY = new LinkedHashMap<>();

    static {
        NPC_PRICES.put("COAL",     1);
        NPC_PRICES.put("IRON",     2);
        NPC_PRICES.put("GOLD",     3);
        NPC_PRICES.put("LAPIS",    1);
        NPC_PRICES.put("REDSTONE", 1);
        NPC_PRICES.put("EMERALD",  4);
        NPC_PRICES.put("DIAMOND",  8);

        ENCHANTED_NPC_PRICES.put("ENCHANTED_COAL",     160);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_IRON",     320);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_GOLD",     480);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_LAPIS",    160);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_REDSTONE", 160);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_EMERALD",  640);
        ENCHANTED_NPC_PRICES.put("ENCHANTED_DIAMOND", 1280);

        DISPLAY_TO_KEY.put("Coal",         "COAL");
        DISPLAY_TO_KEY.put("Iron Ingot",   "IRON");
        DISPLAY_TO_KEY.put("Raw Iron",     "IRON");
        DISPLAY_TO_KEY.put("Gold Ingot",   "GOLD");
        DISPLAY_TO_KEY.put("Raw Gold",     "GOLD");
        DISPLAY_TO_KEY.put("Lapis Lazuli", "LAPIS");
        DISPLAY_TO_KEY.put("Redstone",     "REDSTONE");
        DISPLAY_TO_KEY.put("Emerald",      "EMERALD");
        DISPLAY_TO_KEY.put("Diamond",      "DIAMOND");

        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Coal",         "ENCHANTED_COAL");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Iron Ingot",   "ENCHANTED_IRON");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Iron",         "ENCHANTED_IRON");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Gold Ingot",   "ENCHANTED_GOLD");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Gold",         "ENCHANTED_GOLD");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Lapis Lazuli", "ENCHANTED_LAPIS");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Lapis",        "ENCHANTED_LAPIS");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Redstone",     "ENCHANTED_REDSTONE");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Emerald",      "ENCHANTED_EMERALD");
        ENCHANTED_DISPLAY_TO_KEY.put("Enchanted Diamond",      "ENCHANTED_DIAMOND");
    }

    private static final long WARMUP_DURATION_MS = 10_000;
    private static final long IDLE_RESET_MS       =  5_000;
    private static final long IDLE_END_SESSION_MS = 30_000;

    private boolean sessionActive = false;
    private long sessionStartMs   = 0;
    private long lastMiningEventMs = 0;
    private long warmupStartMs    = 0;
    private boolean warmingUp     = false;

    // Set to true by checkIdleTimeout(), read and cleared by InventoryTracker
    private boolean sessionJustEnded = false;
    private String endSummary = "";

    private long totalProfit = 0;
    private long totalItems  = 0;

    private final Map<String, Long> oreCounts       = new LinkedHashMap<>();
    private final Map<String, Long> enchantedCounts = new LinkedHashMap<>();

    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(Locale.US);

    public synchronized void onMiningActivity() {
        long now = System.currentTimeMillis();

        if (sessionActive) {
            lastMiningEventMs = now;
            return;
        }

        if (warmingUp && (now - lastMiningEventMs) > IDLE_RESET_MS) {
            warmingUp = false;
        }

        if (!warmingUp) {
            warmingUp     = true;
            warmupStartMs = now;
        }

        lastMiningEventMs = now;

        if ((now - warmupStartMs) >= WARMUP_DURATION_MS) {
            startSession();
        }
    }

    /**
     * Called every tick. If session is active and player has been idle 30s, end it.
     */
    public synchronized void checkIdleTimeout() {
        if (!sessionActive) return;
        long now = System.currentTimeMillis();
        if ((now - lastMiningEventMs) >= IDLE_END_SESSION_MS) {
            endSession();
        }
    }

    private void startSession() {
        sessionActive    = true;
        sessionStartMs   = System.currentTimeMillis();
        lastMiningEventMs = System.currentTimeMillis();
        warmingUp        = false;
        totalProfit      = 0;
        totalItems       = 0;
        oreCounts.clear();
        enchantedCounts.clear();
    }

    private void endSession() {
        sessionJustEnded = true;
        endSummary       = buildSummary();
        sessionActive    = false;
        warmingUp        = false;
        lastMiningEventMs = 0;
        warmupStartMs    = 0;
    }

    public synchronized void resetSession() {
        sessionActive    = false;
        warmingUp        = false;
        lastMiningEventMs = 0;
        warmupStartMs    = 0;
        sessionJustEnded = false;
        endSummary       = "";
        totalProfit      = 0;
        totalItems       = 0;
        oreCounts.clear();
        enchantedCounts.clear();
    }

    public synchronized void addOre(String displayName, int count) {
        if (!sessionActive) return;
        String key = DISPLAY_TO_KEY.get(displayName);
        if (key == null) return;
        Integer price = NPC_PRICES.get(key);
        if (price == null) return;
        oreCounts.merge(key, (long) count, Long::sum);
        totalItems  += count;
        totalProfit += (long) price * count;
    }

    public synchronized void addEnchantedOreFromChat(String enchantedDisplayName, int count) {
        if (!sessionActive) return;
        String key = ENCHANTED_DISPLAY_TO_KEY.get(enchantedDisplayName);
        if (key == null) return;
        Integer price = ENCHANTED_NPC_PRICES.get(key);
        if (price == null) return;
        enchantedCounts.merge(key, (long) count, Long::sum);
        totalItems  += count;
        totalProfit += (long) price * count;
    }

    private String buildSummary() {
        return String.format(
                "§a[YBG] §7Session ended! §fProfit: §a%s §7| §fProfit/hr: §6%s §7| §fItems: §f%s §7| §fTime: §f%s",
                formatCoins(totalProfit),
                formatCoins(getProfitPerHour()),
                NUM_FMT.format(totalItems),
                getFormattedTime()
        );
    }

    /** Returns the end summary message and clears the flag. Call once after checkIdleTimeout fires. */
    public synchronized String pollEndSummary() {
        if (!sessionJustEnded) return null;
        sessionJustEnded = false;
        return endSummary;
    }

    public boolean isSessionActive() { return sessionActive; }
    public boolean isWarmingUp()     { return warmingUp && !sessionActive; }

    public double getWarmupProgress() {
        if (!warmingUp || sessionActive) return 0;
        return Math.min(1.0, (double)(System.currentTimeMillis() - warmupStartMs) / WARMUP_DURATION_MS);
    }

    public long getTotalProfit()  { return totalProfit; }
    public long getTotalItems()   { return totalItems; }

    public long getSessionDurationMs() {
        if (!sessionActive) return 0;
        return System.currentTimeMillis() - sessionStartMs;
    }

    public long getProfitPerHour() {
        long ms = getSessionDurationMs();
        if (ms < 5_000) return 0;
        return (long)(totalProfit / (ms / 3_600_000.0));
    }

    public String getFormattedTime() {
        long secs  = getSessionDurationMs() / 1000;
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%dm %02ds", m, s);
    }

    public static String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) return String.format("%.2fB", coins / 1_000_000_000.0);
        if (coins >= 1_000_000L)     return String.format("%.2fM", coins / 1_000_000.0);
        if (coins >= 1_000L)         return String.format("%.1fK", coins / 1_000.0);
        return NUM_FMT.format(coins);
    }

    public Map<String, Long> getOreCounts()       { return oreCounts; }
    public Map<String, Long> getEnchantedCounts() { return enchantedCounts; }
}