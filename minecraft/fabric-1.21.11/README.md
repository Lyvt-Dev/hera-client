# Hera for Minecraft Java 1.21.11

Experimental standalone Fabric client mod, version 0.7.0. The launcher app version is independent. Use a normally authenticated Minecraft launcher and a Fabric 1.21.11 profile with Java 21, Fabric Loader 0.18.1 or newer and Fabric API 0.141.6+1.21.11. Hera launcher 0.1.8 automatically installs this mod and its matching Fabric API into Fabric 1.21.11 profiles.

## Install and test

1. Close Minecraft. Open the game directory of your **Fabric 1.21.11** profile and its `mods` folder.
2. Put `hera-fabric-1.21.11-0.7.0.jar` and the matching Fabric API JAR into that folder. Remove an older Hera JAR from that same profile to avoid duplicate mod IDs.
3. Start the profile. Right Shift opens Hera's module menu. All HUD and gameplay modules start disabled. Legacy settings are migrated to disabled once; only fresh explicit choices are then persisted. The eleven HUD modules and HUD editor are included.
4. Turn **Kristall-Totem** on and press **Schließen**. It is disabled by default.
5. Put a Totem of Undying in hotbar slots 1–9 and select another item. Have a second player within eight blocks hold an End Crystal in their main hand or offhand. Hera selects your first hotbar totem, keeping an already-selected totem. It does not take a totem from other inventory slots or move anything to the offhand.
6. It switches once per threat episode. You can choose another item afterwards; the automatic switch rearms after no nearby other player holds a crystal, after closing a menu or after reconnecting. Without a hotbar totem, no switch occurs. Your own crystal does not trigger it. Dead/spectator players and open menus do not trigger it.

Menu density is independent of Minecraft GUI scale, with transformed mouse hitboxes. The compact module menu uses dark panels, antialiased nine-slice surfaces, original outline icons, static Nunito 600/750 fonts, search, category filters and inline settings. It never submits an extra blur pass. HUD Studio provides selection outlines, dragging, Shift grid snapping, wheel or +/- scaling, clickable text-color/background controls, right-click toggles, arrow-key nudging (Shift for eight pixels), C text color and B background. Reset requires a second click or R press and preserves module toggles/colors. Escape saves and returns to the module menu. Configuration lives in `config/hera/hud.json` and `config/hera/features.json` within that game profile. No tokens, login flow or launcher registration are included in this mod.

## 0.5.0 workspace and rendering fixes

Right Shift now opens a three-pane workspace. Choose a category, search/select a
module, and edit its settings directly on the right. Toggle the module using its
switch or the detail header. Scroll each pane independently. Save and close (or
Escape) persists the setup. Item selection is at the top of the ESP detail pane.

- ESP block results remain visible while the next incremental scan runs.
- Hitboxes, tracer anchors and TNT labels interpolate with the render camera;
  line lengths retain fractional coordinates. This fixes identified tick-step
  behavior, without claiming a measured FPS increase.
- **Rüstung ausblenden** hides your own rendered helmet/chest/legs/boots, with
  independent slot controls. Your armor stays equipped and protects normally.
  Other clients still see it; other players' armor and Elytra stay visible.
- New module defaults OFF. Existing explicit choices persist.

Build and 39 tests pass. The isolated launch opens all 28 module detail panels
and both pickers; a framebuffer screenshot verifies the new layout. Actual
world movement and armor hiding still need gameplay feedback.

## Included modules and controls

All new modules start OFF. Right Shift opens the menu. Select a module to customize it directly in the right-hand detail pane. RGB colors, line widths, categories,
range and module-specific behavior are saved with **Fertig**. Clicking outside
these screens does not dismiss them. HUD Studio controls placement and scale.

- **Keystrokes:** WASD, held left/right mouse buttons and optional space row.
- **Armor HUD:** real equipped armor and optional durability percentages.
- **Hitboxes:** select players, mobs and dropped items; choose RGB color/width.
- **TNT Timer:** fuse countdown for primed TNT and primed TNT minecarts.
- **Item Highlighter:** borders around selected items in inventory and hotbar.
  Item selection is shared with ESP; you can enable Highlighter without ESP.
- **No Fog:** local world fog removal, with water/lava switches.
- **Time / Weather Changer:** local visual day time and clear/rain/thunder.
- **Zoom:** hold **C**; configurable factor.
- **Free Look:** hold **left Alt**; configurable third-person camera distance.
  Player facing and movement direction stay unchanged.
- **Full Bright:** configurable ambient brightness; 100% is the default strength
  when explicitly enabled. Does not save over Minecraft's gamma setting.
- **Freecam:** toggle **V**, move with WASD, rise with space, descend with shift,
  sprint for a speed boost. V returns to the player. Only the camera detaches;
  player physics continues. Movement inputs and attack/use/break actions are
  suppressed while detached. Disconnect/world change/death resets Freecam.

