package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TopkaScreen extends Screen {
    private static final int PANEL_W = 710;
    private static final int PANEL_H = 500;
    private static final int SIDEBAR_W = 138;
    private static final int CARD_W = 258;
    private static final int CARD_H = 54;
    private static final int CARD_GAP_X = 12;
    private static final int CARD_GAP_Y = 8;
    private static final int VISIBLE_ROWS = 5;

    private final Screen parent;
    private Module.Category selectedCategory;
    private EditBox searchBox;
    private String searchQuery = "";
    private int scrollRow;

    private boolean draggingWindow;
    private double dragOffsetX;
    private double dragOffsetY;

    public TopkaScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    private int panelX() {
        return (width - PANEL_W) / 2 + TopkaClient.CONFIG.get().menuOffsetX;
    }

    private int panelY() {
        return (height - PANEL_H) / 2 + TopkaClient.CONFIG.get().menuOffsetY;
    }

    @Override
    protected void init() {
        int x = panelX(), y = panelY();

        addClickTarget(x + 15, y + 92, 108, 26, () -> setCategory(null));
        addClickTarget(x + 15, y + 124, 108, 26, () -> setCategory(Module.Category.COMBAT));
        addClickTarget(x + 15, y + 156, 108, 26, () -> setCategory(Module.Category.VISUAL));
        addClickTarget(x + 15, y + 188, 108, 26, () -> setCategory(Module.Category.HUD));
        addClickTarget(x + 15, y + 220, 108, 26, () -> setCategory(Module.Category.MOVEMENT));

        EditBox previousSearch = this.searchBox;
        this.searchBox = new EditBox(
                this.font,
                x + 160,
                y + 28,
                330,
                22,
                previousSearch,
                Component.literal("Search modules")
        );
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            if (!value.equals(this.searchQuery)) {
                this.searchQuery = value;
                this.scrollRow = 0;
                this.rebuildWidgets();
            }
        });
        this.addWidget(this.searchBox);

        List<Module> filtered = filteredModules();
        clampScroll(filtered);
        int first = scrollRow * 2;
        int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);
        for (int i = first; i < last; i++) {
            Module module = filtered.get(i);
            int visibleIndex = i - first;
            int column = visibleIndex % 2;
            int row = visibleIndex / 2;
            int cardX = x + 160 + column * (CARD_W + CARD_GAP_X);
            int cardY = y + 82 + row * (CARD_H + CARD_GAP_Y);
            addClickTarget(cardX, cardY, CARD_W, CARD_H, () -> {
                module.toggle();
                TopkaClient.CONFIG.save();
            });
        }

        addClickTarget(x + 160, y + 420, 118, 30, () -> minecraft.gui.setScreen(new ThemeScreen(this)));
        addClickTarget(x + 286, y + 420, 144, 30, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        addClickTarget(x + 438, y + 420, 126, 30, this::resetWindowPosition);
        addClickTarget(x + 572, y + 420, 110, 30, this::disableAll);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = panelX(), y = panelY();

        g.fill(0, 0, width, height, 0x8F000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, cfg.panelArgb);
        g.fill(x, y, x + SIDEBAR_W, y + PANEL_H, cfg.sidebarArgb);
        g.fill(x + SIDEBAR_W, y, x + SIDEBAR_W + 1, y + PANEL_H, 0xFF292934);
        g.fill(x, y, x + PANEL_W, y + 3, cfg.accentArgb);

        drawBrand(g, x, y);
        drawSidebar(g, mouseX, mouseY, x, y);

        g.text(font, selectedCategory == null ? "MODULE LIBRARY" : selectedCategory.name(), x + 160, y + 11, 0xFFF7F7FB, true);
        g.text(font, enabledCount() + " active", x + 500, y + 11, cfg.accentArgb, true);

        List<Module> filtered = filteredModules();
        clampScroll(filtered);
        int first = scrollRow * 2;
        int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);

        if (filtered.isEmpty()) {
            g.centeredText(font, "No modules match \"" + searchQuery + "\"", x + 422, y + 210, 0xFF777788);
        } else {
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = x + 160 + column * (CARD_W + CARD_GAP_X);
                int cardY = y + 82 + row * (CARD_H + CARD_GAP_Y);
                drawModuleCard(g, mouseX, mouseY, cardX, cardY, filtered.get(i));
            }
        }

        int rows = (filtered.size() + 1) / 2;
        if (rows > VISIBLE_ROWS) {
            int trackX = x + 696;
            int trackY = y + 82;
            int trackH = VISIBLE_ROWS * (CARD_H + CARD_GAP_Y) - CARD_GAP_Y;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFF24242E);
            int thumbH = Math.max(24, trackH * VISIBLE_ROWS / rows);
            int maxScroll = Math.max(1, rows - VISIBLE_ROWS);
            int thumbY = trackY + (trackH - thumbH) * scrollRow / maxScroll;
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, cfg.accentArgb);
        }

        drawBottomButton(g, mouseX, mouseY, x + 160, y + 420, 118, "Theme Studio");
        drawBottomButton(g, mouseX, mouseY, x + 286, y + 420, 144, "HUD Workspace");
        drawBottomButton(g, mouseX, mouseY, x + 438, y + 420, 126, "Reset Window");
        drawBottomButton(g, mouseX, mouseY, x + 572, y + 420, 110, "Disable All");

        g.text(font, "Left click toggles • Right click opens settings • Mouse wheel scrolls", x + 160, y + 464, 0xFF686879, false);
        g.text(font, "v0.5.0", x + 646, y + 464, 0xFF686879, true);

        if (this.searchBox != null) {
            this.searchBox.extractRenderState(g, mouseX, mouseY, delta);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawBrand(GuiGraphicsExtractor g, int x, int y) {
        var cfg = TopkaClient.CONFIG.get();
        g.text(font, "MOD", x + 18, y + 18, 0xFFFFFFFF, true);
        g.text(font, "MENU", x + 50, y + 18, cfg.accentArgb, true);
        g.text(font, "VISUAL CLIENT", x + 18, y + 37, 0xFF6E6E80, false);
        g.fill(x + 18, y + 59, x + 118, y + 60, 0xFF2B2B36);
        g.fill(x + 18, y + 59, x + 68, y + 60, cfg.accentArgb);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my, int x, int y) {
        drawCategory(g, mx, my, x + 15, y + 92, "All Modules", null);
        drawCategory(g, mx, my, x + 15, y + 124, "Combat", Module.Category.COMBAT);
        drawCategory(g, mx, my, x + 15, y + 156, "Visual", Module.Category.VISUAL);
        drawCategory(g, mx, my, x + 15, y + 188, "HUD", Module.Category.HUD);
        drawCategory(g, mx, my, x + 15, y + 220, "Movement", Module.Category.MOVEMENT);

        var cfg = TopkaClient.CONFIG.get();
        g.text(font, "KEYBIND", x + 18, y + 290, 0xFF646476, false);
        g.fill(x + 15, y + 307, x + 123, y + 337, 0xFF1B1B24);
        g.centeredText(font, TopkaClient.openMenuKey(), x + 69, y + 317, cfg.accentArgb);
        g.text(font, "Same key opens", x + 18, y + 347, 0xFF686879, false);
        g.text(font, "and closes menu.", x + 18, y + 360, 0xFF686879, false);
        g.text(font, "Rebind in Controls.", x + 18, y + 382, 0xFF686879, false);
    }

    private void drawCategory(GuiGraphicsExtractor g, int mx, int my, int x, int y, String label, Module.Category category) {
        boolean selected = selectedCategory == category;
        boolean hover = inside(mx, my, x, y, 108, 26);
        int color = selected ? TopkaClient.CONFIG.get().accentArgb : 0xFFB7B7C4;
        g.fill(x, y, x + 108, y + 26, selected ? 0xFF282435 : (hover ? 0xFF20202A : 0x00101010));
        if (selected) g.fill(x, y, x + 3, y + 26, color);
        g.text(font, label, x + 10, y + 9, color, selected);
    }

    private void drawModuleCard(GuiGraphicsExtractor g, int mx, int my, int x, int y, Module module) {
        var cfg = TopkaClient.CONFIG.get();
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        int background = module.enabled() ? 0xFF20202B : 0xFF17171F;
        if (hover) background = module.enabled() ? 0xFF29293A : 0xFF22222C;

        g.fill(x, y, x + CARD_W, y + CARD_H, background);
        g.fill(x, y, x + 3, y + CARD_H, module.enabled() ? cfg.accentArgb : 0xFF383846);

        g.fill(x + 12, y + 11, x + 38, y + 37, module.enabled() ? 0x332F80FF : 0x332D2D38);
        g.centeredText(font, module.icon(), x + 25, y + 20, module.enabled() ? cfg.accentArgb : 0xFF777789);

        g.text(font, module.name(), x + 48, y + 10, cfg.textArgb, true);
        String description = module.description();
        if (description.length() > 37) description = description.substring(0, 36) + "…";
        g.text(font, description, x + 48, y + 28, 0xFF858596, false);

        int pillX = x + CARD_W - 43;
        g.fill(pillX, y + 10, x + CARD_W - 10, y + 27, module.enabled() ? 0x332BFF8A : 0x334F4F5A);
        g.centeredText(font, module.enabled() ? "ON" : "OFF", pillX + 16, y + 15, module.enabled() ? 0xFF65E89A : 0xFF858592);

        if (hover) {
            g.text(font, "settings ›", x + CARD_W - 67, y + 37, cfg.accentArgb, false);
        }
    }

    private void drawBottomButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = inside(mx, my, x, y, w, 30);
        g.fill(x, y, x + w, y + 30, hover ? 0xFF30303C : 0xFF20202A);
        g.fill(x, y + 29, x + w, y + 30, TopkaClient.CONFIG.get().accentArgb);
        g.centeredText(font, label, x + w / 2, y + 10, 0xFFEDEDF4);
    }

    private List<Module> filteredModules() {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module module : TopkaClient.MODULES.all()) {
            if (selectedCategory != null && module.category() != selectedCategory) continue;
            if (!query.isEmpty()) {
                String haystack = (module.name() + " " + module.description() + " " + module.category().name()).toLowerCase(Locale.ROOT);
                if (!haystack.contains(query)) continue;
            }
            result.add(module);
        }
        return result;
    }

    private void clampScroll(List<Module> modules) {
        int rows = (modules.size() + 1) / 2;
        int max = Math.max(0, rows - VISIBLE_ROWS);
        scrollRow = Math.clamp(scrollRow, 0, max);
    }

    private int enabledCount() {
        int enabled = 0;
        for (Module module : TopkaClient.MODULES.all()) if (module.enabled()) enabled++;
        return enabled;
    }

    private void setCategory(Module.Category category) {
        selectedCategory = category;
        scrollRow = 0;
        rebuildWidgets();
    }

    private void resetWindowPosition() {
        TopkaClient.CONFIG.get().menuOffsetX = 0;
        TopkaClient.CONFIG.get().menuOffsetY = 0;
        TopkaClient.CONFIG.save();
        rebuildWidgets();
    }

    private void disableAll() {
        for (Module module : TopkaClient.MODULES.all()) module.setEnabled(false);
        TopkaClient.CONFIG.save();
    }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run()).pos(x, y).size(w, h).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int x = panelX(), y = panelY();

        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            List<Module> filtered = filteredModules();
            clampScroll(filtered);
            int first = scrollRow * 2;
            int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = x + 160 + column * (CARD_W + CARD_GAP_X);
                int cardY = y + 82 + row * (CARD_H + CARD_GAP_Y);
                if (inside(click.x(), click.y(), cardX, cardY, CARD_W, CARD_H)) {
                    minecraft.gui.setScreen(new ModuleSettingsScreen(this, filtered.get(i)));
                    return true;
                }
            }
        }

        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && inside(click.x(), click.y(), x + SIDEBAR_W, y, PANEL_W - SIDEBAR_W, 66)) {
            draggingWindow = true;
            dragOffsetX = click.x() - x;
            dragOffsetY = click.y() - y;
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (!draggingWindow) return super.mouseDragged(click, dx, dy);

        int centeredX = (width - PANEL_W) / 2;
        int centeredY = (height - PANEL_H) / 2;
        int targetX = (int) (click.x() - dragOffsetX);
        int targetY = (int) (click.y() - dragOffsetY);

        TopkaClient.CONFIG.get().menuOffsetX = Math.clamp(targetX - centeredX, -Math.max(0, centeredX), Math.max(0, centeredX));
        TopkaClient.CONFIG.get().menuOffsetY = Math.clamp(targetY - centeredY, -Math.max(0, centeredY), Math.max(0, centeredY));
        return true;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = panelX(), y = panelY();
        if (inside(mouseX, mouseY, x + 150, y + 70, 550, 330)) {
            List<Module> filtered = filteredModules();
            int rows = (filtered.size() + 1) / 2;
            int max = Math.max(0, rows - VISIBLE_ROWS);
            int before = scrollRow;
            if (scrollY < 0) scrollRow = Math.min(max, scrollRow + 1);
            if (scrollY > 0) scrollRow = Math.max(0, scrollRow - 1);
            if (before != scrollRow) {
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (TopkaClient.menuKeyMatches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() {
        TopkaClient.CONFIG.save();
        minecraft.gui.setScreen(parent);
    }
}
