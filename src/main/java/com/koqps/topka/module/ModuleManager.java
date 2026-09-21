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
        new Module("viewmodel", "ViewModel", "Customize first-person item position and scale.", "◇", Module.Category.VISUAL, false),
        new Module("swing_animations", "Swing Animations", "Extra visual-only first-person swing styles.", "↗", Module.Category.VISUAL, false),
        new Module("ambience", "Ambience", "Client-side visual time override.", "☾", Module.Category.VISUAL, false),
        new Module("baby_mode", "Baby Mode", "Purely visual player model scaling.", "♙", Module.Category.VISUAL, false),
        new Module("cape", "Cape", "Animated original cosmetic cape outline.", "▱", Module.Category.VISUAL, false),
        new Module("wings", "Wings", "Layered modeled wings with bones, feathers and membranes.", "✦", Module.Category.VISUAL, false),
        new Module("back_weapon", "Back Weapon", "Modeled sword, katana, crystal blade or scythe on your back.", "⚔", Module.Category.VISUAL, false),
        new Module("weapon_models", "Weapon Models", "Replace Diamond/Netherite and utility weapons with uploaded Oni or Ender Eye sets.", "⚒", Module.Category.VISUAL, true),
        new Module("head_cosmetic", "Head Cosmetic", "Crown, horns, antlers or arcane crest.", "♛", Module.Category.VISUAL, false),

        new Module("drop_protection", "Drop Protection", "Press Drop twice before protected items leave your hand.", "⛨", Module.Category.PLAYER, false),

        new Module("health_display", "Health Display", "Compact health bar and numeric health.", "♥", Module.Category.HUD, true),
        new Module("health_tags", "Health Tags", "Shows live HP above nearby players.", "❤", Module.Category.HUD, false),
        new Module("armor_display", "Armor Display", "Armor icons and durability percentages.", "◆", Module.Category.HUD, true),
        new Module("map_display", "Location HUD", "Coordinates and facing direction.", "⌖", Module.Category.HUD, false),
        new Module("ping_display", "Ping Display", "Compact latency HUD.", "⌁", Module.Category.HUD, false),
        new Module("crosshair", "Crosshair", "Custom crosshair that replaces vanilla.", "+", Module.Category.HUD, false),

        new Module("sprint", "Auto Sprint", "Client-side automatic sprint.", "»", Module.Category.MOVEMENT, true),

        new Module("projectile_prediction", "Projectile Prediction", "Visual trajectory and impact preview.", "⌁", Module.Category.WORLD, false),
        new Module("waypoints", "Waypoints", "Persistent world markers with distance labels.", "⚑", Module.Category.WORLD, false),

        new Module("item_color", "Item Color", "Highlights configured valuable items in inventories.", "▣", Module.Category.MISC, false),
        new Module("auto_gg", "Auto GG", "Rate-limited friendly message after detected wins.", "GG", Module.Category.MISC, false)
    );

    public List<Module> all() { return modules; }

    public Module byId(String id) {
        return modules.stream().filter(module -> module.id().equals(id)).findFirst().orElseThrow();
    }
}
