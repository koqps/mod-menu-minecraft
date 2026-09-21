package com.koqps.topka;

import com.koqps.topka.config.ConfigManager;
import com.koqps.topka.hud.AmbienceController;
import com.koqps.topka.hud.ArmorHud;
import com.koqps.topka.hud.AutoGgController;
import com.koqps.topka.hud.CrosshairHud;
import com.koqps.topka.hud.CosmeticTextures;
import com.koqps.topka.hud.FullBrightController;
import com.koqps.topka.hud.HealthHud;
import com.koqps.topka.hud.HitColorController;
import com.koqps.topka.hud.HitboxController;
import com.koqps.topka.hud.ItemColorController;
import com.koqps.topka.hud.MapHud;
import com.koqps.topka.hud.PingHud;
import com.koqps.topka.hud.SprintController;
import com.koqps.topka.hud.VisualEffectsController;
import com.koqps.topka.hud.WorldLabels;
import com.koqps.topka.hud.WorldVisuals;
import com.koqps.topka.module.ModuleManager;
import com.koqps.topka.item.OniWeaponSpecialRenderer;
import com.koqps.topka.ui.TopkaScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class TopkaClient implements ClientModInitializer {
    public static final String MOD_ID = "topka";
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ConfigManager CONFIG = new ConfigManager();

    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        OniWeaponSpecialRenderer.init();

        HealthHud.register();
        ArmorHud.register();
        MapHud.register();
        PingHud.register();
        CrosshairHud.register();

        VisualEffectsController.register();
        WorldVisuals.register();
        WorldLabels.register();
        AutoGgController.register();
        ItemColorController.register();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.topka.open_menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT,
                category
        ));

        ClientLifecycleEvents.CLIENT_STARTED.register(CosmeticTextures::register);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HitColorController.tick();
            HitboxController.tick();
            SprintController.tick();
            FullBrightController.tick();
            AmbienceController.tick();
            VisualEffectsController.tick();

            while (openMenu.consumeClick()) {
                toggleMenu();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VisualEffectsController.clear();
            CONFIG.save();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            FullBrightController.restore();
            VisualEffectsController.clear();
            CosmeticTextures.close();
            CONFIG.save();
        });
    }

    private static void toggleMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof TopkaScreen menu) {
            minecraft.gui.setScreen(menu.parentScreen());
        } else {
            minecraft.gui.setScreen(new TopkaScreen(
                    Component.literal("Mod Menu"),
                    minecraft.gui.screen()
            ));
        }
    }

    public static Component openMenuKey() {
        return openMenu == null
                ? Component.literal("Right Shift")
                : openMenu.getTranslatedKeyMessage();
    }

    public static boolean menuKeyMatches(KeyEvent event) {
        return openMenu != null && openMenu.matches(event);
    }
}
