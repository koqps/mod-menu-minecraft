package com.koqps.topka.hud;

/**
 * Custom ambience time is now supplied by ClientLevelTimeMixin through the
 * renderer's day-time accessor. No per-tick world mutation is needed.
 */
public final class AmbienceController {
    private AmbienceController() { }

    public static void tick() {
        // Intentionally empty. Kept as a stable controller hook.
    }
}
