# Verification status

## Mod 0.5.0 workspace redesign, stable ESP and own-armor visibility

Implemented: replaced the paginated card menu with a three-pane workspace: category navigation, searchable/scrollable module list and inline settings. Selecting a module and toggling it are separate actions. Item selection is promoted to the top of the detail pane; search retains keyboard focus; the selected list entry is revealed when returning to the menu. Larger Nunito typography, graphite surfaces, clearer hierarchy and independent list/detail scrolling replace the previous small ellipsis controls and settings-page navigation. HUD and picker spacing accommodates the larger font.

Fixed root causes: incremental block scans previously replaced the visible marker list with the new scan's empty/partial queue on every restart. StableScanResults now retains the last complete snapshot until the replacement scan finishes; explicit configuration/world resets still clear it and rendering still validates actual loaded blocks. Entity overlays previously used tick positions. Hitboxes, ESP tracer anchors and TNT label positions now use getLerpedPos with the camera's render interpolation progress; boxes offset the actual bounding box by the same interpolated displacement. Lines use fractional transforms rather than rounded lengths. Only relevant entities enter the bounded draw list, with stable entity-ID tie ordering. Opening a screen no longer disables world overlays. These changes address identified scheduling/interpolation bugs; no measured FPS improvement is claimed.

New off-by-default Rüstung ausblenden module cancels only selected armor-slot draw calls whose PlayerEntityRenderState ID matches the local player. Helmet/chest/legs/boots can be hidden independently. Inventory, equipment, durability, defense, remote players and packets are untouched; other clients continue to render armor normally. Elytra is a separate renderer and is not hidden by this module.

Automated-tested: Gradle build and 39 tests passed, including stable-snapshot rescan/reset regressions, default-off/independent toggles and new three-pane layout boundaries. The isolated development launch loaded the armor renderer mixin and exercised all 28 module details plus the item/RGB pickers. A real framebuffer capture was inspected for the redesigned menu. During development the launch test caught an embedded-settings initialization error; it was corrected before release. No TypeScript or Rust changes.

Game-tested: no authenticated world session or live movement/armor validation was performed by the agent. Render interpolation and armor behavior still require owner gameplay feedback, particularly with shaders or other render mods. No claim of measured frame times, shader compatibility or newly supported launcher adapters.

## Mod 0.4.0 utility modules and customization

Implemented: Keystrokes (WASD, held mouse buttons, optional space bar), Armor HUD (real equipped stacks and optional durability percentages), customizable projected hitboxes, primed TNT/TNT-minecart fuse timers, selected-item borders in inventory screens and hotbar, local No Fog, Time Changer, Weather Changer, hold-to-Zoom, hold-to-Freelook, Full Bright and Freecam. Every new module defaults off. C holds Zoom, left Alt holds Freelook, V toggles Freecam; all use Minecraft-rebindable key bindings. Freelook changes the camera without player yaw; its third-person distance respects camera collision. Freecam moves only the local camera, suppresses movement input and attack/use/break actions, pauses inventory automation, and resets on disconnect/world changes/death. It does not teleport the server player or stop server physics.

Customization: every module card has a bottom-right settings control. HUD modules expose scale, RGB text color, background toggle and placement editor. Keystrokes exposes mouse/space rows; Armor HUD exposes durability. Other options include RGB colors, hitbox target categories, line width, TNT range, zoom factor, freelook distance, freecam speed, full-bright strength, air/water/lava fog handling, local day time/weather, Auto Totem retry interval, crystal threat radius and Fast Items model/sides/shadow switches. ESP adds independently switchable 3D box edges, vanilla entity glow, tracers (bottom/center origin), labels, RGB color and 1–4 GUI-pixel line width. The projected box edge width is adjustable; vanilla glow uses the game's fixed edge kernel. Item selection is shared by item/block ESP and Item Highlighter. Selected BlockItems now locate matching blocks using already-loaded chunk-section palettes, bounded to 64 palette inspections / four dense section scans per tick and 128 nearest results. Scans populate gradually; unloaded or server-hidden block data and closed-container contents remain unavailable.

