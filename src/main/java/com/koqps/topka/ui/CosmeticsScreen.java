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
 * Laby-style player cosmetics wardrobe. The layout intentionally mirrors the
 * familiar three-part workflow: category browser, cosmetic cards, live preview.
 */
public final class CosmeticsScreen extends Screen {
    private static final int PANEL_W = 980;
    private static final int PANEL_H = 570;

    private static final int CARD_W = 136;
    private static final int CARD_H = 82;
    private static final int CARD_GAP = 10;
    private static final int GRID_X = 204;
    private static final int GRID_Y = 190;

    private static final int[] COLOR_PRESETS = {
            0xFFFFFFFF, 0xFF111318, 0xFFC62A21, 0xFF36D5E7,
            0xFF8B5CF6, 0xFFFFC928, 0xFF53E06F, 0xFFFF67CE
    };

    private enum Section {
        WINGS("Wings"),
        ARMOR("Armor"),
        HATS("Hats"),
        PETS("Pets"),
        SHOES("Shoes"),
        UNDERGLOW("Underglow");

        private final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private record CosmeticEntry(String name, String subtitle, int style) { }

    private static final List<CosmeticEntry> WINGS = List.of(
            new CosmeticEntry("Angel Wings", "Feathered", 0),
            new CosmeticEntry("Demon Wings", "Membrane", 1),
            new CosmeticEntry("Crystal Wings", "Faceted", 2),
            new CosmeticEntry("Dragon Wings", "Scaled", 3),
            new CosmeticEntry("Tech Wings", "Mechanical", 4),
            new CosmeticEntry("Wing Pack I", "Imported", 5),
            new CosmeticEntry("Wing Pack II", "Imported", 6),
            new CosmeticEntry("Wing Pack III", "Imported", 7),
            new CosmeticEntry("Wing Pack IV", "Imported", 8)
    );

    private static final List<CosmeticEntry> ARMOR = List.of(
            new CosmeticEntry("Valkyrie Armor", "Uploaded 3D set", 1),
            new CosmeticEntry("Demonic Armor", "Uploaded 3D set", 2)
    );

    private final Screen parent;
    private Section section = Section.WINGS;
    private EditBox searchBox;
    private String searchQuery = "";

    public CosmeticsScreen(Screen parent) {
        super(Component.literal("Cosmetics"));
        this.parent = parent;
    }

    private float uiScale() {
        float requested = Math.clamp(TopkaClient.CONFIG.get().menuScale, 0.70F, 1.20F);
        float fitX = (width - 20.0F) / PANEL_W;
        float fitY = (height - 20.0F) / PANEL_H;
        return Math.min(requested, Math.min(fitX, fitY));
    }

    private int panelX() {
        return (width - Math.round(PANEL_W * uiScale())) / 2;
    }

    private int panelY() {
        return (height - Math.round(PANEL_H * uiScale())) / 2;
    }

    private int sx(int x) {
        return panelX() + Math.round(x * uiScale());
    }

    private int sy(int y) {
        return panelY() + Math.round(y * uiScale());
    }

    private int ss(int value) {
        return Math.max(1, Math.round(value * uiScale()));
    }

    private int localX(double x) {
        return Math.round((float) ((x - panelX()) / uiScale()));
    }

    private int localY(double y) {
        return Math.round((float) ((y - panelY()) / uiScale()));
    }

    @Override
    protected void init() {
        EditBox previous = searchBox;
        searchBox = new EditBox(
                font,
                sx(204),
                sy(145),
                ss(438),
                ss(24),
                previous,
                UiFont.text("Search cosmetics")
        );
        searchBox.setBordered(false);
        searchBox.setTextShadow(false);
        searchBox.setMaxLength(48);
        searchBox.setHint(UiFont.text("Search..."));
        searchBox.addFormatter((text, index) -> FormattedCharSequence.forward(text, UiFont.STYLE));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> searchQuery = value);
        addRenderableWidget(searchBox);

