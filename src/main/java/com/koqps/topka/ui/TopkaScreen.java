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
    private static final int PANEL_W = 730;
    private static final int PANEL_H = 520;
    private static final int SIDEBAR_W = 148;
    private static final int CARD_W = 264;
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

        addCategoryTarget(x, y, 0, null);
        addCategoryTarget(x, y, 1, Module.Category.COMBAT);
        addCategoryTarget(x, y, 2, Module.Category.VISUAL);
        addCategoryTarget(x, y, 3, Module.Category.PLAYER);
        addCategoryTarget(x, y, 4, Module.Category.MOVEMENT);
        addCategoryTarget(x, y, 5, Module.Category.HUD);
        addCategoryTarget(x, y, 6, Module.Category.WORLD);
        addCategoryTarget(x, y, 7, Module.Category.MISC);

        EditBox previousSearch = this.searchBox;
        this.searchBox = new EditBox(
                this.font,
                x + 172,
                y + 28,
                346,
                22,
                previousSearch,
                Component.literal("Search modules")
        );
        this.searchBox.setMaxLength(48);
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
            int cardX = x + 172 + column * (CARD_W + CARD_GAP_X);
            int cardY = y + 82 + row * (CARD_H + CARD_GAP_Y);
            addClickTarget(cardX, cardY, CARD_W, CARD_H, () -> {
                module.toggle();
                TopkaClient.CONFIG.save();
            });
        }

        addClickTarget(x + 172, y + 426, 104, 30, () -> minecraft.gui.setScreen(new ThemeScreen(this)));
        addClickTarget(x + 284, y + 426, 118, 30, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        addClickTarget(x + 410, y + 426, 118, 30, () -> minecraft.gui.setScreen(new WaypointScreen(this)));
        addClickTarget(x + 536, y + 426, 104, 30, this::resetWindowPosition);
        addClickTarget(x + 648, y + 426, 56, 30, this::disableAll);
    }

    private void addCategoryTarget(int x, int y, int index, Module.Category category) {
        addClickTarget(x + 15, y + 86 + index * 29, 118, 24, () -> setCategory(category));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = panelX(), y = panelY();

        g.fill(0, 0, width, height, 0x92000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, cfg.panelArgb);
        g.fill(x, y, x + SIDEBAR_W, y + PANEL_H, cfg.sidebarArgb);
        g.fill(x + SIDEBAR_W, y, x + SIDEBAR_W + 1, y + PANEL_H, 0xFF292934);
        g.fill(x, y, x + PANEL_W, y + 3, cfg.accentArgb);

        drawBrand(g, x, y);
        drawSidebar(g, mouseX, mouseY, x, y);

        g.text(font, selectedCategory == null ? "MODULE LIBRARY" : selectedCategory.name(), x + 172, y + 11, 0xFFF7F7FB, true);
        g.text(font, enabledCount() + " active", x + 552, y + 11, cfg.accentArgb, true);

        List<Module> filtered = filteredModules();
        clampScroll(filtered);
        int first = scrollRow * 2;
        int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);

        if (filtered.isEmpty()) {
            g.centeredText(font, "No modules match "" + searchQuery + """, x + 440, y + 224, 0xFF777788);
        } else {
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = x + 172 + column * (CARD_W + CARD_GAP_X);
                int cardY = y + 82 + row * (CARD_H + CARD_GAP_Y);
                drawModuleCard(g, mouseX, mouseY, cardX, cardY, filtered.get(i));
            }
        }

        int rows = (filtered.size() + 1) / 2;
        if (rows > VISIBLE_ROWS) {
            int trackX = x + 716;
            int trackY = y + 82;
            int trackH = VISIBLE_ROWS * (CARD_H + CARD_GAP_Y) - CARD_GAP_Y;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFF24242E);
            int thumbH = Math.max(24, trackH * VISIBLE_ROWS / rows);
            int maxScroll = Math.max(1, rows - VISIBLE_ROWS);
            int thumbY = trackY + (trackH - thumbH) * scrollRow / maxScroll;
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, cfg.accentArgb);
        }

        drawBottomButton(g, mouseX, mouseY, x + 172, y + 426, 104, "Theme");
        drawBottomButton(g, mouseX, mouseY, x + 284, y + 426, 118, "HUD Workspace");
        drawBottomButton(g, mouseX, mouseY, x + 410, y + 426, 118, "Waypoints");
        drawBottomButton(g, mouseX, mouseY, x + 536, y + 426, 104, "Reset Window");
        drawBottomButton(g, mouseX, mouseY, x + 648, y + 426, 56, "Off");

        g.text(font, "Left click toggles • Right click settings • Mouse wheel scrolls", x + 172, y + 477, 0xFF686879, false);
        g.text(font, "Mod Menu 0.5.0", x + 618, y + 477, 0xFF686879, true);

        if (this.searchBox != null) {
            this.searchBox.extractRenderState(g, mouseX, mouseY, delta);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawBrand(GuiGraphicsExtractor g, int x, int y) {
        var cfg = TopkaClient.CONFIG.get();
        g.text(font, "MOD", x + 18, y + 18, 0xFFFFFFFF, true);
        g.text(font, "MENU", x + 50, y + 18, cfg.accentArgb, true);
        g.text(font, "PRO CLIENT", x + 18, y + 37, 0xFF6E6E80, false);
        g.fill(x + 18, y + 59, x + 128, y + 60, 0xFF2B2B36);
        g.fill(x + 18, y + 59, x + 76, y + 60, cfg.accentArgb);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my, int x, int y) {
        drawCategoryAt(g, mx, my, x, y, 0, "All Modules", null);
        drawCategoryAt(g, mx, my, x, y, 1, "Combat", Module.Category.COMBAT);
        drawCategoryAt(g, mx, my, x, y, 2, "Visual", Module.Category.VISUAL);
        drawCategoryAt(g, mx, my, x, y, 3, "Player", Module.Category.PLAYER);
        drawCategoryAt(g, mx, my, x, y, 4, "Movement", Module.Category.MOVEMENT);
        drawCategoryAt(g, mx, my, x, y, 5, "HUD", Module.Category.HUD);
        drawCategoryAt(g, mx, my, x, y, 6, "World", Module.Category.WORLD);
        drawCategoryAt(g, mx, my, x, y, 7, "Misc", Module.Category.MISC);

        var cfg = TopkaClient.CONFIG.get();
        g.text(font, "MENU KEY", x + 18, y + 334, 0xFF646476, false);
        g.fill(x + 15, y + 351, x + 133, y + 381, 0xFF1B1B24);
        g.centeredText(font, TopkaClient.openMenuKey(), x + 74, y + 361, cfg.accentArgb);
        g.text(font, "Same key opens/closes.", x + 18, y + 391, 0xFF686879, false);
        g.text(font, "Rebind in Controls.", x + 18, y + 406, 0xFF686879, false);

        g.text(font, "TIP", x + 18, y + 448, 0xFF646476, false);
        g.text(font, "Right-click any card", x + 18, y + 464, 0xFF686879, false);
        g.text(font, "for full settings.", x + 18, y + 478, 0xFF686879, false);
    }

    private void drawCategoryAt(GuiGraphicsExtractor g, int mx, int my, int x, int y, int index, String label, Module.Category category) {
        drawCategory(g, mx, my, x + 15, y + 86 + index * 29, label, category);
    }

    private void drawCategory(GuiGraphicsExtractor g, int mx, int my, int x, int y, String label, Module.Category category) {
        boolean selected = selectedCategory == category;
        boolean hover = inside(mx, my, x, y, 118, 24);
        int color = selected ? TopkaClient.CONFIG.get().accentArgb : 0xFFB7B7C4;
        g.fill(x, y, x + 118, y + 24, selected ? 0xFF282435 : (hover ? 0xFF20202A : 0x00101010));
        if (selected) g.fill(x, y, x + 3, y + 24, color);
        g.text(font, label, x + 10, y + 8, color, selected);
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
        if (description.length() > 38) description = description.substring(0, 37) + "…";
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
                int cardX = x + 172 + column * (CARD_W + CARD_GAP_X);
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
        if (inside(mouseX, mouseY, x + 160, y + 70, 560, 330)) {
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
