package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
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

/**
 * Cosmetic wardrobe using the exact same visual system and coordinate model as
 * the main Mod Menu screen. Sidebar/category and bottom controls use the same
 * real invisible Button widgets as TopkaScreen; cosmetic cards use the same
 * direct mouse hit testing as module cards.
 */
public final class CosmeticsScreen extends Screen {
    private static final int PANEL_W = 730;
    private static final int PANEL_H = 520;
    private static final int SIDEBAR_W = 148;
    private static final int CARD_W = 264;
    private static final int CARD_H = 70;
    private static final int CARD_GAP_X = 12;
    private static final int CARD_GAP_Y = 10;

    private enum Section {
        WINGS("Wings"),
        HELMETS("Helmets"),
        CHESTPLATES("Chestplates"),
        LEGGINGS("Leggings"),
        BOOTS("Boots");

        final String label;
        Section(String label) { this.label = label; }
    }

    private record CosmeticEntry(String name, String description, int style) { }

    private static final List<CosmeticEntry> WINGS = List.of(
            new CosmeticEntry("Angel Wings", "Layered feather wings.", 0),
            new CosmeticEntry("Demon Wings", "Dark membrane wings.", 1),
            new CosmeticEntry("Crystal Wings", "Sharp faceted wings.", 2),
            new CosmeticEntry("Dragon Wings", "Scaled dragon profile.", 3),
            new CosmeticEntry("Tech Wings", "Mechanical neon profile.", 4)
    );

    private static final List<CosmeticEntry> ARMOR = List.of(
            new CosmeticEntry("None", "Hide this armor slot.", 0),
            new CosmeticEntry("Valkyrie", "Silver and gold cosmetic armor.", 1),
            new CosmeticEntry("Demonic", "Black and crimson cosmetic armor.", 2)
    );

    private final Screen parent;
    private Section section = Section.WINGS;
    private EditBox searchBox;
    private String searchQuery = "";

    public CosmeticsScreen(Screen parent) {
        super(Component.literal("Cosmetics"));
        this.parent = parent;
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

    private int sx(int x) {
        return panelX() + Math.round(x * scale());
    }

    private int sy(int y) {
        return panelY() + Math.round(y * scale());
    }

    private int sw(int w) {
        return Math.max(1, Math.round(w * scale()));
    }

    private int localMouseX(double x) {
        return Math.round((float) ((x - panelX()) / scale()));
    }

    private int localMouseY(double y) {
        return Math.round((float) ((y - panelY()) / scale()));
    }

    @Override
    protected void init() {
        addSectionTarget(0, Section.WINGS);
        addSectionTarget(1, Section.HELMETS);
        addSectionTarget(2, Section.CHESTPLATES);
        addSectionTarget(3, Section.LEGGINGS);
        addSectionTarget(4, Section.BOOTS);

        EditBox previous = searchBox;
        searchBox = new EditBox(
                font,
                sx(184),
                sy(35),
                sw(316),
                Math.max(14, sw(17)),
                previous,
                UiFont.text("Search cosmetics")
        );
        searchBox.setBordered(false);
        searchBox.setTextShadow(false);
        searchBox.setMaxLength(48);
        searchBox.setHint(UiFont.text("Search cosmetics..."));
        searchBox.addFormatter((text, index) -> FormattedCharSequence.forward(text, UiFont.STYLE));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> searchQuery = value);
        addRenderableWidget(searchBox);

        addClickTarget(172, 426, 104, 30, this::onClose);
        addClickTarget(284, 426, 118, 30, this::toggleCurrentModule);
        addClickTarget(410, 426, 118, 30, this::equipWholeSet);
        addClickTarget(536, 426, 104, 30, this::clearArmor);
        addClickTarget(648, 426, 56, 30, this::resetColors);
    }

