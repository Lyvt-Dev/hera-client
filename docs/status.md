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

## Launcher 0.1.5 and mod 0.6.0 — 2026-10-27

Implemented: version-specific official Fabric/Forge catalogs replace the fixed 1.21.1 selection. Releases without an official Forge installer remain unavailable. Windows x64 profiles resolve matching Java majors from Mojang's runtime catalog, verify download hashes, and preserve original runtime licensing files. Explicit Java paths remain configurable. The default Fabric 1.21.11 Hera profile installs/repairs embedded Hera 0.6.0 and Fabric API 0.141.6+1.21.11; other game versions/loaders do not receive this incompatible mod. Existing unrelated mods are preserved. Home/profile flows show actual setup progress and keep owner configuration under advanced settings.

Mod implemented: original Hera panorama title menu, smaller three-pane workspace, and experimental D-Tap (disabled by default). D-Tap requires a server-confirmed player hit, estimates target/self damage and reach, confirms placed obsidian/crystal entities, and gates its second crystal on both removal and target totem-pop confirmation. It aborts on invalid state/timeouts and cannot guarantee a pop or kill. All modules remain initially disabled.

Automated-tested: TypeScript check/build and 54 tests; Rust check and 27 unit tests (8 opt-in integrations excluded from the regular run); Gradle build and 49 tests. Real Windows installation integrations passed for Fabric 1.16.5 and 1.21.11 (including bundled Hera/API), Forge 1.8.9, 1.12.2, 1.20.1 and 1.21.11. Official Java 21 download/execution passed. Development Minecraft startup rendered the Hera title/workspace and opened all 29 module settings plus pickers without a startup exception. Screenshots inspected. These are installation/startup checks, not authenticated gameplay.

Game-tested: the owner reports Microsoft approval and successful launcher login. The agent has not observed authenticated gameplay, live D-Tap PvP, or a clean-machine installed-version upgrade. No new platform/version support certification is claimed. Earlier open-item statements about Java auto-install, fixed loader selection and unpublished old releases are historical and superseded by the dated entries.

Third-party review: bundled Fabric API retains Apache-2.0 and nested notices; a complete Apache license accompanies the launcher payload. Hera includes existing Nunito OFL and Fast Items CC0 notices. No reference-client code/assets copied. Java packages download directly from the official Minecraft runtime catalog, with their original legal files; older Oracle Java and newer OpenJDK packages have different terms and are not redistributed inside the installer.

Publication verification, 2026-10-27: source 5ef4a62001602b85340b93c49f37d070e7b7c365 pushed to origin/main. Windows v0.1.5 and mod-0.6.0-mc1.21.11 published. Public updater verification downloaded 9,834,983 installer bytes, verified the updater signatures, and rejected a modified installer. Public mod download SHA256 EAE83D1E7E988B7CB323CC677BB17C3C6D730D8EA84C9837D07C6502D36AD0E3 matches the launcher-embedded payload. Clean-machine install/upgrade and authenticated gameplay remain unobserved.

## Launcher 0.1.6 — Windows start crash hotfix

Observed: owner reported an unresponsive launcher followed by process termination on Minecraft start. Windows Application Error/WER records confirm hera-launcher.exe 0.1.5 terminated with exception 0xc00000fd (stack overflow) on 2026-10-27. No account tokens were read for diagnosis.

Implemented: move the 64 KiB checksum buffer, which survives an await, to heap storage. Heap-pin installation futures at both native start and repair command boundaries. The measured installation future shrank from 67,224 to 2,416 bytes. Regression coverage fails against the old implementation and bounds both checksum and installation future sizes. The fix reduces synchronous command-stack use without increasing thread stack limits or changing authentication.

Automated-tested: cargo check and 29 Rust tests passed (8 opt-in integrations excluded from the regular run); npm run check, 54 tests, and production build passed. The user-reported authenticated start has not yet been repeated by the agent; actual gameplay acceptance remains outstanding.

The opt-in real Fabric 1.21.11 installation/repair integration also passed with the 0.1.6 fix (57.13 seconds), including bundled Hera/Fabric API. This did not authenticate or launch gameplay.

