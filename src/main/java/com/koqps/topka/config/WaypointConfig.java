package com.koqps.topka.config;

public final class WaypointConfig {
    public String name = "Waypoint";
    public int x;
    public int y;
    public int z;
    public String contextKey = "";
    public String dimension = "";
    public int colorArgb = 0xFF8B5CF6;
    public boolean enabled = true;

    public WaypointConfig() { }

    public WaypointConfig(String name, int x, int y, int z, String contextKey, String dimension, int colorArgb) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.contextKey = contextKey;
        this.dimension = dimension;
        this.colorArgb = colorArgb;
    }
}
