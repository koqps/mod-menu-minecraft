package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.Minecraft;

public final class FullBrightController {
    private static boolean applied;
    private static double previousGamma = 1.0D;

    private FullBrightController() { }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        boolean enabled = TopkaClient.MODULES.byId("full_bright").enabled();

        if (enabled && !applied) {
            previousGamma = client.options.gamma().get();
            applied = true;
        }

        if (enabled) {
            double desired = Math.clamp(TopkaClient.CONFIG.get().fullBrightGamma, 1.0D, 16.0D);
            if (Math.abs(client.options.gamma().get() - desired) > 0.001D) {
                client.options.gamma().set(desired);
            }
        } else if (applied) {
            client.options.gamma().set(previousGamma);
            applied = false;
        }
    }

    public static void restore() {
        if (!applied) return;
        Minecraft client = Minecraft.getInstance();
        client.options.gamma().set(previousGamma);
        applied = false;
    }
}
