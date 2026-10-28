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

The look and the module set follow **Ambient** (legitclient.com), recreated from its client
screenshots: flat near-black panels (`#0F0F0F`, headers `#0A0A0A`), one hot pink accent (`#FF0086`)
that runs as a gradient into red (`#FF001C`) through the array list, bars and highlights. Other
themes (Sunset, Ocean, Mint, Custom) are in `ClickGui > Theme`.

**ClickGui** (Right Shift)
- One floating panel per category (Combat, Ghost, Movement, Render, Player, Misc, Client, Hud,
  Scripts) with a Lucide icon, drag by the header, collapse with the chevron. Positions are saved.
- Modules are rows; an enabled one lights up with the accent gradient. Left click toggles, right
  click (or the chevron) opens its settings inline, middle click hides it from the array list.
- Settings like Ambient's: toggle switches, sliders with the value on the label line, dropdowns
  that show the value cut short (`Av...`) and expand into their options, collapsible groups, and a
  color picker (saturation/value square, hue and alpha bars, hex field with copy and paste).
- The pencil on a panel switches it to bind mode: rows show their key, click one and press a key.
- Search bar above the hotbar (typing anywhere goes to it); the eye shows only enabled modules,
  the cloud saves the config. Descriptions show after hovering a row.
- Drawn with Dear ImGui through Minecraft's own gui renderer, so it works on both the OpenGL and
  the Vulkan backend of 26.x. Every ImGui user (screens, hud elements, hotbar, chat) gets its own
  context, all sharing one font atlas with the icons merged in.

**HUD** (Hud panel, drag in the HudEditor)
- Watermark (pills with your account and fps, or `Ambient` style text), ArrayList (gradient names,
  mode suffix, side bar, background), Info (fps, bps, ping, coordinates with the other dimension's),
  Session Information (head, play time, health bar, kills and deaths), Target HUD (head that flashes
  when hit, health, gradient bar, distance, winning or losing), Keybinds, Speed Graph, Inventory,
  Radar, Potions. Name tags are Ambient's pills: health in red, name, distance in blue.
- Chat messages from the client start with `Sinep Sinem »`.
- **Interface** module: glass hotbar and chat (`HudElement`s in `features/gui/hud`), Kawase blur
  behind every panel (dual Kawase with our own blaze3d pipelines), glow, SF Pro everywhere.

**Combat**
- KillAura: target priority, speed limited and sensitivity snapped silent rotations, ray trace,
  cooldown or CPS timing. Attacks are sent right after the rotation packet.
- TargetStrafe: circles the target at a radius, aiming where it will be a few ticks ahead; A/D
  picks the direction, walls flip it. The radius is drawn as a ring with an edge gradient and a
  full or striped outline, like Ambient's.
- MaceSwap, AutoSpear, AutoBreezeCharge, AutoTotem, Criticals, KeyPearl.

**Ghost**
- Hitboxes: bigger pick boxes for your crosshair. SprintReset: W Tap, S Tap or Sprint after every hit.

**Movement**
- MovementFix (Classic remaps your keys, Penis moves along the silent rotation), Sprint,
  KeepSprint (no 60% slowdown or sprint stop on hit), Speed (Strafe bunny hop, Ground), Flight
  (Vanilla, Motion), NoPush (entities, liquids, blocks), NoDelay (jump delay, miss cooldown), Step,
  ReverseStep, FastAccel.

**Render**
- ESP (box or glow), NameTags, StorageESP (glow, box or outline per storage type), Tracers (theme
  gradient by distance), Animations, BlockHighlight.
- Trajectories: where a projectile lands and in how many seconds, for what the held item would
  shoot and for everything already in the air, stepped the way each projectile ticks.
- GlowParticles: glowing particles around you, behind you and out of everything you hit.

**Player**
- Velocity, NoFall, FastPlace, AutoTool (best hotbar tool while mining, then back), NoRotate
  (server teleports keep your camera), AutoRespawn.

**Misc**
- AntiBot, Teams (scoreboard team, name color or armor color; combat modules skip teammates),
  AntiCheat (flags other players for speed, flight, invalid rotation and invalid movement:
  `[AC] Name flagged Speed [x2]`), NickHider (your name is replaced everywhere text is drawn),
  StaffDetector (staff ranks in the tab list, Experimental also flags spectators), LagAlert (lag
  backs and server freezes), ClientSpoofer (brand: Vanilla, Lunar, Fabric, Forge, Badlion, Feather),
  ResourcePackSpoof (reports server packs as loaded without downloading), CompactChat (`[x3]`),
  BaseFinder.

**Scripts**

Lua 5.2 (LuaJ) scripts in `.minecraft/sinepsinem/scripts`, written against the same kind of API as
Ambient's, so its examples work too (`ambient` is an alias of `client`):

```lua
local module = modules.register("API Demo", "Showcases various API features")
local Minecraft = java.import("net.minecraft.client.Minecraft")
local mc = Minecraft.getInstance()
local announce = module:bool("Announce", true)          -- shows up in the ClickGui

module:on("enable", function() print("Hi " .. client.username()) end)
module:on("game_tick", function(event) ... end)
module:on("render_2d", function(event) client.text(event, "hello", 4, 40, 0xFFFF0086) end)
module:on("attack", function(target) return false end)   -- true cancels the hit
```

Events: `enable`, `disable`, `pre_tick`, `game_tick`, `render_2d`, `render_3d`, `attack`,
`packet_send`, `packet_receive` (return true to cancel). Settings: `module:bool`, `module:number`,
`module:text`, each with `:get()` and `:set(v)`. `java.import` gives a class; call statics with a dot
or a colon, instance methods with a colon. A script that errors turns its module off and says why
in chat. Loaded scripts load again on the next start.

`.script folder | list | load <file> | reload <file> | unload <file>` (the `.lua` is optional); an
`example.lua` is put in the folder on first start.

**Other**
- Android launchers (MojoLauncher, PojavLauncher) are supported: imgui-java only ships x86_64 linux
  natives, so the jar bundles its own Android arm64 build, made with `scripts/build-imgui-android.sh`.
- Title screen: the Minecraft logo is replaced (`LogoRenderer` mixin) and an **Alt Manager** button
  sits at half the screen width, at `height - button height` (`TitleScreen` mixin). Offline and
  access token alts, saved to `.minecraft/sinepsinem/alts.json`.

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
builder (with embedded bitmap strikes allowed). Icons are Lucide (ISC, `lucide-license.txt`),
a 10 KB subset merged into every ImGui font. The vanilla atlases for our fonts are sampled
bilinearly with a 1px border around every glyph, so the 4x oversampled glyphs are averaged down
smoothly instead of being point sampled.
