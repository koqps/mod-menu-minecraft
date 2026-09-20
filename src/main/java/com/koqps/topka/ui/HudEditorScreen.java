package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
    private enum DragTarget { NONE, HEALTH, ARMOR, MAP, PING }

    private final Screen parent;
    private DragTarget dragging = DragTarget.NONE;
    private double offsetX;
    private double offsetY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Mod Menu HUD Layout"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        graphics.fill(0, 0, width, height, 0xE0080810);
        graphics.fill(0, 0, width, 48, 0xF20D0D13);
        graphics.fill(0, 46, width, 48, Theme.accent());

        graphics.centeredText(font, "HUD WORKSPACE", width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, "Drag panels • positions save automatically • Right Shift / ESC returns", width / 2, 28, 0xFF9292A4);

        drawGrid(graphics);
        drawHealth(graphics);
        drawArmor(graphics);
        drawMap(graphics);
        drawPing(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawGrid(GuiGraphicsExtractor g) {
        for (int x = 0; x < width; x += 20) g.fill(x, 48, x + 1, height, 0x182F2F3A);
        for (int y = 48; y < height; y += 20) g.fill(0, y, width, y + 1, 0x182F2F3A);
    }

    private void drawHealth(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        int x = c.healthHudX, y = c.healthHudY;
        g.fill(x, y, x + 146, y + 40, c.hudBackgroundArgb);
        g.fill(x, y, x + 3, y + 40, c.accentArgb);
        g.text(font, "HEALTH", x + 10, y + 6, c.mutedTextArgb, false);
        g.text(font, "18.5 +2.0", x + 91, y + 6, c.textArgb, true);
        g.fill(x + 10, y + 23, x + 136, y + 29, 0xFF292934);
        g.fill(x + 10, y + 23, x + 116, y + 29, c.accentArgb);
        g.fill(x + 116, y + 23, x + 130, y + 29, 0xFFFFD166);
        g.text(font, "19 / 20", x + 10, y + 32, 0xFF767688, false);
    }

    private void drawArmor(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        int x = c.armorHudX, y = c.armorHudY;
        g.fill(x, y, x + 170, y + 72, c.hudBackgroundArgb);
        g.fill(x, y, x + 3, y + 72, c.accentArgb);
        g.text(font, "ARMOR", x + 10, y + 6, c.mutedTextArgb, false);
        int sx = x + 10;
        for (int i = 0; i < 4; i++) {
            g.fill(sx, y + 22, sx + 18, y + 40, 0x553A3A47);
            g.fill(sx + 2, y + 24, sx + 16, y + 38, i == 0 ? c.accentArgb : 0x665D5D6C);
            int pct = 93 - i * 11;
            g.text(font, pct + "%", sx - 1, y + 45, pct > 60 ? 0xFF58E38C : 0xFFFFD166, true);
            sx += 38;
        }
    }

    private void drawMap(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        int x = c.mapHudX, y = c.mapHudY;
        g.fill(x, y, x + 158, y + 58, c.hudBackgroundArgb);
        g.fill(x, y, x + 3, y + 58, c.accentArgb);
        g.text(font, "LOCATION", x + 10, y + 6, c.mutedTextArgb, false);
        g.text(font, "X 120  Y 64  Z -48", x + 10, y + 20, c.textArgb, false);
        g.text(font, "NORTH", x + 10, y + 34, c.accentArgb, true);
        g.text(font, "OVERWORLD", x + 56, y + 34, 0xFF77778A, false);
        g.fill(x + 10, y + 49, x + 148, y + 51, 0xFF292934);
        g.fill(x + 67, y + 47, x + 71, y + 53, c.accentArgb);
    }

    private void drawPing(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        int x = c.pingHudX, y = c.pingHudY;
        g.fill(x, y, x + 88, y + 28, c.hudBackgroundArgb);
        g.fill(x, y, x + 3, y + 28, c.accentArgb);
        g.text(font, "PING", x + 10, y + 6, c.mutedTextArgb, false);
        g.text(font, "42 ms", x + 48, y + 6, 0xFF58E38C, true);
        g.fill(x + 10, y + 20, x + 78, y + 23, 0xFF292934);
        g.fill(x + 10, y + 20, x + 68, y + 23, 0xFF58E38C);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);

        var c = TopkaClient.CONFIG.get();
        if (inside(click.x(), click.y(), c.healthHudX, c.healthHudY, 146, 40)) {
            start(DragTarget.HEALTH, click, c.healthHudX, c.healthHudY);
        } else if (inside(click.x(), click.y(), c.armorHudX, c.armorHudY, 170, 72)) {
            start(DragTarget.ARMOR, click, c.armorHudX, c.armorHudY);
        } else if (inside(click.x(), click.y(), c.mapHudX, c.mapHudY, 158, 58)) {
            start(DragTarget.MAP, click, c.mapHudX, c.mapHudY);
        } else if (inside(click.x(), click.y(), c.pingHudX, c.pingHudY, 88, 28)) {
            start(DragTarget.PING, click, c.pingHudX, c.pingHudY);
        } else {
            return super.mouseClicked(click, doubled);
        }
        return true;
    }

    private void start(DragTarget target, MouseButtonEvent click, int x, int y) {
        dragging = target;
        offsetX = click.x() - x;
        offsetY = click.y() - y;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging == DragTarget.NONE) return super.mouseDragged(click, dx, dy);

        int w = switch (dragging) {
            case HEALTH -> 146;
            case ARMOR -> 170;
            case MAP -> 158;
            case PING -> 88;
            default -> 0;
        };
        int h = switch (dragging) {
            case HEALTH -> 40;
            case ARMOR -> 72;
            case MAP -> 58;
            case PING -> 28;
            default -> 0;
        };

        int nx = snap((int) Math.clamp(click.x() - offsetX, 0, Math.max(0, width - w)));
        int ny = snap((int) Math.clamp(click.y() - offsetY, 48, Math.max(48, height - h)));

        var c = TopkaClient.CONFIG.get();
        switch (dragging) {
            case HEALTH -> { c.healthHudX = nx; c.healthHudY = ny; }
            case ARMOR -> { c.armorHudX = nx; c.armorHudY = ny; }
            case MAP -> { c.mapHudX = nx; c.mapHudY = ny; }
            case PING -> { c.pingHudX = nx; c.pingHudY = ny; }
            default -> { }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != DragTarget.NONE) {
            dragging = DragTarget.NONE;
            TopkaClient.CONFIG.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (TopkaClient.menuKeyMatches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static int snap(int value) {
        return Math.round(value / 4.0F) * 4;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void onClose() {
        TopkaClient.CONFIG.save();
        minecraft.gui.setScreen(parent);
    }
}
