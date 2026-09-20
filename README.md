# Mod Menu 0.6.0

A polished client-side Minecraft Java mod for **Minecraft 26.3 + Fabric**, built in Java 25.

Mod Menu focuses on PvP visuals, HUD customization, movement quality-of-life features, world overlays, and cosmetics. It is an original implementation inspired by the type of customization available in visual PvP clients; it does not bundle proprietary Topka code or assets.

## Target

- Minecraft Java 26.3
- Java 25
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Default menu key: Right Shift
- Config: `.minecraft/config/mod-menu.json`

## Core client

- Same rebindable key opens and closes the menu
- Draggable client window with saved position
- Searchable module library
- Combat / Visual / Player / Movement / HUD / World / Misc categories
- Left-click module toggles
- Right-click module settings
- Scrollable advanced settings
- Automatic persistent configuration
- Config recovery and validation
- Java 25 GitHub Actions build + JAR structure verification

## Visual and combat presentation

- No Hurt Cam
- Hit Color
- Categorized custom hitboxes
  - players
  - hostile mobs
  - passive mobs
  - other entities
  - independent colors and toggles
  - expansion and line width
- Targeting indicator
  - box
  - ring
  - both
  - pulse and padding
- Custom crosshair
  - replaces vanilla crosshair when active
  - size / gap / thickness
  - color / outline / dot
  - smooth movement-reactive dynamic spread
- Smooth China Hat
- Halo / Nimb
- Player trails
- Jump circles
- Jump particles
- Hit particles
- Full Bright
- ViewModel transforms
- Swing animation styles
- Client-side ambience time
- Baby Mode visual scaling
- Cosmetic cape outline

## HUD

A dedicated HUD workspace supports draggable and scalable panels.

- Health + absorption
- Armor icons + durability
- Location / coordinates / direction / dimension
- Ping
- Health tags
- Independent HUD scaling
- Per-widget background toggles
- 4-pixel snapping
- Mouse-wheel scaling in the HUD workspace

## Theme Studio

- Accent presets
- Fine RGB controls
- Secondary accent
- Static mode
- Animated gradient mode
- Animated rainbow mode
- Animation-speed controls
- HUD opacity
- panel opacity
- colors propagate throughout the menu/HUD

## World / player utilities

- Persistent waypoints
- Waypoint beams and labels
- Projectile trajectory prediction + impact marker
- Drop protection with confirmation
- Inventory item highlighting
- Rate-limited Auto GG
- Auto Sprint

## Build

Windows PowerShell:

```powershell
java -version
.\gradlew.bat clean build
```

Java must report **25**.

The normal mod JAR is generated in:

```text
build\libs\
```

Use `mod-menu-0.6.0.jar`, not the `-sources.jar`.

## Install

1. Install Fabric Loader for Minecraft 26.3.
2. Put the matching Fabric API JAR in `%appdata%\.minecraft\mods`.
3. Put `mod-menu-0.6.0.jar` in the same folder.
4. Remove older Mod Menu test JARs to avoid duplicate mod IDs.
5. Launch the Fabric 26.3 profile.
6. Press **Right Shift** by default.
7. Rebind the menu key under Minecraft Controls -> Key Binds -> Mod Menu if desired.

## Validation

Every push to `main` runs a Java 25 GitHub Actions build. CI also verifies that the normal JAR contains the Fabric metadata, mixin config, client entrypoint, menu UI, HUD classes, and required mixins before uploading the build artifact.

Minecraft runtime testing on a real client is still the final validation step for graphics-driver and mod-interaction behavior.
