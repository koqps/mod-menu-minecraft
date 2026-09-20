package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HealthHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TopkaClient.MOD_ID, "health_hud");
    private HealthHud() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, HealthHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!TopkaClient.MODULES.byId("health_display").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;

        float health = client.player.getHealth();
        float max = client.player.getMaxHealth();
        float absorption = client.player.getAbsorptionAmount();
        float ratio = max <= 0 ? 0 : Math.clamp(health / max, 0F, 1F);
        float absorbRatio = max <= 0 ? 0 : Math.clamp(absorption / max, 0F, 1F);

        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.healthHudX;
        int y = cfg.healthHudY;
        int w = 146;
        int h = 40;
        float scale = Math.clamp(cfg.healthHudScale, 0.55F, 2.0F);
        int accent = Theme.accent();

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (cfg.healthHudBackground) {
            graphics.fill(0, 0, w, h, cfg.hudBackgroundArgb);
            graphics.fill(0, 0, 3, h, accent);
        }

        graphics.text(client.font, "HEALTH", 10, 6, cfg.mutedTextArgb, false);

        String value = String.format("%.1f", health)
                + (absorption > 0.05F ? "  +" + String.format("%.1f", absorption) : "");
        graphics.text(client.font, value, 90, 6, cfg.textArgb, true);

        graphics.fill(10, 23, 136, 29, 0xFF292934);
        graphics.fill(10, 23, 10 + Math.round(126 * ratio), 29, accent);
        if (absorption > 0.05F) {
            int start = 10 + Math.round(126 * ratio);
            int end = Math.min(136, start + Math.round(126 * absorbRatio));
            graphics.fill(start, 23, end, 29, 0xFFFFD166);
        }
        graphics.text(client.font, Math.round(health) + " / " + Math.round(max), 10, 32, 0xFF767688, false);

        graphics.pose().popMatrix();
    }
}
