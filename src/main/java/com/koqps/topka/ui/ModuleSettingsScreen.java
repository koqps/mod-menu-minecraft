package com.koqps.topka.ui;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.Theme;
import com.koqps.topka.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ModuleSettingsScreen extends Screen {
    private static final int PANEL_W = 590;
    private static final int PANEL_H = 500;
    private static final int[] COLORS = {
            0xFF8B5CF6, 0xFF41C7FF, 0xFF50FA7B, 0xFFFF5C77,
            0xFFFFD166, 0xFFFF7AD9, 0xFFFF8A3D, 0xFFFFFFFF
    };

    private record Row(String label, ValueText value, Runnable minus, Runnable plus) { }
    @FunctionalInterface private interface ValueText { String get(); }

    private final Screen parent;
    private final Module module;
    private final List<Row> rows = new ArrayList<>();

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.name() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    private int x() { return (width - PANEL_W) / 2; }
    private int y() { return (height - PANEL_H) / 2; }

    @Override
    protected void init() {
        rows.clear();
        buildRows();

        int x = x(), y = y();
        addClickTarget(x + PANEL_W - 126, y + 20, 94, 26, () -> {
            module.toggle();
            TopkaClient.CONFIG.save();
        });

        int rowY = y + 92;
        for (Row row : rows) {
            addClickTarget(x + 402, rowY + 5, 36, 26, row.minus());
            addClickTarget(x + 446, rowY + 5, 36, 26, row.plus());
            rowY += 38;
        }

        if (isHudModule()) {
            addClickTarget(x + 32, y + PANEL_H - 52, 164, 28, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        } else if ("waypoints".equals(module.id())) {
            addClickTarget(x + 32, y + PANEL_H - 52, 164, 28, () -> minecraft.gui.setScreen(new WaypointScreen(this)));
        }
        addClickTarget(x + PANEL_W - 196, y + PANEL_H - 52, 164, 28, this::resetModuleSettings);
    }

    private void buildRows() {
        var c = TopkaClient.CONFIG.get();
        switch (module.id()) {
            case "crosshair" -> {
                row("Size", () -> Integer.toString(c.crosshairSize), () -> c.crosshairSize = Math.max(1, c.crosshairSize - 1), () -> c.crosshairSize = Math.min(16, c.crosshairSize + 1));
                row("Center gap", () -> Integer.toString(c.crosshairGap), () -> c.crosshairGap = Math.max(0, c.crosshairGap - 1), () -> c.crosshairGap = Math.min(12, c.crosshairGap + 1));
                row("Thickness", () -> Integer.toString(c.crosshairThickness), () -> c.crosshairThickness = Math.max(1, c.crosshairThickness - 1), () -> c.crosshairThickness = Math.min(6, c.crosshairThickness + 1));
                row("Color", () -> hex(c.crosshairColorArgb), () -> c.crosshairColorArgb = previousColor(c.crosshairColorArgb), () -> c.crosshairColorArgb = nextColor(c.crosshairColorArgb));
                row("Center dot", () -> c.crosshairDot ? "ON" : "OFF", () -> c.crosshairDot = !c.crosshairDot, () -> c.crosshairDot = !c.crosshairDot);
                row("Outline", () -> c.crosshairOutline ? "ON" : "OFF", () -> c.crosshairOutline = !c.crosshairOutline, () -> c.crosshairOutline = !c.crosshairOutline);
            }
            case "hitbox" -> {
                row("Expansion", () -> String.format("%.2f", c.hitboxExpand), () -> c.hitboxExpand = Math.max(0F, c.hitboxExpand - 0.05F), () -> c.hitboxExpand = Math.min(1F, c.hitboxExpand + 0.05F));
                row("Line width", () -> String.format("%.1f", c.hitboxLineWidth), () -> c.hitboxLineWidth = Math.max(1F, c.hitboxLineWidth - 0.25F), () -> c.hitboxLineWidth = Math.min(6F, c.hitboxLineWidth + 0.25F));
                row("Players", () -> c.hitboxPlayers ? "ON" : "OFF", () -> c.hitboxPlayers = !c.hitboxPlayers, () -> c.hitboxPlayers = !c.hitboxPlayers);
                row("Player color", () -> hex(c.hitboxPlayerColorArgb), () -> c.hitboxPlayerColorArgb = previousColor(c.hitboxPlayerColorArgb), () -> c.hitboxPlayerColorArgb = nextColor(c.hitboxPlayerColorArgb));
                row("Hostile mobs", () -> c.hitboxHostile ? "ON" : "OFF", () -> c.hitboxHostile = !c.hitboxHostile, () -> c.hitboxHostile = !c.hitboxHostile);
                row("Hostile color", () -> hex(c.hitboxHostileColorArgb), () -> c.hitboxHostileColorArgb = previousColor(c.hitboxHostileColorArgb), () -> c.hitboxHostileColorArgb = nextColor(c.hitboxHostileColorArgb));
                row("Passive mobs", () -> c.hitboxPassive ? "ON" : "OFF", () -> c.hitboxPassive = !c.hitboxPassive, () -> c.hitboxPassive = !c.hitboxPassive);
                row("Passive color", () -> hex(c.hitboxPassiveColorArgb), () -> c.hitboxPassiveColorArgb = previousColor(c.hitboxPassiveColorArgb), () -> c.hitboxPassiveColorArgb = nextColor(c.hitboxPassiveColorArgb));
                row("Other entities", () -> c.hitboxOther ? "ON" : "OFF", () -> c.hitboxOther = !c.hitboxOther, () -> c.hitboxOther = !c.hitboxOther);
                row("Other color", () -> hex(c.hitboxOtherColorArgb), () -> c.hitboxOtherColorArgb = previousColor(c.hitboxOtherColorArgb), () -> c.hitboxOtherColorArgb = nextColor(c.hitboxOtherColorArgb));
            }
            case "china_hat" -> {
                row("Radius", () -> String.format("%.2f", c.chinaHatRadius), () -> c.chinaHatRadius = Math.max(0.25F, c.chinaHatRadius - 0.05F), () -> c.chinaHatRadius = Math.min(1.25F, c.chinaHatRadius + 0.05F));
                row("Height", () -> String.format("%.2f", c.chinaHatHeight), () -> c.chinaHatHeight = Math.max(0.12F, c.chinaHatHeight - 0.04F), () -> c.chinaHatHeight = Math.min(0.8F, c.chinaHatHeight + 0.04F));
                row("Line width", () -> String.format("%.1f", c.chinaHatLineWidth), () -> c.chinaHatLineWidth = Math.max(1F, c.chinaHatLineWidth - 0.25F), () -> c.chinaHatLineWidth = Math.min(5F, c.chinaHatLineWidth + 0.25F));
                row("Show others", () -> c.chinaHatShowOthers ? "ON" : "OFF", () -> c.chinaHatShowOthers = !c.chinaHatShowOthers, () -> c.chinaHatShowOthers = !c.chinaHatShowOthers);
            }
            case "halo" -> {
                row("Radius", () -> String.format("%.2f", c.haloRadius), () -> c.haloRadius = Math.max(0.2F, c.haloRadius - 0.04F), () -> c.haloRadius = Math.min(1F, c.haloRadius + 0.04F));
                row("Height", () -> String.format("%.2f", c.haloHeight), () -> c.haloHeight = Math.max(0F, c.haloHeight - 0.03F), () -> c.haloHeight = Math.min(0.7F, c.haloHeight + 0.03F));
                row("Line width", () -> String.format("%.1f", c.haloLineWidth), () -> c.haloLineWidth = Math.max(1F, c.haloLineWidth - 0.25F), () -> c.haloLineWidth = Math.min(5F, c.haloLineWidth + 0.25F));
            }
            case "trails" -> {
                row("Lifetime", () -> c.trailLifetimeMs + " ms", () -> c.trailLifetimeMs = Math.max(250, c.trailLifetimeMs - 100), () -> c.trailLifetimeMs = Math.min(3000, c.trailLifetimeMs + 100));
                row("Line width", () -> String.format("%.1f", c.trailLineWidth), () -> c.trailLineWidth = Math.max(1F, c.trailLineWidth - 0.25F), () -> c.trailLineWidth = Math.min(6F, c.trailLineWidth + 0.25F));
                row("Color", () -> hex(c.trailColorArgb), () -> c.trailColorArgb = previousColor(c.trailColorArgb), () -> c.trailColorArgb = nextColor(c.trailColorArgb));
            }
            case "jump_circles" -> {
                row("Radius", () -> String.format("%.2f", c.jumpCircleRadius), () -> c.jumpCircleRadius = Math.max(0.3F, c.jumpCircleRadius - 0.1F), () -> c.jumpCircleRadius = Math.min(3F, c.jumpCircleRadius + 0.1F));
                row("Lifetime", () -> c.jumpCircleLifetimeMs + " ms", () -> c.jumpCircleLifetimeMs = Math.max(250, c.jumpCircleLifetimeMs - 100), () -> c.jumpCircleLifetimeMs = Math.min(2500, c.jumpCircleLifetimeMs + 100));
                row("Line width", () -> String.format("%.1f", c.jumpCircleLineWidth), () -> c.jumpCircleLineWidth = Math.max(1F, c.jumpCircleLineWidth - 0.25F), () -> c.jumpCircleLineWidth = Math.min(6F, c.jumpCircleLineWidth + 0.25F));
                row("Color", () -> hex(c.jumpCircleColorArgb), () -> c.jumpCircleColorArgb = previousColor(c.jumpCircleColorArgb), () -> c.jumpCircleColorArgb = nextColor(c.jumpCircleColorArgb));
            }
            case "jump_particles" -> row("Particle count", () -> Integer.toString(c.jumpParticleCount), () -> c.jumpParticleCount = Math.max(1, c.jumpParticleCount - 1), () -> c.jumpParticleCount = Math.min(32, c.jumpParticleCount + 1));
            case "hit_particles" -> row("Particle count", () -> Integer.toString(c.hitParticleCount), () -> c.hitParticleCount = Math.max(1, c.hitParticleCount - 1), () -> c.hitParticleCount = Math.min(40, c.hitParticleCount + 1));
            case "full_bright" -> row("Gamma", () -> String.format("%.1f", c.fullBrightGamma), () -> c.fullBrightGamma = Math.max(1D, c.fullBrightGamma - 1D), () -> c.fullBrightGamma = Math.min(16D, c.fullBrightGamma + 1D));
            case "viewmodel" -> {
                row("Main X", () -> String.format("%.2f", c.viewMainX), () -> c.viewMainX = Math.max(-1F, c.viewMainX - 0.05F), () -> c.viewMainX = Math.min(1F, c.viewMainX + 0.05F));
                row("Main Y", () -> String.format("%.2f", c.viewMainY), () -> c.viewMainY = Math.max(-1F, c.viewMainY - 0.05F), () -> c.viewMainY = Math.min(1F, c.viewMainY + 0.05F));
                row("Main Z", () -> String.format("%.2f", c.viewMainZ), () -> c.viewMainZ = Math.max(-1F, c.viewMainZ - 0.05F), () -> c.viewMainZ = Math.min(1F, c.viewMainZ + 0.05F));
                row("Scale", () -> String.format("%.2f", c.viewScale), () -> c.viewScale = Math.max(0.35F, c.viewScale - 0.05F), () -> c.viewScale = Math.min(2F, c.viewScale + 0.05F));
                row("Pitch", () -> String.format("%.0f°", c.viewPitch), () -> c.viewPitch -= 5F, () -> c.viewPitch += 5F);
                row("Yaw", () -> String.format("%.0f°", c.viewYaw), () -> c.viewYaw -= 5F, () -> c.viewYaw += 5F);
                row("Roll", () -> String.format("%.0f°", c.viewRoll), () -> c.viewRoll -= 5F, () -> c.viewRoll += 5F);
            }
            case "swing_animations" -> {
                row("Style", () -> switch (Math.floorMod(c.swingMode, 4)) { case 0 -> "SMOOTH"; case 1 -> "SHORT"; case 2 -> "SWIPE"; default -> "OLD SCHOOL"; }, () -> c.swingMode = Math.floorMod(c.swingMode - 1, 4), () -> c.swingMode = Math.floorMod(c.swingMode + 1, 4));
                row("Strength", () -> String.format("%.2f", c.swingStrength), () -> c.swingStrength = Math.max(0F, c.swingStrength - 0.1F), () -> c.swingStrength = Math.min(2F, c.swingStrength + 0.1F));
            }
            case "ambience" -> row("Time", () -> timeName(c.ambienceTime), () -> c.ambienceTime = previousTime(c.ambienceTime), () -> c.ambienceTime = nextTime(c.ambienceTime));
            case "hit_color" -> row("Hit color", () -> hex(c.hitColorArgb), () -> c.hitColorArgb = withAlpha(previousColor(c.hitColorArgb), (c.hitColorArgb >>> 24) & 0xFF), () -> c.hitColorArgb = withAlpha(nextColor(c.hitColorArgb), (c.hitColorArgb >>> 24) & 0xFF));
            case "targeting" -> row("Target color", () -> hex(c.targetColorArgb), () -> c.targetColorArgb = previousColor(c.targetColorArgb), () -> c.targetColorArgb = nextColor(c.targetColorArgb));
            case "sprint" -> row("Stop while using item", () -> c.sprintStopWhileUsingItem ? "ON" : "OFF", () -> c.sprintStopWhileUsingItem = !c.sprintStopWhileUsingItem, () -> c.sprintStopWhileUsingItem = !c.sprintStopWhileUsingItem);
            case "health_tags" -> {
                row("Display style", () -> c.healthTagHearts ? "HEARTS" : "NUMERIC", () -> c.healthTagHearts = !c.healthTagHearts, () -> c.healthTagHearts = !c.healthTagHearts);
                row("Max distance", () -> Math.round(c.healthTagMaxDistance) + " blocks", () -> c.healthTagMaxDistance = Math.max(8D, c.healthTagMaxDistance - 4D), () -> c.healthTagMaxDistance = Math.min(128D, c.healthTagMaxDistance + 4D));
            }
            case "projectile_prediction" -> {
                row("Simulation steps", () -> Integer.toString(c.projectileSteps), () -> c.projectileSteps = Math.max(16, c.projectileSteps - 8), () -> c.projectileSteps = Math.min(160, c.projectileSteps + 8));
                row("Line width", () -> String.format("%.1f", c.projectileLineWidth), () -> c.projectileLineWidth = Math.max(1F, c.projectileLineWidth - 0.25F), () -> c.projectileLineWidth = Math.min(5F, c.projectileLineWidth + 0.25F));
                row("Color", () -> hex(c.projectileColorArgb), () -> c.projectileColorArgb = previousColor(c.projectileColorArgb), () -> c.projectileColorArgb = nextColor(c.projectileColorArgb));
            }
            case "baby_mode" -> {
                row("Model scale", () -> String.format("%.2f", c.babyScale), () -> c.babyScale = Math.max(0.35F, c.babyScale - 0.05F), () -> c.babyScale = Math.min(1F, c.babyScale + 0.05F));
                row("Show others", () -> c.babyModeOthers ? "ON" : "OFF", () -> c.babyModeOthers = !c.babyModeOthers, () -> c.babyModeOthers = !c.babyModeOthers);
            }
            case "cape" -> {
                row("Width", () -> String.format("%.2f", c.capeWidth), () -> c.capeWidth = Math.max(0.30F, c.capeWidth - 0.05F), () -> c.capeWidth = Math.min(1.20F, c.capeWidth + 0.05F));
                row("Height", () -> String.format("%.2f", c.capeHeight), () -> c.capeHeight = Math.max(0.45F, c.capeHeight - 0.05F), () -> c.capeHeight = Math.min(1.60F, c.capeHeight + 0.05F));
                row("Line width", () -> String.format("%.1f", c.capeLineWidth), () -> c.capeLineWidth = Math.max(1F, c.capeLineWidth - 0.25F), () -> c.capeLineWidth = Math.min(5F, c.capeLineWidth + 0.25F));
                row("Color", () -> hex(c.capeColorArgb), () -> c.capeColorArgb = previousColor(c.capeColorArgb), () -> c.capeColorArgb = nextColor(c.capeColorArgb));
                row("Show others", () -> c.capeShowOthers ? "ON" : "OFF", () -> c.capeShowOthers = !c.capeShowOthers, () -> c.capeShowOthers = !c.capeShowOthers);
            }
            case "drop_protection" -> {
                row("Confirm window", () -> c.dropProtectionWindowMs + " ms", () -> c.dropProtectionWindowMs = Math.max(700L, c.dropProtectionWindowMs - 100L), () -> c.dropProtectionWindowMs = Math.min(5000L, c.dropProtectionWindowMs + 100L));
                row("Protect armor", () -> c.protectArmor ? "ON" : "OFF", () -> c.protectArmor = !c.protectArmor, () -> c.protectArmor = !c.protectArmor);
                row("Protect tools", () -> c.protectTools ? "ON" : "OFF", () -> c.protectTools = !c.protectTools, () -> c.protectTools = !c.protectTools);
                row("Protect totems", () -> c.protectTotems ? "ON" : "OFF", () -> c.protectTotems = !c.protectTotems, () -> c.protectTotems = !c.protectTotems);
                row("Protect named", () -> c.protectNamedItems ? "ON" : "OFF", () -> c.protectNamedItems = !c.protectNamedItems, () -> c.protectNamedItems = !c.protectNamedItems);
            }
            case "auto_gg" -> {
                row("Message", () -> c.autoGgMessage, () -> c.autoGgMessage = previousGgMessage(c.autoGgMessage), () -> c.autoGgMessage = nextGgMessage(c.autoGgMessage));
                row("Cooldown", () -> (c.autoGgCooldownMs / 1000L) + " sec", () -> c.autoGgCooldownMs = Math.max(5_000L, c.autoGgCooldownMs - 5_000L), () -> c.autoGgCooldownMs = Math.min(60_000L, c.autoGgCooldownMs + 5_000L));
            }
            case "item_color" -> {
                row("Highlight color", () -> hex(c.itemHighlightColorArgb), () -> c.itemHighlightColorArgb = previousColor(c.itemHighlightColorArgb), () -> c.itemHighlightColorArgb = nextColor(c.itemHighlightColorArgb));
                row("Tracked items", () -> Integer.toString(c.highlightedItems.size()), () -> cycleHighlightPreset(-1), () -> cycleHighlightPreset(1));
            }
            case "waypoints" -> row("Markers", () -> Integer.toString(c.waypoints.size()), () -> { }, () -> { });
            default -> {
                if (isHudModule()) row("HUD position", () -> "OPEN EDITOR BELOW", () -> { }, () -> { });
            }
        }
    }

    private void row(String label, ValueText value, Runnable minus, Runnable plus) {
        rows.add(new Row(label, value, saveWrapped(minus), saveWrapped(plus)));
    }

    private Runnable saveWrapped(Runnable action) {
        return () -> {
            action.run();
            TopkaClient.CONFIG.save();
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var cfg = TopkaClient.CONFIG.get();
        int x = x(), y = y();

        g.fill(0, 0, width, height, 0xAA000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, cfg.panelArgb);
        g.fill(x, y, x + PANEL_W, y + 3, Theme.accent());

        g.text(font, module.icon() + "  " + module.name().toUpperCase(), x + 32, y + 23, 0xFFFFFFFF, true);
        g.text(font, module.description(), x + 32, y + 43, cfg.mutedTextArgb, false);

        int toggleColor = module.enabled() ? Theme.accent() : 0xFF555563;
        g.fill(x + PANEL_W - 126, y + 20, x + PANEL_W - 32, y + 46, module.enabled() ? 0xFF272337 : 0xFF1C1C24);
        g.fill(x + PANEL_W - 126, y + 45, x + PANEL_W - 32, y + 46, toggleColor);
        g.centeredText(font, module.enabled() ? "ENABLED" : "DISABLED", x + PANEL_W - 79, y + 29, toggleColor);

        g.text(font, "SETTINGS", x + 32, y + 72, 0xFF707082, false);

        int rowY = y + 92;
        if (rows.isEmpty()) {
            g.text(font, "This module has no extra settings yet.", x + 32, rowY + 8, 0xFF858596, false);
        }

        for (Row row : rows) {
            g.fill(x + 32, rowY, x + PANEL_W - 32, rowY + 34, 0xFF171720);
            g.text(font, row.label(), x + 46, rowY + 12, 0xFFCBCBD6, false);
            g.text(font, row.value().get(), x + 260, rowY + 12, Theme.accent(), true);
            drawMini(g, mouseX, mouseY, x + 402, rowY + 5, "−");
            drawMini(g, mouseX, mouseY, x + 446, rowY + 5, "+");
            rowY += 38;
        }

        if (isHudModule()) {
            drawBottomButton(g, mouseX, mouseY, x + 32, y + PANEL_H - 52, 164, "Open HUD workspace");
        } else if ("waypoints".equals(module.id())) {
            drawBottomButton(g, mouseX, mouseY, x + 32, y + PANEL_H - 52, 164, "Manage waypoints");
        }
        drawBottomButton(g, mouseX, mouseY, x + PANEL_W - 196, y + PANEL_H - 52, 164, "Reset module settings");

        g.text(font, "ESC / " + TopkaClient.openMenuKey().getString() + " to return", x + 32, y + PANEL_H - 18, 0xFF616171, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawMini(GuiGraphicsExtractor g, int mx, int my, int x, int y, String text) {
        boolean hover = mx >= x && mx < x + 36 && my >= y && my < y + 26;
        g.fill(x, y, x + 36, y + 26, hover ? 0xFF333340 : 0xFF24242E);
        g.centeredText(font, text, x + 18, y + 9, 0xFFF0F0F5);
    }

    private void drawBottomButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 28;
        g.fill(x, y, x + w, y + 28, hover ? 0xFF30303C : 0xFF21212B);
        g.fill(x, y + 27, x + w, y + 28, Theme.accent());
        g.centeredText(font, text, x + w / 2, y + 9, 0xFFEDEDF4);
    }

    private boolean isHudModule() {
        return module.category() == Module.Category.HUD;
    }

    private void addClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run()).pos(x, y).size(w, h).build());
    }

    private void resetModuleSettings() {
        var c = TopkaClient.CONFIG.get();
        switch (module.id()) {
            case "crosshair" -> {
                c.crosshairColorArgb = 0xFFFFFFFF; c.crosshairSize = 5; c.crosshairGap = 2;
                c.crosshairThickness = 1; c.crosshairDot = false; c.crosshairOutline = true;
            }
            case "hitbox" -> {
                c.hitboxExpand = 0F; c.hitboxLineWidth = 2F; c.hitboxColorArgb = 0xFF41C7FF;
                c.hitboxPlayerColorArgb = 0xFF41C7FF; c.hitboxHostileColorArgb = 0xFFFF5C77;
                c.hitboxPassiveColorArgb = 0xFF50FA7B; c.hitboxOtherColorArgb = 0xFFFFD166;
                c.hitboxPlayers = true; c.hitboxHostile = true; c.hitboxPassive = true; c.hitboxOther = false;
            }
            case "china_hat" -> { c.chinaHatRadius = 0.66F; c.chinaHatHeight = 0.32F; c.chinaHatLineWidth = 2F; c.chinaHatShowOthers = false; }
            case "halo" -> { c.haloRadius = 0.46F; c.haloHeight = 0.16F; c.haloLineWidth = 2.2F; }
            case "trails" -> { c.trailLifetimeMs = 900; c.trailLineWidth = 2.2F; c.trailColorArgb = 0xFF8B5CF6; }
            case "jump_circles" -> { c.jumpCircleLifetimeMs = 700; c.jumpCircleRadius = 1.15F; c.jumpCircleLineWidth = 2F; c.jumpCircleColorArgb = 0xFF41C7FF; }
            case "jump_particles" -> c.jumpParticleCount = 10;
            case "hit_particles" -> c.hitParticleCount = 12;
            case "full_bright" -> c.fullBrightGamma = 12D;
            case "viewmodel" -> {
                c.viewMainX = c.viewMainY = c.viewMainZ = 0F;
                c.viewOffX = c.viewOffY = c.viewOffZ = 0F;
                c.viewScale = 1F; c.viewPitch = c.viewYaw = c.viewRoll = 0F;
            }
            case "swing_animations" -> { c.swingMode = 0; c.swingStrength = 1F; }
            case "ambience" -> c.ambienceTime = 6000L;
            case "hit_color" -> c.hitColorArgb = 0xB28B5CF6;
            case "targeting" -> c.targetColorArgb = 0xFFFF5C77;
            case "sprint" -> c.sprintStopWhileUsingItem = true;
            case "health_tags" -> { c.healthTagHearts = true; c.healthTagMaxDistance = 48D; }
            case "projectile_prediction" -> { c.projectileColorArgb = 0xFF41C7FF; c.projectileLineWidth = 2F; c.projectileSteps = 72; }
            case "baby_mode" -> { c.babyScale = 0.65F; c.babyModeOthers = false; }
            case "cape" -> { c.capeColorArgb = 0xFF8B5CF6; c.capeWidth = 0.64F; c.capeHeight = 1.05F; c.capeLineWidth = 2F; c.capeShowOthers = false; }
            case "drop_protection" -> { c.dropProtectionWindowMs = 1800L; c.protectArmor = true; c.protectTools = true; c.protectTotems = true; c.protectNamedItems = true; }
            case "auto_gg" -> { c.autoGgMessage = "gg"; c.autoGgCooldownMs = 15000L; }
            case "item_color" -> {
                c.itemHighlightColorArgb = 0xFF8B5CF6;
                c.highlightedItems.clear();
                c.highlightedItems.add("minecraft:totem_of_undying");
                c.highlightedItems.add("minecraft:enchanted_golden_apple");
                c.highlightedItems.add("minecraft:elytra");
            }
            default -> { }
        }
        TopkaClient.CONFIG.save();
        rebuildWidgets();
    }

    private static long nextTime(long current) {
        long[] values = {1000L, 6000L, 12000L, 13000L, 18000L};
        for (int i = 0; i < values.length; i++) if (Math.floorMod(current, 24000L) == values[i]) return values[(i + 1) % values.length];
        return 6000L;
    }

    private static long previousTime(long current) {
        long[] values = {1000L, 6000L, 12000L, 13000L, 18000L};
        for (int i = 0; i < values.length; i++) if (Math.floorMod(current, 24000L) == values[i]) return values[(i + values.length - 1) % values.length];
        return 6000L;
    }

    private static String timeName(long time) {
        return switch ((int) Math.floorMod(time, 24000L)) {
            case 1000 -> "MORNING";
            case 6000 -> "NOON";
            case 12000 -> "SUNSET";
            case 13000 -> "NIGHT";
            case 18000 -> "MIDNIGHT";
            default -> Long.toString(Math.floorMod(time, 24000L));
        };
    }

    private static int nextColor(int current) {
        int rgb = current | 0xFF000000;
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == rgb) return COLORS[(i + 1) % COLORS.length];
        return COLORS[0];
    }

    private static int previousColor(int current) {
        int rgb = current | 0xFF000000;
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == rgb) return COLORS[(i + COLORS.length - 1) % COLORS.length];
        return COLORS[COLORS.length - 1];
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private void cycleHighlightPreset(int direction) {
        var c = TopkaClient.CONFIG.get();
        int preset;
        if (c.highlightedItems.contains("minecraft:netherite_sword")) preset = 2;
        else if (c.highlightedItems.contains("minecraft:diamond")) preset = 1;
        else preset = 0;
        preset = Math.floorMod(preset + direction, 3);
        c.highlightedItems.clear();
        if (preset == 0) {
            c.highlightedItems.add("minecraft:totem_of_undying");
            c.highlightedItems.add("minecraft:enchanted_golden_apple");
            c.highlightedItems.add("minecraft:elytra");
        } else if (preset == 1) {
            c.highlightedItems.add("minecraft:diamond");
            c.highlightedItems.add("minecraft:emerald");
            c.highlightedItems.add("minecraft:netherite_ingot");
            c.highlightedItems.add("minecraft:ancient_debris");
        } else {
            c.highlightedItems.add("minecraft:netherite_sword");
            c.highlightedItems.add("minecraft:netherite_pickaxe");
            c.highlightedItems.add("minecraft:netherite_axe");
            c.highlightedItems.add("minecraft:totem_of_undying");
            c.highlightedItems.add("minecraft:elytra");
        }
    }

    private static String nextGgMessage(String current) {
        String[] values = {"gg", "GG", "good game", "gg wp"};
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(current)) return values[(i + 1) % values.length];
        return values[0];
    }

    private static String previousGgMessage(String current) {
        String[] values = {"gg", "GG", "good game", "gg wp"};
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(current)) return values[(i + values.length - 1) % values.length];
        return values[0];
    }

    private static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
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
