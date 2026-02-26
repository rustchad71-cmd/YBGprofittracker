package com.ybg.mod.gui;

import com.ybg.mod.YBGClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class YBGScreen extends Screen {

    private static final int PANEL_W = 290;
    private static final int PANEL_H = 290;  // taller to fit the extra slider

    private ButtonWidget toggleBtn;

    public YBGScreen() {
        super(Text.of("YBG Profit Tracker"));
    }

    @Override
    protected void init() {
        int px = (width  - PANEL_W) / 2;
        int py = (height - PANEL_H) / 2;

        // ── Toggle overlay ─────────────────────────────────────────────────
        toggleBtn = ButtonWidget.builder(toggleText(), btn -> {
            YBGClient.config.overlayVisible = !YBGClient.config.overlayVisible;
            btn.setMessage(toggleText());
            YBGClient.config.save();
        }).dimensions(px + 10, py + 45, PANEL_W - 20, 20).build();
        addDrawableChild(toggleBtn);

        // ── X Position slider ──────────────────────────────────────────────
        addDrawableChild(new LabeledSlider(px + 10, py + 80, PANEL_W - 20, 20,
                "X Position", YBGClient.config.overlayX, 0.0, 1.0) {
            @Override protected void applyValue() {
                YBGClient.config.overlayX = value;
                YBGClient.config.save();
            }
        });

        // ── Y Position slider ──────────────────────────────────────────────
        addDrawableChild(new LabeledSlider(px + 10, py + 115, PANEL_W - 20, 20,
                "Y Position", YBGClient.config.overlayY, 0.0, 1.0) {
            @Override protected void applyValue() {
                YBGClient.config.overlayY = value;
                YBGClient.config.save();
            }
        });

        // ── Background Scale slider (0.5 – 2.0) ───────────────────────────
        double bgNorm = (YBGClient.config.bgScale - 0.5) / 1.5;
        addDrawableChild(new LabeledSlider(px + 10, py + 150, PANEL_W - 20, 20,
                "BG Size", bgNorm, 0.0, 1.0) {
            @Override protected void updateMessage() {
                double scale = 0.5 + value * 1.5;
                setMessage(Text.of(String.format("BG Size: %.1fx", scale)));
            }
            @Override protected void applyValue() {
                YBGClient.config.bgScale = 0.5 + value * 1.5;
                YBGClient.config.save();
            }
        });

        // ── Text Scale slider (0.5 – 2.0) ─────────────────────────────────
        double textNorm = (YBGClient.config.textScale - 0.5) / 1.5;
        addDrawableChild(new LabeledSlider(px + 10, py + 185, PANEL_W - 20, 20,
                "Text Size", textNorm, 0.0, 1.0) {
            @Override protected void updateMessage() {
                double scale = 0.5 + value * 1.5;
                setMessage(Text.of(String.format("Text Size: %.1fx", scale)));
            }
            @Override protected void applyValue() {
                YBGClient.config.textScale = 0.5 + value * 1.5;
                YBGClient.config.save();
            }
        });

        // ── Color / Hue slider ─────────────────────────────────────────────
        addDrawableChild(new LabeledSlider(px + 10, py + 220, PANEL_W - 20, 20,
                "HUD Color", YBGClient.config.hudHue, 0.0, 1.0) {
            @Override protected void updateMessage() {
                int color = YBGHud.hueToRgb(value);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8)  & 0xFF;
                int b =  color        & 0xFF;
                setMessage(Text.literal(String.format("HUD Color: §%s■§r (drag to change)",
                        String.format("#%02X%02X%02X", r, g, b))));
            }
            @Override protected void applyValue() {
                YBGClient.config.hudHue = value;
                YBGClient.config.save();
            }
        });

        // ── Reset session ──────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.of("§eReset Session"), btn ->
                YBGClient.oreTracker.resetSession()
        ).dimensions(px + 10, py + 258, (PANEL_W - 25) / 2, 20).build());

        // ── Done ───────────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.of("Done"), btn -> close())
                .dimensions(px + 15 + (PANEL_W - 25) / 2, py + 258, (PANEL_W - 25) / 2, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = (width  - PANEL_W) / 2;
        int py = (height - PANEL_H) / 2;

        int accent = YBGHud.hueToRgb(YBGClient.config.hudHue);

        // Dimmed background
        ctx.fill(0, 0, width, height, 0xAA000000);
        // Panel
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xCC080808);
        // Border
        ctx.fill(px,               py,               px + PANEL_W, py + 1,              accent);
        ctx.fill(px,               py + PANEL_H - 1, px + PANEL_W, py + PANEL_H,        accent);
        ctx.fill(px,               py,               px + 1,       py + PANEL_H,         accent);
        ctx.fill(px + PANEL_W - 1, py,               px + PANEL_W, py + PANEL_H,         accent);

        ctx.drawCenteredTextWithShadow(textRenderer, "§a✦ YBG Profit Tracker", width / 2, py + 10, 0xFFFFFFFF);

        var tracker = YBGClient.oreTracker;
        String status = tracker.isSessionActive()
                ? "§aActive §7| §f" + tracker.getFormattedTime()
                : tracker.isWarmingUp()
                ? String.format("§eWarming up... §7(%.0f%%)", tracker.getWarmupProgress() * 100)
                : "§7No active session";
        ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, py + 26, 0xFFFFFFFF);

        // Slider labels
        ctx.drawTextWithShadow(textRenderer, "§7X Position",  px + 10, py + 69,  0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "§7Y Position",  px + 10, py + 104, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "§7BG Scale",    px + 10, py + 139, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "§7Text Scale",  px + 10, py + 174, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "§7Color",       px + 10, py + 209, 0xFFAAAAAA);

        // Rainbow color preview bar above color slider
        int barX = px + 10;
        int barY = py + 206;
        for (int i = 0; i < PANEL_W - 20; i++) {
            double hue = (double) i / (PANEL_W - 20);
            ctx.fill(barX + i, barY, barX + i + 1, barY + 4, YBGHud.hueToRgb(hue));
        }

        // Live HUD preview (uses current bgScale for sizing)
        int previewX = (int)(YBGClient.config.overlayX * (width  - YBGHud.scaledW()));
        int previewY = (int)(YBGClient.config.overlayY * (height - YBGHud.scaledH()));
        YBGHud.drawPanel(ctx, client, previewX, previewY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    private Text toggleText() {
        return YBGClient.config.overlayVisible
                ? Text.of("§aOverlay: ON")
                : Text.of("§cOverlay: OFF");
    }

    // ── Generic labeled slider base ────────────────────────────────────────────
    private static abstract class LabeledSlider extends SliderWidget {
        private final String baseLabel;
        private final double min, max;

        LabeledSlider(int x, int y, int width, int height,
                      String label, double value, double min, double max) {
            super(x, y, width, height, Text.of(label), (value - min) / (max - min));
            this.baseLabel = label;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double real = min + value * (max - min);
            setMessage(Text.of(String.format("%s: %.0f%%", baseLabel, real * 100)));
        }

        @Override
        protected abstract void applyValue();
    }
}
