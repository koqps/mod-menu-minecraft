package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class CrosshairHud {
    private static float animatedGap;

    private CrosshairHud() { }

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (!TopkaClient.MODULES.byId("crosshair").enabled()) {
                animatedGap = 0.0F;
                original.extractRenderState(graphics, deltaTracker);
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.gui.screen() != null) return;

            var cfg = TopkaClient.CONFIG.get();
            int cx = client.getWindow().getGuiScaledWidth() / 2;
            int cy = client.getWindow().getGuiScaledHeight() / 2;
            int size = Math.max(1, cfg.crosshairSize);
            int gap = computeGap(client, cfg.crosshairGap, cfg.crosshairDynamic, cfg.crosshairDynamicMaxGap);
            int t = Math.max(1, cfg.crosshairThickness);
            int color = cfg.crosshairRainbow ? rainbowColor(0.0F) : cfg.crosshairColorArgb;

            if (cfg.crosshairStyle == 3) {
                drawDot(graphics, cx, cy, t, color, cfg.crosshairOutline, cfg.crosshairOutlineArgb);
                return;
            }

            if (cfg.crosshairOutline) {
                drawStyle(graphics, cfg.crosshairStyle, cx, cy, size + 1, gap, t + 2, cfg.crosshairOutlineArgb);
            }
            drawStyle(graphics, cfg.crosshairStyle, cx, cy, size, gap, t, color);

            if (cfg.crosshairDot) {
                drawDot(graphics, cx, cy, t, color, cfg.crosshairOutline, cfg.crosshairOutlineArgb);
            }
        });
    }

    private static void drawStyle(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int style,
            int cx,
            int cy,
            int size,
            int gap,
            int thickness,
            int color
    ) {
        switch (Math.floorMod(style, 4)) {
            case 1 -> drawBrackets(graphics, cx, cy, size, gap, thickness, color);
            case 2 -> drawTShape(graphics, cx, cy, size, gap, thickness, color);
            default -> drawCross(graphics, cx, cy, size, gap, thickness, color);
        }
    }

    private static void drawDot(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int cx,
            int cy,
            int thickness,
            int color,
            boolean outline,
            int outlineColor
    ) {
        int radius = Math.max(1, thickness);
        if (outline) {
            graphics.fill(cx - radius - 1, cy - radius - 1, cx + radius + 2, cy + radius + 2, outlineColor);
        }
        graphics.fill(cx - radius, cy - radius, cx + radius + 1, cy + radius + 1, color);
    }

    private static int computeGap(Minecraft client, int baseGap, boolean dynamic, int maxGap) {
        if (!dynamic || client.player == null) {
            animatedGap += (baseGap - animatedGap) * 0.24F;
            return Math.max(0, Math.round(animatedGap));
        }

        Vec3 movement = client.player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        float extra = (float) Math.min(
                Math.max(0, maxGap - baseGap),
                horizontalSpeed * 18.0D
                        + (client.player.onGround() ? 0.0D : 2.0D)
                        + (client.player.isSprinting() ? 1.5D : 0.0D)
        );
        float target = Math.min(maxGap, baseGap + extra);
        animatedGap += (target - animatedGap) * 0.24F;
        return Math.max(0, Math.round(animatedGap));
    }

    private static void drawCross(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int cx,
            int cy,
            int size,
            int gap,
            int thickness,
            int color
    ) {
        graphics.fill(cx - thickness / 2, cy - gap - size, cx + (thickness + 1) / 2, cy - gap, color);
        graphics.fill(cx - thickness / 2, cy + gap, cx + (thickness + 1) / 2, cy + gap + size, color);
        graphics.fill(cx - gap - size, cy - thickness / 2, cx - gap, cy + (thickness + 1) / 2, color);
        graphics.fill(cx + gap, cy - thickness / 2, cx + gap + size, cy + (thickness + 1) / 2, color);
    }

    private static void drawBrackets(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int cx,
            int cy,
            int size,
            int gap,
            int thickness,
            int color
    ) {
        graphics.fill(cx - gap - size, cy - thickness / 2, cx - gap, cy + (thickness + 1) / 2, color);
        graphics.fill(cx + gap, cy - thickness / 2, cx + gap + size, cy + (thickness + 1) / 2, color);

        int cap = Math.max(2, size / 2);
        graphics.fill(cx - gap - size, cy - cap, cx - gap - size + thickness, cy + cap + 1, color);
        graphics.fill(cx + gap + size - thickness, cy - cap, cx + gap + size, cy + cap + 1, color);
    }

    private static void drawTShape(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int cx,
            int cy,
            int size,
            int gap,
            int thickness,
            int color
    ) {
        graphics.fill(cx - gap - size, cy - thickness / 2, cx - gap, cy + (thickness + 1) / 2, color);
        graphics.fill(cx + gap, cy - thickness / 2, cx + gap + size, cy + (thickness + 1) / 2, color);
        graphics.fill(cx - thickness / 2, cy + gap, cx + (thickness + 1) / 2, cy + gap + size, color);
    }

    private static int rainbowColor(float offset) {
        float hue = (System.currentTimeMillis() / 3000.0F + offset) % 1.0F;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.78F, 1.0F) & 0x00FFFFFF);
    }
}
