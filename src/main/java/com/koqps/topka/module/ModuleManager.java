package com.koqps.topka.module;

import java.util.List;

public final class ModuleManager {
    private final List<Module> modules = List.of(
        new Module("no_hurt_cam", "No Hurt Cam", "Removes vanilla damage camera tilt.", "◉", Module.Category.COMBAT, true),
        new Module("targeting", "Targeting", "Highlights the entity under your crosshair.", "◎", Module.Category.COMBAT, false),

        new Module("hit_color", "Hit Color", "Recolors the vanilla hurt flash.", "✦", Module.Category.VISUAL, true),
        new Module("hitbox", "Hitboxes", "Custom entity boxes with configurable expansion.", "□", Module.Category.VISUAL, false),
        new Module("china_hat", "China Hat", "Smooth conical cosmetic hat.", "△", Module.Category.VISUAL, false),
        new Module("halo", "Halo / Nimb", "Animated halo above your player.", "○", Module.Category.VISUAL, false),
        new Module("trails", "Trails", "Fading movement trail behind your player.", "〰", Module.Category.VISUAL, false),
        new Module("jump_circles", "Jump Circles", "Animated rings whenever you jump.", "◌", Module.Category.VISUAL, false),
        new Module("jump_particles", "Jump Particles", "Particle burst whenever you jump.", "✧", Module.Category.VISUAL, false),
        new Module("hit_particles", "Hit Particles", "Extra particles when you attack an entity.", "✹", Module.Category.VISUAL, false),
        new Module("full_bright", "Full Bright", "Client-side brightness override.", "☀", Module.Category.VISUAL, false),

        new Module("health_display", "Health Display", "Compact health bar and numeric health.", "♥", Module.Category.HUD, true),
        new Module("armor_display", "Armor Display", "Armor icons and durability percentages.", "◆", Module.Category.HUD, true),
        new Module("map_display", "Location HUD", "Coordinates and facing direction.", "⌖", Module.Category.HUD, false),
        new Module("ping_display", "Ping Display", "Compact latency HUD.", "⌁", Module.Category.HUD, false),
        new Module("crosshair", "Crosshair", "Custom crosshair that replaces vanilla.", "+", Module.Category.HUD, false),

        new Module("sprint", "Auto Sprint", "Client-side automatic sprint.", "»", Module.Category.MOVEMENT, true)
    );

    public List<Module> all() { return modules; }

    public Module byId(String id) {
        return modules.stream().filter(module -> module.id().equals(id)).findFirst().orElseThrow();
    }
}
