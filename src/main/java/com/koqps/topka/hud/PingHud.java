package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;

public final class PingHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TopkaClient.MOD_ID, "ping_hud");

    private PingHud() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, PingHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!TopkaClient.MODULES.byId("ping_display").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null || client.getConnection() == null) return;

        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) return;

        int ping = Math.max(0, info.getLatency());
        int color = ping < 80 ? 0xFF58E38C : ping < 160 ? 0xFFFFD166 : 0xFFFF5C77;
        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.pingHudX;
        int y = cfg.pingHudY;
        int w = 88;
        int h = 28;
        float scale = Math.clamp(cfg.pingHudScale, 0.55F, 2.0F);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (cfg.pingHudBackground) {
            graphics.fill(0, 0, w, h, cfg.hudBackgroundArgb);
            graphics.fill(0, 0, 3, h, Theme.accent());
        }
        graphics.text(client.font, "PING", 10, 6, 0xFF9999AA, false);
        graphics.text(client.font, ping + " ms", 48, 6, color, true);
        graphics.fill(10, 20, 78, 23, 0xFF292934);
        int bar = Math.min(68, Math.max(3, 68 - Math.min(65, ping / 4)));
        graphics.fill(10, 20, 10 + bar, 23, color);

        graphics.pose().popMatrix();
    }
}
