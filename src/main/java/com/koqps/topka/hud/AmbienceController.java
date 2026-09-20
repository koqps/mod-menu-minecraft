package com.koqps.topka.hud;

/**
 * Keeps the latest server-provided day time so the ambience mixin can return
 * vanilla time instantly when the module is disabled without mutating the
 * client world every frame.
 */
public final class AmbienceController {
    private static long serverDayTime;

    private AmbienceController() { }

    public static void tick() {
        // Rendering reads the time through ClientLevelTimeMixin.
    }

    public static void onServerTime(long dayTime) {
        serverDayTime = dayTime;
    }

    public static long serverDayTime() {
        return serverDayTime;
    }
}
