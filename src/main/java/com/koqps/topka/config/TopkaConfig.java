package com.koqps.topka.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TopkaConfig {
    public int cosmeticRendererVersion = 3;
    public Map<String, Boolean> modules = new HashMap<>();
    public List<WaypointConfig> waypoints = new ArrayList<>();
    public List<String> highlightedItems = new ArrayList<>(List.of(
            "minecraft:totem_of_undying",
            "minecraft:enchanted_golden_apple",
            "minecraft:elytra"
    ));

    // Theme
    public int accentArgb = 0xFF8B5CF6;
    public int secondaryAccentArgb = 0xFF41C7FF;
    public int panelArgb = 0xF20D0D13;
    public int sidebarArgb = 0xFF14141C;
    public int hudBackgroundArgb = 0xD9121219;
    public int textArgb = 0xFFF5F5FA;
    public int mutedTextArgb = 0xFF9292A4;
    public boolean rainbowTheme = false;
    public boolean gradientTheme = false;
    public float themeAnimationSpeed = 0.35F;
    public float menuScale = 1.0F;
    public int menuStyle = 0;

    // Combat / visuals
    public int hitColorArgb = 0xB28B5CF6;
    public int hitboxColorArgb = 0xFF41C7FF;
    public int hitboxPlayerColorArgb = 0xFF41C7FF;
    public int hitboxHostileColorArgb = 0xFFFF5C77;
    public int hitboxPassiveColorArgb = 0xFF50FA7B;
    public int hitboxOtherColorArgb = 0xFFFFD166;
    public boolean hitboxPlayers = true;
    public boolean hitboxHostile = true;
    public boolean hitboxPassive = true;
    public boolean hitboxOther = false;
    public int targetColorArgb = 0xFFFF5C77;
    public int targetMode = 2;
    public boolean targetPulse = true;
    public float targetPadding = 0.12F;
    public int trailColorArgb = 0xFF8B5CF6;
    public int jumpCircleColorArgb = 0xFF41C7FF;
    public float hitboxExpand = 0.00F;
    public float hitboxLineWidth = 2.0F;

    // China Hat / Halo
    public float chinaHatRadius = 0.66F;
    public float chinaHatHeight = 0.32F;
    public float chinaHatLineWidth = 2.0F;
    public boolean chinaHatShowOthers = false;
    public int chinaHatColorArgb = 0xFF8B5CF6;
    public int chinaHatStyle = 1;
    public boolean chinaHatRainbow = false;
    public float haloRadius = 0.46F;
    public float haloHeight = 0.16F;
    public float haloLineWidth = 2.2F;
    public int haloColorArgb = 0xFF41C7FF;
    public int haloStyle = 1;
    public boolean haloRainbow = false;

    // Trails / jump effects
    public int trailLifetimeMs = 900;
    public float trailLineWidth = 2.2F;
    public int trailStyle = 1;
    public float trailHeight = 0.42F;
    public float trailWidth = 0.72F;
    public int trailLayers = 3;
    public boolean trailRainbow = false;
    public boolean trailGlow = true;
    public int jumpCircleLifetimeMs = 700;
    public float jumpCircleRadius = 1.15F;
    public float jumpCircleLineWidth = 2.0F;
    public int jumpCircleStyle = 1;
    public int jumpCircleLayers = 2;
    public boolean jumpCircleRainbow = false;
    public int jumpParticleCount = 10;
    public int hitParticleCount = 12;
    public int jumpParticleStyle = 0;
    public int hitParticleStyle = 0;

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

    // Health tags
    public boolean healthTagHearts = true;
    public double healthTagMaxDistance = 48.0D;

    // Projectile prediction
    public int projectileColorArgb = 0xFF41C7FF;
    public float projectileLineWidth = 2.0F;
    public int projectileSteps = 72;

    // Baby mode
    public float babyScale = 0.65F;
    public boolean babyModeOthers = false;

    // Cape
    public int capeColorArgb = 0xFF8B5CF6;
    public float capeWidth = 0.64F;
    public float capeHeight = 1.05F;
    public float capeLineWidth = 2.0F;
    public boolean capeShowOthers = false;
    public int capeStyle = 0;
    public int capeOpacity = 155;
    public boolean capeRainbow = false;
    public boolean capeGlow = true;

    // Wings
    public int wingsStyle = 0;
    public float wingsScale = 1.0F;
    public float wingsSpread = 0.95F;
    public float wingsFlapSpeed = 1.0F;
    public float wingsFlapAmount = 0.16F;
    public int wingsPrimaryColorArgb = 0xFF8B5CF6;
    public int wingsSecondaryColorArgb = 0xFF41C7FF;
    public int wingsOpacity = 235;
    public boolean wingsRainbow = false;
    public boolean wingsGlow = false;
    public boolean wingsShowOthers = false;
    public int wingsDetail = 5;
    public float wingsDepth = 0.12F;
    public float wingsBoneWidth = 1.8F;
    public float wingsVerticalOffset = 0.0F;
    public float wingsBackOffset = 0.12F;
    public float wingsTilt = 0.0F;
    public float wingsFold = 0.0F;

    // Back weapon
    public int backWeaponStyle = 0;
    public float backWeaponScale = 1.0F;
    public float backWeaponAngle = 36.0F;
    public float backWeaponOffsetY = 0.0F;
    public int backWeaponPrimaryArgb = 0xFFB7C7FF;
    public int backWeaponSecondaryArgb = 0xFF8B5CF6;
    public int backWeaponOpacity = 225;
    public boolean backWeaponGlow = true;
    public boolean backWeaponRainbow = false;
    public boolean backWeaponShowOthers = false;

    // Head cosmetic
    public int headCosmeticStyle = 0;
    public float headCosmeticScale = 1.0F;
    public int headCosmeticPrimaryArgb = 0xFFFFD166;
    public int headCosmeticSecondaryArgb = 0xFFFF8A3D;
    public int headCosmeticOpacity = 225;
    public boolean headCosmeticGlow = true;
    public boolean headCosmeticRainbow = false;
    public boolean headCosmeticShowOthers = false;

    // Inventory item highlighting
    public int itemHighlightColorArgb = 0xFF8B5CF6;

    // Drop protection
    public long dropProtectionWindowMs = 1800L;
    public boolean protectArmor = true;
    public boolean protectTools = true;
    public boolean protectTotems = true;
    public boolean protectNamedItems = true;

    // Auto GG
    public String autoGgMessage = "gg";
    public long autoGgCooldownMs = 15000L;

    // HUD positions
    public int healthHudX = 12;
    public int healthHudY = 12;
    public int armorHudX = 12;
    public int armorHudY = 52;
    public int mapHudX = 12;
    public int mapHudY = 124;
    public int pingHudX = 12;
    public int pingHudY = 178;
    public float healthHudScale = 1.0F;
    public float armorHudScale = 1.0F;
    public float mapHudScale = 1.0F;
    public float pingHudScale = 1.0F;
    public boolean healthHudBackground = true;
    public boolean armorHudBackground = true;
    public boolean mapHudBackground = true;
    public boolean pingHudBackground = true;

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
    public boolean crosshairDynamic = false;
    public int crosshairDynamicMaxGap = 10;
    public int crosshairStyle = 0;
    public boolean crosshairRainbow = false;

    // Movement
    public boolean sprintAlways = true;
    public boolean sprintStopWhileUsingItem = true;
}