        int y = 190;
        for (Section value : Section.values()) {
            final Section target = value;
            addWidget(Button.builder(Component.empty(), b -> section = target)
                    .pos(sx(42), sy(y))
                    .size(ss(136), ss(36))
                    .build());
            y += 44;
        }

        addWidget(Button.builder(Component.empty(), b -> onClose())
                .pos(sx(32), sy(24))
                .size(ss(90), ss(28))
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int mx = localX(mouseX);
        int my = localY(mouseY);

        g.fill(0, 0, width, height, 0xB5000000);

        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        g.pose().scale(uiScale(), uiScale());

        // Laby-style shell.
        g.fill(0, 0, PANEL_W, PANEL_H, 0xFF10152A);
        g.fill(0, 0, PANEL_W, 58, 0xFF0B1030);
        g.fill(0, 58, PANEL_W, 108, 0xFF1A2253);
        g.fill(0, 108, PANEL_W, PANEL_H, 0xFF131A37);

        g.text(font, UiFont.text("<  MOD MENU"), 32, 34, 0xFFF2F4FF, true);
        drawTopNav(g, 300, "MOD MENU", false);
        drawTopNav(g, 424, "ADDONS", false);
        drawTopNav(g, 536, "WIDGETS", false);
        drawTopNav(g, 664, "PLAYER", true);

        // Player page tabs.
        drawSubTab(g, 202, "COSMETICS", true);
        drawSubTab(g, 354, "EMOTES", false);
        drawSubTab(g, 486, "SKIN", false);

        // Browser.
        g.fill(26, 124, 660, 544, 0xB9222B5A);
        g.fill(36, 136, 650, 178, 0xAA121936);
        g.text(font, UiFont.text("COSMETICS"), 48, 151, 0xFFF5F6FF, true);
        g.text(font, UiFont.text("Choose a category and click a cosmetic card."), 318, 151, 0xFF9AA6D8, false);

        // Category rail.
        int cy = 190;
        for (Section value : Section.values()) {
            boolean selected = value == section;
            boolean hover = mx >= 42 && mx < 178 && my >= cy && my < cy + 36;
            g.fill(42, cy, 178, cy + 36,
                    selected ? 0xFF2C376B : (hover ? 0xFF222C59 : 0xB5151C3E));
            if (selected) g.fill(42, cy, 46, cy + 36, Theme.accent());
            g.text(font, UiFont.text(value.label), 56, cy + 13,
                    selected ? 0xFFFFFFFF : 0xFFB6BFDF, selected);
            cy += 44;
        }

        // Search surface.
        g.fill(198, 139, 648, 175, 0xFF0E1531);
        g.fill(198, 174, 648, 176, 0xFF46538E);

        List<CosmeticEntry> entries = filteredEntries();
        if (entries.isEmpty()) {
            String message = section == Section.WINGS || section == Section.ARMOR
                    ? "No cosmetics match your search."
                    : "Coming soon.";
            g.centeredText(font, UiFont.text(message), 420, 312, 0xFF98A3C9);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                int column = i % 3;
                int row = i / 3;
                int x = GRID_X + column * (CARD_W + CARD_GAP);
                int y = GRID_Y + row * (CARD_H + CARD_GAP);
                drawCard(g, mx, my, x, y, entries.get(i));
            }
        }

        drawPreviewPanel(g, mx, my);

        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawTopNav(GuiGraphicsExtractor g, int x, String label, boolean selected) {
        if (selected) {
            g.fill(x - 18, 56, x + 88, 59, 0xFF55C7FF);
        }
        g.text(font, UiFont.text(label), x, 28, selected ? 0xFF65D6FF : 0xFFD8DDF5, true);
    }

    private void drawSubTab(GuiGraphicsExtractor g, int x, String label, boolean selected) {
        g.fill(x - 16, 70, x + 116, 98, selected ? 0xFF11183A : 0x6634437A);
        g.centeredText(font, UiFont.text(label), x + 50, 80,
                selected ? 0xFFFFFFFF : 0xFFC0C8E8);
    }

