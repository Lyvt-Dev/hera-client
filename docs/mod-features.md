# Hera HUD mod: implemented feature inventory

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
