package com.koqps.topka.config;

import java.util.HashMap;
import java.util.Map;

public final class TopkaConfig {
    public Map<String, Boolean> modules = new HashMap<>();

    // Theme
    public int accentArgb = 0xFF8B5CF6;
    public int secondaryAccentArgb = 0xFF41C7FF;
    public int panelArgb = 0xF20D0D13;
    public int sidebarArgb = 0xFF14141C;
    public int hudBackgroundArgb = 0xD9121219;
    public int textArgb = 0xFFF5F5FA;
    public int mutedTextArgb = 0xFF9292A4;

    // Combat / visuals
    public int hitColorArgb = 0xB28B5CF6;
    public int hitboxColorArgb = 0xFF41C7FF;
    public int targetColorArgb = 0xFFFF5C77;
    public int trailColorArgb = 0xFF8B5CF6;
    public int jumpCircleColorArgb = 0xFF41C7FF;
    public float hitboxExpand = 0.00F;
    public float hitboxLineWidth = 2.0F;

    // China Hat / Halo
    public float chinaHatRadius = 0.66F;
    public float chinaHatHeight = 0.32F;
    public float chinaHatLineWidth = 2.0F;
    public boolean chinaHatShowOthers = false;
    public float haloRadius = 0.46F;
    public float haloHeight = 0.16F;
    public float haloLineWidth = 2.2F;

    // Trails / jump effects
    public int trailLifetimeMs = 900;
    public float trailLineWidth = 2.2F;
    public int jumpCircleLifetimeMs = 700;
    public float jumpCircleRadius = 1.15F;
    public float jumpCircleLineWidth = 2.0F;
    public int jumpParticleCount = 10;
    public int hitParticleCount = 12;

    // Projectile trajectory
    public int trajectoryColorArgb = 0xFF50FA7B;
    public float trajectoryLineWidth = 2.0F;
    public int trajectorySteps = 56;

    // Full bright
    public double fullBrightGamma = 12.0D;

    // ViewModel / swing
    public float viewMainX = 0.0F;
    public float viewMainY = 0.0F;
    public float viewMainZ = 0.0F;
    public float viewOffX = 0.0F;
    public float viewOffY = 0.0F;
    public float viewOffZ = 0.0F;
    public float viewScale = 1.0F;
    public float viewPitch = 0.0F;
    public float viewYaw = 0.0F;
    public float viewRoll = 0.0F;
    public int swingMode = 0;
    public float swingStrength = 1.0F;

    // Ambience
    public long ambienceTime = 6000L;

    // HUD positions
    public int healthHudX = 12;
    public int healthHudY = 12;
    public int armorHudX = 12;
    public int armorHudY = 52;
    public int mapHudX = 12;
    public int mapHudY = 124;
    public int pingHudX = 12;
    public int pingHudY = 178;

    // Menu
    public int menuOffsetX = 0;
    public int menuOffsetY = 0;

    // Crosshair
    public int crosshairColorArgb = 0xFFFFFFFF;
    public int crosshairOutlineArgb = 0xCC000000;
    public int crosshairSize = 5;
    public int crosshairGap = 2;
    public int crosshairThickness = 1;
    public boolean crosshairDot = false;
    public boolean crosshairOutline = true;

    // Movement
    public boolean sprintAlways = true;
    public boolean sprintStopWhileUsingItem = true;
}