Automated-tested: Gradle build and 33 tests pass, covering all-off defaults, independent module toggles, old-config migration, persisted settings, numeric normalization, inventory slot mapping and viewport line clipping. A separate development client opened the menu, all 27 settings screens, the item picker and RGB picker and exited successfully with HERA_SMOKE_OK. The launch check also forces affected entity/item/input/handled-screen classes to load so mixin startup errors surface. No TypeScript/native code changed.

Game-tested: the launch check does not enter a world or authenticate an account. Actual world rendering, camera movement, inventory automation, shader compatibility and server behavior still need owner testing. No supported-adapter or authenticated-gameplay claim is added. This is a standalone experimental Fabric 1.21.11 mod release, separate from the launcher/updater's Windows version.

## Mod 0.3.0 configurable ESP, Auto Totem and Fast Items

Implemented: all new modules default off. Item ESP has a searchable Minecraft item registry picker (localized name or registry ID), independent item selection, selected-only filter, pagination, persisted exact-ID selection and 16–128 block range. Empty selection marks nothing. Selected loaded item entities receive vanilla glowing outlines. Selected loaded inventory block entities (for example barrels, chests and shulker boxes) receive projected name/distance markers through walls, with a bounded loaded-chunk scan every ten ticks and at most 128 nearest markers. No hidden container contents are queried, inferred or claimed. Player and mob outlines are independent opt-ins. Entities outside the shared range or unloaded entities are not revealed.

Auto Totem uses a single vanilla SWAP into offhand, preserves the displaced item in the source slot, pauses for screens/cursor stacks/death/spectator and retries no faster than ten ticks. Kristall-Totem remains separate. Fast Items integrates the explicitly requested Lyvt-Dev/FastItems 1.21.11 port at pinned revision 131a2bc2e25630b704433afd94b338078ee7c5f6; CC0 reviewed and bundled with provenance. Integration is disabled by default and its rendering mixins are skipped if standalone fastitems is installed. No reference-client implementation copied. UI adds searchable item cards, selection toggles, module state accents and active count; existing Nunito/static fonts and single-blur behavior retained.

Automated-tested: Gradle build and 24 tests pass, including empty/exact/bounded selection, config opt-in persistence and all main-inventory/hotbar slot mappings. Remapped JAR metadata and bundled mixin/license resources inspected. No TypeScript or Rust changes in this mod update.

Game-tested: these new ESP, inventory automation and integrated Fast Items rendering paths have not yet been exercised in a live world. Compatibility with shaders, other render mods, anticheat rules and unusual resource-pack models remains unverified. Delivered as a separate experimental mod release for owner feedback; no new supported-adapter claim.

## Mod 0.2.3 typography/rasterization/menu-density correction

Owner screenshots showed thin/broken variable-font glyphs, coarse rasterized corners and an oversized menu. Switched Hera's font resources to derived static Nunito weights 600 and 750, internal family Hera UI, with original copyright/full OFL and a derived-font notice retained. Removed the unused variable-font resources. Added original procedural antialiased nine-slice masks and outline icons; no third-party client art/code copied. The menu now uses shorter labels, actual module descriptions and a compact dark two-column layout. HUD badges no longer have the decorative bullet. All-off defaults/legacy migration and the duplicate-blur fix remain.

Build/test: Gradle build and 16 tests, including configuration activation and GUI layout boundaries. A development-only preview is guarded by both Fabric development mode and explicit `hera.previewUi`; it opens the menu without a world. Minecraft F2 capture verified actual loaded static fonts, smooth surfaces, icons and the module screen. No extra blur crash occurred in that menu preview. The preview exposed large sizing at GUI scale 4, so final code applies a physical-density transform to rendering and matching mouse events. User stopped Computer Use with Escape and requested the JAR for personal feedback; final density transform and updated HUD remain unverified in a world. No offline or authenticated gameplay was entered by the agent.

Asset production used local FontTools/Pillow; those tools are not runtime dependencies or bundled code. Minecraft game data and screenshots remain excluded from Git and releases.


## Mod 0.2.2 crash/defaults/HUD correction

Owner feedback and local crash report confirmed a real multiplayer screen crash in 0.2.1: `IllegalStateException: Can only blur once per frame`, originating in `ModuleScreen.render`. Removed the explicit second blur invocation; do not reintroduce it. The report establishes that the owner loaded the mod in a live world, but does not validate the module menu or all gameplay features. No private crash report, server/player details or local game data are published.

