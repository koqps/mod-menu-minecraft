package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import com.koqps.topka.config.WaypointConfig;
import com.koqps.topka.hud.WaypointController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class WaypointScreen extends Screen {
    private static final int PANEL_W = 620;
    private static final int PANEL_H = 430;
    private static final int MAX_ROWS = 7;

    private final Screen parent;
    private WaypointConfig selected;
    private EditBox nameBox;
    private int scroll;

    public WaypointScreen(Screen parent) {
        super(Component.literal("Mod Menu Waypoints"));
        this.parent = parent;
    }

    private int x() { return (width - PANEL_W) / 2; }
    private int y() { return (height - PANEL_H) / 2; }

    @Override
    protected void init() {
        int x = x(), y = y();

        List<WaypointConfig> list = currentWaypoints();
        clampScroll(list);

        if (selected != null && !list.contains(selected)) selected = null;
        if (selected == null && !list.isEmpty()) selected = list.get(Math.min(scroll, list.size() - 1));

        int first = scroll;
        int last = Math.min(list.size(), first + MAX_ROWS);
        for (int i = first; i < last; i++) {
            WaypointConfig waypoint = list.get(i);
            int rowY = y + 92 + (i - first) * 36;
            addClickTarget(x + 26, rowY, PANEL_W - 52, 30, () -> {
                selected = waypoint;
                rebuildWidgets();
            });
        }

        addClickTarget(x + 26, y + 360, 126, 30, () -> {
            if (WaypointController.addCurrent()) {
                List<WaypointConfig> refreshed = currentWaypoints();
                if (!refreshed.isEmpty()) selected = refreshed.get(refreshed.size() - 1);
                scroll = Math.max(0, refreshed.size() - MAX_ROWS);
                rebuildWidgets();
            }
        });

        addClickTarget(x + 160, y + 360, 98, 30, () -> {
            if (selected != null) {
                selected.enabled = !selected.enabled;
                TopkaClient.CONFIG.save();
                rebuildWidgets();
            }
        });

        addClickTarget(x + 266, y + 360, 98, 30, () -> {
            if (selected != null) {
                WaypointController.cycleColor(selected);
                rebuildWidgets();
            }
        });

        addClickTarget(x + 372, y + 360, 98, 30, () -> {
            if (selected != null) {
                WaypointController.remove(selected);
                selected = null;
                rebuildWidgets();
            }
        });

        addClickTarget(x + 478, y + 360, 116, 30, this::onClose);

        this.nameBox = new EditBox(
                font,
                x + 302,
                y + 50,
                292,
                24,
                Component.literal("Waypoint name")
        );
        this.nameBox.setMaxLength(48);
        this.nameBox.setValue(selected == null || selected.name == null ? "" : selected.name);
        this.nameBox.setResponder(value -> {
            if (selected != null) {
                String clean = value == null ? "" : value.trim();
                selected.name = clean.isEmpty() ? "Waypoint" : clean;
                TopkaClient.CONFIG.save();
            }
        });
        this.addWidget(nameBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = x(), y = y();

        g.fill(0, 0, width, height, 0xAA000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, cfg.panelArgb);
        g.fill(x, y, x + PANEL_W, y + 3, Theme.accent());

        g.text(font, "WAYPOINT STUDIO", x + 26, y + 20, 0xFFFFFFFF, true);
        g.text(font, "Markers are isolated per server/world and dimension.", x + 26, y + 38, cfg.mutedTextArgb, false);
        g.text(font, "NAME", x + 254, y + 58, 0xFF727283, false);

        List<WaypointConfig> list = currentWaypoints();
        clampScroll(list);

        if (list.isEmpty()) {
            g.centeredText(font, "No waypoints here yet — add your current position.", x + PANEL_W / 2, y + 200, 0xFF7D7D8D);
        } else {
            int first = scroll;
            int last = Math.min(list.size(), first + MAX_ROWS);
            for (int i = first; i < last; i++) {
                WaypointConfig waypoint = list.get(i);
                int rowY = y + 92 + (i - first) * 36;
                boolean isSelected = waypoint == selected;
                boolean hover = mouseX >= x + 26 && mouseX < x + PANEL_W - 26
                        && mouseY >= rowY && mouseY < rowY + 30;

                g.fill(x + 26, rowY, x + PANEL_W - 26, rowY + 30,
                        isSelected ? 0xFF29293A : (hover ? 0xFF22222D : 0xFF17171F));
                g.fill(x + 26, rowY, x + 30, rowY + 30, waypoint.colorArgb);
                g.text(font, waypoint.name == null ? "Waypoint" : waypoint.name, x + 40, rowY + 8, cfg.textArgb, true);
                g.text(font, waypoint.x + "  " + waypoint.y + "  " + waypoint.z, x + 260, rowY + 8, 0xFF8E8E9F, false);
                g.text(font, waypoint.enabled ? "ON" : "OFF", x + PANEL_W - 64, rowY + 8,
                        waypoint.enabled ? 0xFF64E89A : 0xFF7C7C88, true);
            }
        }

        drawButton(g, mouseX, mouseY, x + 26, y + 360, 126, "Add current");
        drawButton(g, mouseX, mouseY, x + 160, y + 360, 98, selected != null && selected.enabled ? "Disable" : "Enable");
        drawButton(g, mouseX, mouseY, x + 266, y + 360, 98, "Color");
        drawButton(g, mouseX, mouseY, x + 372, y + 360, 98, "Delete");
        drawButton(g, mouseX, mouseY, x + 478, y + 360, 116, "Back");

        g.text(font, "Mouse wheel scrolls the list", x + 26, y + 405, 0xFF666676, false);
        g.text(font, "ESC / " + TopkaClient.openMenuKey().getString() + " to return", x + 362, y + 405, 0xFF666676, false);

        if (nameBox != null) nameBox.extractRenderState(g, mouseX, mouseY, delta);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private List<WaypointConfig> currentWaypoints() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        String context = WaypointController.currentContextKey(client);
        if (context == null) return List.of();
        String dimension = client.level.dimension().identifier().toString();

        List<WaypointConfig> result = new ArrayList<>();
        for (WaypointConfig waypoint : TopkaClient.CONFIG.get().waypoints) {
            if (context.equals(waypoint.contextKey) && dimension.equals(waypoint.dimension)) {
                result.add(waypoint);
            }
        }
        return result;
    }

    private void clampScroll(List<WaypointConfig> list) {
        scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - MAX_ROWS));
    }

    private void drawButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 30;
        g.fill(x, y, x + w, y + 30, hover ? 0xFF30303C : 0xFF20202A);
        g.fill(x, y + 29, x + w, y + 30, Theme.accent());
        g.centeredText(font, label, x + w / 2, y + 10, 0xFFEDEDF4);
    }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run()).pos(x, y).size(w, h).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<WaypointConfig> list = currentWaypoints();
        int before = scroll;
        if (scrollY < 0) scroll = Math.min(Math.max(0, list.size() - MAX_ROWS), scroll + 1);
        if (scrollY > 0) scroll = Math.max(0, scroll - 1);
        if (before != scroll) {
            rebuildWidgets();
            return true;
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

    @Override
    public void onClose() {
        TopkaClient.CONFIG.save();
        minecraft.gui.setScreen(parent);
    }
}
