# Mod Menu 0.7.0

A polished client-side Minecraft Java mod for **Minecraft 26.3 + Fabric**, built for Java 25.

Mod Menu is an original visual/PvP/QoL client project. It is inspired by the presentation and customization expected from modern PvP clients, but does not include proprietary Topka source code or assets.

## Target

- Minecraft Java 26.3
- Java 25
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Default menu key: Right Shift
- Config: `.minecraft/config/mod-menu.json`

## 0.7.0 highlights

### Client menu
- Search field is now embedded directly into the client header and uses the same drag/scale coordinate system as the rest of the window.
- Main client UI can be resized from **0.70x to 1.35x**.
- The same interface scale also applies to Theme Studio, module settings and the waypoint manager; HUD widgets retain their own independent scales.
- Four menu designs:
  - Pro
  - Neon
  - Glass
  - Minimal
- Static, animated gradient, and rainbow theme modes.
- Secondary accent, RGB tuning, opacity and animation-speed controls.
- Dedicated `topka:ui` font resource is used by the main menu, Theme Studio, module settings, HUD workspace and waypoint UI so the client does not automatically inherit the normal `minecraft:default` font from a texture pack.
- Draggable window position remains persistent.
- Search, module click targets and right-click settings stay functional while the main window is scaled.

### Ambience / custom time
- Replaced the old per-tick `setTimeFromServer` rewrite with a client render-time `dayTime()` override.
- Incoming server time is still cached for vanilla restoration.
- This removes the server-packet vs client-tick race that caused custom day/night to flicker.

### Trails
Trail rendering has been expanded from a thin line into a configurable visual system:
- LINE
- RIBBON
- WINGS
- BEAM
- width
- height
- line thickness
- lifetime
- 1–6 layers
- glow
- rainbow
- color

The default Ribbon preset is deliberately larger and more luminous, closer to the wide wake/ribbon look shown in the visual reference.

### Jump circles
- SINGLE
- GLOW
- STACK
- PULSE
- radius
- lifetime
- thickness
- layers
- rainbow
- color

The Glow style uses a bright core plus wider translucent rings for the luminous ground-circle look.

### China Hat
- OUTLINE
- MESH
- DENSE
- AURA
- radius
- cone height
- line width
- color
- rainbow
- self / other-player support

### Halo / Nimb
- SINGLE
- GLOW
- TRIPLE
- PULSE
- radius
- height
- thickness
- color
- rainbow

### Crosshair
When enabled, Mod Menu still suppresses the vanilla crosshair completely.

Styles:
- CLASSIC
- BRACKETS
- T-SHAPE
- DOT

Settings:
- size
- center gap
- thickness
- color
- rainbow
- center dot
- outline
- dynamic spread
- dynamic maximum gap

### Particles
Hit and jump particles now support:
- MIXED
- HEARTS
- SPARKS
- CRITS
- separate particle count controls

### Health tags
- Reworked for Minecraft 26.3 deferred name-tag submission.
- Displays a compact **heart strip only** above nearby players.
- No numeric HP label is added by Mod Menu.
- Color transitions from green toward red as health drops.
- Configurable maximum distance.

### HUD
The HUD workspace supports draggable/scalable:
- Health + absorption
- Armor icons + durability
- Location / coordinates / direction / dimension
- Ping

Each panel has its own scale and background toggle. Mouse wheel over a panel in the HUD workspace changes its size.

## Other included modules

- No Hurt Cam
- Hit Color
- custom categorized hitboxes
- target box/ring indicator
- Auto Sprint
- Full Bright
- ViewModel
- Swing Animations
- Baby Mode
- cape visual
- waypoints
- waypoint labels/beams
- projectile prediction
- item highlighting
- drop protection
- Auto GG

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

Install the normal `mod-menu-0.7.0.jar`, not `mod-menu-0.7.0-sources.jar`.

## Install

1. Install Fabric Loader for Minecraft 26.3.
2. Put Fabric API matching Minecraft 26.3 in `%appdata%\.minecraft\mods`.
3. Put `mod-menu-0.7.0.jar` in the same folder.
4. Remove older Mod Menu / Topka development JARs so the internal mod id is not duplicated.
5. Launch the Fabric 26.3 profile.
6. Press Right Shift by default.
7. Rebind it under Controls -> Key Binds -> Mod Menu if desired.

## Validation

Every push to `main` runs a Java 25 GitHub Actions build.

CI verifies that the normal JAR contains:
- `fabric.mod.json`
- `topka.client.mixins.json`
- the client entrypoint
- No Hurt Cam mixin
- flicker-free ClientLevel time mixin
- main menu/HUD classes
- the dedicated UI font definition

A green CI build verifies compilation and packaging. Actual rendering appearance, graphics-driver behavior and compatibility with a user's complete mod/texture-pack stack still require an in-game launch test.