0.1.6 release verification: source 1ad076915ea9d5ee121d3eb571fbdb7a839c4849 pushed and signed Windows v0.1.6 published. Public updater verification downloaded 9,834,506 bytes, verified signatures and rejected a modified installer. No clean-machine upgrade or authenticated gameplay observation is claimed.

## Mod 0.6.1 / launcher 0.1.7

Implemented: owner's unchanged Hera pumpkin logo in title screen, module workspace and HUD Studio; refined title/account/button styling. Account head uses Minecraft PlayerSkinProvider's asynchronous cached supplier and PlayerSkinDrawer, with vanilla fallback during loading. No additional avatar service or login data in UI code.

Packet correctness: removed direct UpdateSelectedSlotC2SPacket sends from CrystalTotem and D-Tap (including slot restoration). Local mapped vanilla bytecode confirms ClientPlayerInteractionManager synchronizes the selected slot on its tick and before interactions; the previous extra send did not update its cached slot and could duplicate that notification. AutoTotem retains vanilla clickSlot SWAP-to-offhand with handler/cursor checks. All three automatons pause during item use; CrystalTotem/D-Tap also reject a non-player inventory handler. These code findings do not establish the cause of the owner's private anti-cheat ban.

D-Tap's new cautious option defaults on while D-Tap itself stays off: minimum four ticks between actions, separate slot-selection wait, actual crosshair block/face/hit vector required for placement, actual crosshair crystal and recovered attack cooldown required for attacks. Manual aiming is needed; otherwise bounded timeouts abort. Original behavior remains selectable. This is correctness/conservative execution work, not a tested anti-cheat bypass. No private plugin source, check details, or server trace was provided beyond "unfair advantage" / reported "bad packets".

Launcher mod-catalog fix: remove the remaining Minecraft 1.21.1-only guard and filters in native Modrinth catalog and backend provider boundary. Validate the selected version using the existing profile rules, then use it for search and version listings for both providers. Existing download compatibility/integrity checks remain enforced. Launcher bundles the updated 0.6.1 mod.

Automated-tested: Gradle build and 51 tests passed. Explicit development startup rendered the owner logo and fallback player head, opened all 29 module panels and item/color pickers, and emitted HERA_SMOKE_OK; title/workspace screenshots inspected. npm check, 58 tests, and production build passed. Real authenticated custom-skin rendering, live PvP, and anti-cheat compatibility remain unverified.

Native verification: cargo check and 30 unit tests passed (8 opt-in integrations excluded from regular run). Real Modrinth search, Sodium download/hash verification, installation into a fresh Fabric 1.21.11 UUID profile and idempotent second installation passed in 3.50 seconds. The first integration attempt correctly rejected a conflicting older Sodium record in a reused test profile; the test now allocates a fresh UUID. CurseForge version-filter behavior has automated provider-double coverage; no live owner API-key/gateway proof was added.

Publication: source 4d7607b46d1cef2cfbe7fd79cf7f3339532c4535 pushed; Windows v0.1.7 and mod-0.6.1-mc1.21.11 published. Public updater verification downloaded 9,906,866 installer bytes, verified signatures and rejected a modified installer. Public mod SHA256 B92EDF3A3A8473CA7149DAC21F38F151AE61CFDF97289476CAEC5CA801D2B00B matches the launcher payload. Gameplay/anti-cheat compatibility and clean-machine upgrade remain unobserved.

## Launcher 0.1.8 / mod 0.7.0 — local capes and performance pass

Implemented: typed native local Cape PNG read/save/toggle/remove commands, strict PNG decode/re-encode with size/atlas limits, UUID profile directories and symlink guards; no arbitrary paths or account tokens in React/config. Launcher Capes page includes texture preview and explicit per-profile/account controls. Mod polls metadata asynchronously, reloads changed texture only, and overrides only the matching local player's cape; remote players and elytra remain unchanged. The owner explicitly deferred the shared service. PNG 0.18.1 MIT license reviewed and bundled in native resources. See docs/capes.md.

