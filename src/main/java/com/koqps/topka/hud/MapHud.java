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
        if (client.player == null || client.level == null || client.gui.screen() != null) return;

        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.mapHudX, y = cfg.mapHudY;
        int w = 158, h = 58;
        String dimension = client.level.dimension().identifier().getPath().replace('_', ' ').toUpperCase();
        String direction = client.player.getDirection().getName().toUpperCase();

        graphics.fill(x, y, x + w, y + h, cfg.hudBackgroundArgb);
        graphics.fill(x, y, x + 3, y + h, Theme.accent());
        graphics.text(client.font, "LOCATION", x + 10, y + 6, cfg.mutedTextArgb, false);
        graphics.text(client.font,
                "X " + client.player.getBlockX() + "  Y " + client.player.getBlockY() + "  Z " + client.player.getBlockZ(),
                x + 10, y + 20, cfg.textArgb, false);
        graphics.text(client.font, direction, x + 10, y + 34, Theme.accent(), true);
        graphics.text(client.font, dimension, x + 56, y + 34, 0xFF77778A, false);
        graphics.fill(x + 10, y + 49, x + 148, y + 51, 0xFF292934);
        int center = x + 79;
        graphics.fill(center - 2, y + 47, center + 2, y + 53, Theme.accent());
    }
}