All new HUD modules now default disabled. Version-2 HUD/feature settings migrate old automatically enabled settings to disabled once, retain HUD coordinates, refresh the old HUD style, and persist subsequent explicit opt-ins. Gameplay modules also require fresh opt-in for legacy files. Rounded translucent rectangles now use disjoint spans, eliminating unintended alpha accumulation/striped fills. HUD chips use a compact 20px height, white Nunito values, subtle outlines and small accent dots; editor hit bounds match the new height.

Automated-tested: Gradle build passed with 16 tests, including new all-off defaults, legacy activation migration with retained layout, explicit activation persistence and legacy gameplay-feature reset. The new JAR's menu omits the offending extra blur call. Actual new-build in-world/menu visual testing remains with the owner; no claim of observed crash-free gameplay is made from compilation alone.


## Mod 0.2.1 Liquid Glass UI

Implemented: original translucent glass panels, vanilla menu background blur, rounded cards, Nunito (bundled unchanged with full SIL OFL license), HUD/game category filters, search, keyboard-accessible custom buttons and pagination. HUD Studio adds selection outlines, clickable scale/color/background controls, grid toggle, arrow-key nudging and a two-step layout reset preserving module toggles/colors. HUD uses rounded surfaces and the separate Hera font; the default Minecraft font is not overridden. Crystal-totem behavior remains unchanged.

Automated-tested: Gradle build and 12 JUnit tests passed, including scaled GUI layout/page boundaries. Remapped JAR metadata/font paths and included license checked. Development client reached the title screen; actual resource loading exposed a duplicate font-folder path, now fixed. Native capture failed; Minecraft's own F2 capture verified the title screen only. The mod menu and HUD Studio were not visually validated in-game; owner explicitly requested the JAR immediately for feedback. No world was entered, no authenticated gameplay observed. Test-client game data/screenshots are excluded via the module's `/run/` ignore rule.


## Standalone Fabric 1.21.11 mod 0.2.0 (26 October 2026)

Implemented separately in `minecraft/fabric-1.21.11`: nine HUD modules, editor and module menu ported to the 1.21.11 HUD registry, 2D matrix stack, keybinding categories and input records. Existing 1.21.1 sources remain unchanged. The new optional `Kristall-Totem` module detects another living non-spectator player with an End Crystal in either hand within eight blocks, selects a Totem of Undying only from hotbar slots 0–8 and sends the normal selected-slot update. It defaults off, persists its opt-in separately, does nothing while a screen is open/dead/spectating/disconnected, does not move inventory items, and switches once per continuous threat episode. Own crystals do not trigger it. It rearms after the threat clears or the blocked state resets. Installation/test steps and dependency provenance are in the module README.

Automated-tested: JDK 21 / Gradle 9.2.1 / Loom 1.14.10 `build` passed, including 10 JUnit tests (HUD normalization, slot-policy boundaries/rearming, config opt-in persistence and invalid-config handling). The output JAR has remapped intermediary Minecraft references, correct 1.21.11 metadata, feature/editor classes and no bundled Minecraft or JUnit classes. First download was interrupted by upstream DNS; retry succeeded. No TypeScript or native implementation changed; this is a separate mod release, not a launcher installer update. New CI job builds this standalone project without replacing the old mod job.

Game-tested: not yet. No authenticated in-game HUD/input or two-player crystal detection has been observed. Deliver as an experimental test build, not certified support; the owner will test it via an existing authenticated Fabric launcher. Hera's launcher-side Fabric adapter remains 1.21.1 and app approval remains blocked.


## 0.1.4 profile dialog fixes

New-profile Fabric/Forge tiles are selectable from the default 26.3 setup. Selecting either changes the draft to their implemented Minecraft 1.21.1 target and announces the change inline; version changes back to other versions explicitly explain the return to Vanilla. Existing profiles keep their version/loader immutable. Backdrop and dialog-padding clicks no longer dismiss modals. Escape, the close icon and Cancel remain deliberate dismissal actions; cancel events prevent the browser from closing a dialog independently of the guarded React close handler.

