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
        if (config.waypoints == null) config.waypoints = new ArrayList<>();
        if (config.highlightedItems == null) config.highlightedItems = new ArrayList<>();

        config.themeAnimationSpeed = Math.clamp(config.themeAnimationSpeed, 0.05F, 2.0F);
        if (config.rainbowTheme && config.gradientTheme) config.gradientTheme = false;

        config.hitboxExpand = Math.clamp(config.hitboxExpand, 0.0F, 1.0F);
        config.hitboxLineWidth = Math.clamp(config.hitboxLineWidth, 1.0F, 6.0F);

        config.chinaHatRadius = Math.clamp(config.chinaHatRadius, 0.25F, 1.25F);
        config.chinaHatHeight = Math.clamp(config.chinaHatHeight, 0.12F, 0.8F);
        config.chinaHatLineWidth = Math.clamp(config.chinaHatLineWidth, 1.0F, 5.0F);

        config.haloRadius = Math.clamp(config.haloRadius, 0.2F, 1.0F);
        config.haloHeight = Math.clamp(config.haloHeight, 0.0F, 0.7F);
        config.haloLineWidth = Math.clamp(config.haloLineWidth, 1.0F, 5.0F);

        config.trailLifetimeMs = Math.clamp(config.trailLifetimeMs, 250, 3000);
        config.trailLineWidth = Math.clamp(config.trailLineWidth, 1.0F, 6.0F);
        config.jumpCircleLifetimeMs = Math.clamp(config.jumpCircleLifetimeMs, 250, 2500);
        config.jumpCircleRadius = Math.clamp(config.jumpCircleRadius, 0.3F, 3.0F);
        config.jumpCircleLineWidth = Math.clamp(config.jumpCircleLineWidth, 1.0F, 6.0F);
        config.jumpParticleCount = Math.clamp(config.jumpParticleCount, 1, 32);
        config.hitParticleCount = Math.clamp(config.hitParticleCount, 1, 40);

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
        config.capeLineWidth = Math.clamp(config.capeLineWidth, 1.0F, 5.0F);

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
