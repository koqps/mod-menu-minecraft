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
        float ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, health / max));
        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.healthHudX, y = cfg.healthHudY;
        int w = 132, h = 34;
        int accent = Theme.accent();

        graphics.fill(x, y, x + w, y + h, 0xD9121219);
        graphics.fill(x, y, x + 3, y + h, accent);
        graphics.text(client.font, "HEALTH", x + 10, y + 6, 0xFF9C9CAE, false);
        graphics.text(client.font, Math.round(health) + " / " + Math.round(max), x + 82, y + 6, 0xFFF5F5FA, true);
        graphics.fill(x + 10, y + 22, x + 122, y + 27, 0xFF292934);
        graphics.fill(x + 10, y + 22, x + 10 + Math.round(112 * ratio), y + 27, accent);
    }
}