## 0.1.3 versions and profile UX (26 October 2026)

Implemented: official Mojang catalog through 26.3, including releases, snapshots, historical alpha and beta entries (915 catalog entries observed). Search, type filters, incremental list rendering and direct profile creation replace the 1.21.1-only catalog. The profile editor defaults to Vanilla, shows loader choices with their actual scope, keeps advanced Java configuration collapsed, and prevents changing the version or loader of an existing profile. Official IDs containing spaces are accepted; traversal and separators are rejected.

Native Vanilla installation now follows per-version metadata for Java 8/16/17/21/25, legacy `minecraftArguments`, named virtual/resource assets, and official hash-verified logging configuration. JVMs are still user-installed; 26.3 requires Java 25. This is an experimental adapter expansion, **not a claim that every version is game-tested**. Fabric/Forge adapters and the Hera HUD mod remain 1.21.1-specific; incompatible loader/version combinations are rejected before downloading.

Automated: `npm run check`, `npm test` (48 tests), `npm run build`; Rust unit tests (20 passed, four network tests opt-in). Real Windows installation/argument smoke matrix: rd-132211, 1.0, 1.8.9, 1.12.2, 1.20.1 and 26.3. Downloads verify official hashes/sizes, classpath entries exist and legacy/modern game arguments resolve. Tests never acquire account credentials or start offline gameplay. Browser checks cover 26.3/1.8.9 search, type filtering, version preselection and profile-dialog cancellation. Other operating systems, exhaustive installation of 915 versions, native Java 8/17/25 process start and authenticated gameplay remain unverified.

Game-tested: none. Minecraft Services still rejects Hera's app registration. No authentication bypass was implemented. Mod feature inventory: [mod-features.md](mod-features.md).


## 0.1.2 Minecraft 403 diagnosis

The owner reached Minecraft authentication after Microsoft/Xbox/XSTS and reported HTTP 403. The owner subsequently reproduced `MC_APP_NOT_APPROVED` in 0.1.2, confirming the app-registration rejection. Version 0.1.2 distinguishes an explicit `Invalid app registration` response (`MC_APP_NOT_APPROVED`) from unknown 403 errors (`MC_AUTH_FORBIDDEN`), without exposing arbitrary provider payloads or credentials. An approval-request draft is in [app-approval-de.md](app-approval-de.md). No approval was submitted or granted by this change; authenticated gameplay remains unverified.

The same release adds the owner's Discord-site visual language and locally bundled OFL Nunito, an original Minecraft hero, consistent orange/charcoal components and persistent selected-profile state. See [design and asset provenance](launcher-design.md). TypeScript checks/builds and 38 tests passed; Rust tests passed with 18 unit tests and 3 opt-in live-download tests skipped. Browser font/image loading, no observed horizontal overflow, profile selection across reload, live Modrinth results and modal cancellation were verified. Vitest is explicitly scoped to Hera's `tests/**/*.test.ts`; the unrelated VConnect project uses a different test runner and was not modified.

Development snapshot: 26 October 2026. This is a foundation implementation, not feature parity with NoRisk/Lunar and not a production release.

## Implemented

