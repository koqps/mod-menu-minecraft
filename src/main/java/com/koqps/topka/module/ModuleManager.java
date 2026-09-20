package com.koqps.topka.module;

import java.util.List;

public final class ModuleManager {
    private final List<Module> modules = List.of(
        new Module("health_display", "Health Display", "Compact health bar and numeric health.", "♥", Module.Category.HUD, true),
        new Module("armor_display", "Armor Display", "Shows equipped armor and durability.", "◆", Module.Category.HUD, true),
        new Module("map_display", "Map Display", "Compact coordinate and direction map panel.", "⌖", Module.Category.HUD, false),
        new Module("hit_color", "Hit Color", "Recolors the vanilla entity hurt flash.", "✦", Module.Category.VISUAL, true),
        new Module("hitbox", "Hitboxes", "Entity hitboxes with configurable size and color.", "□", Module.Category.VISUAL, false),
        new Module("china_hat", "China Hat", "Cosmetic conical hat visual.", "△", Module.Category.VISUAL, false),
        new Module("no_hurt_cam", "No Hurt Cam", "Disables camera tilt when you take damage.", "◉", Module.Category.COMBAT, true),
        new Module("sprint", "Sprint", "Client-side automatic sprint.", "»", Module.Category.MOVEMENT, true),
        new Module("crosshair", "Crosshair", "Custom crosshair; hides vanilla crosshair while enabled.", "+", Module.Category.HUD, false)
    );

    public List<Module> all() { return modules; }

    public Module byId(String id) {
        return modules.stream().filter(module -> module.id().equals(id)).findFirst().orElseThrow();
    }
}
