# Mod Menu 0.5.0 — Minecraft 26.3 Fabric / Java

**Mod Menu** is an original client-side visual, HUD, PvP-feedback, and quality-of-life mod for Minecraft Java 26.3. It is inspired by the polish and convenience of modern PvP clients, but all code and visuals in this project are independently implemented.

## Toolchain
- Minecraft Java 26.3
- Java 25
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Gradle 9.5.1 bootstrap

## Main features
- Same rebindable key opens and closes the menu (Right Shift by default)
- Draggable, searchable module menu with category navigation
- Per-module settings screens and instant config persistence
- Theme Studio and draggable HUD workspace
- Health, armor, coordinates, ping and custom crosshair HUDs
- Health tags above nearby players
- Custom hitboxes and targeting outline
- Hit color and No Hurt Cam
- Auto Sprint
- China Hat, Halo / Nimb, trails, jump circles and particles
- Hit particles
- Full Bright and client-side ambience time
- ViewModel and visual swing animation presets
- Visual Baby Mode
- Original animated cape outline
- Projectile trajectory / impact preview
- Persistent server/world + dimension waypoints with labels and beams
- Protected-item double-drop confirmation
- Rate-limited Auto GG

## Build
Install Java/JDK 25, then from PowerShell:

```powershell
.\gradlew.bat clean build
```

The normal mod JAR is created in `build/libs/`. Do **not** use the `-sources.jar`.

## Install
Put the normal Mod Menu JAR and Fabric API for Minecraft 26.3 in:

```text
%appdata%\.minecraft\mods
```

Launch your Fabric Loader 26.3 profile.

## Controls
The default menu key is **Right Shift**. Rebind it through:

**Options → Controls → Key Binds → Mod Menu**

The same key opens and closes the menu.

## Configuration
Settings are persisted to:

```text
.minecraft/config/mod-menu.json
```

## Verification
GitHub Actions builds under Java 25, runs a clean Gradle build, checks that Fabric metadata, entrypoints, mixins, menus, and render classes are actually present in the normal JAR, and uploads the compiled JAR as an artifact.
