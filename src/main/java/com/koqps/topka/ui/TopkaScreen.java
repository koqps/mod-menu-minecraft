package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class TopkaScreen extends Screen {
    private static final int PANEL_W = 650;
    private static final int PANEL_H = 430;
    private static final int[] ACCENTS = {0xFF8B5CF6,0xFF41C7FF,0xFF50FA7B,0xFFFF4D6D,0xFFFFD166,0xFFFF7AD9,0xFFFFFFFF};
    private static final int[] HIT_COLORS = {0xB28B5CF6,0xB2FF4D6D,0xB241C7FF,0xB250FA7B,0xB2FFD166,0xB2FFFFFF};
    private static final int[] CROSSHAIR_COLORS = {0xFFFFFFFF,0xFF8B5CF6,0xFFFF4D6D,0xFF41C7FF,0xFF50FA7B,0xFFFFD166};
    private static final int[] HITBOX_COLORS = {0xFF41C7FF,0xFF8B5CF6,0xFFFF4D6D,0xFF50FA7B,0xFFFFD166,0xFFFFFFFF};

    private final Screen parent;
    private Module.Category selectedCategory;
    private int accentIndex;
    private int hitColorIndex;
    private int crosshairColorIndex;
    private int hitboxColorIndex;
    private boolean draggingWindow;
    private double dragOffsetX;
    private double dragOffsetY;

    public TopkaScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
        var cfg = TopkaClient.CONFIG.get();
        accentIndex = findColor(ACCENTS, cfg.accentArgb);
        hitColorIndex = findColor(HIT_COLORS, cfg.hitColorArgb);
        crosshairColorIndex = findColor(CROSSHAIR_COLORS, cfg.crosshairColorArgb);
        hitboxColorIndex = findColor(HITBOX_COLORS, cfg.hitboxColorArgb);
    }

    public Screen parentScreen() { return parent; }

    private int panelX() { return (width - PANEL_W) / 2 + TopkaClient.CONFIG.get().menuOffsetX; }
    private int panelY() { return (height - PANEL_H) / 2 + TopkaClient.CONFIG.get().menuOffsetY; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = panelX(), y = panelY();
        int accent = TopkaClient.CONFIG.get().accentArgb;
        graphics.fill(0, 0, width, height, 0x78000000);
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFA0D0D13);
        graphics.fill(x, y, x + 124, y + PANEL_H, 0xFF14141C);
        graphics.fill(x + 124, y, x + 126, y + PANEL_H, 0xFF282836);
        graphics.fill(x, y, x + PANEL_W, y + 3, accent);

        graphics.text(font, "MOD MENU", x + 18, y + 18, 0xFFFFFFFF, true);
        graphics.text(font, "CLIENT", x + 18, y + 34, accent, true);
        graphics.text(font, "drag header to move", x + 18, y + 50, 0xFF666677, false);

        drawCategory(graphics, mouseX, mouseY, x + 12, y + 82, "All", null);
        drawCategory(graphics, mouseX, mouseY, x + 12, y + 112, "Combat", Module.Category.COMBAT);
        drawCategory(graphics, mouseX, mouseY, x + 12, y + 142, "Visual", Module.Category.VISUAL);
        drawCategory(graphics, mouseX, mouseY, x + 12, y + 172, "HUD", Module.Category.HUD);
        drawCategory(graphics, mouseX, mouseY, x + 12, y + 202, "Movement", Module.Category.MOVEMENT);

        graphics.text(font, "MENU KEY", x + 18, y + 270, 0xFF6F6F81, false);
        graphics.text(font, TopkaClient.openMenuKey(), x + 18, y + 286, accent, true);
        graphics.text(font, "same key opens/closes", x + 18, y + 303, 0xFF666677, false);
        graphics.text(font, "Change in Controls", x + 18, y + 318, 0xFF666677, false);

        graphics.text(font, selectedCategory == null ? "ALL MODULES" : selectedCategory.name(), x + 142, y + 20, 0xFFF6F6FA, true);
        graphics.text(font, "Click a card to toggle • controls below customize", x + 142, y + 38, 0xFF858596, false);

        int visibleIndex = 0;
        for (Module module : TopkaClient.MODULES.all()) {
            if (selectedCategory != null && module.category() != selectedCategory) continue;
            int column = visibleIndex % 2;
            int row = visibleIndex / 2;
            int cardX = x + 142 + column * 250;
            int cardY = y + 66 + row * 59;
            boolean hovered = inside(mouseX, mouseY, cardX, cardY, 242, 51);
            int bg = hovered ? 0xFF292936 : (module.enabled() ? 0xFF20202C : 0xFF181820);
            graphics.fill(cardX, cardY, cardX + 242, cardY + 51, bg);
            graphics.fill(cardX, cardY, cardX + 3, cardY + 51, module.enabled() ? accent : 0xFF3B3B48);
            graphics.text(font, module.icon(), cardX + 11, cardY + 10, module.enabled() ? accent : 0xFF77778A, true);
            graphics.text(font, module.name(), cardX + 31, cardY + 8, 0xFFF2F2F7, true);
            graphics.text(font, module.description(), cardX + 11, cardY + 27, 0xFF888899, false);
            graphics.text(font, module.enabled() ? "ON" : "OFF", cardX + 205, cardY + 8, module.enabled() ? accent : 0xFF6F6F7D, true);
            visibleIndex++;
        }

        var cfg = TopkaClient.CONFIG.get();
        int controlsY = y + 365;
        drawSmallButton(graphics, mouseX, mouseY, x + 142, controlsY, 77, "Theme");
        drawSmallButton(graphics, mouseX, mouseY, x + 225, controlsY, 84, "Hit flash");
        drawSmallButton(graphics, mouseX, mouseY, x + 315, controlsY, 92, "Hitbox color");
        drawSmallButton(graphics, mouseX, mouseY, x + 413, controlsY, 78, "Hitbox +");
        drawSmallButton(graphics, mouseX, mouseY, x + 497, controlsY, 75, "Crosshair");
        drawSmallButton(graphics, mouseX, mouseY, x + 578, controlsY, 54, "Size +");
        drawSmallButton(graphics, mouseX, mouseY, x + 142, controlsY + 28, 128, "Edit HUD layout");
        drawSmallButton(graphics, mouseX, mouseY, x + 276, controlsY + 28, 110, "Hitbox reset");

        graphics.text(font, "Hitbox expand " + String.format("%.2f", cfg.hitboxExpand) + "  •  Crosshair " + cfg.crosshairSize + "/" + cfg.crosshairGap + "/" + cfg.crosshairThickness,
            x + 398, controlsY + 35, 0xFF777788, false);
        graphics.text(font, "v0.4.0", x + 18, y + 405, 0xFF555565, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        int x = panelX(), y = panelY();
        addClickTarget(x + 12, y + 82, 96, 22, () -> selectCategory(null));
        addClickTarget(x + 12, y + 112, 96, 22, () -> selectCategory(Module.Category.COMBAT));
        addClickTarget(x + 12, y + 142, 96, 22, () -> selectCategory(Module.Category.VISUAL));
        addClickTarget(x + 12, y + 172, 96, 22, () -> selectCategory(Module.Category.HUD));
        addClickTarget(x + 12, y + 202, 96, 22, () -> selectCategory(Module.Category.MOVEMENT));

        int visibleIndex = 0;
        for (Module module : TopkaClient.MODULES.all()) {
            if (selectedCategory != null && module.category() != selectedCategory) continue;
            int column = visibleIndex % 2, row = visibleIndex / 2;
            int cardX = x + 142 + column * 250, cardY = y + 66 + row * 59;
            addClickTarget(cardX, cardY, 242, 51, () -> { module.toggle(); TopkaClient.CONFIG.save(); });
            visibleIndex++;
        }

        int controlsY = y + 365;
        addClickTarget(x + 142, controlsY, 77, 22, () -> cycleAccent());
        addClickTarget(x + 225, controlsY, 84, 22, () -> cycleHitColor());
        addClickTarget(x + 315, controlsY, 92, 22, () -> cycleHitboxColor());
        addClickTarget(x + 413, controlsY, 78, 22, () -> {
            var cfg = TopkaClient.CONFIG.get(); cfg.hitboxExpand = cfg.hitboxExpand >= 0.50F ? 0.0F : cfg.hitboxExpand + 0.05F; TopkaClient.CONFIG.save();
        });
        addClickTarget(x + 497, controlsY, 75, 22, () -> cycleCrosshairColor());
        addClickTarget(x + 578, controlsY, 54, 22, () -> {
            var cfg = TopkaClient.CONFIG.get(); cfg.crosshairSize = cfg.crosshairSize >= 12 ? 2 : cfg.crosshairSize + 1; TopkaClient.CONFIG.save();
        });
        addClickTarget(x + 142, controlsY + 28, 128, 22, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        addClickTarget(x + 276, controlsY + 28, 110, 22, () -> { TopkaClient.CONFIG.get().hitboxExpand = 0.0F; TopkaClient.CONFIG.save(); });
    }

    private void cycleAccent() { accentIndex = (accentIndex + 1) % ACCENTS.length; TopkaClient.CONFIG.get().accentArgb = ACCENTS[accentIndex]; TopkaClient.CONFIG.save(); }
    private void cycleHitColor() { hitColorIndex = (hitColorIndex + 1) % HIT_COLORS.length; TopkaClient.CONFIG.get().hitColorArgb = HIT_COLORS[hitColorIndex]; TopkaClient.CONFIG.save(); }
    private void cycleCrosshairColor() { crosshairColorIndex = (crosshairColorIndex + 1) % CROSSHAIR_COLORS.length; TopkaClient.CONFIG.get().crosshairColorArgb = CROSSHAIR_COLORS[crosshairColorIndex]; TopkaClient.CONFIG.save(); }
    private void cycleHitboxColor() { hitboxColorIndex = (hitboxColorIndex + 1) % HITBOX_COLORS.length; TopkaClient.CONFIG.get().hitboxColorArgb = HITBOX_COLORS[hitboxColorIndex]; TopkaClient.CONFIG.save(); }

    private void selectCategory(Module.Category category) { selectedCategory = category; rebuildWidgets(); }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), button -> action.run()).pos(x, y).size(w, h).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int x = panelX(), y = panelY();
        if (inside(click.x(), click.y(), x + 126, y, PANEL_W - 126, 58)) {
            draggingWindow = true;
            dragOffsetX = click.x() - x;
            dragOffsetY = click.y() - y;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingWindow) {
            int centeredX = (width - PANEL_W) / 2;
            int centeredY = (height - PANEL_H) / 2;
            int targetX = (int) (click.x() - dragOffsetX);
            int targetY = (int) (click.y() - dragOffsetY);
            TopkaClient.CONFIG.get().menuOffsetX = Math.clamp(targetX - centeredX, -Math.max(0, centeredX), Math.max(0, centeredX));
            TopkaClient.CONFIG.get().menuOffsetY = Math.clamp(targetY - centeredY, -Math.max(0, centeredY), Math.max(0, centeredY));
            rebuildWidgets();
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (draggingWindow) {
            draggingWindow = false;
            TopkaClient.CONFIG.save();
            rebuildWidgets();
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

    private void drawCategory(GuiGraphicsExtractor graphics, int mx, int my, int x, int y, String text, Module.Category category) {
        boolean selected = selectedCategory == category;
        boolean hovered = inside(mx, my, x, y, 96, 22);
        int accent = TopkaClient.CONFIG.get().accentArgb;
        graphics.fill(x, y, x + 96, y + 22, selected ? 0xFF2C2937 : (hovered ? 0xFF242431 : 0xFF181820));
        if (selected) graphics.fill(x, y, x + 3, y + 22, accent);
        graphics.text(font, text, x + 9, y + 7, selected ? accent : 0xFFB8B8C5, false);
    }

    private void drawSmallButton(GuiGraphicsExtractor graphics, int mx, int my, int x, int y, int w, String text) {
        boolean hovered = inside(mx, my, x, y, w, 22);
        graphics.fill(x, y, x + w, y + 22, hovered ? 0xFF333340 : 0xFF22222C);
        graphics.fill(x, y + 21, x + w, y + 22, TopkaClient.CONFIG.get().accentArgb);
        graphics.text(font, text, x + 7, y + 7, 0xFFEAEAF2, false);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static int findColor(int[] colors, int current) { for (int i = 0; i < colors.length; i++) if (colors[i] == current) return i; return 0; }

    @Override
    public void onClose() { TopkaClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
}
