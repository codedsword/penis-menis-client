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
  It sits on a Kawase blurred glass panel over a frosted backdrop with flying glow particles
  (they drift up, twinkle, link up when close and dodge the mouse).
- ImGui output goes through Minecraft's own gui renderer, so it works on both the OpenGL and the
  Vulkan backend of 26.x. Every user of ImGui gets its own context (the screens, the hotbar, the
  chat, ...) sharing one font atlas, so several can draw in the same frame.
- **Interface** module (on by default) holds the look of the client:
  - Custom hotbar and chat, both `HudElement`s (`features/gui/hud`). Mixins only ask the element
    whether it is active and hand it vanilla's state; the element draws its chrome through ImGui and
    leaves items and text to vanilla, so item models, tooltips and chat links keep working.
    - Hotbar: one glass panel in the vanilla footprint, a selection that glides between slots, the
      selected item lifted, attack indicator as a ring.
    - Chat: the per line backgrounds become one blurred panel whose top eases to the oldest
      visible line, new lines slide in and fade in. The chat screen input box is glass too. All
      chat drawing goes through vanilla's `ChatGraphicsAccess`, our access sits in front of it.
  - Kawase blur: dual Kawase (down/up sampling, 1 to 6 passes) of the frame behind the gui, run
    with our own blaze3d pipelines right before the gui is drawn. Every glass panel samples it.
  - Glow and tint of the panels, animation smoothness, rounding, gui particle count.
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
- Trajectories: where a projectile lands and in how many seconds, both for what the held item would
  shoot (drawn bow, charged crossbow, trident, pearls, potions, snowballs, eggs, experience bottles,
  wind charges) and for every projectile already in the air. The path is stepped with the same
  order of gravity, drag and movement as the projectile's own tick; the landing spot gets a glass
  label with the countdown and what it will hit.
- GlowParticles: custom glowing particles floating around you, trailing behind you and bursting
  out of everything you hit, drawn as additive lights and hidden behind walls.

**Misc**
- AntiBot: tab list, name, duplicate name, ping and age checks. Flagged players are skipped by
  every module that targets or highlights players. The friend system and MCF were removed.
- BaseFinder: scores loaded chunks by player made block entities (storage, hoppers, signs, ...),
  reports likely bases in chat and outlines them.

Commands use the `.` prefix, e.g. `.toggle killaura`, `.killaura range 3.5`, `.esp mode glow`.

## Fonts

Everything uses **SF Pro Medium**: ImGui, the hud, and with `Interface > SFProEverywhere` every
piece of vanilla text in the default font too (chat, menus, scoreboard, item names). Glyphs the
font lacks fall back to the vanilla font.

SF Pro belongs to Apple and its license does not allow shipping it in a mod, so the jar ships
**Inter Medium** (SIL OFL 1.1, see `assets/sinepsinem/font/ofl.txt`), which was drawn after SF Pro
and is very close to it. If you have SF Pro (Apple's download installs it), drop
`SF-Pro-Text-Medium.otf` (or `SF-Pro-Display-Medium.otf`) into `.minecraft/sinepsinem/fonts/` and
both ImGui and the vanilla text renderer will use it; the otf (CFF) files are accepted even though
vanilla only takes TrueType.

Both paths rasterize glyphs to bitmap atlases with FreeType using light hinting, which keeps glyph
shapes intact instead of snapping them to the pixel grid. The ImGui atlas uses ImGui's FreeType
builder (with embedded bitmap strikes allowed). The vanilla atlases for our fonts are sampled
bilinearly with a 1px border around every glyph, so the 4x oversampled glyphs are averaged down
smoothly instead of being point sampled.