    private void drawCard(
            GuiGraphicsExtractor g,
            int mx,
            int my,
            int x,
            int y,
            CosmeticEntry entry
    ) {
        boolean selected = isSelected(entry);
        boolean hover = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;

        g.fill(x, y, x + CARD_W, y + CARD_H,
                selected ? 0xFF354174 : (hover ? 0xFF2A3565 : 0xC51B244D));
        g.fill(x, y + CARD_H - 22, x + CARD_W, y + CARD_H, 0xDD0C1230);
        if (selected) {
            g.fill(x, y, x + CARD_W, y + 3, Theme.accent());
            g.fill(x + CARD_W - 18, y + CARD_H - 18, x + CARD_W - 8, y + CARD_H - 8, 0xFF67E36F);
        }

        drawCardIcon(g, x + 8, y + 8, entry);
        g.text(font, UiFont.text(entry.name), x + 8, y + CARD_H - 17, 0xFFF6F7FF, true);
        g.text(font, UiFont.text(entry.subtitle), x + 64, y + 14, 0xFFA8B2D5, false);
        g.text(font, UiFont.text(selected ? "ACTIVE" : "SELECT"), x + 64, y + 31,
                selected ? 0xFF63EA72 : 0xFF65D6FF, false);
    }

    private void drawCardIcon(GuiGraphicsExtractor g, int x, int y, CosmeticEntry entry) {
        g.fill(x, y, x + 48, y + 44, 0xAA10162F);

        if (section == Section.WINGS) {
            int primary = wingPreviewColor(entry.style);
            g.fill(x + 22, y + 8, x + 26, y + 38, 0xFFE9ECFF);
            g.fill(x + 5, y + 12, x + 22, y + 18, primary);
            g.fill(x + 1, y + 20, x + 22, y + 26, primary);
            g.fill(x + 7, y + 28, x + 22, y + 34, primary);
            g.fill(x + 26, y + 12, x + 43, y + 18, primary);
            g.fill(x + 26, y + 20, x + 47, y + 26, primary);
            g.fill(x + 26, y + 28, x + 41, y + 34, primary);
        } else {
            int color = entry.style == 2 ? 0xFF8E1825 : 0xFFE0B94F;
            g.fill(x + 17, y + 4, x + 31, y + 15, color);
            g.fill(x + 12, y + 16, x + 36, y + 32, color);
            g.fill(x + 7, y + 17, x + 12, y + 33, color);
            g.fill(x + 36, y + 17, x + 41, y + 33, color);
            g.fill(x + 15, y + 32, x + 22, y + 42, color);
            g.fill(x + 26, y + 32, x + 33, y + 42, color);
        }
    }

    private int wingPreviewColor(int style) {
        return switch (style) {
            case 1 -> 0xFFB62937;
            case 2 -> 0xFF7DD9FF;
            case 3 -> 0xFF6B2CC5;
            case 4 -> 0xFF42D4DB;
            case 5 -> 0xFFECECF4;
            case 6 -> 0xFFCF5E43;
            case 7 -> 0xFF8D7AF2;
            case 8 -> 0xFF5CD7CA;
            default -> 0xFFF3F3F6;
        };
    }

