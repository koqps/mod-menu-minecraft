package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class MapHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TopkaClient.MOD_ID, "map_hud");
    private MapHud() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, MapHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!TopkaClient.MODULES.byId("map_display").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;
        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.mapHudX, y = cfg.mapHudY;
        int w = 132, h = 48;
        graphics.fill(x, y, x + w, y + h, 0xD9121219);
        graphics.fill(x, y, x + 3, y + h, Theme.accent());
        graphics.text(client.font, "MAP", x + 10, y + 6, 0xFF9C9CAE, false);
        graphics.text(client.font, "X " + client.player.getBlockX() + "   Z " + client.player.getBlockZ(), x + 10, y + 20, 0xFFF3F3F7, false);
        graphics.text(client.font, client.player.getDirection().getName().toUpperCase(), x + 10, y + 33, Theme.accent(), true);
    }
}
