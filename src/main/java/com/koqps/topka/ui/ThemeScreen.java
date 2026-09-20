package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ThemeScreen extends Screen {
    private static final int PANEL_W = 620;
    private static final int PANEL_H = 500;
    private static final int[] PRESETS = {
            0xFF8B5CF6, 0xFF41C7FF, 0xFF50FA7B, 0xFFFF5C77,
            0xFFFFD166, 0xFFFF7AD9, 0xFFFF8A3D, 0xFFFFFFFF
    };

    private final Screen parent;

    public ThemeScreen(Screen parent) {
        super(Component.literal("Mod Menu Theme"));
        this.parent = parent;
    }

    private float uiScale() {
        return Math.clamp(TopkaClient.CONFIG.get().menuScale, 0.70F, 1.35F);
    }

    private int panelX() {
        return (width - Math.round(PANEL_W * uiScale())) / 2;
    }

    private int panelY() {
        return (height - Math.round(PANEL_H * uiScale())) / 2;
    }

    private int sx(int local) {
        return panelX() + Math.round(local * uiScale());
    }

    private int sy(int local) {
        return panelY() + Math.round(local * uiScale());
    }

    private int ss(int value) {
        return Math.max(1, Math.round(value * uiScale()));
    }

    private int localMouseX(double mouseX) {
        return Math.round((float) ((mouseX - panelX()) / uiScale()));
    }

    private int localMouseY(double mouseY) {
        return Math.round((float) ((mouseY - panelY()) / uiScale()));
    }

    @Override
    protected void init() {
        for (int i = 0; i < PRESETS.length; i++) {
            final int color = PRESETS[i];
            int px = 42 + (i % 4) * 132;
            int py = 86 + (i / 4) * 42;
            addLocalClickTarget(px, py, 118, 30, () -> {
                TopkaClient.CONFIG.get().accentArgb = color;
                TopkaClient.CONFIG.save();
            });
        }

        addLocalClickTarget(42, 197, 34, 24, () -> shiftChannel(16, -8));
        addLocalClickTarget(80, 197, 34, 24, () -> shiftChannel(16, 8));
        addLocalClickTarget(170, 197, 34, 24, () -> shiftChannel(8, -8));
        addLocalClickTarget(208, 197, 34, 24, () -> shiftChannel(8, 8));
        addLocalClickTarget(298, 197, 34, 24, () -> shiftChannel(0, -8));
        addLocalClickTarget(336, 197, 34, 24, () -> shiftChannel(0, 8));

        addLocalClickTarget(42, 246, 152, 28, this::cycleThemeMode);
        addLocalClickTarget(202, 246, 152, 28, this::cycleSecondaryAccent);
        addLocalClickTarget(362, 246, 84, 28, () -> changeSpeed(-0.05F));
        addLocalClickTarget(454, 246, 84, 28, () -> changeSpeed(0.05F));

        addLocalClickTarget(42, 305, 152, 28, this::cycleMenuStyle);
        addLocalClickTarget(202, 305, 96, 28, () -> changeMenuScale(-0.05F));
        addLocalClickTarget(306, 305, 96, 28, () -> changeMenuScale(0.05F));
        addLocalClickTarget(410, 305, 128, 28, this::resetMenuScale);

        addLocalClickTarget(42, 365, 120, 28, () -> changeHudOpacity(-16));
        addLocalClickTarget(170, 365, 120, 28, () -> changeHudOpacity(16));
        addLocalClickTarget(298, 365, 120, 28, () -> changePanelOpacity(-12));
        addLocalClickTarget(426, 365, 120, 28, () -> changePanelOpacity(12));

        addLocalClickTarget(42, 421, 190, 28, this::resetTheme);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);
        int accent = Theme.accent();
        int secondary = Theme.secondary();

        g.fill(0, 0, width, height, 0xAA000000);
        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        g.pose().scale(uiScale(), uiScale());

        g.fill(0, 0, PANEL_W, PANEL_H, cfg.panelArgb);
        g.fill(0, 0, PANEL_W, 3, accent);

        g.text(font, UiFont.text("THEME STUDIO"), 26, 21, 0xFFFFFFFF, true);
        g.text(font, UiFont.text("Colors, menu design, animation and interface scale."), 26, 39, cfg.mutedTextArgb, false);

        g.fill(PANEL_W - 132, 19, PANEL_W - 30, 49, 0xFF181820);
        g.fill(PANEL_W - 128, 23, PANEL_W - 81, 45, accent);
        g.fill(PANEL_W - 77, 23, PANEL_W - 30, 45, secondary);

        g.text(font, UiFont.text("ACCENT PRESETS"), 42, 68, 0xFF707082, false);
        for (int i = 0; i < PRESETS.length; i++) {
            int px = 42 + (i % 4) * 132;
            int py = 86 + (i / 4) * 42;
            int color = PRESETS[i];
            boolean selected = cfg.accentArgb == color && !cfg.rainbowTheme && !cfg.gradientTheme;
            g.fill(px, py, px + 118, py + 30, selected ? 0xFF2F2F3D : 0xFF1B1B24);
            g.fill(px + 7, py + 7, px + 23, py + 23, color);
            g.text(font, UiFont.text(selected ? "ACTIVE" : "SELECT"), px + 33, py + 10, selected ? color : 0xFFB2B2C0, true);
        }

        int r = (cfg.accentArgb >>> 16) & 0xFF;
        int gr = (cfg.accentArgb >>> 8) & 0xFF;
        int b = cfg.accentArgb & 0xFF;
        g.text(font, UiFont.text("RGB FINE TUNING"), 42, 176, 0xFF707082, false);
        drawAdjust(g, 42, 197, "R " + r, 0xFFFF6B7D);
        drawAdjust(g, 170, 197, "G " + gr, 0xFF5CE39A);
        drawAdjust(g, 298, 197, "B " + b, 0xFF5CB8FF);
        g.fill(430, 188, 562, 226, cfg.accentArgb);
        g.text(font, UiFont.text(hex(cfg.accentArgb)), 462, 202, contrastText(cfg.accentArgb), true);

        g.text(font, UiFont.text("COLOR ANIMATION"), 42, 232, 0xFF707082, false);
        drawButton(g, mx, my, 42, 246, 152, "Mode: " + modeName());
        drawButton(g, mx, my, 202, 246, 152, "Secondary " + hex(cfg.secondaryAccentArgb));
        drawButton(g, mx, my, 362, 246, 84, "Slower");
        drawButton(g, mx, my, 454, 246, 84, "Faster");

        g.text(font, UiFont.text("MENU DESIGN & SIZE"), 42, 291, 0xFF707082, false);
        drawButton(g, mx, my, 42, 305, 152, "Design: " + menuStyleName());
        drawButton(g, mx, my, 202, 305, 96, "Smaller");
        drawButton(g, mx, my, 306, 305, 96, "Larger");
        drawButton(g, mx, my, 410, 305, 128, "Reset size");
        g.text(font, UiFont.text("Interface " + String.format("%.2fx", cfg.menuScale) + "  •  all client panels use this scale"), 42, 339, cfg.mutedTextArgb, false);

        int hudAlpha = (cfg.hudBackgroundArgb >>> 24) & 0xFF;
        int panelAlpha = (cfg.panelArgb >>> 24) & 0xFF;
        g.text(font, UiFont.text("SURFACE OPACITY"), 42, 351, 0xFF707082, false);
        drawButton(g, mx, my, 42, 365, 120, "HUD −");
        drawButton(g, mx, my, 170, 365, 120, "HUD +");
        drawButton(g, mx, my, 298, 365, 120, "Panel −");
        drawButton(g, mx, my, 426, 365, 120, "Panel +");
        g.text(font, UiFont.text("HUD " + percent(hudAlpha) + "   Panel " + percent(panelAlpha)), 42, 399, cfg.mutedTextArgb, false);

        drawButton(g, mx, my, 42, 421, 190, "Reset professional theme");
        g.text(font, UiFont.text("Dedicated Mod Menu font enabled"), 252, 430, 0xFF686878, false);
        g.text(font, UiFont.text("ESC / " + TopkaClient.openMenuKey().getString() + " to return"), 372, 470, 0xFF686878, false);

        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawAdjust(GuiGraphicsExtractor g, int x, int y, String label, int color) {
        g.text(font, UiFont.text(label), x, y - 12, color, true);
        g.fill(x, y, x + 34, y + 24, 0xFF22222D);
        g.fill(x + 38, y, x + 72, y + 24, 0xFF22222D);
        g.centeredText(font, UiFont.text("−"), x + 17, y + 8, 0xFFECECF2);
        g.centeredText(font, UiFont.text("+"), x + 55, y + 8, 0xFFECECF2);
    }

    private void drawButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 28;
        g.fill(x, y, x + w, y + 28, hover ? 0xFF30303C : 0xFF21212B);
        g.fill(x, y + 27, x + w, y + 28, Theme.accent());
        g.centeredText(font, UiFont.text(text), x + w / 2, y + 9, 0xFFEDEDF4);
    }

    private void addLocalClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run())
                .pos(sx(x), sy(y))
                .size(ss(w), ss(h))
                .build());
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

    private void cycleMenuStyle() {
        var c = TopkaClient.CONFIG.get();
        c.menuStyle = Math.floorMod(c.menuStyle + 1, 4);
        TopkaClient.CONFIG.save();
    }

    private String menuStyleName() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> "NEON";
            case 2 -> "GLASS";
            case 3 -> "MINIMAL";
            default -> "PRO";
        };
    }

    private void changeMenuScale(float amount) {
        var c = TopkaClient.CONFIG.get();
        c.menuScale = Math.clamp(c.menuScale + amount, 0.70F, 1.35F);
        TopkaClient.CONFIG.save();
        rebuildWidgets();
    }

    private void resetMenuScale() {
        TopkaClient.CONFIG.get().menuScale = 1.0F;
        TopkaClient.CONFIG.save();
        rebuildWidgets();
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
        c.menuScale = 1.0F;
        c.menuStyle = 0;
        TopkaClient.CONFIG.save();
        rebuildWidgets();
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
