package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.Minecraft;

public final class AmbienceController {
    private static boolean applied;
    private static long previousTime;

    private AmbienceController() { }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            applied = false;
            return;
        }

        boolean enabled = TopkaClient.MODULES.byId("ambience").enabled();
        if (enabled) {
            if (!applied) {
                previousTime = client.level.getDayTime();
                applied = true;
            }
            client.level.setTimeFromServer(Math.floorMod(TopkaClient.CONFIG.get().ambienceTime, 24000L));
        } else if (applied) {
            client.level.setTimeFromServer(previousTime);
            applied = false;
        }
    }
}
