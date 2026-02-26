package com.ybg.mod.gui;

import com.ybg.mod.YBGClient;
import com.ybg.mod.tracker.OreTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class YBGHud {

    // Base (unscaled) panel dimensions
    static final int PANEL_W = 186;
    static final int PANEL_H = 92;
    private static final int PAD    = 7;
    private static final int LINE_H = 11;

    private static final int BG_COLOR    = 0xCC080808;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int VALUE_COLOR = 0xFFFFFFFF;

    public static void register() {
        HudRenderCallback.EVENT.register(YBGHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!YBGClient.config.overlayVisible) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof YBGScreen)) return;

        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        int x = (int)(YBGClient.config.overlayX * (screenW - scaledW()));
        int y = (int)(YBGClient.config.overlayY * (screenH - scaledH()));

        drawPanel(ctx, mc, x, y);
    }

    // Panel pixel size is driven by bgScale
    static int scaledW() { return (int)(PANEL_W * YBGClient.config.bgScale); }
    static int scaledH() { return (int)(PANEL_H * YBGClient.config.bgScale); }

    static void drawPanel(DrawContext ctx, MinecraftClient mc, int x, int y) {
        OreTracker tracker  = YBGClient.oreTracker;
        double bgScale      = YBGClient.config.bgScale;
        double textScale    = YBGClient.config.textScale;

        int sw    = (int)(PANEL_W * bgScale);
        int sh    = (int)(PANEL_H * bgScale);
        int pad   = (int)(PAD    * bgScale);
        // Line height follows textScale so text spacing grows/shrinks independently
        int lineH = (int)(LINE_H * textScale) + 2;
        int accentColor = hueToRgb(YBGClient.config.hudHue);

        // Background
        ctx.fill(x, y, x + sw, y + sh, BG_COLOR);

        // Border
        ctx.fill(x,          y,          x + sw, y + 1,          accentColor);
        ctx.fill(x,          y + sh - 1, x + sw, y + sh,         accentColor);
        ctx.fill(x,          y,          x + 1,  y + sh,         accentColor);
        ctx.fill(x + sw - 1, y,          x + sw, y + sh,         accentColor);

        int tx = x + pad;
        int ty = y + pad;

        ctx.drawText(mc.textRenderer, "✦ YBG Profit Tracker", tx, ty, accentColor, true);

        if (!tracker.isSessionActive()) {
            if (tracker.isWarmingUp()) {
                int pct   = (int)(tracker.getWarmupProgress() * 100);
                int barW  = sw - pad * 2;
                int fillW = (int)(barW * tracker.getWarmupProgress());
                int barH  = Math.max(2, (int)(4 * bgScale));
                ctx.drawText(mc.textRenderer, "Warming up... (" + pct + "%)", tx, ty + lineH, 0xFFFFAA00, true);
                int barY = ty + lineH * 2;
                ctx.fill(tx, barY, tx + barW,  barY + barH, 0xFF333333);
                ctx.fill(tx, barY, tx + fillW, barY + barH, 0xFFFFAA00);
            } else {
                ctx.drawText(mc.textRenderer, "Mine to start a session...", tx, ty + lineH, 0xFF777777, true);
            }
            return;
        }

        drawStat(ctx, mc, tx, ty + lineH,     "Total Profit:", OreTracker.formatCoins(tracker.getTotalProfit()),   0xFF55FF55);
        drawStat(ctx, mc, tx, ty + lineH * 2, "Profit/Hour:",  OreTracker.formatCoins(tracker.getProfitPerHour()), 0xFFFFAA00);
        drawStat(ctx, mc, tx, ty + lineH * 3, "Total Items:",  String.valueOf(tracker.getTotalItems()),             VALUE_COLOR);
        drawStat(ctx, mc, tx, ty + lineH * 4, "Session Time:", tracker.getFormattedTime(),                         VALUE_COLOR);
    }

    private static void drawStat(DrawContext ctx, MinecraftClient mc,
                                 int x, int y, String label, String value, int valueColor) {
        ctx.drawText(mc.textRenderer, label, x, y, LABEL_COLOR, true);
        int lw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, value, x + lw + 4, y, valueColor, true);
    }

    public static int hueToRgb(double hue) {
        float h = (float)(hue * 6.0);
        float f = h - (float) Math.floor(h);
        int t = (int)(255 * f);
        int q = (int)(255 * (1 - f));
        int r, g, b;
        switch ((int) h % 6) {
            case 0  -> { r = 255; g = t;   b = 0; }
            case 1  -> { r = q;   g = 255; b = 0; }
            case 2  -> { r = 0;   g = 255; b = t; }
            case 3  -> { r = 0;   g = q;   b = 255; }
            case 4  -> { r = t;   g = 0;   b = 255; }
            default -> { r = 255; g = 0;   b = q; }
        }
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
