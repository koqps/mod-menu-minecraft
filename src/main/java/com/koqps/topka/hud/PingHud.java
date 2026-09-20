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

        graphics.fill(x, y, x + w, y + h, cfg.hudBackgroundArgb);
        graphics.fill(x, y, x + 3, y + h, Theme.accent());
        graphics.text(client.font, "PING", x + 10, y + 6, 0xFF9999AA, false);
        graphics.text(client.font, ping + " ms", x + 48, y + 6, color, true);
        graphics.fill(x + 10, y + 20, x + 78, y + 23, 0xFF292934);
        int bar = Math.min(68, Math.max(3, 68 - Math.min(65, ping / 4)));
        graphics.fill(x + 10, y + 20, x + 10 + bar, y + 23, color);
    }
}
