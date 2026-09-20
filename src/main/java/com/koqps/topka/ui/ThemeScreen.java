package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ThemeScreen extends Screen {
    private static final int PANEL_W = 560;
    private static final int PANEL_H = 360;
    private static final int[] PRESETS = {
            0xFF8B5CF6, 0xFF41C7FF, 0xFF50FA7B, 0xFFFF5C77,
            0xFFFFD166, 0xFFFF7AD9, 0xFFFF8A3D, 0xFFFFFFFF
    };

    private final Screen parent;

    public ThemeScreen(Screen parent) {
        super(Component.literal("Mod Menu Theme"));
        this.parent = parent;
    }

    private int x() { return (width - PANEL_W) / 2; }
    private int y() { return (height - PANEL_H) / 2; }

    @Override
    protected void init() {
        int x = x(), y = y();
        for (int i = 0; i < PRESETS.length; i++) {
            final int color = PRESETS[i];
            int px = x + 42 + (i % 4) * 116;
            int py = y + 88 + (i / 4) * 42;
            addClickTarget(px, py, 102, 30, () -> {
                TopkaClient.CONFIG.get().accentArgb = color;
                TopkaClient.CONFIG.save();
            });
        }

        addClickTarget(x + 42, y + 200, 34, 24, () -> shiftChannel(16, -8));
        addClickTarget(x + 80, y + 200, 34, 24, () -> shiftChannel(16, 8));
        addClickTarget(x + 150, y + 200, 34, 24, () -> shiftChannel(8, -8));
        addClickTarget(x + 188, y + 200, 34, 24, () -> shiftChannel(8, 8));
        addClickTarget(x + 258, y + 200, 34, 24, () -> shiftChannel(0, -8));
        addClickTarget(x + 296, y + 200, 34, 24, () -> shiftChannel(0, 8));

        addClickTarget(x + 42, y + 258, 120, 28, () -> {
            var c = TopkaClient.CONFIG.get();
            int alpha = ((c.hudBackgroundArgb >>> 24) & 0xFF);
            alpha = Math.max(40, alpha - 16);
            c.hudBackgroundArgb = (alpha << 24) | (c.hudBackgroundArgb & 0x00FFFFFF);
            TopkaClient.CONFIG.save();
        });
        addClickTarget(x + 170, y + 258, 120, 28, () -> {
            var c = TopkaClient.CONFIG.get();
            int alpha = ((c.hudBackgroundArgb >>> 24) & 0xFF);
            alpha = Math.min(255, alpha + 16);
            c.hudBackgroundArgb = (alpha << 24) | (c.hudBackgroundArgb & 0x00FFFFFF);
            TopkaClient.CONFIG.save();
        });
        addClickTarget(x + 344, y + 258, 174, 28, this::resetTheme);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = x(), y = y();

        g.fill(0, 0, width, height, 0xAA000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFA0D0D13);
        g.fill(x, y, x + PANEL_W, y + 3, cfg.accentArgb);

        g.text(font, "THEME STUDIO", x + 26, y + 22, 0xFFFFFFFF, true);
        g.text(font, "Build a consistent accent across the menu and HUD.", x + 26, y + 40, 0xFF8E8E9F, false);

        g.text(font, "ACCENT PRESETS", x + 42, y + 70, 0xFF707082, false);
        for (int i = 0; i < PRESETS.length; i++) {
            int px = x + 42 + (i % 4) * 116;
            int py = y + 88 + (i / 4) * 42;
            int color = PRESETS[i];
            boolean selected = cfg.accentArgb == color;
            g.fill(px, py, px + 102, py + 30, selected ? 0xFF2F2F3D : 0xFF1B1B24);
            g.fill(px + 7, py + 7, px + 23, py + 23, color);
            g.text(font, selected ? "ACTIVE" : "SELECT", px + 33, py + 10, selected ? color : 0xFFB2B2C0, true);
        }

        int r = (cfg.accentArgb >>> 16) & 0xFF;
        int gr = (cfg.accentArgb >>> 8) & 0xFF;
        int b = cfg.accentArgb & 0xFF;
        g.text(font, "RGB FINE TUNING", x + 42, y + 179, 0xFF707082, false);
        drawAdjust(g, x + 42, y + 200, "R " + r, 0xFFFF6B7D);
        drawAdjust(g, x + 150, y + 200, "G " + gr, 0xFF5CE39A);
        drawAdjust(g, x + 258, y + 200, "B " + b, 0xFF5CB8FF);
        g.fill(x + 390, y + 190, x + 518, y + 230, cfg.accentArgb);
        g.text(font, hex(cfg.accentArgb), x + 416, y + 204, contrastText(cfg.accentArgb), true);

        int alpha = (cfg.hudBackgroundArgb >>> 24) & 0xFF;
        g.text(font, "HUD OPACITY  " + Math.round(alpha / 255F * 100F) + "%", x + 42, y + 242, 0xFF8E8E9F, false);
        drawButton(g, mouseX, mouseY, x + 42, y + 258, 120, "Less opaque");
        drawButton(g, mouseX, mouseY, x + 170, y + 258, 120, "More opaque");
        drawButton(g, mouseX, mouseY, x + 344, y + 258, 174, "Reset theme");

        g.text(font, "Changes save instantly", x + 42, y + 322, 0xFF686878, false);
        g.text(font, "ESC / " + TopkaClient.openMenuKey().getString() + " to return", x + 348, y + 322, 0xFF686878, false);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawAdjust(GuiGraphicsExtractor g, int x, int y, String label, int color) {
        g.text(font, label, x, y - 12, color, true);
        g.fill(x, y, x + 34, y + 24, 0xFF22222D);
        g.fill(x + 38, y, x + 72, y + 24, 0xFF22222D);
        g.centeredText(font, "−", x + 17, y + 8, 0xFFECECF2);
        g.centeredText(font, "+", x + 55, y + 8, 0xFFECECF2);
    }

    private void drawButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 28;
        g.fill(x, y, x + w, y + 28, hover ? 0xFF30303C : 0xFF21212B);
        g.fill(x, y + 27, x + w, y + 28, TopkaClient.CONFIG.get().accentArgb);
        g.centeredText(font, text, x + w / 2, y + 9, 0xFFEDEDF4);
    }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run()).pos(x, y).size(w, h).build());
    }

    private void shiftChannel(int shift, int amount) {
        var cfg = TopkaClient.CONFIG.get();
        int color = cfg.accentArgb;
        int value = (color >>> shift) & 0xFF;
        value = Math.clamp(value + amount, 0, 255);
        cfg.accentArgb = (color & ~(0xFF << shift)) | (value << shift);
        TopkaClient.CONFIG.save();
    }

    private void resetTheme() {
        var c = TopkaClient.CONFIG.get();
        c.accentArgb = 0xFF8B5CF6;
        c.secondaryAccentArgb = 0xFF41C7FF;
        c.panelArgb = 0xF20D0D13;
        c.sidebarArgb = 0xFF14141C;
        c.hudBackgroundArgb = 0xD9121219;
        c.textArgb = 0xFFF5F5FA;
        c.mutedTextArgb = 0xFF9292A4;
        TopkaClient.CONFIG.save();
    }

    private static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    private static int contrastText(int argb) {
        int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 145 ? 0xFF111116 : 0xFFFFFFFF;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (TopkaClient.menuKeyMatches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        TopkaClient.CONFIG.save();
        minecraft.gui.setScreen(parent);
    }
}
