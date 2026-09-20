package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
    private enum DragTarget { NONE, HEALTH, ARMOR, MAP }
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
        graphics.fill(0, 0, width, height, 0xD0080810);
        graphics.centeredText(font, "HUD LAYOUT", width / 2, 18, 0xFFFFFFFF);
        graphics.centeredText(font, "Drag each panel anywhere • ESC returns and saves", width / 2, 34, 0xFF9292A4);
        drawHealth(graphics);
        drawArmor(graphics);
        drawMap(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawHealth(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get(); int x = c.healthHudX, y = c.healthHudY;
        g.fill(x, y, x + 132, y + 34, 0xEE121219); g.fill(x, y, x + 3, y + 34, c.accentArgb);
        g.text(font, "HEALTH DISPLAY", x + 10, y + 6, 0xFFFFFFFF, true);
        g.fill(x + 10, y + 22, x + 122, y + 27, 0xFF292934); g.fill(x + 10, y + 22, x + 80, y + 27, c.accentArgb);
    }

    private void drawArmor(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get(); int x = c.armorHudX, y = c.armorHudY;
        g.fill(x, y, x + 164, y + 58, 0xEE121219); g.fill(x, y, x + 3, y + 58, c.accentArgb);
        g.text(font, "ARMOR DISPLAY", x + 10, y + 6, 0xFFFFFFFF, true);
        g.text(font, "Helmet  92%", x + 10, y + 21, 0xFFBDBDCA, false);
        g.text(font, "Chestplate  87%", x + 10, y + 33, 0xFFBDBDCA, false);
        g.text(font, "Boots  75%", x + 10, y + 45, 0xFFBDBDCA, false);
    }

    private void drawMap(GuiGraphicsExtractor g) {
        var c = TopkaClient.CONFIG.get(); int x = c.mapHudX, y = c.mapHudY;
        g.fill(x, y, x + 132, y + 48, 0xEE121219); g.fill(x, y, x + 3, y + 48, c.accentArgb);
        g.text(font, "MAP DISPLAY", x + 10, y + 6, 0xFFFFFFFF, true);
        g.text(font, "X 120   Z -48", x + 10, y + 21, 0xFFBDBDCA, false);
        g.text(font, "NORTH", x + 10, y + 34, c.accentArgb, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var c = TopkaClient.CONFIG.get();
        if (inside(click.x(), click.y(), c.healthHudX, c.healthHudY, 132, 34)) start(DragTarget.HEALTH, click, c.healthHudX, c.healthHudY);
        else if (inside(click.x(), click.y(), c.armorHudX, c.armorHudY, 164, 58)) start(DragTarget.ARMOR, click, c.armorHudX, c.armorHudY);
        else if (inside(click.x(), click.y(), c.mapHudX, c.mapHudY, 132, 48)) start(DragTarget.MAP, click, c.mapHudX, c.mapHudY);
        else return super.mouseClicked(click, doubled);
        return true;
    }

    private void start(DragTarget target, MouseButtonEvent click, int x, int y) {
        dragging = target; offsetX = click.x() - x; offsetY = click.y() - y;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging == DragTarget.NONE) return super.mouseDragged(click, dx, dy);
        int w = dragging == DragTarget.ARMOR ? 164 : 132;
        int h = dragging == DragTarget.HEALTH ? 34 : (dragging == DragTarget.ARMOR ? 58 : 48);
        int nx = (int) Math.clamp(click.x() - offsetX, 0, Math.max(0, width - w));
        int ny = (int) Math.clamp(click.y() - offsetY, 48, Math.max(48, height - h));
        var c = TopkaClient.CONFIG.get();
        switch (dragging) {
            case HEALTH -> { c.healthHudX = nx; c.healthHudY = ny; }
            case ARMOR -> { c.armorHudX = nx; c.armorHudY = ny; }
            case MAP -> { c.mapHudX = nx; c.mapHudY = ny; }
            default -> { }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != DragTarget.NONE) { dragging = DragTarget.NONE; TopkaClient.CONFIG.save(); return true; }
        return super.mouseReleased(click);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    @Override
    public void onClose() { TopkaClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
}
