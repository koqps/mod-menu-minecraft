package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.WaypointConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WaypointController {
    private static final int[] COLORS = {
            0xFF8B5CF6, 0xFF41C7FF, 0xFF50FA7B, 0xFFFF5C77,
            0xFFFFD166, 0xFFFF7AD9, 0xFFFF8A3D, 0xFFFFFFFF
    };

    private WaypointController() { }

    public static boolean addCurrent() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;

        String context = currentContextKey(client);
        if (context == null) return false;

        String dimension = client.level.dimension().identifier().toString();
        int index = TopkaClient.CONFIG.get().waypoints.size() + 1;
        WaypointConfig waypoint = new WaypointConfig(
                "Waypoint " + index,
                client.player.getBlockX(),
                client.player.getBlockY(),
                client.player.getBlockZ(),
                context,
                dimension,
                COLORS[(index - 1) % COLORS.length]
        );
        TopkaClient.CONFIG.get().waypoints.add(waypoint);
        TopkaClient.CONFIG.save();
        return true;
    }

    public static List<WaypointConfig> visibleWaypoints() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        String context = currentContextKey(client);
        if (context == null) return List.of();
        String dimension = client.level.dimension().identifier().toString();

        List<WaypointConfig> result = new ArrayList<>();
        for (WaypointConfig waypoint : TopkaClient.CONFIG.get().waypoints) {
            if (!waypoint.enabled) continue;
            if (!context.equals(waypoint.contextKey)) continue;
            if (!dimension.equals(waypoint.dimension)) continue;
            result.add(waypoint);
        }
        return result;
    }

    public static String currentContextKey(Minecraft client) {
        if (client == null || client.level == null) return null;

        if (client.isMultiplayerServer()) {
            ServerData server = client.getCurrentServer();
            if (server == null || server.ip == null || server.ip.isBlank()) return null;
            return "server:" + server.ip.trim().toLowerCase(Locale.ROOT);
        }

        if (client.isLocalServer() && client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            Path directory = client.getSingleplayerServer().getServerDirectory();
            Path name = directory.getFileName();
            if (name != null && !name.toString().isBlank()) return "world:" + name;
        }

        return "local";
    }

    public static void cycleColor(WaypointConfig waypoint) {
        int current = waypoint.colorArgb | 0xFF000000;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == current) {
                waypoint.colorArgb = COLORS[(i + 1) % COLORS.length];
                TopkaClient.CONFIG.save();
                return;
            }
        }
        waypoint.colorArgb = COLORS[0];
        TopkaClient.CONFIG.save();
    }

    public static void remove(WaypointConfig waypoint) {
        TopkaClient.CONFIG.get().waypoints.remove(waypoint);
        TopkaClient.CONFIG.save();
    }
}
