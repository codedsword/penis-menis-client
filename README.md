# Sinep Sinem Client

PvP utility client for Minecraft **26.2** (Fabric).

## Building

Needs JDK 25.

```sh
./gradlew build          # jar ends up in build/libs
./gradlew runClient      # dev client, every mixin is audited at startup
SINEPSINEM_QUICKPLAY="New World" ./gradlew runClient   # straight into a singleplayer world
```

## What's in it

**GUI**
- ClickGui (Right Shift) drawn with Dear ImGui, styled after legitclient.com. Search, animated
  toggles, inline settings, key binds (click the bind, press a key or mouse button, Esc/Del clears).
- ImGui output goes through Minecraft's own gui renderer, so it works on both the OpenGL and the
  Vulkan backend of 26.x.
- Android launchers (MojoLauncher, PojavLauncher) are supported: imgui-java only ships x86_64 linux
  natives, so the jar bundles its own Android arm64 build, made with `scripts/build-imgui-android.sh`.
- Title screen: the Minecraft logo is replaced (`LogoRenderer` mixin) and an **Alt Manager** button
  sits at half the screen width, at `height - button height` (`TitleScreen` mixin). Offline and
  access token alts, saved to `.minecraft/sinepsinem/alts.json`.

**Combat**
- KillAura: target priority, speed limited and sensitivity snapped silent rotations, ray trace,
  cooldown or CPS timing. Attacks are sent right after the rotation packet.
- MaceSwap: swaps to a mace for falling hits (picks density or breach), then back.
- AutoSpear: starts charging a spear while you run at a target and holds the aim for the whole
  charge, using the spear's own speed threshold.
- AutoBreezeCharge: wind charge at your feet on jump, and right before landing from big falls.
- AutoTotem, Criticals, KeyPearl.

**Movement**
- MovementFix, for silent rotations:
  - `Classic`: you are moved with the rotation's yaw but your keys are remapped so you still walk
    where the camera looks (the remapped keys are what the server gets).
  - `Penis`: no remapping, you move along the look vector of the silent rotation while the camera
    stays where it is.
- Sprint, Step, ReverseStep, FastAccel.

**Render**
- ArrayList, NameTags, ESP (box or glow), Animations (swing styles, item position/scale, swing
  speed), BlockHighlight.

**Misc**
- AntiBot: tab list, name, duplicate name, ping and age checks. Flagged players are skipped by
  every module that targets or highlights players. The friend system and MCF were removed.
- BaseFinder: scores loaded chunks by player made block entities (storage, hoppers, signs, ...),
  reports likely bases in chat and outlines them.

Commands use the `.` prefix, e.g. `.toggle killaura`, `.killaura range 3.5`, `.esp mode glow`.

## Fonts

Product Sans is proprietary, so the jar ships **Google Sans** (its successor, SIL OFL 1.1, see
`assets/sinepsinem/font/ofl.txt`). It is used for ImGui and, through vanilla ttf font providers, for the
hud and the title screen.

Both paths rasterize glyphs to bitmap atlases with FreeType using light hinting, which keeps glyph
shapes intact instead of snapping them to the pixel grid. The ImGui atlas uses ImGui's FreeType
builder (with embedded bitmap strikes allowed). The vanilla atlases for our fonts are sampled
bilinearly with a 1px border around every glyph, so the 4x oversampled glyphs are averaged down
smoothly instead of being point sampled. If you own Product Sans, drop `ProductSans-Regular.ttf` and
`ProductSans-Bold.ttf` into `.minecraft/sinepsinem/fonts/` and the ImGui gui will use those instead.
