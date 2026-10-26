# Verification status

## 0.1.2 Minecraft 403 diagnosis

The owner reached Minecraft authentication after Microsoft/Xbox/XSTS and reported HTTP 403. The original generic error did not retain a specific cause, so missing Minecraft app approval is not yet confirmed. Version 0.1.2 distinguishes an explicit `Invalid app registration` response (`MC_APP_NOT_APPROVED`) from unknown 403 errors (`MC_AUTH_FORBIDDEN`), without exposing arbitrary provider payloads or credentials. An approval-request draft is in [app-approval-de.md](app-approval-de.md). No approval was submitted or granted by this change; authenticated gameplay remains unverified.

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