Performance: no full entity sort per rendered frame; nearest-128 tick cache with live interpolation and bounded glow; reusable projected corners/clip buffers and offscreen rejection; incremental container cursor with 4,096-cell/64-section cap and cooperative 1 ms target; D-Tap evaluates at most four candidates per step, reuses damage estimates, and skips exposure checks while awaiting aim/cooldown. Visible HUD text/width snapshots and common TNT/key/armor labels are reused; marker/font caches are bounded and reset on resource reload. Selected items use identity lookup, and fallback options/sprite identifiers are reused. Cape SkinTextures overrides are cached, with metadata-only checks for unchanged files. Other module hooks audited for bounded/disabled behavior. Detailed scope, tradeoffs and reproducible clipping measurement in docs/performance.md.

Automated-tested: npm check/build and 58 tests passed; cargo check and 33 tests passed (8 opt-in integrations skipped); Gradle build and 57 tests passed. Native cape tests cover corrupt/wrong-size PNGs, profile isolation, persistence, replacement, enable/disable and removal. Mod tests cover local-only ownership/atlas bounds, top-K equivalence over 5,000 candidates, clipping equivalence over 10,000 inputs, and cooperative budgets. Final development startup emitted HERA_SMOKE_OK after player mixin loading, 29 module panels and pickers. Clipping microbenchmark: 59.17 -> 0.00 allocated bytes/segment, matching checksums; this is not a Minecraft FPS result.

Game-tested limits: no authenticated in-world cape acceptance, full feature-by-feature comparative FPS/frame-time capture, PvP/anti-cheat compatibility or clean-machine upgrade observed. Extra rendered visuals still cost CPU/GPU time. No zero-FPS-impact guarantee or new adapter support certification is claimed. Publication was held during the performance pass; the owner then explicitly requested publishing this update.

0.1.8 publication verification: source 18b1fd431383eac9f015cf6468c256a0e22f4cee pushed. Final Windows v0.1.8 and mod-0.7.0-mc1.21.11 published after owner requested the update. Public updater verification downloaded 10,004,804 bytes, verified signatures and rejected a modified installer. Public mod SHA256 331051FEC095B65E8FBA6294B6C419136E014EF7153420F9630865B289EAC5FD matches the final embedded performance payload. The earlier pre-performance local installer was rebuilt before publication. Gameplay/FPS/cape acceptance limits above remain.


## Mod 0.8.0 — Custom Crosshair and Own Nametag (release held)

Implemented: two independent, disabled-by-default modules under Darstellung. Custom Crosshair offers color, length, gap, thickness, center dot, black outline, opacity and an optional imported transparent PNG. The preview/import screen accepts one dropped PNG, validates its header and bounded dimensions before native decode (256 KiB, 256 × 256 maximum), copies it into the current game profile at config/hera/crosshair.png, preserves aspect ratio and caches one texture. No filesystem access, image decoding or texture allocation occurs per rendered frame. Missing PNGs fall back to the configured geometric crosshair. First-person only; spectator crosshair remains vanilla. Own Nametag enables Minecraft's native name label only for the local player in third-person, optionally while sneaking; other players' name rendering is unchanged. Settings persist through the existing per-profile features.json. No new libraries or third-party assets.

Automated-tested: Gradle build and all 60 tests passed, including PNG header/dimension/size rejection, configuration round-trip, disabled defaults and independent module toggles. Development startup loaded the new player/HUD mixins, rendered all 31 module settings panels plus the crosshair preview and existing pickers, and emitted HERA_SMOKE_OK. Launcher check, 66 JavaScript tests and production build also passed for the pending desktop redesign.

Game-tested: startup/menu coverage only; authenticated in-world own-name visibility, actual drag-and-drop PNG acceptance and comparative frame times have not been observed. No zero-FPS-impact claim. Windows updater publication remains paused at the owner's WAIT instruction; the public launcher still embeds mod 0.7.0. The standalone 0.8.0 JAR is available locally for testing and will be integrated before the next installer build.


## Launcher 0.2.0 — desktop redesign and mod 0.8.0 integration

Implemented: redesigned eight-page graphite desktop UI, compact/collapsible navigation, central Play workspace, profile rows/search/filter/pagination, accessible tabs and dialogs, profile-removal confirmation, appearance/reduced-motion controls and stage-correct launch/download feedback. Real owned-game process query and throttled renderer progress; official loader proxy fixes browser-preview CORS. Includes mod 0.8.0 Custom Crosshair and Own Nametag for Fabric 1.21.11, hash-pinned with matching Fabric API. No other game/loader receives the incompatible payload. Owner explicitly resumed client publication after the additions.

