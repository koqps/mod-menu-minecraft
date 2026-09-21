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
    private static final int VISIBLE_ROWS = 8;
    private static final int[] COLORS = {
            0xFF8B5CF6, 0xFF41C7FF, 0xFF50FA7B, 0xFFFF5C77,
            0xFFFFD166, 0xFFFF7AD9, 0xFFFF8A3D, 0xFFFFFFFF
    };

    private record Row(String label, ValueText value, Runnable minus, Runnable plus) { }
    @FunctionalInterface private interface ValueText { String get(); }

    private final Screen parent;
    private final Module module;
    private final List<Row> rows = new ArrayList<>();
    private int scrollRow;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.name() + " Settings"));
        this.parent = parent;
        this.module = module;
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
        rows.clear();
        buildRows();

        addLocalClickTarget(PANEL_W - 126, 20, 94, 26, () -> {
            module.toggle();
            TopkaClient.CONFIG.save();
        });

        clampScroll();
        int first = scrollRow;
        int last = Math.min(rows.size(), first + VISIBLE_ROWS);
        int rowY = 92;
        for (int i = first; i < last; i++) {
            Row row = rows.get(i);
            addLocalClickTarget(402, rowY + 5, 36, 26, row.minus());
            addLocalClickTarget(446, rowY + 5, 36, 26, row.plus());
            rowY += 38;
        }

        if (isHudModule()) {
            addLocalClickTarget(32, PANEL_H - 52, 164, 28, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        } else if ("waypoints".equals(module.id())) {
            addLocalClickTarget(32, PANEL_H - 52, 164, 28, () -> minecraft.gui.setScreen(new WaypointScreen(this)));
        }
        addLocalClickTarget(PANEL_W - 196, PANEL_H - 52, 164, 28, this::resetModuleSettings);
    }

    private void buildRows() {
        var c = TopkaClient.CONFIG.get();
        switch (module.id()) {
            case "crosshair" -> {
                row("Style", () -> crosshairStyleName(c.crosshairStyle), () -> c.crosshairStyle = Math.floorMod(c.crosshairStyle - 1, 4), () -> c.crosshairStyle = Math.floorMod(c.crosshairStyle + 1, 4));
                row("Size", () -> Integer.toString(c.crosshairSize), () -> c.crosshairSize = Math.max(1, c.crosshairSize - 1), () -> c.crosshairSize = Math.min(16, c.crosshairSize + 1));
                row("Center gap", () -> Integer.toString(c.crosshairGap), () -> c.crosshairGap = Math.max(0, c.crosshairGap - 1), () -> c.crosshairGap = Math.min(12, c.crosshairGap + 1));
                row("Thickness", () -> Integer.toString(c.crosshairThickness), () -> c.crosshairThickness = Math.max(1, c.crosshairThickness - 1), () -> c.crosshairThickness = Math.min(6, c.crosshairThickness + 1));
                row("Color", () -> hex(c.crosshairColorArgb), () -> c.crosshairColorArgb = previousColor(c.crosshairColorArgb), () -> c.crosshairColorArgb = nextColor(c.crosshairColorArgb));
                row("Center dot", () -> c.crosshairDot ? "ON" : "OFF", () -> c.crosshairDot = !c.crosshairDot, () -> c.crosshairDot = !c.crosshairDot);
                row("Outline", () -> c.crosshairOutline ? "ON" : "OFF", () -> c.crosshairOutline = !c.crosshairOutline, () -> c.crosshairOutline = !c.crosshairOutline);
                row("Dynamic spread", () -> c.crosshairDynamic ? "ON" : "OFF", () -> c.crosshairDynamic = !c.crosshairDynamic, () -> c.crosshairDynamic = !c.crosshairDynamic);
                row("Dynamic max gap", () -> Integer.toString(c.crosshairDynamicMaxGap), () -> c.crosshairDynamicMaxGap = Math.max(2, c.crosshairDynamicMaxGap - 1), () -> c.crosshairDynamicMaxGap = Math.min(24, c.crosshairDynamicMaxGap + 1));
                row("Rainbow", () -> c.crosshairRainbow ? "ON" : "OFF", () -> c.crosshairRainbow = !c.crosshairRainbow, () -> c.crosshairRainbow = !c.crosshairRainbow);
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
                row("Style", () -> hatStyleName(c.chinaHatStyle), () -> c.chinaHatStyle = Math.floorMod(c.chinaHatStyle - 1, 4), () -> c.chinaHatStyle = Math.floorMod(c.chinaHatStyle + 1, 4));
                row("Radius", () -> String.format("%.2f", c.chinaHatRadius), () -> c.chinaHatRadius = Math.max(0.25F, c.chinaHatRadius - 0.05F), () -> c.chinaHatRadius = Math.min(1.25F, c.chinaHatRadius + 0.05F));
                row("Height", () -> String.format("%.2f", c.chinaHatHeight), () -> c.chinaHatHeight = Math.max(0.12F, c.chinaHatHeight - 0.04F), () -> c.chinaHatHeight = Math.min(0.8F, c.chinaHatHeight + 0.04F));
                row("Line width", () -> String.format("%.1f", c.chinaHatLineWidth), () -> c.chinaHatLineWidth = Math.max(1F, c.chinaHatLineWidth - 0.25F), () -> c.chinaHatLineWidth = Math.min(7F, c.chinaHatLineWidth + 0.25F));
                row("Color", () -> hex(c.chinaHatColorArgb), () -> c.chinaHatColorArgb = previousColor(c.chinaHatColorArgb), () -> c.chinaHatColorArgb = nextColor(c.chinaHatColorArgb));
                row("Rainbow", () -> c.chinaHatRainbow ? "ON" : "OFF", () -> c.chinaHatRainbow = !c.chinaHatRainbow, () -> c.chinaHatRainbow = !c.chinaHatRainbow);
                row("Show others", () -> c.chinaHatShowOthers ? "ON" : "OFF", () -> c.chinaHatShowOthers = !c.chinaHatShowOthers, () -> c.chinaHatShowOthers = !c.chinaHatShowOthers);
            }
            case "halo" -> {
                row("Style", () -> haloStyleName(c.haloStyle), () -> c.haloStyle = Math.floorMod(c.haloStyle - 1, 5), () -> c.haloStyle = Math.floorMod(c.haloStyle + 1, 5));
                row("Radius", () -> String.format("%.2f", c.haloRadius), () -> c.haloRadius = Math.max(0.2F, c.haloRadius - 0.04F), () -> c.haloRadius = Math.min(1F, c.haloRadius + 0.04F));
                row("Height", () -> String.format("%.2f", c.haloHeight), () -> c.haloHeight = Math.max(0F, c.haloHeight - 0.03F), () -> c.haloHeight = Math.min(0.7F, c.haloHeight + 0.03F));
                row("Line width", () -> String.format("%.1f", c.haloLineWidth), () -> c.haloLineWidth = Math.max(1F, c.haloLineWidth - 0.25F), () -> c.haloLineWidth = Math.min(7F, c.haloLineWidth + 0.25F));
                row("Color", () -> hex(c.haloColorArgb), () -> c.haloColorArgb = previousColor(c.haloColorArgb), () -> c.haloColorArgb = nextColor(c.haloColorArgb));
                row("Rainbow", () -> c.haloRainbow ? "ON" : "OFF", () -> c.haloRainbow = !c.haloRainbow, () -> c.haloRainbow = !c.haloRainbow);
            }
            case "trails" -> {
                row("Style", () -> trailStyleName(c.trailStyle), () -> c.trailStyle = Math.floorMod(c.trailStyle - 1, 4), () -> c.trailStyle = Math.floorMod(c.trailStyle + 1, 4));
                row("Lifetime", () -> c.trailLifetimeMs + " ms", () -> c.trailLifetimeMs = Math.max(250, c.trailLifetimeMs - 100), () -> c.trailLifetimeMs = Math.min(3000, c.trailLifetimeMs + 100));
                row("Thickness", () -> String.format("%.1f", c.trailLineWidth), () -> c.trailLineWidth = Math.max(1F, c.trailLineWidth - 0.5F), () -> c.trailLineWidth = Math.min(12F, c.trailLineWidth + 0.5F));
                row("Ribbon width", () -> String.format("%.2f", c.trailWidth), () -> c.trailWidth = Math.max(0.1F, c.trailWidth - 0.1F), () -> c.trailWidth = Math.min(2.5F, c.trailWidth + 0.1F));
                row("Ribbon height", () -> String.format("%.2f", c.trailHeight), () -> c.trailHeight = Math.max(0.05F, c.trailHeight - 0.05F), () -> c.trailHeight = Math.min(1.8F, c.trailHeight + 0.05F));
                row("Layers", () -> Integer.toString(c.trailLayers), () -> c.trailLayers = Math.max(1, c.trailLayers - 1), () -> c.trailLayers = Math.min(6, c.trailLayers + 1));
                row("Glow", () -> c.trailGlow ? "ON" : "OFF", () -> c.trailGlow = !c.trailGlow, () -> c.trailGlow = !c.trailGlow);
                row("Rainbow", () -> c.trailRainbow ? "ON" : "OFF", () -> c.trailRainbow = !c.trailRainbow, () -> c.trailRainbow = !c.trailRainbow);
                row("Color", () -> hex(c.trailColorArgb), () -> c.trailColorArgb = previousColor(c.trailColorArgb), () -> c.trailColorArgb = nextColor(c.trailColorArgb));
            }
            case "jump_circles" -> {
                row("Style", () -> ringStyleName(c.jumpCircleStyle), () -> c.jumpCircleStyle = Math.floorMod(c.jumpCircleStyle - 1, 4), () -> c.jumpCircleStyle = Math.floorMod(c.jumpCircleStyle + 1, 4));
                row("Radius", () -> String.format("%.2f", c.jumpCircleRadius), () -> c.jumpCircleRadius = Math.max(0.3F, c.jumpCircleRadius - 0.1F), () -> c.jumpCircleRadius = Math.min(4F, c.jumpCircleRadius + 0.1F));
                row("Lifetime", () -> c.jumpCircleLifetimeMs + " ms", () -> c.jumpCircleLifetimeMs = Math.max(250, c.jumpCircleLifetimeMs - 100), () -> c.jumpCircleLifetimeMs = Math.min(2500, c.jumpCircleLifetimeMs + 100));
                row("Thickness", () -> String.format("%.1f", c.jumpCircleLineWidth), () -> c.jumpCircleLineWidth = Math.max(1F, c.jumpCircleLineWidth - 0.5F), () -> c.jumpCircleLineWidth = Math.min(10F, c.jumpCircleLineWidth + 0.5F));
                row("Layers", () -> Integer.toString(c.jumpCircleLayers), () -> c.jumpCircleLayers = Math.max(1, c.jumpCircleLayers - 1), () -> c.jumpCircleLayers = Math.min(5, c.jumpCircleLayers + 1));
                row("Rainbow", () -> c.jumpCircleRainbow ? "ON" : "OFF", () -> c.jumpCircleRainbow = !c.jumpCircleRainbow, () -> c.jumpCircleRainbow = !c.jumpCircleRainbow);
                row("Color", () -> hex(c.jumpCircleColorArgb), () -> c.jumpCircleColorArgb = previousColor(c.jumpCircleColorArgb), () -> c.jumpCircleColorArgb = nextColor(c.jumpCircleColorArgb));
            }
            case "jump_particles" -> {
                row("Type", () -> particleStyleName(c.jumpParticleStyle), () -> c.jumpParticleStyle = Math.floorMod(c.jumpParticleStyle - 1, 4), () -> c.jumpParticleStyle = Math.floorMod(c.jumpParticleStyle + 1, 4));
                row("Particle count", () -> Integer.toString(c.jumpParticleCount), () -> c.jumpParticleCount = Math.max(1, c.jumpParticleCount - 1), () -> c.jumpParticleCount = Math.min(48, c.jumpParticleCount + 1));
            }
            case "hit_particles" -> {
                row("Type", () -> particleStyleName(c.hitParticleStyle), () -> c.hitParticleStyle = Math.floorMod(c.hitParticleStyle - 1, 4), () -> c.hitParticleStyle = Math.floorMod(c.hitParticleStyle + 1, 4));
                row("Particle count", () -> Integer.toString(c.hitParticleCount), () -> c.hitParticleCount = Math.max(1, c.hitParticleCount - 1), () -> c.hitParticleCount = Math.min(64, c.hitParticleCount + 1));
            }
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
            case "targeting" -> {
                row("Style", () -> switch (c.targetMode) { case 0 -> "BOX"; case 1 -> "RING"; default -> "BOTH"; }, () -> c.targetMode = Math.floorMod(c.targetMode - 1, 3), () -> c.targetMode = Math.floorMod(c.targetMode + 1, 3));
                row("Target color", () -> hex(c.targetColorArgb), () -> c.targetColorArgb = previousColor(c.targetColorArgb), () -> c.targetColorArgb = nextColor(c.targetColorArgb));
                row("Pulse", () -> c.targetPulse ? "ON" : "OFF", () -> c.targetPulse = !c.targetPulse, () -> c.targetPulse = !c.targetPulse);
                row("Ring padding", () -> String.format("%.2f", c.targetPadding), () -> c.targetPadding = Math.max(0F, c.targetPadding - 0.02F), () -> c.targetPadding = Math.min(0.75F, c.targetPadding + 0.02F));
            }
            case "sprint" -> row("Stop while using item", () -> c.sprintStopWhileUsingItem ? "ON" : "OFF", () -> c.sprintStopWhileUsingItem = !c.sprintStopWhileUsingItem, () -> c.sprintStopWhileUsingItem = !c.sprintStopWhileUsingItem);
            case "health_tags" -> {
                row("Style", () -> "HEARTS ONLY", () -> { }, () -> { });
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
                row("Style", () -> capeStyleName(c.capeStyle), () -> c.capeStyle = Math.floorMod(c.capeStyle - 1, 4), () -> c.capeStyle = Math.floorMod(c.capeStyle + 1, 4));
                row("Width", () -> String.format("%.2f", c.capeWidth), () -> c.capeWidth = Math.max(0.30F, c.capeWidth - 0.05F), () -> c.capeWidth = Math.min(1.20F, c.capeWidth + 0.05F));
                row("Height", () -> String.format("%.2f", c.capeHeight), () -> c.capeHeight = Math.max(0.45F, c.capeHeight - 0.05F), () -> c.capeHeight = Math.min(1.60F, c.capeHeight + 0.05F));
                row("Outline", () -> String.format("%.1f", c.capeLineWidth), () -> c.capeLineWidth = Math.max(1F, c.capeLineWidth - 0.25F), () -> c.capeLineWidth = Math.min(8F, c.capeLineWidth + 0.25F));
                row("Opacity", () -> Integer.toString(c.capeOpacity), () -> c.capeOpacity = Math.max(30, c.capeOpacity - 10), () -> c.capeOpacity = Math.min(235, c.capeOpacity + 10));
                row("Color", () -> hex(c.capeColorArgb), () -> c.capeColorArgb = previousColor(c.capeColorArgb), () -> c.capeColorArgb = nextColor(c.capeColorArgb));
                row("Glow", () -> c.capeGlow ? "ON" : "OFF", () -> c.capeGlow = !c.capeGlow, () -> c.capeGlow = !c.capeGlow);
                row("Rainbow", () -> c.capeRainbow ? "ON" : "OFF", () -> c.capeRainbow = !c.capeRainbow, () -> c.capeRainbow = !c.capeRainbow);
                row("Show others", () -> c.capeShowOthers ? "ON" : "OFF", () -> c.capeShowOthers = !c.capeShowOthers, () -> c.capeShowOthers = !c.capeShowOthers);
            }
            case "wings" -> {
                row("Style", () -> wingStyleName(c.wingsStyle), () -> c.wingsStyle = Math.floorMod(c.wingsStyle - 1, 9), () -> c.wingsStyle = Math.floorMod(c.wingsStyle + 1, 9));
                row("Detail", () -> Integer.toString(c.wingsDetail), () -> c.wingsDetail = Math.max(1, c.wingsDetail - 1), () -> c.wingsDetail = Math.min(5, c.wingsDetail + 1));
                row("Scale", () -> String.format("%.2f", c.wingsScale), () -> c.wingsScale = Math.max(0.45F, c.wingsScale - 0.05F), () -> c.wingsScale = Math.min(2.25F, c.wingsScale + 0.05F));
                row("Spread", () -> String.format("%.2f", c.wingsSpread), () -> c.wingsSpread = Math.max(0.35F, c.wingsSpread - 0.05F), () -> c.wingsSpread = Math.min(1.65F, c.wingsSpread + 0.05F));
                row("Depth", () -> String.format("%.2f", c.wingsDepth), () -> c.wingsDepth = Math.max(0.02F, c.wingsDepth - 0.02F), () -> c.wingsDepth = Math.min(0.42F, c.wingsDepth + 0.02F));
                row("Bone width", () -> String.format("%.1f", c.wingsBoneWidth), () -> c.wingsBoneWidth = Math.max(1F, c.wingsBoneWidth - 0.25F), () -> c.wingsBoneWidth = Math.min(8F, c.wingsBoneWidth + 0.25F));
                row("Vertical", () -> String.format("%.2f", c.wingsVerticalOffset), () -> c.wingsVerticalOffset = Math.max(-0.75F, c.wingsVerticalOffset - 0.05F), () -> c.wingsVerticalOffset = Math.min(0.75F, c.wingsVerticalOffset + 0.05F));
                row("Back offset", () -> String.format("%.2f", c.wingsBackOffset), () -> c.wingsBackOffset = Math.max(-0.20F, c.wingsBackOffset - 0.05F), () -> c.wingsBackOffset = Math.min(0.75F, c.wingsBackOffset + 0.05F));
                row("Tilt", () -> String.format("%.0f°", c.wingsTilt), () -> c.wingsTilt = Math.max(-35F, c.wingsTilt - 5F), () -> c.wingsTilt = Math.min(35F, c.wingsTilt + 5F));
                row("Fold", () -> String.format("%.2f", c.wingsFold), () -> c.wingsFold = Math.max(0F, c.wingsFold - 0.05F), () -> c.wingsFold = Math.min(0.70F, c.wingsFold + 0.05F));
                row("Flap speed", () -> String.format("%.2f", c.wingsFlapSpeed), () -> c.wingsFlapSpeed = Math.max(0.10F, c.wingsFlapSpeed - 0.10F), () -> c.wingsFlapSpeed = Math.min(3.0F, c.wingsFlapSpeed + 0.10F));
                row("Flap amount", () -> String.format("%.2f", c.wingsFlapAmount), () -> c.wingsFlapAmount = Math.max(0.0F, c.wingsFlapAmount - 0.03F), () -> c.wingsFlapAmount = Math.min(0.55F, c.wingsFlapAmount + 0.03F));
                row("Opacity", () -> Integer.toString(c.wingsOpacity), () -> c.wingsOpacity = Math.max(30, c.wingsOpacity - 10), () -> c.wingsOpacity = Math.min(235, c.wingsOpacity + 10));
                row("Primary", () -> hex(c.wingsPrimaryColorArgb), () -> c.wingsPrimaryColorArgb = previousColor(c.wingsPrimaryColorArgb), () -> c.wingsPrimaryColorArgb = nextColor(c.wingsPrimaryColorArgb));
                row("Secondary", () -> hex(c.wingsSecondaryColorArgb), () -> c.wingsSecondaryColorArgb = previousColor(c.wingsSecondaryColorArgb), () -> c.wingsSecondaryColorArgb = nextColor(c.wingsSecondaryColorArgb));
                row("Glow", () -> c.wingsGlow ? "ON" : "OFF", () -> c.wingsGlow = !c.wingsGlow, () -> c.wingsGlow = !c.wingsGlow);
                row("Rainbow", () -> c.wingsRainbow ? "ON" : "OFF", () -> c.wingsRainbow = !c.wingsRainbow, () -> c.wingsRainbow = !c.wingsRainbow);
                row("Show others", () -> c.wingsShowOthers ? "ON" : "OFF", () -> c.wingsShowOthers = !c.wingsShowOthers, () -> c.wingsShowOthers = !c.wingsShowOthers);
                row("Pack scale", () -> String.format("%.2f", c.importedWingScale), () -> c.importedWingScale = Math.max(0.35F, c.importedWingScale - 0.05F), () -> c.importedWingScale = Math.min(2.5F, c.importedWingScale + 0.05F));
                row("Pack vertical", () -> String.format("%.2f", c.importedWingVerticalOffset), () -> c.importedWingVerticalOffset = Math.max(-1F, c.importedWingVerticalOffset - 0.05F), () -> c.importedWingVerticalOffset = Math.min(1F, c.importedWingVerticalOffset + 0.05F));
                row("Pack back", () -> String.format("%.2f", c.importedWingBackOffset), () -> c.importedWingBackOffset = Math.max(-0.3F, c.importedWingBackOffset - 0.05F), () -> c.importedWingBackOffset = Math.min(1F, c.importedWingBackOffset + 0.05F));
                row("Pack tint", () -> hex(c.importedWingTintArgb), () -> c.importedWingTintArgb = previousColor(c.importedWingTintArgb), () -> c.importedWingTintArgb = nextColor(c.importedWingTintArgb));
            }
            case "weapon_models" -> {
                row("Diamond set", () -> weaponThemeName(c.diamondWeaponTheme), () -> c.diamondWeaponTheme = Math.floorMod(c.diamondWeaponTheme - 1, 3), () -> c.diamondWeaponTheme = Math.floorMod(c.diamondWeaponTheme + 1, 3));
                row("Netherite set", () -> weaponThemeName(c.netheriteWeaponTheme), () -> c.netheriteWeaponTheme = Math.floorMod(c.netheriteWeaponTheme - 1, 3), () -> c.netheriteWeaponTheme = Math.floorMod(c.netheriteWeaponTheme + 1, 3));
                row("Bow / Shield / Mace", () -> weaponThemeName(c.utilityWeaponTheme), () -> c.utilityWeaponTheme = Math.floorMod(c.utilityWeaponTheme - 1, 3), () -> c.utilityWeaponTheme = Math.floorMod(c.utilityWeaponTheme + 1, 3));
            }
            case "little_demon" -> {
                row("Scale", () -> String.format("%.2f", c.littleDemonScale), () -> c.littleDemonScale = Math.max(0.35F, c.littleDemonScale - 0.05F), () -> c.littleDemonScale = Math.min(2.5F, c.littleDemonScale + 0.05F));
                row("Vertical", () -> String.format("%.2f", c.littleDemonVerticalOffset), () -> c.littleDemonVerticalOffset = Math.max(-1F, c.littleDemonVerticalOffset - 0.05F), () -> c.littleDemonVerticalOffset = Math.min(1F, c.littleDemonVerticalOffset + 0.05F));
                row("Back offset", () -> String.format("%.2f", c.littleDemonBackOffset), () -> c.littleDemonBackOffset = Math.max(-0.3F, c.littleDemonBackOffset - 0.05F), () -> c.littleDemonBackOffset = Math.min(1F, c.littleDemonBackOffset + 0.05F));
                row("Tint", () -> hex(c.littleDemonTintArgb), () -> c.littleDemonTintArgb = previousColor(c.littleDemonTintArgb), () -> c.littleDemonTintArgb = nextColor(c.littleDemonTintArgb));
                row("Show others", () -> c.littleDemonShowOthers ? "ON" : "OFF", () -> c.littleDemonShowOthers = !c.littleDemonShowOthers, () -> c.littleDemonShowOthers = !c.littleDemonShowOthers);
            }
            case "back_weapon" -> {
                row("Style", () -> backWeaponStyleName(c.backWeaponStyle), () -> c.backWeaponStyle = Math.floorMod(c.backWeaponStyle - 1, 4), () -> c.backWeaponStyle = Math.floorMod(c.backWeaponStyle + 1, 4));
                row("Scale", () -> String.format("%.2f", c.backWeaponScale), () -> c.backWeaponScale = Math.max(0.45F, c.backWeaponScale - 0.05F), () -> c.backWeaponScale = Math.min(2.25F, c.backWeaponScale + 0.05F));
                row("Angle", () -> String.format("%.0f°", c.backWeaponAngle), () -> c.backWeaponAngle = Math.max(-80F, c.backWeaponAngle - 5F), () -> c.backWeaponAngle = Math.min(80F, c.backWeaponAngle + 5F));
                row("Vertical offset", () -> String.format("%.2f", c.backWeaponOffsetY), () -> c.backWeaponOffsetY = Math.max(-0.8F, c.backWeaponOffsetY - 0.05F), () -> c.backWeaponOffsetY = Math.min(0.8F, c.backWeaponOffsetY + 0.05F));
                row("Opacity", () -> Integer.toString(c.backWeaponOpacity), () -> c.backWeaponOpacity = Math.max(40, c.backWeaponOpacity - 10), () -> c.backWeaponOpacity = Math.min(255, c.backWeaponOpacity + 10));
                row("Primary", () -> hex(c.backWeaponPrimaryArgb), () -> c.backWeaponPrimaryArgb = previousColor(c.backWeaponPrimaryArgb), () -> c.backWeaponPrimaryArgb = nextColor(c.backWeaponPrimaryArgb));
                row("Secondary", () -> hex(c.backWeaponSecondaryArgb), () -> c.backWeaponSecondaryArgb = previousColor(c.backWeaponSecondaryArgb), () -> c.backWeaponSecondaryArgb = nextColor(c.backWeaponSecondaryArgb));
                row("Glow", () -> c.backWeaponGlow ? "ON" : "OFF", () -> c.backWeaponGlow = !c.backWeaponGlow, () -> c.backWeaponGlow = !c.backWeaponGlow);
                row("Rainbow", () -> c.backWeaponRainbow ? "ON" : "OFF", () -> c.backWeaponRainbow = !c.backWeaponRainbow, () -> c.backWeaponRainbow = !c.backWeaponRainbow);
                row("Show others", () -> c.backWeaponShowOthers ? "ON" : "OFF", () -> c.backWeaponShowOthers = !c.backWeaponShowOthers, () -> c.backWeaponShowOthers = !c.backWeaponShowOthers);
            }
            case "head_cosmetic" -> {
                row("Style", () -> headCosmeticStyleName(c.headCosmeticStyle), () -> c.headCosmeticStyle = Math.floorMod(c.headCosmeticStyle - 1, 4), () -> c.headCosmeticStyle = Math.floorMod(c.headCosmeticStyle + 1, 4));
                row("Scale", () -> String.format("%.2f", c.headCosmeticScale), () -> c.headCosmeticScale = Math.max(0.45F, c.headCosmeticScale - 0.05F), () -> c.headCosmeticScale = Math.min(2F, c.headCosmeticScale + 0.05F));
                row("Opacity", () -> Integer.toString(c.headCosmeticOpacity), () -> c.headCosmeticOpacity = Math.max(40, c.headCosmeticOpacity - 10), () -> c.headCosmeticOpacity = Math.min(255, c.headCosmeticOpacity + 10));
                row("Primary", () -> hex(c.headCosmeticPrimaryArgb), () -> c.headCosmeticPrimaryArgb = previousColor(c.headCosmeticPrimaryArgb), () -> c.headCosmeticPrimaryArgb = nextColor(c.headCosmeticPrimaryArgb));
                row("Secondary", () -> hex(c.headCosmeticSecondaryArgb), () -> c.headCosmeticSecondaryArgb = previousColor(c.headCosmeticSecondaryArgb), () -> c.headCosmeticSecondaryArgb = nextColor(c.headCosmeticSecondaryArgb));
                row("Glow", () -> c.headCosmeticGlow ? "ON" : "OFF", () -> c.headCosmeticGlow = !c.headCosmeticGlow, () -> c.headCosmeticGlow = !c.headCosmeticGlow);
                row("Rainbow", () -> c.headCosmeticRainbow ? "ON" : "OFF", () -> c.headCosmeticRainbow = !c.headCosmeticRainbow, () -> c.headCosmeticRainbow = !c.headCosmeticRainbow);
                row("Show others", () -> c.headCosmeticShowOthers ? "ON" : "OFF", () -> c.headCosmeticShowOthers = !c.headCosmeticShowOthers, () -> c.headCosmeticShowOthers = !c.headCosmeticShowOthers);
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
            case "health_display" -> {
                row("Scale", () -> String.format("%.2f", c.healthHudScale), () -> c.healthHudScale = Math.max(0.55F, c.healthHudScale - 0.05F), () -> c.healthHudScale = Math.min(2.0F, c.healthHudScale + 0.05F));
                row("Background", () -> c.healthHudBackground ? "ON" : "OFF", () -> c.healthHudBackground = !c.healthHudBackground, () -> c.healthHudBackground = !c.healthHudBackground);
            }
            case "armor_display" -> {
                row("Scale", () -> String.format("%.2f", c.armorHudScale), () -> c.armorHudScale = Math.max(0.55F, c.armorHudScale - 0.05F), () -> c.armorHudScale = Math.min(2.0F, c.armorHudScale + 0.05F));
                row("Background", () -> c.armorHudBackground ? "ON" : "OFF", () -> c.armorHudBackground = !c.armorHudBackground, () -> c.armorHudBackground = !c.armorHudBackground);
            }
            case "map_display" -> {
                row("Scale", () -> String.format("%.2f", c.mapHudScale), () -> c.mapHudScale = Math.max(0.55F, c.mapHudScale - 0.05F), () -> c.mapHudScale = Math.min(2.0F, c.mapHudScale + 0.05F));
                row("Background", () -> c.mapHudBackground ? "ON" : "OFF", () -> c.mapHudBackground = !c.mapHudBackground, () -> c.mapHudBackground = !c.mapHudBackground);
            }
            case "ping_display" -> {
                row("Scale", () -> String.format("%.2f", c.pingHudScale), () -> c.pingHudScale = Math.max(0.55F, c.pingHudScale - 0.05F), () -> c.pingHudScale = Math.min(2.0F, c.pingHudScale + 0.05F));
                row("Background", () -> c.pingHudBackground ? "ON" : "OFF", () -> c.pingHudBackground = !c.pingHudBackground, () -> c.pingHudBackground = !c.pingHudBackground);
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
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);

        g.fill(0, 0, width, height, 0xAA000000);
        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        g.pose().scale(uiScale(), uiScale());

        g.fill(0, 0, PANEL_W, PANEL_H, cfg.panelArgb);
        g.fill(0, 0, PANEL_W, 3, Theme.accent());

        g.text(font, UiFont.text(module.icon() + "  " + module.name().toUpperCase()), 32, 23, 0xFFFFFFFF, true);
        g.text(font, UiFont.trim(font, module.description(), 360), 32, 43, cfg.mutedTextArgb, false);

        int toggleColor = module.enabled() ? Theme.accent() : 0xFF555563;
        g.fill(PANEL_W - 126, 20, PANEL_W - 32, 46, module.enabled() ? 0xFF272337 : 0xFF1C1C24);
        g.fill(PANEL_W - 126, 45, PANEL_W - 32, 46, toggleColor);
        g.centeredText(font, UiFont.text(module.enabled() ? "ENABLED" : "DISABLED"), PANEL_W - 79, 29, toggleColor);

        g.text(font, UiFont.text("SETTINGS"), 32, 72, 0xFF707082, false);

        clampScroll();
        int rowY = 92;
        if (rows.isEmpty()) {
            g.text(font, UiFont.text("This module has no extra settings yet."), 32, rowY + 8, 0xFF858596, false);
        }

        int first = scrollRow;
        int last = Math.min(rows.size(), first + VISIBLE_ROWS);
        for (int i = first; i < last; i++) {
            Row row = rows.get(i);
            g.fill(32, rowY, PANEL_W - 32, rowY + 34, 0xFF171720);
            g.text(font, UiFont.text(row.label()), 46, rowY + 12, 0xFFCBCBD6, false);
            g.text(font, UiFont.text(row.value().get()), 260, rowY + 12, Theme.accent(), true);
            drawMini(g, mx, my, 402, rowY + 5, "−");
            drawMini(g, mx, my, 446, rowY + 5, "+");
            rowY += 38;
        }

        if (rows.size() > VISIBLE_ROWS) {
            int trackX = PANEL_W - 23;
            int trackY = 92;
            int trackH = VISIBLE_ROWS * 38 - 4;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFF252530);
            int thumbH = Math.max(26, trackH * VISIBLE_ROWS / rows.size());
            int maxScroll = rows.size() - VISIBLE_ROWS;
            int thumbY = trackY + (trackH - thumbH) * scrollRow / Math.max(1, maxScroll);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, Theme.accent());
            g.text(font, UiFont.text("Mouse wheel for more settings"), 340, 72, 0xFF656576, false);
        }

        if (isHudModule()) {
            drawBottomButton(g, mx, my, 32, PANEL_H - 52, 164, "Open HUD workspace");
        } else if ("waypoints".equals(module.id())) {
            drawBottomButton(g, mx, my, 32, PANEL_H - 52, 164, "Manage waypoints");
        }
        drawBottomButton(g, mx, my, PANEL_W - 196, PANEL_H - 52, 164, "Reset module settings");

        g.text(font, UiFont.text("Scale " + String.format("%.2fx", uiScale()) + "  •  ESC / "
                + TopkaClient.openMenuKey().getString() + " to return"), 32, PANEL_H - 18, 0xFF616171, false);

        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void clampScroll() {
        scrollRow = Math.clamp(scrollRow, 0, Math.max(0, rows.size() - VISIBLE_ROWS));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = localMouseX(mouseX);
        int my = localMouseY(mouseY);
        if (mx >= 24 && mx < PANEL_W - 24 && my >= 82 && my < 405) {
            int before = scrollRow;
            if (scrollY < 0) scrollRow++;
            if (scrollY > 0) scrollRow--;
            clampScroll();
            if (before != scrollRow) {
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawMini(GuiGraphicsExtractor g, int mx, int my, int x, int y, String text) {
        boolean hover = mx >= x && mx < x + 36 && my >= y && my < y + 26;
        g.fill(x, y, x + 36, y + 26, hover ? 0xFF333340 : 0xFF24242E);
        g.centeredText(font, UiFont.text(text), x + 18, y + 9, 0xFFF0F0F5);
    }

    private void drawBottomButton(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 28;
        g.fill(x, y, x + w, y + 28, hover ? 0xFF30303C : 0xFF21212B);
        g.fill(x, y + 27, x + w, y + 28, Theme.accent());
        g.centeredText(font, UiFont.text(text), x + w / 2, y + 9, 0xFFEDEDF4);
    }

    private boolean isHudModule() {
        return module.category() == Module.Category.HUD;
    }

    private void addLocalClickTarget(int x, int y, int w, int h, Runnable action) {
        addWidget(Button.builder(Component.empty(), b -> action.run())
                .pos(sx(x), sy(y))
                .size(ss(w), ss(h))
                .build());
    }

    private void resetModuleSettings() {
        var c = TopkaClient.CONFIG.get();
        switch (module.id()) {
            case "crosshair" -> {
                c.crosshairColorArgb = 0xFFFFFFFF; c.crosshairSize = 5; c.crosshairGap = 2;
                c.crosshairThickness = 1; c.crosshairDot = false; c.crosshairOutline = true;
                c.crosshairDynamic = false; c.crosshairDynamicMaxGap = 10;
                c.crosshairStyle = 0; c.crosshairRainbow = false;
            }
            case "hitbox" -> {
                c.hitboxExpand = 0F; c.hitboxLineWidth = 2F; c.hitboxColorArgb = 0xFF41C7FF;
                c.hitboxPlayerColorArgb = 0xFF41C7FF; c.hitboxHostileColorArgb = 0xFFFF5C77;
                c.hitboxPassiveColorArgb = 0xFF50FA7B; c.hitboxOtherColorArgb = 0xFFFFD166;
                c.hitboxPlayers = true; c.hitboxHostile = true; c.hitboxPassive = true; c.hitboxOther = false;
            }
            case "china_hat" -> {
                c.chinaHatRadius = 0.66F; c.chinaHatHeight = 0.32F; c.chinaHatLineWidth = 2F;
                c.chinaHatShowOthers = false; c.chinaHatColorArgb = 0xFF8B5CF6; c.chinaHatStyle = 1; c.chinaHatRainbow = false;
            }
            case "halo" -> {
                c.haloRadius = 0.46F; c.haloHeight = 0.16F; c.haloLineWidth = 2.2F;
                c.haloColorArgb = 0xFF41C7FF; c.haloStyle = 1; c.haloRainbow = false;
                c.importedHaloScale = 1F; c.importedHaloHeight = 0.18F; c.importedHaloTintArgb = 0xFFFFFFFF;
            }
            case "trails" -> {
                c.trailLifetimeMs = 900; c.trailLineWidth = 2.2F; c.trailColorArgb = 0xFF8B5CF6;
                c.trailStyle = 1; c.trailHeight = 0.42F; c.trailWidth = 0.72F; c.trailLayers = 3;
                c.trailRainbow = false; c.trailGlow = true;
            }
            case "jump_circles" -> {
                c.jumpCircleLifetimeMs = 700; c.jumpCircleRadius = 1.15F; c.jumpCircleLineWidth = 2F;
                c.jumpCircleColorArgb = 0xFF41C7FF; c.jumpCircleStyle = 1; c.jumpCircleLayers = 2; c.jumpCircleRainbow = false;
            }
            case "jump_particles" -> { c.jumpParticleCount = 10; c.jumpParticleStyle = 0; }
            case "hit_particles" -> { c.hitParticleCount = 12; c.hitParticleStyle = 0; }
            case "full_bright" -> c.fullBrightGamma = 12D;
            case "viewmodel" -> {
                c.viewMainX = c.viewMainY = c.viewMainZ = 0F;
                c.viewOffX = c.viewOffY = c.viewOffZ = 0F;
                c.viewScale = 1F; c.viewPitch = c.viewYaw = c.viewRoll = 0F;
            }
            case "swing_animations" -> { c.swingMode = 0; c.swingStrength = 1F; }
            case "ambience" -> c.ambienceTime = 6000L;
            case "hit_color" -> c.hitColorArgb = 0xB28B5CF6;
            case "targeting" -> { c.targetColorArgb = 0xFFFF5C77; c.targetMode = 2; c.targetPulse = true; c.targetPadding = 0.12F; }
            case "sprint" -> c.sprintStopWhileUsingItem = true;
            case "health_tags" -> { c.healthTagHearts = true; c.healthTagMaxDistance = 48D; }
            case "projectile_prediction" -> { c.projectileColorArgb = 0xFF41C7FF; c.projectileLineWidth = 2F; c.projectileSteps = 72; }
            case "baby_mode" -> { c.babyScale = 0.65F; c.babyModeOthers = false; }
            case "cape" -> {
                c.capeColorArgb = 0xFF8B5CF6; c.capeWidth = 0.64F; c.capeHeight = 1.05F; c.capeLineWidth = 2F;
                c.capeShowOthers = false; c.capeStyle = 0; c.capeOpacity = 155; c.capeRainbow = false; c.capeGlow = true;
            }
            case "wings" -> {
                c.wingsStyle = 0; c.wingsScale = 1.0F; c.wingsSpread = 0.95F;
                c.wingsFlapSpeed = 1.0F; c.wingsFlapAmount = 0.16F; c.wingsOpacity = 235;
                c.wingsPrimaryColorArgb = 0xFFFFFFFF; c.wingsSecondaryColorArgb = 0xFFC8CDD8;
                c.wingsRainbow = false; c.wingsGlow = false; c.wingsShowOthers = false;
                c.wingsDetail = 5; c.wingsDepth = 0.12F; c.wingsBoneWidth = 1.8F;
                c.wingsVerticalOffset = 0F; c.wingsBackOffset = 0.12F; c.wingsTilt = 0F; c.wingsFold = 0F;
                c.importedWingScale = 1F; c.importedWingVerticalOffset = 0F; c.importedWingBackOffset = 0.10F;
                c.importedWingTintArgb = 0xFFFFFFFF;
            }
            case "weapon_models" -> {
                c.diamondWeaponTheme = 2; c.netheriteWeaponTheme = 1; c.utilityWeaponTheme = 1;
            }
            case "little_demon" -> {
                c.littleDemonScale = 1F; c.littleDemonVerticalOffset = 0F; c.littleDemonBackOffset = 0.08F;
                c.littleDemonTintArgb = 0xFFFFFFFF; c.littleDemonShowOthers = false;
            }
            case "back_weapon" -> {
                c.backWeaponStyle = 0; c.backWeaponScale = 1.0F; c.backWeaponAngle = 36F; c.backWeaponOffsetY = 0F;
                c.backWeaponPrimaryArgb = 0xFFB7C7FF; c.backWeaponSecondaryArgb = 0xFF8B5CF6;
                c.backWeaponOpacity = 225; c.backWeaponGlow = true; c.backWeaponRainbow = false; c.backWeaponShowOthers = false;
            }
            case "head_cosmetic" -> {
                c.headCosmeticStyle = 0; c.headCosmeticScale = 1.0F;
                c.headCosmeticPrimaryArgb = 0xFFFFD166; c.headCosmeticSecondaryArgb = 0xFFFF8A3D;
                c.headCosmeticOpacity = 225; c.headCosmeticGlow = true; c.headCosmeticRainbow = false; c.headCosmeticShowOthers = false;
            }
            case "drop_protection" -> { c.dropProtectionWindowMs = 1800L; c.protectArmor = true; c.protectTools = true; c.protectTotems = true; c.protectNamedItems = true; }
            case "auto_gg" -> { c.autoGgMessage = "gg"; c.autoGgCooldownMs = 15000L; }
            case "item_color" -> {
                c.itemHighlightColorArgb = 0xFF8B5CF6;
                c.highlightedItems.clear();
                c.highlightedItems.add("minecraft:totem_of_undying");
                c.highlightedItems.add("minecraft:enchanted_golden_apple");
                c.highlightedItems.add("minecraft:elytra");
            }
            case "health_display" -> { c.healthHudScale = 1.0F; c.healthHudBackground = true; }
            case "armor_display" -> { c.armorHudScale = 1.0F; c.armorHudBackground = true; }
            case "map_display" -> { c.mapHudScale = 1.0F; c.mapHudBackground = true; }
            case "ping_display" -> { c.pingHudScale = 1.0F; c.pingHudBackground = true; }
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

    private static String capeStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 1 -> "SPLIT";
            case 2 -> "ROYAL";
            case 3 -> "ENERGY";
            default -> "FABRIC";
        };
    }

    private static String backWeaponStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 1 -> "KATANA";
            case 2 -> "CRYSTAL";
            case 3 -> "SCYTHE";
            default -> "GREAT SWORD";
        };
    }

    private static String headCosmeticStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 1 -> "HORNS";
            case 2 -> "ANTLERS";
            case 3 -> "ARCANE";
            default -> "CROWN";
        };
    }

    private static String wingStyleName(int style) {
        return switch (Math.floorMod(style, 9)) {
            case 1 -> "DEMON";
            case 2 -> "CRYSTAL";
            case 3 -> "DRAGON";
            case 4 -> "TECH";
            case 5 -> "PACK WING I";
            case 6 -> "PACK WING II";
            case 7 -> "PACK WING III";
            case 8 -> "PACK WING IV";
            default -> "ANGEL";
        };
    }

    private static String weaponThemeName(int theme) {
        return switch (Math.floorMod(theme, 3)) {
            case 1 -> "ONI";
            case 2 -> "ENDER EYE";
            default -> "VANILLA";
        };
    }

    private static String crosshairStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 1 -> "BRACKETS";
            case 2 -> "T-SHAPE";
            case 3 -> "DOT";
            default -> "CLASSIC";
        };
    }

    private static String hatStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 0 -> "OUTLINE";
            case 1 -> "MESH";
            case 2 -> "DENSE";
            default -> "AURA";
        };
    }

    private static String haloStyleName(int style) {
        return switch (Math.floorMod(style, 5)) {
            case 0 -> "SINGLE";
            case 1 -> "GLOW";
            case 2 -> "TRIPLE";
            case 3 -> "PULSE";
            default -> "PACK HALO";
        };
    }

    private static String trailStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 0 -> "LINE";
            case 1 -> "RIBBON";
            case 2 -> "WINGS";
            default -> "BEAM";
        };
    }

    private static String ringStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 0 -> "SINGLE";
            case 1 -> "GLOW";
            case 2 -> "STACK";
            default -> "PULSE";
        };
    }

    private static String particleStyleName(int style) {
        return switch (Math.floorMod(style, 4)) {
            case 1 -> "HEARTS";
            case 2 -> "SPARKS";
            case 3 -> "CRITS";
            default -> "MIXED";
        };
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
