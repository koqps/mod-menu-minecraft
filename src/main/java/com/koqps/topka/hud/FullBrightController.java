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
            // Minecraft 26.3 validates the Brightness option against its
            // vanilla 0..1 range. Writing values like 12.0 every tick causes
            // continuous "Illegal option value" log spam and the value is
            // rejected anyway. Keep this controller within the legal range.
            double desired = Math.clamp(TopkaClient.CONFIG.get().fullBrightGamma, 0.0D, 1.0D);
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
