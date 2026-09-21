package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import com.koqps.topka.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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

    private float scale() {
        return Math.clamp(TopkaClient.CONFIG.get().menuScale, 0.70F, 1.35F);
    }

    private int scaledPanelWidth() {
        return Math.round(PANEL_W * scale());
    }

    private int scaledPanelHeight() {
        return Math.round(PANEL_H * scale());
    }

    private int panelX() {
        return (width - scaledPanelWidth()) / 2 + TopkaClient.CONFIG.get().menuOffsetX;
    }

    private int panelY() {
        return (height - scaledPanelHeight()) / 2 + TopkaClient.CONFIG.get().menuOffsetY;
    }

    private int sx(int localX) {
        return panelX() + Math.round(localX * scale());
    }

    private int sy(int localY) {
        return panelY() + Math.round(localY * scale());
    }

    private int sw(int localWidth) {
        return Math.max(1, Math.round(localWidth * scale()));
    }

    private int localMouseX(double mouseX) {
        return Math.round((float) ((mouseX - panelX()) / scale()));
    }

    private int localMouseY(double mouseY) {
        return Math.round((float) ((mouseY - panelY()) / scale()));
    }

    @Override
    protected void init() {
        addCategoryTarget(0, null);
        addCategoryTarget(1, Module.Category.COMBAT);
        addCategoryTarget(2, Module.Category.VISUAL);
        addCategoryTarget(3, Module.Category.PLAYER);
        addCategoryTarget(4, Module.Category.MOVEMENT);
        addCategoryTarget(5, Module.Category.HUD);
        addCategoryTarget(6, Module.Category.WORLD);
        addCategoryTarget(7, Module.Category.MISC);

        EditBox previousSearch = searchBox;
        searchBox = new EditBox(
                font,
                sx(184),
                sy(35),
                sw(316),
                Math.max(14, sw(17)),
                previousSearch,
                UiFont.text("Search modules")
        );
        searchBox.setBordered(false);
        searchBox.setTextShadow(false);
        searchBox.setMaxLength(48);
        searchBox.setHint(UiFont.text("Search modules..."));
        searchBox.addFormatter((text, index) -> FormattedCharSequence.forward(text, UiFont.STYLE));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            if (!value.equals(searchQuery)) {
                searchQuery = value;
                scrollRow = 0;
            }
        });
        addRenderableWidget(searchBox);

        addClickTarget(172, 426, 104, 30, () -> minecraft.gui.setScreen(new ThemeScreen(this)));
        addClickTarget(284, 426, 118, 30, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        addClickTarget(410, 426, 118, 30, () -> minecraft.gui.setScreen(new CosmeticsScreen(this)));
        addClickTarget(536, 426, 104, 30, () -> minecraft.gui.setScreen(new WaypointScreen(this)));
        addClickTarget(648, 426, 56, 30, this::disableAll);
    }

    private void addCategoryTarget(int index, Module.Category category) {
        addClickTarget(15, 86 + index * 29, 118, 24, () -> setCategory(category));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int localMx = localMouseX(mouseX);
        int localMy = localMouseY(mouseY);
        int x = panelX();
        int y = panelY();
        float scale = scale();

        g.fill(0, 0, width, height, 0x92000000);

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);

        int panel = panelColor();
        int sidebar = sidebarColor();
        int divider = dividerColor();

        g.fill(0, 0, PANEL_W, PANEL_H, panel);
        g.fill(0, 0, SIDEBAR_W, PANEL_H, sidebar);
        g.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, PANEL_H, divider);

        if (cfg.menuStyle == 1) {
            g.fill(0, 0, PANEL_W, 2, Theme.accent());
            g.fill(0, PANEL_H - 2, PANEL_W, PANEL_H, Theme.secondary());
        } else if (cfg.menuStyle == 2) {
            g.fill(0, 0, PANEL_W, 1, 0x88FFFFFF);
            g.fill(0, 0, 1, PANEL_H, 0x44FFFFFF);
        } else {
            g.fill(0, 0, PANEL_W, 3, Theme.accent());
        }

        drawBrand(g);
        drawSidebar(g, localMx, localMy);

        g.text(font, UiFont.text(selectedCategory == null ? "MODULE LIBRARY" : selectedCategory.name()), 172, 11, 0xFFF7F7FB, true);
        g.text(font, UiFont.text(enabledCount() + " active"), 552, 11, Theme.accent(), true);

        // Search is visually part of the client header. The EditBox itself is
        // borderless and sits inside this surface.
        g.fill(168, 28, 522, 58, searchBackground());
        g.fill(168, 57, 522, 58, cfg.menuStyle == 2 ? 0x44777788 : Theme.accent());
        g.text(font, UiFont.text("⌕"), 174, 38, Theme.accent(), false);

        List<Module> filtered = filteredModules();
        clampScroll(filtered);
        int first = scrollRow * 2;
        int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);

        if (filtered.isEmpty()) {
            g.centeredText(font, UiFont.text("No modules match \"" + searchQuery + "\""), 440, 224, 0xFF777788);
        } else {
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = 172 + column * (CARD_W + CARD_GAP_X);
                int cardY = 82 + row * (CARD_H + CARD_GAP_Y);
                drawModuleCard(g, localMx, localMy, cardX, cardY, filtered.get(i));
            }
        }

        int rows = (filtered.size() + 1) / 2;
        if (rows > VISIBLE_ROWS) {
            int trackX = 716;
            int trackY = 82;
            int trackH = VISIBLE_ROWS * (CARD_H + CARD_GAP_Y) - CARD_GAP_Y;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFF24242E);
            int thumbH = Math.max(24, trackH * VISIBLE_ROWS / rows);
            int maxScroll = Math.max(1, rows - VISIBLE_ROWS);
            int thumbY = trackY + (trackH - thumbH) * scrollRow / maxScroll;
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, Theme.accent());
        }

        drawBottomButton(g, localMx, localMy, 172, 426, 104, "Theme");
        drawBottomButton(g, localMx, localMy, 284, 426, 118, "HUD Workspace");
        drawBottomButton(g, localMx, localMy, 410, 426, 118, "Cosmetics");
        drawBottomButton(g, localMx, localMy, 536, 426, 104, "Waypoints");
        drawBottomButton(g, localMx, localMy, 648, 426, 56, "Off");

        g.text(font, UiFont.text("Left click toggles • Right click settings • Mouse wheel scrolls"), 172, 477, 0xFF686879, false);
        g.text(font, UiFont.text("Mod Menu 0.14.2"), 618, 477, 0xFF686879, true);

        g.pose().popMatrix();

        // Search is a real renderable widget so its cursor, focus and typing work
        // correctly. Do not manually draw it a second time.
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawBrand(GuiGraphicsExtractor g) {
        g.text(font, UiFont.text("MOD"), 18, 18, 0xFFFFFFFF, true);
        g.text(font, UiFont.text("MENU"), 50, 18, Theme.accent(), true);
        g.text(font, UiFont.text(styleName()), 18, 37, 0xFF6E6E80, false);
        g.fill(18, 59, 128, 60, 0xFF2B2B36);
        g.fill(18, 59, 76, 60, Theme.accent());
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        drawCategoryAt(g, mx, my, 0, "All Modules", null);
        drawCategoryAt(g, mx, my, 1, "Combat", Module.Category.COMBAT);
        drawCategoryAt(g, mx, my, 2, "Visual", Module.Category.VISUAL);
        drawCategoryAt(g, mx, my, 3, "Player", Module.Category.PLAYER);
        drawCategoryAt(g, mx, my, 4, "Movement", Module.Category.MOVEMENT);
        drawCategoryAt(g, mx, my, 5, "HUD", Module.Category.HUD);
        drawCategoryAt(g, mx, my, 6, "World", Module.Category.WORLD);
        drawCategoryAt(g, mx, my, 7, "Misc", Module.Category.MISC);

        g.text(font, UiFont.text("MENU KEY"), 18, 334, 0xFF646476, false);
        g.fill(15, 351, 133, 381, sidebarButtonColor());
        g.centeredText(font, UiFont.text(TopkaClient.openMenuKey().getString()), 74, 361, Theme.accent());
        g.text(font, UiFont.text("Same key opens/closes."), 18, 391, 0xFF686879, false);
        g.text(font, UiFont.text("Rebind in Controls."), 18, 406, 0xFF686879, false);

        g.text(font, UiFont.text("TIP"), 18, 448, 0xFF646476, false);
        g.text(font, UiFont.text("Right-click any card"), 18, 464, 0xFF686879, false);
        g.text(font, UiFont.text("for full settings."), 18, 478, 0xFF686879, false);
    }

    private void drawCategoryAt(GuiGraphicsExtractor g, int mx, int my, int index, String label, Module.Category category) {
        drawCategory(g, mx, my, 15, 86 + index * 29, label, category);
    }

    private void drawCategory(GuiGraphicsExtractor g, int mx, int my, int x, int y, String label, Module.Category category) {
        boolean selected = selectedCategory == category;
        boolean hover = inside(mx, my, x, y, 118, 24);
        int color = selected ? Theme.accent() : 0xFFB7B7C4;
        g.fill(x, y, x + 118, y + 24, selected ? selectedCategoryColor() : (hover ? hoverColor() : 0x00101010));
        if (selected) g.fill(x, y, x + 3, y + 24, color);
        g.text(font, UiFont.text(label), x + 10, y + 8, color, selected);
    }

    private void drawModuleCard(GuiGraphicsExtractor g, int mx, int my, int x, int y, Module module) {
        var cfg = TopkaClient.CONFIG.get();
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        int background = cardColor(module.enabled(), hover);

        g.fill(x, y, x + CARD_W, y + CARD_H, background);
        if (cfg.menuStyle != 3) {
            g.fill(x, y, x + 3, y + CARD_H, module.enabled() ? Theme.accent() : 0xFF383846);
        } else if (module.enabled()) {
            g.fill(x, y + CARD_H - 2, x + CARD_W, y + CARD_H, Theme.accent());
        }

        g.fill(x + 12, y + 11, x + 38, y + 37, module.enabled() ? Theme.withAlpha(Theme.accent(), 54) : 0x332D2D38);
        g.centeredText(font, UiFont.text(module.icon()), x + 25, y + 20, module.enabled() ? Theme.accent() : 0xFF777789);

        g.text(font, UiFont.text(module.name()), x + 48, y + 10, cfg.textArgb, true);
        String description = module.description();
        g.text(font, UiFont.trim(font, description, 162), x + 48, y + 28, 0xFF858596, false);

        int pillX = x + CARD_W - 43;
        g.fill(pillX, y + 10, x + CARD_W - 10, y + 27, module.enabled() ? Theme.withAlpha(Theme.accent(), 52) : 0x334F4F5A);
        g.centeredText(font, UiFont.text(module.enabled() ? "ON" : "OFF"), pillX + 16, y + 15, module.enabled() ? Theme.accent() : 0xFF858592);

        if (hover) {
            g.text(font, UiFont.text("settings ›"), x + CARD_W - 67, y + 37, Theme.accent(), false);
        }
    }

    private void drawBottomButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = inside(mx, my, x, y, w, 30);
        g.fill(x, y, x + w, y + 30, hover ? hoverColor() : buttonColor());
        g.fill(x, y + 29, x + w, y + 30, Theme.accent());
        g.centeredText(font, UiFont.text(label), x + w / 2, y + 10, 0xFFEDEDF4);
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
        scrollRow = Math.clamp(scrollRow, 0, Math.max(0, rows - VISIBLE_ROWS));
    }

    private int enabledCount() {
        int enabled = 0;
        for (Module module : TopkaClient.MODULES.all()) if (module.enabled()) enabled++;
        return enabled;
    }

    private void setCategory(Module.Category category) {
        selectedCategory = category;
        scrollRow = 0;
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

    private void addClickTarget(int localX, int localY, int localW, int localH, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run())
                .pos(sx(localX), sy(localY))
                .size(sw(localW), sw(localH))
                .build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = localMouseX(click.x());
        int my = localMouseY(click.y());

        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            List<Module> filtered = filteredModules();
            clampScroll(filtered);
            int first = scrollRow * 2;
            int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = 172 + column * (CARD_W + CARD_GAP_X);
                int cardY = 82 + row * (CARD_H + CARD_GAP_Y);
                if (inside(mx, my, cardX, cardY, CARD_W, CARD_H)) {
                    minecraft.gui.setScreen(new ModuleSettingsScreen(this, filtered.get(i)));
                    return true;
                }
            }
        }

        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            List<Module> filtered = filteredModules();
            clampScroll(filtered);
            int first = scrollRow * 2;
            int last = Math.min(filtered.size(), first + VISIBLE_ROWS * 2);
            for (int i = first; i < last; i++) {
                int visibleIndex = i - first;
                int column = visibleIndex % 2;
                int row = visibleIndex / 2;
                int cardX = 172 + column * (CARD_W + CARD_GAP_X);
                int cardY = 82 + row * (CARD_H + CARD_GAP_Y);
                if (inside(mx, my, cardX, cardY, CARD_W, CARD_H)) {
                    Module selected = filtered.get(i);
                    if ("weapon_models".equals(selected.id())) {
                        minecraft.gui.setScreen(new ModuleSettingsScreen(this, selected));
                    } else if ("wings".equals(selected.id()) || "armor_cosmetic".equals(selected.id())) {
                        minecraft.gui.setScreen(new CosmeticsScreen(this));
                    } else {
                        selected.toggle();
                        TopkaClient.CONFIG.save();
                    }
                    return true;
                }
            }
        }

        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT
                && inside(mx, my, SIDEBAR_W, 0, PANEL_W - SIDEBAR_W, 28)) {
            draggingWindow = true;
            dragOffsetX = click.x() - panelX();
            dragOffsetY = click.y() - panelY();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (!draggingWindow) return super.mouseDragged(click, dx, dy);

        int panelW = scaledPanelWidth();
        int panelH = scaledPanelHeight();
        int targetX = (int) (click.x() - dragOffsetX);
        int targetY = (int) (click.y() - dragOffsetY);

        int centeredX = (width - panelW) / 2;
        int centeredY = (height - panelH) / 2;
        int minOffsetX = -centeredX;
        int maxOffsetX = Math.max(minOffsetX, width - panelW - centeredX);
        int minOffsetY = -centeredY;
        int maxOffsetY = Math.max(minOffsetY, height - panelH - centeredY);

        TopkaClient.CONFIG.get().menuOffsetX = Math.clamp(targetX - centeredX, minOffsetX, maxOffsetX);
        TopkaClient.CONFIG.get().menuOffsetY = Math.clamp(targetY - centeredY, minOffsetY, maxOffsetY);
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
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);
        if (inside(mx, my, 160, 70, 560, 330)) {
            List<Module> filtered = filteredModules();
            int rows = (filtered.size() + 1) / 2;
            int max = Math.max(0, rows - VISIBLE_ROWS);
            int before = scrollRow;
            if (scrollY < 0) scrollRow = Math.min(max, scrollRow + 1);
            if (scrollY > 0) scrollRow = Math.max(0, scrollRow - 1);
            if (before != scrollRow) {
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

    private String styleName() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> "NEON CLIENT";
            case 2 -> "GLASS CLIENT";
            case 3 -> "MINIMAL CLIENT";
            default -> "PRO CLIENT";
        };
    }

    private int panelColor() {
        var c = TopkaClient.CONFIG.get();
        return switch (c.menuStyle) {
            case 1 -> 0xF5090B12;
            case 2 -> (Math.clamp((c.panelArgb >>> 24) & 0xFF, 130, 220) << 24) | 0x00131824;
            case 3 -> 0xFA101014;
            default -> c.panelArgb;
        };
    }

    private int sidebarColor() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> 0xFF0C1019;
            case 2 -> 0xD9161D27;
            case 3 -> 0xFF0B0B0E;
            default -> TopkaClient.CONFIG.get().sidebarArgb;
        };
    }

    private int dividerColor() {
        return TopkaClient.CONFIG.get().menuStyle == 1 ? Theme.withAlpha(Theme.secondary(), 96) : 0xFF292934;
    }

    private int searchBackground() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> Theme.withAlpha(Theme.accent(), 24);
            case 2 -> 0x88242C38;
            case 3 -> 0xFF17171B;
            default -> 0xFF181820;
        };
    }

    private int selectedCategoryColor() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> Theme.withAlpha(Theme.accent(), 48);
            case 2 -> 0x664A5568;
            case 3 -> 0xFF1B1B21;
            default -> 0xFF282435;
        };
    }

    private int hoverColor() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> 0xFF202735;
            case 2 -> 0xAA303846;
            case 3 -> 0xFF222226;
            default -> 0xFF30303C;
        };
    }

    private int buttonColor() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 1 -> 0xFF151C27;
            case 2 -> 0x99303949;
            case 3 -> 0xFF18181C;
            default -> 0xFF20202A;
        };
    }

    private int sidebarButtonColor() {
        return switch (TopkaClient.CONFIG.get().menuStyle) {
            case 2 -> 0x88242C38;
            default -> 0xFF1B1B24;
        };
    }

    private int cardColor(boolean enabled, boolean hover) {
        int style = TopkaClient.CONFIG.get().menuStyle;
        if (style == 1) {
            if (hover) return enabled ? 0xFF202A38 : 0xFF18202A;
            return enabled ? 0xFF17212D : 0xFF111720;
        }
        if (style == 2) {
            if (hover) return enabled ? 0xCC31394A : 0xBB2B323F;
            return enabled ? 0xAA252D3B : 0x99202731;
        }
        if (style == 3) {
            if (hover) return enabled ? 0xFF24242A : 0xFF202024;
            return enabled ? 0xFF1B1B20 : 0xFF151518;
        }
        if (hover) return enabled ? 0xFF29293A : 0xFF22222C;
        return enabled ? 0xFF20202B : 0xFF17171F;
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