Key bindings can be changed in Minecraft's controls. ESP settings now include
3D box edges, tracers (bottom or center origin), RGB color, width, labels and
entity glow. Box edge width is adjustable; vanilla glow's own width is fixed.
Selected block items also locate matching placed blocks in loaded chunks.
Search is incremental, capped at 128 nearest block results; it cannot expose
unloaded or server-hidden data or hidden container inventories.

Build and 39 automated tests pass. The isolated client launch opened the menu,
all 28 module detail panels and both pickers without crashing. These new world and
camera features have not yet been tested in actual gameplay or with shaders.

## Existing modules

- **Item-ESP** opens the searchable item picker. Search by localized name or ID, select the items to mark, then enable Item-ESP. Empty selection marks nothing. The range button cycles 16, 32, 64 and 128 blocks. Drops receive outlines; selected loaded blocks receive name/distance markers through walls. Select `minecraft:barrel` (Fass) to locate barrels. Their hidden contents cannot be read by the client. Only already-loaded data is searched.
- **Spieler-ESP / Mob-ESP** independently enable outlines within the same range. Disabling Hera ESP preserves vanilla server-assigned glow.
- **Auto-Totem** swaps an inventory totem into the offhand, moving the old offhand item into its inventory slot. It waits while menus are open or the cursor carries an item. It is independent from Kristall-Totem.
- **Fast Items** enables the integrated camera-facing drop renderer with flat-item side removal and preserved shadows. Special renderers remain vanilla. If standalone Fast Items is installed, that mod owns the rendering and Hera's integration is skipped. See THIRD-PARTY.md for the reviewed CC0 license and pinned source.

All modules start off. Activate only the features you want. New rendering and automation still need in-game feedback; a successful build is not a gameplay compatibility claim.

## Build

Use JDK 21 and Gradle 9.2.1: `gradle -p minecraft/fabric-1.21.11 build --no-daemon`. The distributable is the remapped JAR under `build/libs`; do not install the `-sources.jar`.

## Dependencies and provenance

Ported from Hera's own 1.21.1 code; no reference-client implementation copied. Runtime dependencies are external and are not bundled: Fabric API (Apache-2.0, reviewed https://github.com/FabricMC/fabric-api/blob/1.21.11/LICENSE), Fabric Loader (Apache-2.0); Yarn mappings (CC0-1.0, reviewed https://github.com/FabricMC/yarn/blob/1.21.11/LICENSE) are build-time data. Minecraft binaries, libraries and Fabric API are not redistributed in the Hera JAR. JUnit is test-only. Nunito is derived from the launcher font under SIL OFL 1.1: FontTools exports static 600/750 weights with internal family renamed Hera UI. The original copyright/license and a derived-font notice are retained. The complete license is included as META-INF/licenses/OFL-Nunito.txt. It uses a separate Hera font resource and does not replace the game font globally. Rounded-mask and outline-icon textures are original procedural assets. No copied client art or shaders are included. Developer-only title-screen UI preview: `gradle -p minecraft/fabric-1.21.11 runClient -PuiPreview` (does not enter a world and is gated by both Fabric development mode and an explicit VM property).

Compilation and policy tests do not establish in-game compatibility. See ../../docs/status.md for separate build/test/gameplay evidence.

## 0.6.0 title menu and D-Tap

The original Hera title menu uses Minecraft's panorama, native world/server/options screens and actual session name. The three-pane module workspace is smaller. All modules remain disabled until explicitly enabled.

Experimental D-Tap requires hotbar obsidian and two crystals, a server-confirmed hit on another player holding a totem, suitable placement/reach and conservative estimated damage. Settings expose delay, minimum target damage, maximum self damage and health reserve. Server confirmation gates placement and the second crystal; changing world/menu/slot or losing suitability aborts. Estimates cannot guarantee totem pops or kills. Aborting may leave already spent blocks/crystals. It has unit/startup coverage but no live PvP acceptance yet.


0.6.1 adds the owner's Hera pumpkin logo to the title screen, module workspace and HUD Studio, refines title-button focus/hover treatment, and renders the account's head through Minecraft's asynchronous skin provider (vanilla fallback while loading). Authenticated custom-skin acceptance is still required. D-Tap has no anti-cheat bypass or compatibility guarantee; server rules may reject automated actions.

0.6.1 D-Tap: cautious mode defaults on without enabling D-Tap itself. In this mode actions have at least four ticks between them, hotbar changes wait before interaction, placement uses the actual crosshair block/face/hit vector, crystal attacks require the actual crosshair entity and full attack recovery. The player must aim throughout the sequence; otherwise it times out. The original mode remains configurable. This is not evidence of compatibility with any anti-cheat and cannot guarantee avoiding bans. The owner's private plugin/check details were not supplied; only the generic message "unfair advantage" was reported. No server integration was tested.


0.7.0 adds local capes through the launcher (docs/capes.md) and the performance work recorded in docs/performance.md. ESP candidates update at 10 Hz while positions still interpolate every frame. At most 128 relevant entities participate in Hera overlays/glow. Container scanning and D-Tap planning are incremental; large scans may finish later. No zero-FPS-impact or anti-cheat guarantee is made.
