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
        graphics.fill(0, 0, width, height, 0xE0080810);
        graphics.fill(0, 0, width, 48, 0xF20D0D13);
        graphics.fill(0, 46, width, 48, Theme.accent());

        graphics.centeredText(font, "HUD WORKSPACE", width / 2, 11, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                "Drag panels • mouse wheel scales hovered panel • positions save automatically",
                width / 2,
                27,
                0xFF9292A4
        );

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
        float scale = Math.clamp(c.healthHudScale, 0.55F, 2.0F);

        g.pose().pushMatrix();
        g.pose().translate(c.healthHudX, c.healthHudY);
        g.pose().scale(scale, scale);

        if (c.healthHudBackground) {
            g.fill(0, 0, 146, 40, c.hudBackgroundArgb);
            g.fill(0, 0, 3, 40, Theme.accent());
        }
        g.text(font, "HEALTH", 10, 6, c.mutedTextArgb, false);
        g.text(font, "18.5 +2.0", 91, 6, c.textArgb, true);
        g.fill(10, 23, 136, 29, 0xFF292934);
        g.fill(10, 23, 116, 29, Theme.accent());
        g.fill(116, 23, 130, 29, 0xFFFFD166);
        g.text(font, "19 / 20", 10, 32, 0xFF767688, false);

        g.pose().popMatrix();
    }

    private void drawArmor(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        float scale = Math.clamp(c.armorHudScale, 0.55F, 2.0F);

        g.pose().pushMatrix();
        g.pose().translate(c.armorHudX, c.armorHudY);
        g.pose().scale(scale, scale);

        if (c.armorHudBackground) {
            g.fill(0, 0, 170, 72, c.hudBackgroundArgb);
            g.fill(0, 0, 3, 72, Theme.accent());
        }
        g.text(font, "ARMOR", 10, 6, c.mutedTextArgb, false);
        int sx = 10;
        for (int i = 0; i < 4; i++) {
            g.fill(sx, 22, sx + 18, 40, 0x553A3A47);
            g.fill(sx + 2, 24, sx + 16, 38, i == 0 ? Theme.accent() : 0x665D5D6C);
            int pct = 93 - i * 11;
            g.text(font, pct + "%", sx - 1, 45, pct > 60 ? 0xFF58E38C : 0xFFFFD166, true);
            sx += 38;
        }

        g.pose().popMatrix();
    }

    private void drawMap(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        float scale = Math.clamp(c.mapHudScale, 0.55F, 2.0F);

        g.pose().pushMatrix();
        g.pose().translate(c.mapHudX, c.mapHudY);
        g.pose().scale(scale, scale);

        if (c.mapHudBackground) {
            g.fill(0, 0, 158, 58, c.hudBackgroundArgb);
            g.fill(0, 0, 3, 58, Theme.accent());
        }
        g.text(font, "LOCATION", 10, 6, c.mutedTextArgb, false);
        g.text(font, "X 120  Y 64  Z -48", 10, 20, c.textArgb, false);
        g.text(font, "NORTH", 10, 34, Theme.accent(), true);
        g.text(font, "OVERWORLD", 56, 34, 0xFF77778A, false);
        g.fill(10, 49, 148, 51, 0xFF292934);
        g.fill(67, 47, 71, 53, Theme.accent());

        g.pose().popMatrix();
    }

    private void drawPing(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get();
        float scale = Math.clamp(c.pingHudScale, 0.55F, 2.0F);

        g.pose().pushMatrix();
        g.pose().translate(c.pingHudX, c.pingHudY);
        g.pose().scale(scale, scale);

        if (c.pingHudBackground) {
            g.fill(0, 0, 88, 28, c.hudBackgroundArgb);
            g.fill(0, 0, 3, 28, Theme.accent());
        }
        g.text(font, "PING", 10, 6, c.mutedTextArgb, false);
        g.text(font, "42 ms", 48, 6, 0xFF58E38C, true);
        g.fill(10, 20, 78, 23, 0xFF292934);
        g.fill(10, 20, 68, 23, 0xFF58E38C);

        g.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }

        DragTarget target = hoveredTarget(click.x(), click.y());
        if (target == DragTarget.NONE) {
            return super.mouseClicked(click, doubled);
        }

        var c = TopkaClient.CONFIG.get();
        switch (target) {
            case HEALTH -> start(target, click, c.healthHudX, c.healthHudY);
            case ARMOR -> start(target, click, c.armorHudX, c.armorHudY);
            case MAP -> start(target, click, c.mapHudX, c.mapHudY);
            case PING -> start(target, click, c.pingHudX, c.pingHudY);
            default -> { }
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
        if (dragging == DragTarget.NONE) {
            return super.mouseDragged(click, dx, dy);
        }

        int w = scaledWidth(dragging);
        int h = scaledHeight(dragging);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DragTarget target = hoveredTarget(mouseX, mouseY);
        if (target == DragTarget.NONE || scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        float delta = scrollY > 0 ? 0.05F : -0.05F;
        var c = TopkaClient.CONFIG.get();
        switch (target) {
            case HEALTH -> c.healthHudScale = Math.clamp(c.healthHudScale + delta, 0.55F, 2.0F);
            case ARMOR -> c.armorHudScale = Math.clamp(c.armorHudScale + delta, 0.55F, 2.0F);
            case MAP -> c.mapHudScale = Math.clamp(c.mapHudScale + delta, 0.55F, 2.0F);
            case PING -> c.pingHudScale = Math.clamp(c.pingHudScale + delta, 0.55F, 2.0F);
            default -> { }
        }
        TopkaClient.CONFIG.save();
        return true;
    }

    private DragTarget hoveredTarget(double mx, double my) {
        var c = TopkaClient.CONFIG.get();
        if (inside(mx, my, c.healthHudX, c.healthHudY, scaledWidth(DragTarget.HEALTH), scaledHeight(DragTarget.HEALTH))) return DragTarget.HEALTH;
        if (inside(mx, my, c.armorHudX, c.armorHudY, scaledWidth(DragTarget.ARMOR), scaledHeight(DragTarget.ARMOR))) return DragTarget.ARMOR;
        if (inside(mx, my, c.mapHudX, c.mapHudY, scaledWidth(DragTarget.MAP), scaledHeight(DragTarget.MAP))) return DragTarget.MAP;
        if (inside(mx, my, c.pingHudX, c.pingHudY, scaledWidth(DragTarget.PING), scaledHeight(DragTarget.PING))) return DragTarget.PING;
        return DragTarget.NONE;
    }

    private int scaledWidth(DragTarget target) {
        return Math.round(baseWidth(target) * scale(target));
    }

    private int scaledHeight(DragTarget target) {
        return Math.round(baseHeight(target) * scale(target));
    }

    private int baseWidth(DragTarget target) {
        return switch (target) {
            case HEALTH -> 146;
            case ARMOR -> 170;
            case MAP -> 158;
            case PING -> 88;
            default -> 0;
        };
    }

    private int baseHeight(DragTarget target) {
        return switch (target) {
            case HEALTH -> 40;
            case ARMOR -> 72;
            case MAP -> 58;
            case PING -> 28;
            default -> 0;
        };
    }

    private float scale(DragTarget target) {
        var c = TopkaClient.CONFIG.get();
        return switch (target) {
            case HEALTH -> Math.clamp(c.healthHudScale, 0.55F, 2.0F);
            case ARMOR -> Math.clamp(c.armorHudScale, 0.55F, 2.0F);
            case MAP -> Math.clamp(c.mapHudScale, 0.55F, 2.0F);
            case PING -> Math.clamp(c.pingHudScale, 0.55F, 2.0F);
            default -> 1.0F;
        };
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
