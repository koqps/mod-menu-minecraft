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
        int x = cfg.healthHudX, y = cfg.healthHudY;
        int w = 146, h = 40;
        int accent = Theme.accent();

        graphics.fill(x, y, x + w, y + h, cfg.hudBackgroundArgb);
        graphics.fill(x, y, x + 3, y + h, accent);
        graphics.text(client.font, "HEALTH", x + 10, y + 6, cfg.mutedTextArgb, false);

        String value = String.format("%.1f", health) + (absorption > 0.05F ? "  +" + String.format("%.1f", absorption) : "");
        graphics.text(client.font, value, x + 90, y + 6, cfg.textArgb, true);

        graphics.fill(x + 10, y + 23, x + 136, y + 29, 0xFF292934);
        graphics.fill(x + 10, y + 23, x + 10 + Math.round(126 * ratio), y + 29, accent);
        if (absorption > 0.05F) {
            int start = x + 10 + Math.round(126 * ratio);
            int end = Math.min(x + 136, start + Math.round(126 * absorbRatio));
            graphics.fill(start, y + 23, end, y + 29, 0xFFFFD166);
        }
        graphics.text(client.font, Math.round(health) + " / " + Math.round(max), x + 10, y + 32, 0xFF767688, false);
    }
}
