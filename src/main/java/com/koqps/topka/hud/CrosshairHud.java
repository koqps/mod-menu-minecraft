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

            if (cfg.crosshairOutline) {
                drawCross(graphics, cx, cy, size + 1, gap, t + 2, cfg.crosshairOutlineArgb);
            }
            drawCross(graphics, cx, cy, size, gap, t, cfg.crosshairColorArgb);

            if (cfg.crosshairDot) {
                if (cfg.crosshairOutline) {
                    graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, cfg.crosshairOutlineArgb);
                }
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, cfg.crosshairColorArgb);
            }
        });
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
}
