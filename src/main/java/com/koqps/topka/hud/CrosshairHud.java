package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;

public final class CrosshairHud {
    private CrosshairHud() { }

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (!TopkaClient.MODULES.byId("crosshair").enabled()) {
                original.extractRenderState(graphics, deltaTracker);
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.gui.screen() != null) return;

            var cfg = TopkaClient.CONFIG.get();
            int cx = client.getWindow().getGuiScaledWidth() / 2;
            int cy = client.getWindow().getGuiScaledHeight() / 2;
            int size = Math.max(1, cfg.crosshairSize);
            int gap = Math.max(0, cfg.crosshairGap);
            int t = Math.max(1, cfg.crosshairThickness);

            if (cfg.crosshairOutline) {
                drawCross(graphics, cx, cy, size + 1, gap, t + 2, cfg.crosshairOutlineArgb);
            }
            drawCross(graphics, cx, cy, size, gap, t, cfg.crosshairColorArgb);

            if (cfg.crosshairDot) {
                if (cfg.crosshairOutline) graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, cfg.crosshairOutlineArgb);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, cfg.crosshairColorArgb);
            }
        });
    }

    private static void drawCross(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int cx, int cy, int size, int gap, int t, int color) {
        graphics.fill(cx - t / 2, cy - gap - size, cx + (t + 1) / 2, cy - gap, color);
        graphics.fill(cx - t / 2, cy + gap, cx + (t + 1) / 2, cy + gap + size, color);
        graphics.fill(cx - gap - size, cy - t / 2, cx - gap, cy + (t + 1) / 2, color);
        graphics.fill(cx + gap, cy - t / 2, cx + gap + size, cy + (t + 1) / 2, color);
    }
}