Automated-tested: TypeScript check, 66 JavaScript tests and production build passed. Mod Gradle build, 60 tests and development startup covering 31 panels plus pickers passed. Native checks rerun after updating the embedded payload. See desktop-redesign.md for actual renderer workflow and viewport coverage. Authenticated gameplay, real OS DPI and clean-machine upgrade remain unobserved; no new adapter certification or FPS guarantee.

0.2.0 publication: source c1c0e8d8ec0f3a98ac4df1b2061dcfb52f36d324 pushed; Windows v0.2.0 published. Public updater verification downloaded 9,981,599 bytes, validated the existing updater signing identity and rejected a tampered installer. Embedded mod 0.8.0 SHA256 047CBB078DA48254F58A06C25E50E5E7D8800EA4B0C09490FBA96FCD59B53E82. Final native cargo check and 33 tests passed (8 opt-in integrations ignored). Authenticated gameplay and clean-machine installation were not repeated.


## Mod 0.9.0 — HUD Studio and visible D-Tap aiming (owner test build)

Implemented: independently configured HUD text/background opacity, padding, corner radius, fonts, text alignment, accent controls where used, expanded coordinates, precision/chunk/biome/facing, real latency coloring, measured HUD frame-interval graph, genuine mouse-edge CPS with bounded rolling windows, potion icons/name/amplifier/duration, session timer, sprint status, inventory preview and equipment orientation/durability bars. Existing schema-2 layouts migrate with new appearance defaults; all new modules remain off. A private no-argument JSON constructor preserves defaults for older layouts. Editor has explicit element selection, live drag/resize, edge/peer snapping, anchor-preserving placement, keyboard nudging, bounded undo/redo, overlap hints, confirmed reset, three opt-in presets and up to eight saved custom layouts. Advanced appearance controls collapse; key animations are optional and default off. Reuses the existing licensed fonts/assets; no third-party implementation copied.

D-Tap: optional visible auto-aim rotates the actual player view toward the planned support/crystal and waits for the ordinary client tick. No silent/spoofed rotation packets added. Support ray endpoints are nudged inside the clicked face; crystal clicks accept a visible face of the same base. Placement interaction results are checked before advancing. Candidate scanning restarts when the target changes block position. Existing reach, damage, inventory, line-of-sight and server-confirmation guards remain. These guards can still reject a sequence; no perfect pop/kill or anti-cheat compatibility claim.

Verification: 67 unit tests cover configuration migration/presets, anchor round trips, bounds, click windows and aim math. A development-only local singleplayer test rendered real values and potion icons at 1280×720, 1920×1080 and 2560×1440 and emitted HERA_HUD_WORLD_OK. Actual screenshots inspected; the 1920 editor capture was intercepted by a menu/input change and is not evidence of editor acceptance at that size. First preview run found a no-world CPS bounds error, corrected before delivery. Initial timing with all 16 modules: 40 CPU submission samples, median 0.911 ms / p95 2.348 ms; this is neither GPU time nor an enabled/disabled FPS comparison. Full multiplayer D-Tap placement/pop/kill and user interaction acceptance remain unverified. The owner requested no further game launches and will test the final JAR. Launcher updater remains at 0.2.0/mod 0.8.0 until this test build is integrated into a subsequent release.


## Launcher 0.2.1 — bundle HUD/D-Tap mod 0.9.0

Implemented: hash-pinned Hera 0.9.0 payload replaces the managed mod on install/repair for Fabric 1.21.11; matching Fabric API retained. Frontend and native version metadata agree. Other Minecraft versions/loaders remain excluded. Existing profiles, user mods, account credentials and HUD layouts retain the existing isolation/migration behavior. User explicitly requested integration into the launcher; no additional game launches performed.

Validation: TypeScript check and 66 JavaScript tests passed after correcting the frontend bundle version. Initial parallel test run also hit an unrelated API-test timeout; the complete suite passed with one worker. The mod itself passed 67 Gradle tests in the preceding turn. Native checks, signed installer and public release verification recorded below on completion. D-Tap server acceptance remains a user-test limitation.
