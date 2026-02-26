package com.ybg.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class YBGConfig {

    // ── Visibility & position ─────────────────────────────────────────────────
    public boolean overlayVisible = true;

    /** 0.0 = left edge, 1.0 = right edge */
    public double overlayX = 0.01;

    /** 0.0 = top, 1.0 = bottom */
    public double overlayY = 0.01;

    // ── Scale (split into background panel size and text size) ────────────────
    /** Background panel scale. 0.5 = half, 1.0 = normal, 2.0 = double */
    public double bgScale   = 1.0;

    /** Text/font scale multiplier rendered inside the HUD. 0.5 – 2.0 */
    public double textScale = 1.0;

    /** Hue 0.0 – 1.0 (maps to 0–360 degrees, default green) */
    public double hudHue = 0.33;

    // ── Persistence ───────────────────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ybg-profit-tracker.json";

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** Load from disk, or return defaults if the file doesn't exist yet. */
    public static YBGConfig load() {
        Path path = configPath();
        if (path.toFile().exists()) {
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
                YBGConfig cfg = GSON.fromJson(reader, YBGConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                System.err.println("[YBG] Failed to load config, using defaults: " + e.getMessage());
            }
        }
        return new YBGConfig();
    }

    /** Save current values to disk. */
    public void save() {
        Path path = configPath();
        try {
            // Make sure the config directory exists
            path.getParent().toFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[YBG] Failed to save config: " + e.getMessage());
        }
    }
}