- React/TypeScript/Tauri 2 monorepo, original graphite/violet design, Zustand navigation and TanStack Query async state.
- Shared Zod profile/settings models; browser preview storage and native SQLite storage separated explicitly.
- Profile create/edit/duplicate/remove, import/export (metadata only), isolated game paths, RAM and Java configuration. Imported Java paths are replaced with the local detected runtime for safety.
- Native Microsoft window with authorization-code/PKCE and refresh integration, Xbox/Minecraft exchange, ownership check, OS-keychain refresh storage and multiple account metadata. Public client-ID configuration is editable in advanced Settings without rebuilding; still requires an actual app registration and has not authenticated in real use.
- Experimental vanilla/Fabric/Forge 1.21.1 x86_64 adapters: official metadata/assets/libraries, SHA-1/size verification, bounded downloads/retries, reuse/repair, Java 21 validation, process start/exit/stop. No certified version support yet.
- Mod browser: Modrinth search, releases, loader/game-version filtering, required dependency resolution, hash-verified downloads and profile manifests. Browser OPFS storage and native staged filesystem installs are separate. CurseForge official-API gateway and author distribution restrictions are implemented; no real API key is configured. See [mods setup and scope](mods.md).
- Independent Fabric 1.21.1 mod: nine real-data HUD sources, searchable toggle menu, keybind, drag/scale/grid editor, two text colors, background toggle, layout reset and persistent config. Values are sampled at 4 Hz to avoid formatting each render frame.
- Fastify settings-sync API with Minecraft identity verification, request validation, PostgreSQL revision checks, Redis rate limits, health routes and Docker service definitions. Not connected to launcher yet.
- CI definitions for TypeScript, native OS matrix, Java mod, manual development-installer artifact builds. Workflows have not been run on GitHub.
- Windows NSIS installer configuration and native Tauri updater: HTTPS GitHub release channel `Lyvt-Dev/hera-client`, release notes, signature-verified download/install, byte progress, Minecraft-running guard and release packaging script. See [installer and updates](installer-updates.md). No release has been published.

## Confirmed so far

| Check | Evidence |
|---|---|
| TypeScript strict checks | `npm run check` passed |
| Schema/API/mod tests | 38 tests passed; dependency resolution, cycles, pinned conflicts, unsafe origins, CurseForge missing-key/author opt-out plus API auth, input validation, Java executable constraints, identity isolation, revision conflict and unavailable readiness |
| Web/backend production builds | Passed, including final shared-schema rebuild |
| npm advisory scan | 0 known advisories after updating the initial test runner |
| Browser UI | Home rendered and visually inspected; profile created and observed after reload; reduced-motion setting observed after reload; browser install/start buttons disabled |
| Fabric build | Final Gradle 8.14.3 / Temurin 21 build succeeded, two JUnit tests passed |
| Native Rust | 14 tests passed, including SQLite persistence, client-ID account guard, immutable profile loader and mod filename/ID validation |
| Real Mojang installation / repair | First opt-in integration test passed in 568.83 s: official 1.21.1 libraries/client and 3,911 assets downloaded and verified; deliberately corrupted client JAR repaired and verified on second install. Repeated against final source: passed in 177.85 s using the cache and another deliberate corruption/repair |
| Real Modrinth install | Sodium downloaded, verified and recorded in an isolated Fabric profile; duplicate installation unchanged; passed in 1.94 s |
| Real loader installation | Fabric 0.16.14 libraries and official Forge 52.1.20 installer/runtime installed; generated metadata and library hashes checked, final integration test passed in 61.69 s. No game process launched |
| Browser mod persistence | Sodium actually downloaded into the Fabric profile through the UI and still listed after reload |
| Windows standalone build | `cargo build --features custom-protocol` succeeded; debug EXE embeds web assets; owned launcher process remained alive for 5 seconds with Vite stopped, then was stopped. This checks process initialization, not rendered native UI or gameplay |
| Windows installer | Final optimized NSIS setup generated successfully: `releases/Hera Client_0.1.0_x64-setup.exe` (4,379,201 bytes), updater `.sig`, SHA-256 checksum and `latest.json`. Checksum and manifest/signature-file consistency verified. Installer script syntax checked. Installation/uninstallation on a clean Windows machine and a two-version online upgrade have not been exercised |
| Download/update UI | Browser Downloads page inspected: updater section and selected-profile Minecraft install/repair action render; native-only actions correctly unavailable in browser. Final TypeScript check and production builds passed; 38 tests passed on retry with a 20-second test limit after one backend initialization timed out during heavy Rust compilation. Final Rust tests: 14 passed, 3 opt-in network tests skipped |

Test doubles exist only in automated API/provider/dependency tests, never as production services. The real-download test stores approximately 0.95 GB of official game files in ignored `.local/install-smoke`; these files are never release artifacts. The first Vite preview stopped because it watched a locked Rust build executable; `src-tauri` is now excluded from its watcher and the preview was restarted successfully.

## Not verified / open

