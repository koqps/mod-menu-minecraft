package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ThemeScreen extends Screen {
    private static final int PANEL_W = 590;
    private static final int PANEL_H = 430;
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
            int px = x + 42 + (i % 4) * 124;
            int py = y + 86 + (i / 4) * 42;
            addClickTarget(px, py, 110, 30, () -> {
                TopkaClient.CONFIG.get().accentArgb = color;
                TopkaClient.CONFIG.save();
            });
        }

        addClickTarget(x + 42, y + 197, 34, 24, () -> shiftChannel(16, -8));
        addClickTarget(x + 80, y + 197, 34, 24, () -> shiftChannel(16, 8));
        addClickTarget(x + 162, y + 197, 34, 24, () -> shiftChannel(8, -8));
        addClickTarget(x + 200, y + 197, 34, 24, () -> shiftChannel(8, 8));
        addClickTarget(x + 282, y + 197, 34, 24, () -> shiftChannel(0, -8));
        addClickTarget(x + 320, y + 197, 34, 24, () -> shiftChannel(0, 8));

        addClickTarget(x + 42, y + 246, 148, 28, this::cycleThemeMode);
        addClickTarget(x + 198, y + 246, 148, 28, this::cycleSecondaryAccent);
        addClickTarget(x + 354, y + 246, 80, 28, () -> changeSpeed(-0.05F));
        addClickTarget(x + 442, y + 246, 80, 28, () -> changeSpeed(0.05F));

        addClickTarget(x + 42, y + 306, 120, 28, () -> changeHudOpacity(-16));
        addClickTarget(x + 170, y + 306, 120, 28, () -> changeHudOpacity(16));
        addClickTarget(x + 298, y + 306, 120, 28, () -> changePanelOpacity(-12));
        addClickTarget(x + 426, y + 306, 120, 28, () -> changePanelOpacity(12));

        addClickTarget(x + 42, y + 355, 178, 28, this::resetTheme);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = x(), y = y();
        int accent = Theme.accent();
        int secondary = Theme.secondary();

        g.fill(0, 0, width, height, 0xAA000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, cfg.panelArgb);
        g.fill(x, y, x + PANEL_W, y + 3, accent);

        g.text(font, "THEME STUDIO", x + 26, y + 21, 0xFFFFFFFF, true);
        g.text(font, "Customize the entire client with static, gradient or rainbow color.", x + 26, y + 39, cfg.mutedTextArgb, false);

        g.fill(x + PANEL_W - 132, y + 19, x + PANEL_W - 30, y + 49, 0xFF181820);
        g.fill(x + PANEL_W - 128, y + 23, x + PANEL_W - 81, y + 45, accent);
        g.fill(x + PANEL_W - 77, y + 23, x + PANEL_W - 30, y + 45, secondary);

        g.text(font, "ACCENT PRESETS", x + 42, y + 68, 0xFF707082, false);
        for (int i = 0; i < PRESETS.length; i++) {
            int px = x + 42 + (i % 4) * 124;
            int py = y + 86 + (i / 4) * 42;
            int color = PRESETS[i];
            boolean selected = cfg.accentArgb == color && !cfg.rainbowTheme && !cfg.gradientTheme;
            g.fill(px, py, px + 110, py + 30, selected ? 0xFF2F2F3D : 0xFF1B1B24);
            g.fill(px + 7, py + 7, px + 23, py + 23, color);
            g.text(font, selected ? "ACTIVE" : "SELECT", px + 33, py + 10, selected ? color : 0xFFB2B2C0, true);
        }

        int r = (cfg.accentArgb >>> 16) & 0xFF;
        int gr = (cfg.accentArgb >>> 8) & 0xFF;
        int b = cfg.accentArgb & 0xFF;
        g.text(font, "RGB FINE TUNING", x + 42, y + 176, 0xFF707082, false);
        drawAdjust(g, x + 42, y + 197, "R " + r, 0xFFFF6B7D);
        drawAdjust(g, x + 162, y + 197, "G " + gr, 0xFF5CE39A);
        drawAdjust(g, x + 282, y + 197, "B " + b, 0xFF5CB8FF);
        g.fill(x + 410, y + 188, x + 546, y + 226, cfg.accentArgb);
        g.text(font, hex(cfg.accentArgb), x + 442, y + 202, contrastText(cfg.accentArgb), true);

        g.text(font, "ANIMATION", x + 42, y + 232, 0xFF707082, false);
        drawButton(g, mouseX, mouseY, x + 42, y + 246, 148, "Mode: " + modeName());
        drawButton(g, mouseX, mouseY, x + 198, y + 246, 148, "Secondary " + hex(cfg.secondaryAccentArgb));
        drawButton(g, mouseX, mouseY, x + 354, y + 246, 80, "Slower");
        drawButton(g, mouseX, mouseY, x + 442, y + 246, 80, "Faster");
        g.text(font, "Speed " + String.format("%.2f", cfg.themeAnimationSpeed), x + 430, y + 280, cfg.mutedTextArgb, false);

        int hudAlpha = (cfg.hudBackgroundArgb >>> 24) & 0xFF;
        int panelAlpha = (cfg.panelArgb >>> 24) & 0xFF;
        g.text(font, "SURFACE OPACITY", x + 42, y + 292, 0xFF707082, false);
        drawButton(g, mouseX, mouseY, x + 42, y + 306, 120, "HUD −");
        drawButton(g, mouseX, mouseY, x + 170, y + 306, 120, "HUD +");
        drawButton(g, mouseX, mouseY, x + 298, y + 306, 120, "Panel −");
        drawButton(g, mouseX, mouseY, x + 426, y + 306, 120, "Panel +");
        g.text(font, "HUD " + percent(hudAlpha) + "   Panel " + percent(panelAlpha), x + 42, y + 340, cfg.mutedTextArgb, false);

        drawButton(g, mouseX, mouseY, x + 42, y + 355, 178, "Reset professional theme");
        g.text(font, "Changes save instantly", x + 42, y + 401, 0xFF686878, false);
        g.text(font, "ESC / " + TopkaClient.openMenuKey().getString() + " to return", x + 360, y + 401, 0xFF686878, false);

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
        g.fill(x, y + 27, x + w, y + 28, Theme.accent());
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

    private void cycleThemeMode() {
        var c = TopkaClient.CONFIG.get();
        if (!c.gradientTheme && !c.rainbowTheme) {
            c.gradientTheme = true;
        } else if (c.gradientTheme) {
            c.gradientTheme = false;
            c.rainbowTheme = true;
        } else {
            c.rainbowTheme = false;
        }
        TopkaClient.CONFIG.save();
    }

    private String modeName() {
        var c = TopkaClient.CONFIG.get();
        if (c.rainbowTheme) return "RAINBOW";
        if (c.gradientTheme) return "GRADIENT";
        return "STATIC";
    }

    private void cycleSecondaryAccent() {
        var c = TopkaClient.CONFIG.get();
        int current = c.secondaryAccentArgb;
        int next = PRESETS[0];
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i] == current) {
                next = PRESETS[(i + 1) % PRESETS.length];
                break;
            }
        }
        c.secondaryAccentArgb = next;
        TopkaClient.CONFIG.save();
    }

    private void changeSpeed(float amount) {
        var c = TopkaClient.CONFIG.get();
        c.themeAnimationSpeed = Math.clamp(c.themeAnimationSpeed + amount, 0.05F, 2.0F);
        TopkaClient.CONFIG.save();
    }

    private void changeHudOpacity(int delta) {
        var c = TopkaClient.CONFIG.get();
        int alpha = Math.clamp(((c.hudBackgroundArgb >>> 24) & 0xFF) + delta, 40, 255);
        c.hudBackgroundArgb = (alpha << 24) | (c.hudBackgroundArgb & 0x00FFFFFF);
        TopkaClient.CONFIG.save();
    }

    private void changePanelOpacity(int delta) {
        var c = TopkaClient.CONFIG.get();
        int alpha = Math.clamp(((c.panelArgb >>> 24) & 0xFF) + delta, 128, 255);
        c.panelArgb = (alpha << 24) | (c.panelArgb & 0x00FFFFFF);
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
        c.rainbowTheme = false;
        c.gradientTheme = false;
        c.themeAnimationSpeed = 0.35F;
        TopkaClient.CONFIG.save();
    }

    private static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    private static String percent(int alpha) {
        return Math.round(alpha / 255F * 100F) + "%";
    }

    private static int contrastText(int argb) {
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
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
