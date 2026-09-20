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
        int x = cfg.mapHudX;
        int y = cfg.mapHudY;
        int w = 158;
        int h = 58;
        float scale = Math.clamp(cfg.mapHudScale, 0.55F, 2.0F);
        String dimension = client.level.dimension().identifier().getPath().replace('_', ' ').toUpperCase();
        String direction = client.player.getDirection().getName().toUpperCase();

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (cfg.mapHudBackground) {
            graphics.fill(0, 0, w, h, cfg.hudBackgroundArgb);
            graphics.fill(0, 0, 3, h, Theme.accent());
        }
        graphics.text(client.font, "LOCATION", 10, 6, cfg.mutedTextArgb, false);
        graphics.text(
                client.font,
                "X " + client.player.getBlockX() + "  Y " + client.player.getBlockY() + "  Z " + client.player.getBlockZ(),
                10,
                20,
                cfg.textArgb,
                false
        );
        graphics.text(client.font, direction, 10, 34, Theme.accent(), true);
        graphics.text(client.font, dimension, 56, 34, 0xFF77778A, false);
        graphics.fill(10, 49, 148, 51, 0xFF292934);
        graphics.fill(67, 47, 71, 53, Theme.accent());

        graphics.pose().popMatrix();
    }
}