    private void addSectionTarget(int index, Section value) {
        addClickTarget(15, 86 + index * 29, 118, 24, () -> {
            section = value;
            searchQuery = "";
            if (searchBox != null) searchBox.setValue("");
        });
    }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run())
                .pos(sx(x), sy(y))
                .size(sw(w), sw(h))
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);

        g.fill(0, 0, width, height, 0x92000000);

        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        g.pose().scale(scale(), scale());

        g.fill(0, 0, PANEL_W, PANEL_H, panelColor());
        g.fill(0, 0, SIDEBAR_W, PANEL_H, sidebarColor());
        g.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, PANEL_H, dividerColor());

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
        drawSidebar(g, mx, my);

        g.text(font, UiFont.text(section.label.toUpperCase()), 172, 11, 0xFFF7F7FB, true);
        g.text(font, UiFont.text(activeSummary()), 552, 11, Theme.accent(), true);

        g.fill(168, 28, 522, 58, searchBackground());
        g.fill(168, 57, 522, 58, cfg.menuStyle == 2 ? 0x44777788 : Theme.accent());
        g.text(font, UiFont.text("⌕"), 174, 38, Theme.accent(), false);

        List<CosmeticEntry> entries = filteredEntries();
        if (entries.isEmpty()) {
            g.centeredText(font, UiFont.text("No cosmetics match "" + searchQuery + """), 440, 224, 0xFF777788);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                int column = i % 2;
                int row = i / 2;
                int x = 172 + column * (CARD_W + CARD_GAP_X);
                int y = 82 + row * (CARD_H + CARD_GAP_Y);
                drawCard(g, mx, my, x, y, entries.get(i));
            }
        }

        drawBottomButton(g, mx, my, 172, 426, 104, "Back");
        drawBottomButton(g, mx, my, 284, 426, 118, moduleEnabled() ? "Disable" : "Enable");
        drawBottomButton(g, mx, my, 410, 426, 118, "Whole Set");
        drawBottomButton(g, mx, my, 536, 426, 104, "Clear Armor");
        drawBottomButton(g, mx, my, 648, 426, 56, "Reset");

        g.text(font, UiFont.text("Left click selects • Search stays focused • Armor slots are independent"), 172, 477, 0xFF686879, false);
        g.text(font, UiFont.text("Cosmetics 0.12.4"), 616, 477, 0xFF686879, true);

        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawBrand(GuiGraphicsExtractor g) {
        g.text(font, UiFont.text("MOD"), 18, 18, 0xFFFFFFFF, true);
        g.text(font, UiFont.text("COSMETICS"), 50, 18, Theme.accent(), true);
        g.text(font, UiFont.text(styleName()), 18, 37, 0xFF6E6E80, false);
        g.fill(18, 59, 128, 60, 0xFF2B2B36);
        g.fill(18, 59, 76, 60, Theme.accent());
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        drawSection(g, mx, my, 0, Section.WINGS);
        drawSection(g, mx, my, 1, Section.HELMETS);
        drawSection(g, mx, my, 2, Section.CHESTPLATES);
        drawSection(g, mx, my, 3, Section.LEGGINGS);
        drawSection(g, mx, my, 4, Section.BOOTS);

        var c = TopkaClient.CONFIG.get();
        g.text(font, UiFont.text("CURRENT"), 18, 260, 0xFF646476, false);
        g.text(font, UiFont.text("Wing: " + wingName(c.wingsStyle)), 18, 278, 0xFFB7B7C4, false);
        g.text(font, UiFont.text("Helmet: " + armorName(c.armorHelmetStyle)), 18, 294, 0xFFB7B7C4, false);
        g.text(font, UiFont.text("Chest: " + armorName(c.armorChestStyle)), 18, 310, 0xFFB7B7C4, false);
        g.text(font, UiFont.text("Legs: " + armorName(c.armorLeggingsStyle)), 18, 326, 0xFFB7B7C4, false);
        g.text(font, UiFont.text("Boots: " + armorName(c.armorBootsStyle)), 18, 342, 0xFFB7B7C4, false);

        g.text(font, UiFont.text("TIP"), 18, 448, 0xFF646476, false);
        g.text(font, UiFont.text("Mix Valkyrie + Demonic"), 18, 464, 0xFF686879, false);
        g.text(font, UiFont.text("pieces independently."), 18, 478, 0xFF686879, false);
    }

    private void drawSection(GuiGraphicsExtractor g, int mx, int my, int index, Section value) {
        int x = 15;
        int y = 86 + index * 29;
        boolean selected = section == value;
        boolean hover = inside(mx, my, x, y, 118, 24);
        int color = selected ? Theme.accent() : 0xFFB7B7C4;
        g.fill(x, y, x + 118, y + 24, selected ? selectedCategoryColor() : (hover ? hoverColor() : 0x00101010));
        if (selected) g.fill(x, y, x + 3, y + 24, color);
        g.text(font, UiFont.text(value.label), x + 10, y + 8, color, selected);
    }

    private void drawCard(GuiGraphicsExtractor g, int mx, int my, int x, int y, CosmeticEntry entry) {
        boolean selected = selectedStyle() == entry.style;
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);

        g.fill(x, y, x + CARD_W, y + CARD_H, cardColor(selected, hover));
        g.fill(x, y, x + 3, y + CARD_H, selected ? Theme.accent() : 0xFF383846);

        int iconColor = previewColor(entry.style);
        g.fill(x + 12, y + 12, x + 52, y + 52, selected ? Theme.withAlpha(Theme.accent(), 44) : 0x332D2D38);
        drawIcon(g, x + 18, y + 18, entry.style, iconColor);

        g.text(font, UiFont.text(entry.name), x + 64, y + 13, TopkaClient.CONFIG.get().textArgb, true);
        g.text(font, UiFont.trim(font, entry.description, 140), x + 64, y + 32, 0xFF858596, false);

        int pillX = x + CARD_W - 55;
        g.fill(pillX, y + 13, x + CARD_W - 10, y + 31,
                selected ? Theme.withAlpha(Theme.accent(), 52) : 0x334F4F5A);
        g.centeredText(font, UiFont.text(selected ? "ON" : "SELECT"), pillX + 22, y + 18,
                selected ? Theme.accent() : 0xFF858592);
    }

    private void drawIcon(GuiGraphicsExtractor g, int x, int y, int style, int color) {
        if (section == Section.WINGS) {
            g.fill(x + 15, y + 4, x + 19, y + 30, 0xFFE6E8F0);
            g.fill(x, y + 7, x + 15, y + 13, color);
            g.fill(x - 3, y + 15, x + 15, y + 21, color);
            g.fill(x + 19, y + 7, x + 34, y + 13, color);
            g.fill(x + 19, y + 15, x + 37, y + 21, color);
            return;
        }

        if (style == 0) {
            g.fill(x + 4, y + 15, x + 34, y + 19, 0xFF696978);
            return;
        }

        if (section == Section.HELMETS) {
            g.fill(x + 7, y + 5, x + 31, y + 23, color);
            g.fill(x + 3, y + 12, x + 35, y + 18, color);
        } else if (section == Section.CHESTPLATES) {
            g.fill(x + 8, y + 5, x + 30, y + 31, color);
            g.fill(x + 1, y + 8, x + 8, y + 29, color);
            g.fill(x + 30, y + 8, x + 37, y + 29, color);
        } else if (section == Section.LEGGINGS) {
            g.fill(x + 7, y + 5, x + 31, y + 15, color);
            g.fill(x + 7, y + 15, x + 16, y + 34, color);
            g.fill(x + 22, y + 15, x + 31, y + 34, color);
        } else {
            g.fill(x + 5, y + 15, x + 16, y + 34, color);
            g.fill(x + 22, y + 15, x + 33, y + 34, color);
        }
    }

    private int previewColor(int style) {
        if (section == Section.WINGS) {
            return switch (style) {
                case 1 -> 0xFFC32D3D;
                case 2 -> 0xFF71D9F7;
                case 3 -> 0xFF7243C7;
                case 4 -> 0xFF38D6D2;
                default -> 0xFFF2F2F6;
            };
        }
        return style == 2 ? 0xFF9B2135 : 0xFFE5C45C;
    }

    private List<CosmeticEntry> filteredEntries() {
        List<CosmeticEntry> source = section == Section.WINGS ? WINGS : ARMOR;
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return source;

        List<CosmeticEntry> result = new ArrayList<>();
        for (CosmeticEntry e : source) {
            if ((e.name + " " + e.description).toLowerCase(Locale.ROOT).contains(query)) {
                result.add(e);
            }
        }
        return result;
    }

    private int selectedStyle() {
        var c = TopkaClient.CONFIG.get();
        return switch (section) {
            case WINGS -> c.wingsStyle;
            case HELMETS -> c.armorHelmetStyle;
            case CHESTPLATES -> c.armorChestStyle;
            case LEGGINGS -> c.armorLeggingsStyle;
            case BOOTS -> c.armorBootsStyle;
        };
    }

    private void select(CosmeticEntry entry) {
        var c = TopkaClient.CONFIG.get();

        switch (section) {
            case WINGS -> {
                c.wingsStyle = entry.style;
                TopkaClient.MODULES.byId("wings").setEnabled(true);
            }
            case HELMETS -> c.armorHelmetStyle = entry.style;
            case CHESTPLATES -> c.armorChestStyle = entry.style;
            case LEGGINGS -> c.armorLeggingsStyle = entry.style;
            case BOOTS -> c.armorBootsStyle = entry.style;
        }

        if (section != Section.WINGS) {
            TopkaClient.MODULES.byId("armor_cosmetic").setEnabled(
                    c.armorHelmetStyle != 0
                            || c.armorChestStyle != 0
                            || c.armorLeggingsStyle != 0
                            || c.armorBootsStyle != 0
            );
        }

        TopkaClient.CONFIG.save();
    }

    private void toggleCurrentModule() {
        if (section == Section.WINGS) {
            TopkaClient.MODULES.byId("wings").toggle();
        } else {
            TopkaClient.MODULES.byId("armor_cosmetic").toggle();
        }
        TopkaClient.CONFIG.save();
    }

    private void equipWholeSet() {
        var c = TopkaClient.CONFIG.get();
        int style = section == Section.WINGS ? 1 : Math.max(1, selectedStyle());
        if (section == Section.WINGS) {
            c.wingsStyle = style;
            TopkaClient.MODULES.byId("wings").setEnabled(true);
        } else {
            c.armorHelmetStyle = style;
            c.armorChestStyle = style;
            c.armorLeggingsStyle = style;
            c.armorBootsStyle = style;
            TopkaClient.MODULES.byId("armor_cosmetic").setEnabled(true);
        }
        TopkaClient.CONFIG.save();
    }

    private void clearArmor() {
        var c = TopkaClient.CONFIG.get();
        c.armorHelmetStyle = 0;
        c.armorChestStyle = 0;
        c.armorLeggingsStyle = 0;
        c.armorBootsStyle = 0;
        TopkaClient.MODULES.byId("armor_cosmetic").setEnabled(false);
        TopkaClient.CONFIG.save();
    }

    private void resetColors() {
        var c = TopkaClient.CONFIG.get();
        c.wingsPrimaryColorArgb = 0xFFFFFFFF;
        c.wingsSecondaryColorArgb = 0xFFC8CDD8;
        c.armorCosmeticTintArgb = 0xFFFFFFFF;
        TopkaClient.CONFIG.save();
    }

    private boolean moduleEnabled() {
        return section == Section.WINGS
                ? TopkaClient.MODULES.byId("wings").enabled()
                : TopkaClient.MODULES.byId("armor_cosmetic").enabled();
    }

    private String activeSummary() {
        if (section == Section.WINGS) return moduleEnabled() ? "wings on" : "wings off";
        var c = TopkaClient.CONFIG.get();
        int active = 0;
        if (c.armorHelmetStyle != 0) active++;
        if (c.armorChestStyle != 0) active++;
        if (c.armorLeggingsStyle != 0) active++;
        if (c.armorBootsStyle != 0) active++;
        return active + "/4 armor slots";
    }

    private String wingName(int style) {
        return switch (style) {
            case 1 -> "Demon";
            case 2 -> "Crystal";
            case 3 -> "Dragon";
            case 4 -> "Tech";
            default -> "Angel";
        };
    }

    private String armorName(int style) {
        return switch (style) {
            case 1 -> "Valkyrie";
            case 2 -> "Demonic";
            default -> "None";
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = localMouseX(click.x());
        int my = localMouseY(click.y());

        if (click.button() == 0) {
            List<CosmeticEntry> entries = filteredEntries();
            for (int i = 0; i < entries.size(); i++) {
                int column = i % 2;
                int row = i / 2;
                int x = 172 + column * (CARD_W + CARD_GAP_X);
                int y = 82 + row * (CARD_H + CARD_GAP_Y);
                if (inside(mx, my, x, y, CARD_W, CARD_H)) {
                    select(entries.get(i));
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
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

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
            case 1 -> 0xFA07080D;
            case 2 -> 0xB9121822;
            case 3 -> 0xFF0B0B0D;
            default -> 0xFF111116;
        };
    }

    private int dividerColor() {
        return TopkaClient.CONFIG.get().menuStyle == 2 ? 0x44777788 : 0xFF2B2B36;
    }

    private int searchBackground() {
        return TopkaClient.CONFIG.get().menuStyle == 2 ? 0x551A2230 : 0xFF1A1A23;
    }

    private int hoverColor() {
        return TopkaClient.CONFIG.get().menuStyle == 2 ? 0x553A4658 : 0xFF292936;
    }

    private int buttonColor() {
        return TopkaClient.CONFIG.get().menuStyle == 2 ? 0x55202A38 : 0xFF20202A;
    }

    private int selectedCategoryColor() {
        return Theme.withAlpha(Theme.accent(), TopkaClient.CONFIG.get().menuStyle == 2 ? 50 : 38);
    }

    private int cardColor(boolean selected, boolean hover) {
        if (selected) return TopkaClient.CONFIG.get().menuStyle == 2
                ? Theme.withAlpha(Theme.accent(), 36)
                : 0xFF232231;
        if (hover) return hoverColor();
        return TopkaClient.CONFIG.get().menuStyle == 2 ? 0x44171D28 : 0xFF181820;
    }

    private void drawBottomButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = inside(mx, my, x, y, w, 30);
        g.fill(x, y, x + w, y + 30, hover ? hoverColor() : buttonColor());
        g.fill(x, y + 29, x + w, y + 30, Theme.accent());
        g.centeredText(font, UiFont.text(label), x + w / 2, y + 10, 0xFFEDEDF4);
    }
}
