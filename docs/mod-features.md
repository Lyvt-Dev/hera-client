# Fabric 1.21.11 — 0.4.0

All modules start disabled. Right Shift opens the menu; click the bottom-right
ellipsis on any module card for its settings. Colors use an RGB picker.

- HUD: FPS, coordinates, ping, memory, clock, direction, speed, biome, server,
  Keystrokes (WASD/mouse/space) and equipped armor with durability. Move/scale
  widgets in HUD Studio; its bounds include the larger new widgets.
- World: hitboxes by target category, TNT and primed TNT-minecart timers, item/
  selected-block/player/mob ESP with configurable 3D edges, color, width,
  tracers and labels. Vanilla entity-glow thickness itself remains fixed.
- Item Highlighter: exact selected item IDs get inventory/hotbar borders.
  The item picker is shared with ESP; choosing items does not enable ESP.
- Visuals: local fog removal (water/lava optional), day-time/weather overrides,
  configurable ambient full bright, C-held zoom and Alt-held freelook.
- Freecam: V toggle; WASD, space/shift, sprint speed boost. Camera only; player
  physics continues. No interaction/attack/break input or inventory automation
  while detached. Resets on world/disconnect/death.
- Existing optional Crystal-Totem, offhand Auto Totem and CC0 Fast Items now
  expose their own range/timing/render options.

Only loaded server-supplied data can be marked; container contents stay unknown.
Block searches are incremental and capped. See status.md for distinct automated,
launch-tested and world-tested evidence. The older section below describes the
original 1.21.1 implementation, not the current 1.21.11 feature set.

## Legacy Fabric 1.21.1 implementation

The independent Fabric mod targets Minecraft Java 1.21.1, Fabric and Java 21. It is a separate JAR; the launcher does not automatically inject it into every profile. Source builds and tests exist; authenticated gameplay has not yet been observed.

Nine real-data HUD modules:

- FPS: current client frame rate.
- Coordinates: player XYZ with one decimal place.
- Ping: current player-list latency when available.
- Memory: used and maximum JVM heap in MB.
- Clock: local HH:mm:ss.
- Direction: player's horizontal facing.
- Speed: horizontal velocity in blocks/second.
- Biome: biome registry identifier at the player.
- Server: current multiplayer address, or Singleplayer.

Right Shift opens the module menu (Minecraft keybinding can be rebound). The menu supports text search, scrolling, individual toggles and saving. The HUD editor supports drag placement, Shift snapping to an eight-pixel grid, wheel scaling from 0.5 to 3, right-click toggling, C to switch between two text colors, B to toggle background, R to reset the layout and Escape to save and return. Position uses normalized coordinates with screen bounds. JSON configuration persists under the game's config directory; invalid numeric values are normalized and write errors are shown. HUD data is sampled four times per second; F1 hides the HUD outside editing.

Not implemented: keystrokes/CPS, armor/potion HUDs, zoom, freelook, cosmetics, friends, voice chat or combat automation. No claim of NoRisk/LiquidBounce feature parity, cross-version mod compatibility or observed in-game validation.
