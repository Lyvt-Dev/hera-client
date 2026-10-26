# Hera for Minecraft Java 1.21.11

Experimental standalone Fabric client mod, version 0.2.2. The launcher app version is independent. Use a normally authenticated Minecraft launcher and a Fabric 1.21.11 profile with Java 21, Fabric Loader 0.18.1 or newer and Fabric API 0.141.6+1.21.11. The Hera launcher itself still has a 1.21.1 Fabric adapter; use your existing launcher for this test build.

## Install and test

1. Close Minecraft. Open the game directory of your **Fabric 1.21.11** profile and its `mods` folder.
2. Put `hera-fabric-1.21.11-0.2.2.jar` and the matching Fabric API JAR into that folder. Remove an older Hera JAR from that same profile to avoid duplicate mod IDs.
3. Start the profile. Right Shift opens Hera's module menu. All HUD and gameplay modules start disabled. Legacy settings are migrated to disabled once; only fresh explicit choices are then persisted. The nine HUD modules and HUD editor are included.
4. Turn **Kristall-Totem** on and press **Fertig**. It is disabled by default.
5. Put a Totem of Undying in hotbar slots 1–9 and select another item. Have a second player within eight blocks hold an End Crystal in their main hand or offhand. Hera selects your first hotbar totem, keeping an already-selected totem. It does not take a totem from other inventory slots or move anything to the offhand.
6. It switches once per threat episode. You can choose another item afterwards; the automatic switch rearms after no nearby other player holds a crystal, after closing a menu or after reconnecting. Without a hotbar totem, no switch occurs. Your own crystal does not trigger it. Dead/spectator players and open menus do not trigger it.

The Liquid Glass menu has translucent cards, no extra blur pass, Nunito typography, search, HUD/game filters and pagination. HUD Studio provides selection outlines, dragging, Shift grid snapping, wheel or +/- scaling, clickable text-color/background controls, right-click toggles, arrow-key nudging (Shift for eight pixels), C text color and B background. Reset requires a second click or R press and preserves module toggles/colors. Escape saves and returns to the module menu. Configuration lives in `config/hera/hud.json` and `config/hera/features.json` within that game profile. No tokens, login flow or launcher registration are included in this mod.

## Build

Use JDK 21 and Gradle 9.2.1: `gradle -p minecraft/fabric-1.21.11 build --no-daemon`. The distributable is the remapped JAR under `build/libs`; do not install the `-sources.jar`.

## Dependencies and provenance

Ported from Hera's own 1.21.1 code; no reference-client implementation copied. Runtime dependencies are external and are not bundled: Fabric API (Apache-2.0, reviewed https://github.com/FabricMC/fabric-api/blob/1.21.11/LICENSE), Fabric Loader (Apache-2.0); Yarn mappings (CC0-1.0, reviewed https://github.com/FabricMC/yarn/blob/1.21.11/LICENSE) are build-time data. Minecraft binaries, libraries and Fabric API are not redistributed in the Hera JAR. JUnit is test-only. Nunito is bundled unchanged from the launcher under SIL OFL 1.1. The complete license is included as META-INF/licenses/OFL-Nunito.txt. It uses a separate Hera font resource and does not replace the game font globally. No copied client art or shaders are included.

Compilation and policy tests do not establish in-game compatibility. See ../../docs/status.md for separate build/test/gameplay evidence.
