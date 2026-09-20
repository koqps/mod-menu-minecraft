package com.koqps.topka.config;

import java.util.HashMap;
import java.util.Map;

public final class TopkaConfig {
    public Map<String, Boolean> modules = new HashMap<>();

    public int accentArgb = 0xFF8B5CF6;
    public int hitColorArgb = 0xB28B5CF6;
    public int hitboxColorArgb = 0xFF41C7FF;
    public float hitboxExpand = 0.00F;

    public int healthHudX = 12;
    public int healthHudY = 12;
    public int armorHudX = 12;
    public int armorHudY = 48;
    public int mapHudX = 12;
    public int mapHudY = 112;

    public int menuOffsetX = 0;
    public int menuOffsetY = 0;

    public int crosshairColorArgb = 0xFFFFFFFF;
    public int crosshairSize = 5;
    public int crosshairGap = 2;
    public int crosshairThickness = 1;
    public boolean sprintAlways = true;
}