    private void drawPreviewPanel(GuiGraphicsExtractor g, int mx, int my) {
        var cfg = TopkaClient.CONFIG.get();

        g.fill(674, 124, 954, 544, 0xB51C2553);
        g.fill(686, 136, 942, 432, 0xFF0F1738);
        g.text(font, UiFont.text("PLAYER"), 696, 148, 0xFFFFFFFF, true);
        g.text(font, UiFont.text("PREVIEW"), 880, 148, 0xFF7281BB, false);

        int cx = 814;
        int top = 196;

        if (section == Section.WINGS && TopkaClient.MODULES.byId("wings").enabled()) {
            int wing = cfg.wingsStyle >= 5 ? cfg.importedWingTintArgb : cfg.wingsPrimaryColorArgb;
            drawPreviewWings(g, cx, top + 46, wing);
        }

        // Blocky Minecraft-style mannequin.
        g.fill(cx - 18, top, cx + 18, top + 34, 0xFFE8D1B0);
        g.fill(cx - 24, top + 36, cx + 24, top + 94, 0xFFF0F2F7);
        g.fill(cx - 38, top + 38, cx - 24, top + 90, 0xFFE8D1B0);
        g.fill(cx + 24, top + 38, cx + 38, top + 90, 0xFFE8D1B0);
        g.fill(cx - 22, top + 94, cx - 3, top + 160, 0xFF22242C);
        g.fill(cx + 3, top + 94, cx + 22, top + 160, 0xFF22242C);

        if (section == Section.ARMOR && TopkaClient.MODULES.byId("armor_cosmetic").enabled()) {
            int tint = cfg.armorCosmeticTintArgb;
            int armor = multiplyRgb(
                    cfg.armorCosmeticStyle == 2 ? 0xFF9F1E2E : 0xFFE0BA52,
                    tint
            );
            drawPreviewArmor(g, cx, top, armor, cfg.armorCosmeticStyle == 2);
        }

        String selected = selectedName();
        g.fill(686, 444, 942, 532, 0xFF111936);
        g.text(font, UiFont.text(selected), 700, 456, 0xFFFFFFFF, true);
        g.text(font, UiFont.text(section == Section.ARMOR ? "Armor cosmetic" : "Wing cosmetic"),
                700, 474, 0xFF98A3CA, false);

        boolean enabled = selectedModuleEnabled();
        g.fill(872, 454, 926, 478, enabled ? 0xFF275F3A : 0xFF3C4052);
        g.centeredText(font, UiFont.text(enabled ? "ON" : "OFF"), 899, 462,
                enabled ? 0xFF62EC7A : 0xFFADB4CF);

        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            int x = 700 + i * 27;
            g.fill(x, 496, x + 20, 516, COLOR_PRESETS[i]);
            if (mx >= x && mx < x + 20 && my >= 496 && my < 516) {
                g.fill(x - 2, 494, x + 22, 496, 0xFFFFFFFF);
                g.fill(x - 2, 516, x + 22, 518, 0xFFFFFFFF);
            }
        }
    }

    private void drawPreviewWings(GuiGraphicsExtractor g, int cx, int y, int color) {
        int c = (0xFF << 24) | (color & 0x00FFFFFF);
        for (int i = 0; i < 4; i++) {
            int yy = y + i * 13;
            g.fill(cx - 72 - i * 8, yy, cx - 20, yy + 9, c);
            g.fill(cx + 20, yy, cx + 72 + i * 8, yy + 9, c);
        }
    }

    private void drawPreviewArmor(GuiGraphicsExtractor g, int cx, int top, int color, boolean demonic) {
        g.fill(cx - 21, top - (demonic ? 8 : 3), cx + 21, top + 35, color);
        g.fill(cx - 28, top + 34, cx + 28, top + 96, color);
        g.fill(cx - 42, top + 36, cx - 25, top + 91, color);
        g.fill(cx + 25, top + 36, cx + 42, top + 91, color);
        g.fill(cx - 24, top + 94, cx - 2, top + 162, color);
        g.fill(cx + 2, top + 94, cx + 24, top + 162, color);
        if (demonic) {
            g.fill(cx - 30, top - 16, cx - 20, top + 2, color);
            g.fill(cx + 20, top - 16, cx + 30, top + 2, color);
        }
    }

