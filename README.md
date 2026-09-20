# Mod Menu 0.4.3 — Minecraft 26.3 Fabric / Java

This is the next Java/Fabric build of the client formerly labeled Topka New. The visible name is now **Mod Menu**.

## 0.4.3 changes
- Same rebindable key opens **and closes** the client menu.
- Main client window is draggable by the top header.
- Reworked HUD layout with separate draggable Health, Armor, and Map panels.
- Theme/accent color presets affect the menu and HUD.
- Custom hitboxes are rendered independently with configurable color and expansion.
- Custom crosshair suppresses Minecraft's vanilla crosshair while enabled.
- China Hat cosmetic visual (world-space wireframe conical hat).
- Armor Display with durability percentages.
- Map Display with coordinates and facing direction.
- Hit flash, crosshair, hitbox, and theme colors can be cycled from the menu.

## Build on Windows
1. Install Java/JDK 25.
2. Open PowerShell in this folder.
3. Run `java -version` and confirm version 25.
4. Run `./gradlew.bat build`.
5. The normal mod JAR is created in `build/libs/` — do not use the `-sources.jar` file.

## Install
Put the built Mod Menu JAR and Fabric API for Minecraft 26.3 in `%appdata%/.minecraft/mods`, then launch the Fabric 26.3 profile.

The default menu key is Right Shift. Rebind it in Minecraft's Controls -> Key Binds -> Mod Menu.

Config is stored at `.minecraft/config/mod-menu.json`.

## 0.4.3 packaging fix
The project now uses one client-only source set so Fabric mixin classes are guaranteed to be packaged in the final JAR. This fixes the startup ClassNotFoundException for `NoHurtCamMixin`.
