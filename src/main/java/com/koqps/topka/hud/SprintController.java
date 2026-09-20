package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.Minecraft;

public final class SprintController {
    private SprintController() { }

    public static void tick() {
        if (!TopkaClient.MODULES.byId("sprint").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;
        if (TopkaClient.CONFIG.get().sprintAlways && client.options.keyUp.isDown() && !client.player.isCrouching()) {
            client.player.setSprinting(true);
        }
    }
}