    private static int multiplyRgb(int base, int tint) {
        int r = ((base >> 16) & 255) * ((tint >> 16) & 255) / 255;
        int g = ((base >> 8) & 255) * ((tint >> 8) & 255) / 255;
        int b = (base & 255) * (tint & 255) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private List<CosmeticEntry> filteredEntries() {
        List<CosmeticEntry> source = switch (section) {
            case WINGS -> WINGS;
            case ARMOR -> ARMOR;
            default -> List.of();
        };

        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return source;

        List<CosmeticEntry> result = new ArrayList<>();
        for (CosmeticEntry entry : source) {
            if ((entry.name + " " + entry.subtitle).toLowerCase(Locale.ROOT).contains(query)) {
                result.add(entry);
            }
        }
        return result;
    }

    private boolean isSelected(CosmeticEntry entry) {
        var cfg = TopkaClient.CONFIG.get();
        return switch (section) {
            case WINGS -> TopkaClient.MODULES.byId("wings").enabled()
                    && cfg.wingsStyle == entry.style;
            case ARMOR -> TopkaClient.MODULES.byId("armor_cosmetic").enabled()
                    && cfg.armorCosmeticStyle == entry.style;
            default -> false;
        };
    }

    private String selectedName() {
        var cfg = TopkaClient.CONFIG.get();
        if (section == Section.ARMOR) {
            return cfg.armorCosmeticStyle == 2 ? "Demonic Armor" : "Valkyrie Armor";
        }
        for (CosmeticEntry entry : WINGS) {
            if (entry.style == cfg.wingsStyle) return entry.name;
        }
        return "Angel Wings";
    }

    private boolean selectedModuleEnabled() {
        return section == Section.ARMOR
                ? TopkaClient.MODULES.byId("armor_cosmetic").enabled()
                : TopkaClient.MODULES.byId("wings").enabled();
    }

    private void apply(CosmeticEntry entry) {
        var cfg = TopkaClient.CONFIG.get();
        if (section == Section.ARMOR) {
            cfg.armorCosmeticStyle = entry.style;
            cfg.armorCosmeticTintArgb = 0xFFFFFFFF;
            TopkaClient.MODULES.byId("armor_cosmetic").setEnabled(true);
        } else {
            cfg.wingsStyle = entry.style;
            if (entry.style >= 5) cfg.importedWingTintArgb = 0xFFFFFFFF;
            TopkaClient.MODULES.byId("wings").setEnabled(true);
        }
        TopkaClient.CONFIG.save();
    }

    private void toggleSelectedModule() {
        if (section == Section.ARMOR) {
            TopkaClient.MODULES.byId("armor_cosmetic").toggle();
        } else if (section == Section.WINGS) {
            TopkaClient.MODULES.byId("wings").toggle();
        }
        TopkaClient.CONFIG.save();
    }

    private void applyColor(int color) {
        var cfg = TopkaClient.CONFIG.get();
        if (section == Section.ARMOR) {
            cfg.armorCosmeticTintArgb = color;
        } else if (section == Section.WINGS) {
            cfg.wingsPrimaryColorArgb = color;
            cfg.importedWingTintArgb = color;
        }
        TopkaClient.CONFIG.save();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = localX(click.x());
        int my = localY(click.y());

        if (click.button() == 0) {
            List<CosmeticEntry> entries = filteredEntries();
            for (int i = 0; i < entries.size(); i++) {
                int column = i % 3;
                int row = i / 3;
                int x = GRID_X + column * (CARD_W + CARD_GAP);
                int y = GRID_Y + row * (CARD_H + CARD_GAP);
                if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) {
                    apply(entries.get(i));
                    return true;
                }
            }

            if (mx >= 872 && mx < 926 && my >= 454 && my < 478
                    && (section == Section.WINGS || section == Section.ARMOR)) {
                toggleSelectedModule();
                return true;
            }

            if ((section == Section.WINGS || section == Section.ARMOR)
                    && my >= 496 && my < 516) {
                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    int x = 700 + i * 27;
                    if (mx >= x && mx < x + 20) {
                        applyColor(COLOR_PRESETS[i]);
                        return true;
                    }
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
}
