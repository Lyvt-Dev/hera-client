# Hera for Minecraft Java 1.21.11

Experimental standalone Fabric client mod, version 0.4.0. The launcher app version is independent. Use a normally authenticated Minecraft launcher and a Fabric 1.21.11 profile with Java 21, Fabric Loader 0.18.1 or newer and Fabric API 0.141.6+1.21.11. The Hera launcher itself still has a 1.21.1 Fabric adapter; use your existing launcher for this test build.

## Install and test

1. Close Minecraft. Open the game directory of your **Fabric 1.21.11** profile and its `mods` folder.
2. Put `hera-fabric-1.21.11-0.4.0.jar` and the matching Fabric API JAR into that folder. Remove an older Hera JAR from that same profile to avoid duplicate mod IDs.
3. Start the profile. Right Shift opens Hera's module menu. All HUD and gameplay modules start disabled. Legacy settings are migrated to disabled once; only fresh explicit choices are then persisted. The eleven HUD modules and HUD editor are included.
4. Turn **Kristall-Totem** on and press **Schließen**. It is disabled by default.
5. Put a Totem of Undying in hotbar slots 1–9 and select another item. Have a second player within eight blocks hold an End Crystal in their main hand or offhand. Hera selects your first hotbar totem, keeping an already-selected totem. It does not take a totem from other inventory slots or move anything to the offhand.
6. It switches once per threat episode. You can choose another item afterwards; the automatic switch rearms after no nearby other player holds a crystal, after closing a menu or after reconnecting. Without a hotbar totem, no switch occurs. Your own crystal does not trigger it. Dead/spectator players and open menus do not trigger it.

Menu density is independent of Minecraft GUI scale, with transformed mouse hitboxes. The compact module menu uses dark panels, antialiased nine-slice surfaces, original outline icons, static Nunito 600/750 fonts, search, HUD/game filters and pagination. It never submits an extra blur pass. HUD Studio provides selection outlines, dragging, Shift grid snapping, wheel or +/- scaling, clickable text-color/background controls, right-click toggles, arrow-key nudging (Shift for eight pixels), C text color and B background. Reset requires a second click or R press and preserves module toggles/colors. Escape saves and returns to the module menu. Configuration lives in `config/hera/hud.json` and `config/hera/features.json` within that game profile. No tokens, login flow or launcher registration are included in this mod.

## 0.4.0 modules and controls

All new modules start OFF. Right Shift opens the menu. Click the bottom-right
**…** on a card to customize that module. RGB colors, line widths, categories,
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

Build and 33 automated tests pass. The isolated client launch opened the menu,
all 27 settings screens and both pickers without crashing. These new world and
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