- No real Microsoft login: the owner supplied a public app client ID and it is the desktop default, but authentication and Minecraft API authorization remain unverified. See [setup](microsoft-setup.md). No ownership-authenticated Minecraft process or gameplay has been observed.
- No real-world HUD/editor or modded gameplay acceptance and no platform certification. Loader installation is now tested separately from game launch.
- No PostgreSQL/Redis live integration proof; Docker reports that the Docker Engine named pipe does not exist (daemon not running).
- No Linux/macOS builds were executed locally. ARM64 adapter is explicitly rejected.
- Download speed/ETA, range resume, cancellation, Java auto-install, log UI/crash analysis, profile file bundles, custom JVM args and pack installation remain open.
- Full HUD module catalog and advanced editor features, mod updates/removal/modpack import, cosmetics/3D viewer, social/chat/relay, moments, arcade, Discord/server manager remain planned.
- CurseForge live search/download remains unverified without the owner's API key and operated gateway. The Microsoft app ID was supplied by the owner; no registration was performed by the agent.
- No published releases or end-to-end installed-version upgrade/rollback proof, Windows Authenticode certificate, measured performance gains, exhaustive dependency-license review or operational backend deployment.

## Manual game acceptance before support claims

Authenticate an owned Java account with our app registration, install from empty cache, enter a world, close/reopen, repair corruption, test Java mismatch and network errors, switch accounts, and confirm logout. Install Fabric/API/Hera independently, inspect each HUD against real game data, drag/scale/toggle, reload config, test small/large UI resolutions, disconnect/reconnect and benchmark with/without compatible optimization mods. Repeat for each claimed OS/version.

## 0.1.1 login and player-facing cleanup

- Replaced device-code UI with a separate Microsoft login webview, native PKCE/S256, random state, strict callback origin/parameter checks, close/cancel/expiry handling and automatic account completion. No credentials or authorization codes enter React.
- Removed roadmap placeholders, early-development labels and developer setup instructions from normal account/home/profile flows. Owner configuration remains under advanced settings.
- Reviewed NoRisk launcher v3 source/license as a behavioral reference; no NoRisk implementation, branding, service credentials or application identity was copied. This remains Hera's own implementation, not a claim of full NoRisk/LiquidBounce feature parity.
- Real Microsoft authorization initially rejected the unregistered redirect URI. After the owner saved the native-client URI, Microsoft displayed the normal account picker. No password was entered by the agent and no ownership-authenticated gameplay is claimed.
- Checks: 38 TypeScript tests and 17 Rust unit tests pass (3 opt-in network tests skipped). Callback tests include forged state, duplicate codes and lookalike hosts; PKCE matches the RFC example.

## Published release verification

- Source commit f290350 was pushed to Lyvt-Dev/hera-client. Signed Windows setup 0.1.1 was published with updater manifest and checksum.
- GitHub normalizes spaces in asset names to dots. Packaging now uses stable ASCII asset names and the published manifest was corrected to the actual asset URL.
- scripts/verify-release.py downloaded all 4,454,298 published installer bytes via the real update channel, verified both Minisign Ed25519 signatures against the embedded public key, and confirmed that a changed byte is rejected. This is a real transport/signature check; installation/upgrade on a separate Windows machine remains untested.
- GitHub browser authentication succeeded via Git Credential Manager. Only source and public release artifacts were pushed; no local key, token or game data was committed.
- Automated approval review initially required explicit consent before transferring the signing key. The owner then expressly approved storing it as an encrypted Actions secret. The key was sealed with GitHub's public key, stored as TAURI_SIGNING_PRIVATE_KEY and a new Windows workflow was dispatched. No private key was printed, committed or uploaded as an artifact.

0.1.2 packaging: final Windows NSIS setup built successfully at 7,008,258 bytes with embedded Nunito/font license, pumpkin avatar and generated world image. All final TypeScript checks, 38 scoped tests and web/backend builds passed; final Rust run passed 18 tests (3 opt-in integrations skipped). Publishing uses an isolated Git index based on origin/main so the unrelated local VConnect commit and worktree edits are not included in the Hera source release.

0.1.4 verification: TypeScript checks/build and all 48 tests passed. Browser interaction verified Fabric and Forge selection, automatic 1.21.1 preselection, and that an outside click preserves the open dialog, selected loader and unsaved profile name. No mod/native behavior changed beyond the app version; no new gameplay claim.
