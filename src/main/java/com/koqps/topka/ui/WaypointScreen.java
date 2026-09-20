package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.WaypointConfig;
import com.koqps.topka.hud.Theme;
import com.koqps.topka.hud.WaypointController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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

    private float uiScale() {
        return Math.clamp(TopkaClient.CONFIG.get().menuScale, 0.70F, 1.35F);
    }

    private int panelX() {
        return (width - Math.round(PANEL_W * uiScale())) / 2;
    }

    private int panelY() {
        return (height - Math.round(PANEL_H * uiScale())) / 2;
    }

    private int sx(int local) {
        return panelX() + Math.round(local * uiScale());
    }

    private int sy(int local) {
        return panelY() + Math.round(local * uiScale());
    }

    private int ss(int value) {
        return Math.max(1, Math.round(value * uiScale()));
    }

    private int localMouseX(double mouseX) {
        return Math.round((float) ((mouseX - panelX()) / uiScale()));
    }

    private int localMouseY(double mouseY) {
        return Math.round((float) ((mouseY - panelY()) / uiScale()));
    }

    @Override
    protected void init() {
        List<WaypointConfig> list = currentWaypoints();
        clampScroll(list);

        if (selected != null && !list.contains(selected)) selected = null;
        if (selected == null && !list.isEmpty()) selected = list.get(Math.min(scroll, list.size() - 1));

        int first = scroll;
        int last = Math.min(list.size(), first + MAX_ROWS);
        for (int i = first; i < last; i++) {
            WaypointConfig waypoint = list.get(i);
            int rowY = 92 + (i - first) * 36;
            addLocalClickTarget(26, rowY, PANEL_W - 52, 30, () -> {
                selected = waypoint;
                rebuildWidgets();
            });
        }

        addLocalClickTarget(26, 360, 126, 30, () -> {
            if (WaypointController.addCurrent()) {
                List<WaypointConfig> refreshed = currentWaypoints();
                if (!refreshed.isEmpty()) selected = refreshed.get(refreshed.size() - 1);
                scroll = Math.max(0, refreshed.size() - MAX_ROWS);
                rebuildWidgets();
            }
        });

        addLocalClickTarget(160, 360, 98, 30, () -> {
            if (selected != null) {
                selected.enabled = !selected.enabled;
                TopkaClient.CONFIG.save();
                rebuildWidgets();
            }
        });

        addLocalClickTarget(266, 360, 98, 30, () -> {
            if (selected != null) {
                WaypointController.cycleColor(selected);
                rebuildWidgets();
            }
        });

        addLocalClickTarget(372, 360, 98, 30, () -> {
            if (selected != null) {
                WaypointController.remove(selected);
                selected = null;
                rebuildWidgets();
            }
        });

        addLocalClickTarget(478, 360, 116, 30, this::onClose);

        nameBox = new EditBox(
                font,
                sx(302),
                sy(50),
                ss(292),
                ss(24),
                UiFont.text("Waypoint name")
        );
        nameBox.setBordered(false);
        nameBox.setTextShadow(false);
        nameBox.setMaxLength(48);
        nameBox.setHint(UiFont.text("Waypoint name..."));
        nameBox.addFormatter((text, index) -> FormattedCharSequence.forward(text, UiFont.STYLE));
        nameBox.setValue(selected == null || selected.name == null ? "" : selected.name);
        nameBox.setResponder(value -> {
            if (selected != null) {
                String clean = value == null ? "" : value.trim();
                selected.name = clean.isEmpty() ? "Waypoint" : clean;
                TopkaClient.CONFIG.save();
            }
        });
        addRenderableWidget(nameBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);

        g.fill(0, 0, width, height, 0xAA000000);
        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        g.pose().scale(uiScale(), uiScale());

        g.fill(0, 0, PANEL_W, PANEL_H, cfg.panelArgb);
        g.fill(0, 0, PANEL_W, 3, Theme.accent());

        g.text(font, UiFont.text("WAYPOINT STUDIO"), 26, 20, 0xFFFFFFFF, true);
        g.text(font, UiFont.text("Markers are isolated per server/world and dimension."), 26, 38, cfg.mutedTextArgb, false);
        g.text(font, UiFont.text("NAME"), 254, 58, 0xFF727283, false);
        g.fill(296, 46, 598, 78, 0xFF171720);
        g.fill(296, 77, 598, 78, Theme.accent());

        List<WaypointConfig> list = currentWaypoints();
        clampScroll(list);

        if (list.isEmpty()) {
            g.centeredText(font, UiFont.text("No waypoints here yet — add your current position."), PANEL_W / 2, 200, 0xFF7D7D8D);
        } else {
            int first = scroll;
            int last = Math.min(list.size(), first + MAX_ROWS);
            for (int i = first; i < last; i++) {
                WaypointConfig waypoint = list.get(i);
                int rowY = 92 + (i - first) * 36;
                boolean isSelected = waypoint == selected;
                boolean hover = mx >= 26 && mx < PANEL_W - 26 && my >= rowY && my < rowY + 30;

                g.fill(26, rowY, PANEL_W - 26, rowY + 30,
                        isSelected ? 0xFF29293A : (hover ? 0xFF22222D : 0xFF17171F));
                g.fill(26, rowY, 30, rowY + 30, waypoint.colorArgb);
                g.text(font, UiFont.trim(font, waypoint.name == null ? "Waypoint" : waypoint.name, 190), 40, rowY + 8, cfg.textArgb, true);
                g.text(font, UiFont.text(waypoint.x + "  " + waypoint.y + "  " + waypoint.z), 260, rowY + 8, 0xFF8E8E9F, false);
                g.text(font, UiFont.text(waypoint.enabled ? "ON" : "OFF"), PANEL_W - 64, rowY + 8,
                        waypoint.enabled ? 0xFF64E89A : 0xFF7C7C88, true);
            }
        }

        drawButton(g, mx, my, 26, 360, 126, "Add current");
        drawButton(g, mx, my, 160, 360, 98, selected != null && selected.enabled ? "Disable" : "Enable");
        drawButton(g, mx, my, 266, 360, 98, "Color");
        drawButton(g, mx, my, 372, 360, 98, "Delete");
        drawButton(g, mx, my, 478, 360, 116, "Back");

        g.text(font, UiFont.text("Mouse wheel scrolls the list"), 26, 405, 0xFF666676, false);
        g.text(font, UiFont.text("Scale " + String.format("%.2fx", uiScale()) + "  •  ESC / "
                + TopkaClient.openMenuKey().getString() + " to return"), 328, 405, 0xFF666676, false);

        g.pose().popMatrix();
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
        g.centeredText(font, UiFont.text(label), x + w / 2, y + 10, 0xFFEDEDF4);
    }

    private void addLocalClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run())
                .pos(sx(x), sy(y))
                .size(ss(w), ss(h))
                .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);
        if (mx < 20 || mx > PANEL_W - 20 || my < 82 || my > 350) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

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
