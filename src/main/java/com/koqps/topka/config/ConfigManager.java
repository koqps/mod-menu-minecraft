package com.koqps.topka.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.koqps.topka.TopkaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mod-menu.json");
    private static final Path TEMP_PATH = PATH.resolveSibling("mod-menu.json.tmp");

    private TopkaConfig config = new TopkaConfig();

    public TopkaConfig get() {
        return config;
    }

    public void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH, StandardCharsets.UTF_8);
                TopkaConfig loaded = GSON.fromJson(json, TopkaConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (Exception exception) {
                backupBrokenConfig();
                config = new TopkaConfig();
            }
        }

        normalize();

        TopkaClient.MODULES.all().forEach(module -> {
            Boolean state = config.modules.get(module.id());
            if (state != null) {
                module.setEnabled(state);
            }
            config.modules.put(module.id(), module.enabled());
        });

        save();
    }

    public void save() {
        normalize();
        TopkaClient.MODULES.all().forEach(module -> config.modules.put(module.id(), module.enabled()));

        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(
                    TEMP_PATH,
                    GSON.toJson(config),
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        TEMP_PATH,
                        PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        TEMP_PATH,
                        PATH,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException ignored) {
            try {
                Files.deleteIfExists(TEMP_PATH);
            } catch (IOException ignoredAgain) {
                // Nothing else to do. A later save will retry.
            }
        }
    }

    public void resetToDefaults() {
        config = new TopkaConfig();
        TopkaClient.MODULES.all().forEach(module -> {
            Boolean defaultState = config.modules.get(module.id());
            if (defaultState != null) {
                module.setEnabled(defaultState);
            }
        });
        save();
    }

    private void normalize() {
        if (config.modules == null) config.modules = new HashMap<>();

        // v2 switches the old transparent debug-quad wing preset to the new
        // textured solid material. Preserve user colors/size while upgrading
        // the visual defaults once.
        if (config.cosmeticRendererVersion < 2) {
            config.wingsOpacity = Math.max(config.wingsOpacity, 220);
            config.wingsDetail = Math.max(config.wingsDetail, 4);
            if (Math.abs(config.wingsBoneWidth - 3.2F) < 0.01F) config.wingsBoneWidth = 2.4F;
            config.cosmeticRendererVersion = 2;
        }

        // v3 replaces the broad fan-like Angel profile with separated,
        // pointed 3D feather solids. Reduce the old structural glow and depth
        // automatically so existing configs do not preserve the mesh look.
        if (config.cosmeticRendererVersion < 3) {
            if (config.wingsStyle == 0) {
                config.wingsGlow = false;
                config.wingsDetail = Math.max(config.wingsDetail, 5);
                config.wingsDepth = Math.min(config.wingsDepth <= 0.0F ? 0.12F : config.wingsDepth, 0.14F);
                config.wingsBoneWidth = Math.min(config.wingsBoneWidth <= 0.0F ? 1.8F : config.wingsBoneWidth, 2.0F);
                config.wingsOpacity = Math.max(config.wingsOpacity, 230);
            }
            config.cosmeticRendererVersion = 3;
        }

        // v4 uses the uploaded Blockbench Angel model/texture as the visual
        // reference. Only replace the old stock purple/cyan colors; user-made
        // color choices remain untouched.
        if (config.cosmeticRendererVersion < 4) {
            if (config.wingsStyle == 0) {
                if (config.wingsPrimaryColorArgb == 0xFF8B5CF6) config.wingsPrimaryColorArgb = 0xFFFFFFFF;
                if (config.wingsSecondaryColorArgb == 0xFF41C7FF) config.wingsSecondaryColorArgb = 0xFFC8CDD8;
                config.wingsGlow = false;
                config.wingsDetail = 5;
                config.wingsDepth = Math.min(config.wingsDepth, 0.12F);
                config.wingsBoneWidth = Math.min(config.wingsBoneWidth, 1.8F);
                config.wingsOpacity = Math.max(config.wingsOpacity, 235);
            }
            config.cosmeticRendererVersion = 4;
        }

        if (config.cosmeticRendererVersion < 5) {
            config.diamondWeaponTheme = 1;
            config.netheriteWeaponTheme = 1;
            config.utilityWeaponTheme = 1;
            config.cosmeticRendererVersion = 5;
        }

        if (config.cosmeticRendererVersion < 6) {
            // The Ender Eye item pack is retired because several tool meshes use
            // incompatible pivots. Existing Ender selections migrate to Oni.
            if (config.diamondWeaponTheme == 2) config.diamondWeaponTheme = 1;
            if (config.netheriteWeaponTheme == 2) config.netheriteWeaponTheme = 1;
            if (config.utilityWeaponTheme == 2) config.utilityWeaponTheme = 1;
            config.importedWingTintArgb = 0xFFFFFFFF;
            config.wingsOpacity = Math.max(config.wingsOpacity, 230);
            config.armorCosmeticStyle = Math.clamp(config.armorCosmeticStyle, 1, 2);
            config.cosmeticRendererVersion = 6;
        }

        if (config.cosmeticRendererVersion < 7) {
            // Migrate the old rigid whole-body armor selection into four actual
            // Minecraft armor/body slots. The new renderer skins these parts to
            // the animated player model instead of drawing one static OBJ shell.
            int style = Math.clamp(config.armorCosmeticStyle, 1, 2);
            config.armorHelmetStyle = style;
            config.armorChestStyle = style;
            config.armorLeggingsStyle = style;
            config.armorBootsStyle = style;
            config.cosmeticRendererVersion = 7;
        }

        if (config.cosmeticRendererVersion < 8) {
            // Imported wing OBJ files did not include their source textures, so
            // those variants produced rectangular/grid geometry. Map any old
            // imported selection onto the equivalent stable modeled family.
            if (config.wingsStyle >= 5) config.wingsStyle = Math.floorMod(config.wingsStyle, 5);
            config.importedWingTintArgb = 0xFFFFFFFF;
            config.cosmeticRendererVersion = 8;
        }

        if (config.cosmeticRendererVersion < 9) {
            // Move everybody back to the uploaded Angel-wing presentation so
            // configs left on the old Crystal/Tech debug-like wings don't keep
            // rendering the fan/grid silhouette.
            config.wingsStyle = 0;
            config.wingsPrimaryColorArgb = 0xFFFFFFFF;
            config.wingsSecondaryColorArgb = 0xFFC8CDD8;
            config.wingsGlow = false;
            config.wingsRainbow = false;
            config.wingsOpacity = 245;
            config.wingsDetail = 5;
            config.wingsDepth = Math.min(config.wingsDepth, 0.10F);
            config.cosmeticRendererVersion = 9;
        }

        if (config.cosmeticRendererVersion < 10) {
            // Use the user's replacement set-wings ZIP directly.
            config.wingsStyle = 0;
            config.wingsPrimaryColorArgb = 0xFFFFFFFF;
            config.wingsSecondaryColorArgb = 0xFFC8CDD8;
            config.wingsOpacity = 245;
            config.wingsGlow = false;
            config.wingsRainbow = false;
            config.importedWingScale = 1.0F;
            config.importedWingVerticalOffset = 0.0F;
            config.importedWingBackOffset = 0.10F;
            config.cosmeticRendererVersion = 10;
        }
        if (config.waypoints == null) config.waypoints = new ArrayList<>();
        if (config.highlightedItems == null) config.highlightedItems = new ArrayList<>();

        config.themeAnimationSpeed = Math.clamp(config.themeAnimationSpeed, 0.05F, 2.0F);
        config.menuScale = Math.clamp(config.menuScale, 0.70F, 1.35F);
        config.menuStyle = Math.clamp(config.menuStyle, 0, 3);
        if (config.rainbowTheme && config.gradientTheme) config.gradientTheme = false;

        config.targetMode = Math.clamp(config.targetMode, 0, 2);
        config.targetPadding = Math.clamp(config.targetPadding, 0.0F, 0.75F);

        config.hitboxExpand = Math.clamp(config.hitboxExpand, 0.0F, 1.0F);
        config.hitboxLineWidth = Math.clamp(config.hitboxLineWidth, 1.0F, 6.0F);

        config.chinaHatRadius = Math.clamp(config.chinaHatRadius, 0.25F, 1.25F);
        config.chinaHatHeight = Math.clamp(config.chinaHatHeight, 0.12F, 0.8F);
        config.chinaHatLineWidth = Math.clamp(config.chinaHatLineWidth, 1.0F, 7.0F);
        config.chinaHatStyle = Math.clamp(config.chinaHatStyle, 0, 3);

        config.haloRadius = Math.clamp(config.haloRadius, 0.2F, 1.0F);
        config.haloHeight = Math.clamp(config.haloHeight, 0.0F, 0.7F);
        config.haloLineWidth = Math.clamp(config.haloLineWidth, 1.0F, 7.0F);
        config.haloStyle = Math.clamp(config.haloStyle, 0, 4);

        config.trailLifetimeMs = Math.clamp(config.trailLifetimeMs, 250, 3000);
        config.trailLineWidth = Math.clamp(config.trailLineWidth, 1.0F, 12.0F);
        config.trailStyle = Math.clamp(config.trailStyle, 0, 3);
        config.trailHeight = Math.clamp(config.trailHeight, 0.05F, 1.8F);
        config.trailWidth = Math.clamp(config.trailWidth, 0.10F, 2.5F);
        config.trailLayers = Math.clamp(config.trailLayers, 1, 6);
        config.jumpCircleLifetimeMs = Math.clamp(config.jumpCircleLifetimeMs, 250, 2500);
        config.jumpCircleRadius = Math.clamp(config.jumpCircleRadius, 0.3F, 4.0F);
        config.jumpCircleLineWidth = Math.clamp(config.jumpCircleLineWidth, 1.0F, 10.0F);
        config.jumpCircleStyle = Math.clamp(config.jumpCircleStyle, 0, 3);
        config.jumpCircleLayers = Math.clamp(config.jumpCircleLayers, 1, 5);
        config.jumpParticleCount = Math.clamp(config.jumpParticleCount, 1, 48);
        config.hitParticleCount = Math.clamp(config.hitParticleCount, 1, 64);
        config.jumpParticleStyle = Math.clamp(config.jumpParticleStyle, 0, 3);
        config.hitParticleStyle = Math.clamp(config.hitParticleStyle, 0, 3);

        config.fullBrightGamma = Math.clamp(config.fullBrightGamma, 1.0D, 16.0D);

        config.viewMainX = Math.clamp(config.viewMainX, -1.0F, 1.0F);
        config.viewMainY = Math.clamp(config.viewMainY, -1.0F, 1.0F);
        config.viewMainZ = Math.clamp(config.viewMainZ, -1.0F, 1.0F);
        config.viewOffX = Math.clamp(config.viewOffX, -1.0F, 1.0F);
        config.viewOffY = Math.clamp(config.viewOffY, -1.0F, 1.0F);
        config.viewOffZ = Math.clamp(config.viewOffZ, -1.0F, 1.0F);
        config.viewScale = Math.clamp(config.viewScale, 0.35F, 2.0F);
        config.swingStrength = Math.clamp(config.swingStrength, 0.0F, 2.0F);

        config.healthTagMaxDistance = Math.clamp(config.healthTagMaxDistance, 8.0D, 128.0D);

        config.projectileSteps = Math.clamp(config.projectileSteps, 16, 160);
        config.projectileLineWidth = Math.clamp(config.projectileLineWidth, 1.0F, 5.0F);

        config.babyScale = Math.clamp(config.babyScale, 0.35F, 1.0F);

        config.capeWidth = Math.clamp(config.capeWidth, 0.30F, 1.20F);
        config.capeHeight = Math.clamp(config.capeHeight, 0.45F, 1.60F);
        config.capeLineWidth = Math.clamp(config.capeLineWidth, 1.0F, 8.0F);
        config.capeStyle = Math.clamp(config.capeStyle, 0, 3);
        config.capeOpacity = Math.clamp(config.capeOpacity, 30, 235);

        config.wingsStyle = Math.clamp(config.wingsStyle, 0, 4);
        config.wingsScale = Math.clamp(config.wingsScale, 0.45F, 2.25F);
        config.wingsSpread = Math.clamp(config.wingsSpread, 0.35F, 1.65F);
        config.wingsFlapSpeed = Math.clamp(config.wingsFlapSpeed, 0.10F, 3.0F);
        config.wingsFlapAmount = Math.clamp(config.wingsFlapAmount, 0.0F, 0.55F);
        config.wingsOpacity = Math.clamp(config.wingsOpacity, 30, 235);
        config.wingsDetail = Math.clamp(config.wingsDetail, 1, 5);
        config.wingsDepth = Math.clamp(config.wingsDepth, 0.02F, 0.42F);
        config.wingsBoneWidth = Math.clamp(config.wingsBoneWidth, 1.0F, 8.0F);
        config.wingsVerticalOffset = Math.clamp(config.wingsVerticalOffset, -0.75F, 0.75F);
        config.wingsBackOffset = Math.clamp(config.wingsBackOffset, -0.20F, 0.75F);
        config.wingsTilt = Math.clamp(config.wingsTilt, -35.0F, 35.0F);
        config.wingsFold = Math.clamp(config.wingsFold, 0.0F, 0.70F);

        config.diamondWeaponTheme = Math.clamp(config.diamondWeaponTheme, 0, 1);
        config.netheriteWeaponTheme = Math.clamp(config.netheriteWeaponTheme, 0, 1);
        config.utilityWeaponTheme = Math.clamp(config.utilityWeaponTheme, 0, 1);

        config.importedWingScale = Math.clamp(config.importedWingScale, 0.35F, 2.5F);
        config.importedWingVerticalOffset = Math.clamp(config.importedWingVerticalOffset, -1.0F, 1.0F);
        config.importedWingBackOffset = Math.clamp(config.importedWingBackOffset, -0.3F, 1.0F);
        config.importedHaloScale = Math.clamp(config.importedHaloScale, 0.35F, 2.5F);
        config.importedHaloHeight = Math.clamp(config.importedHaloHeight, -0.5F, 1.5F);
        config.littleDemonScale = Math.clamp(config.littleDemonScale, 0.35F, 2.5F);
        config.littleDemonVerticalOffset = Math.clamp(config.littleDemonVerticalOffset, -1.0F, 1.0F);
        config.littleDemonBackOffset = Math.clamp(config.littleDemonBackOffset, -0.3F, 1.0F);

        config.armorCosmeticStyle = Math.clamp(config.armorCosmeticStyle, 1, 2);
        config.armorHelmetStyle = Math.clamp(config.armorHelmetStyle, 0, 2);
        config.armorChestStyle = Math.clamp(config.armorChestStyle, 0, 2);
        config.armorLeggingsStyle = Math.clamp(config.armorLeggingsStyle, 0, 2);
        config.armorBootsStyle = Math.clamp(config.armorBootsStyle, 0, 2);
        config.armorCosmeticScale = Math.clamp(config.armorCosmeticScale, 0.70F, 1.35F);
        config.armorCosmeticVerticalOffset = Math.clamp(config.armorCosmeticVerticalOffset, -0.35F, 0.35F);

        config.backWeaponStyle = Math.clamp(config.backWeaponStyle, 0, 3);
        config.backWeaponScale = Math.clamp(config.backWeaponScale, 0.45F, 2.25F);
        config.backWeaponAngle = Math.clamp(config.backWeaponAngle, -80.0F, 80.0F);
        config.backWeaponOffsetY = Math.clamp(config.backWeaponOffsetY, -0.8F, 0.8F);
        config.backWeaponOpacity = Math.clamp(config.backWeaponOpacity, 40, 255);

        config.headCosmeticStyle = Math.clamp(config.headCosmeticStyle, 0, 3);
        config.headCosmeticScale = Math.clamp(config.headCosmeticScale, 0.45F, 2.0F);
        config.headCosmeticOpacity = Math.clamp(config.headCosmeticOpacity, 40, 255);

        config.dropProtectionWindowMs = Math.clamp(config.dropProtectionWindowMs, 700L, 5000L);
        config.autoGgCooldownMs = Math.clamp(config.autoGgCooldownMs, 5000L, 60000L);
        if (config.autoGgMessage == null || config.autoGgMessage.isBlank()) {
            config.autoGgMessage = "gg";
        } else if (config.autoGgMessage.length() > 64) {
            config.autoGgMessage = config.autoGgMessage.substring(0, 64);
        }

        config.crosshairSize = Math.clamp(config.crosshairSize, 1, 16);
        config.crosshairGap = Math.clamp(config.crosshairGap, 0, 12);
        config.crosshairThickness = Math.clamp(config.crosshairThickness, 1, 6);
        config.crosshairDynamicMaxGap = Math.clamp(config.crosshairDynamicMaxGap, 2, 24);
        config.crosshairStyle = Math.clamp(config.crosshairStyle, 0, 3);

        config.healthHudScale = Math.clamp(config.healthHudScale, 0.55F, 2.0F);
        config.armorHudScale = Math.clamp(config.armorHudScale, 0.55F, 2.0F);
        config.mapHudScale = Math.clamp(config.mapHudScale, 0.55F, 2.0F);
        config.pingHudScale = Math.clamp(config.pingHudScale, 0.55F, 2.0F);

        config.menuOffsetX = Math.clamp(config.menuOffsetX, -4000, 4000);
        config.menuOffsetY = Math.clamp(config.menuOffsetY, -4000, 4000);

        config.ambienceTime = Math.floorMod(config.ambienceTime, 24000L);
    }

    private static void backupBrokenConfig() {
        try {
            if (!Files.exists(PATH)) return;

            String suffix = ".broken-" + Instant.now().toEpochMilli();
            Path backup = PATH.resolveSibling(PATH.getFileName() + suffix);
            Files.copy(PATH, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Loading still falls back to defaults even if the backup cannot be written.
        }
    }
}
