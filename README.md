# Hera Client

Independent Minecraft Java client ecosystem under active development. **This repository is not yet a production client. No Minecraft version is currently certified supported.** See [verification status](docs/status.md), [architecture](docs/architecture.md) and [feature acceptance matrix](docs/parity-matrix.md).

## Start

Requires Node.js 24+, npm, Rust stable, the [Tauri platform prerequisites](https://v2.tauri.app/start/prerequisites/) and Java 21 for Minecraft/Fabric. Dependency lockfiles are committed.

```powershell
npm ci
npm run dev        # explicit browser preview at http://127.0.0.1:1420
npm run desktop    # actual Tauri application
```

Local tools downloaded during development are ignored in `.local/`. On this workspace, run `./scripts/desktop.ps1` to use the project-local Rust and Java installation. No global PATH was changed.
`./scripts/check.ps1` runs the checks with the local toolchain, including the Fabric build. Add `-LiveDownloads` to opt into the real Mojang installation/repair test (downloads game files into `.local/install-smoke`).

The standalone Windows development binary is `apps/launcher/src-tauri/target/debug/hera-launcher.exe` after `cargo build --locked --features custom-protocol --manifest-path apps/launcher/src-tauri/Cargo.toml`. It embeds the production web build and needs no Vite server. It is an unsigned development EXE, not a tested installer or production release.
Use one development server at a time: stop `npm run dev` before `npm run desktop`. The standalone EXE can run independently of either command.

Windows setup: `./scripts/installer.ps1` builds the NSIS installer, update signature, checksum and GitHub release manifest in `releases/`. The Downloads page provides Minecraft install/repair and the native launcher updater. The default release channel is `Lyvt-Dev/hera-client`; no release is published automatically. See [installer and update setup](docs/installer-updates.md) for signing-key backup and publishing instructions.

## First native workflow

1. Register your own Microsoft public-client application with personal-account/desktop authorization-code support. Confirm Minecraft/Xbox API access eligibility. Save the public client ID under Settings → Advanced account settings in the desktop app, or set `HERA_MICROSOFT_CLIENT_ID` as a build-time default. Do not reuse another launcher's app identity. The ID is public configuration, not a secret. No Microsoft client secret belongs in this desktop app.
2. Start the desktop app, open Account and select “Mit Microsoft anmelden”. Complete authentication in the Microsoft window; Hera connects the account automatically. Refresh credentials are stored in OS keychain; UUID/name in SQLite.
3. Create a vanilla 1.21.1 profile, set its Java 21 executable and RAM. Install/repair downloads official Minecraft files with hash verification into Hera's application data directory.
4. Start the profile using the authenticated account. Ownership/profile are verified before each start. No offline/cracked mode. The start path is implemented but still requires successful real-account gameplay validation.

The browser preview cannot install or launch Minecraft. Its localStorage is independent of native SQLite. Profiles export only metadata; worlds, packs, JARs and credentials are never included. Removing a profile retains its game directory for manual recovery. Fabric and Forge are now offered as experimental profile loaders. No arbitrary JVM arguments are accepted.

## Fabric client

```powershell
gradle -p minecraft build
```

Use Gradle 8.14.3 and Java 21. Build output: `minecraft/fabric-modern/build/libs/hera-fabric-1.21.1-0.1.0.jar`. Install manually in an independent Fabric 1.21.1 instance with Fabric API. The Hera launcher now installs Fabric 0.16.14 or Forge 52.1.20 into separate 1.21.1 profiles. Installing the Hera mod itself is still manual. Right Shift opens the module menu. HUD editor supports dragging, Shift grid snapping, wheel scaling, right-click toggle, C color, B background, R reset and Esc save. Config: `<game>/config/hera/hud.json`. Gameplay checks remain mandatory.

## Checks

```powershell
npm run check
npm test
npm run build
cargo test --manifest-path apps/launcher/src-tauri/Cargo.toml
gradle -p minecraft build
```

## Backend development

```powershell
$env:POSTGRES_PASSWORD='choose-a-local-development-password'
docker compose -f infrastructure/docker/compose.yml up -d
$env:DATABASE_URL='postgres://hera:YOUR_PASSWORD@127.0.0.1:5432/hera'
$env:REDIS_URL='redis://127.0.0.1:6379'
npm run backend
```

The backend binds 127.0.0.1:3100 and requires PostgreSQL and Redis. Health endpoints: `/health/live`, `/health/ready`. Settings: GET/PUT `/v1/me/settings`, with a valid Minecraft bearer session. PUT `{ "revision": 0, "settings": { "reducedMotion": false, "accent": "violet", "scale": 1 } }`; later writes use the current revision. No browser CORS, chat, social or cosmetics service is exposed. Deploy only behind TLS with secret management and infrastructure review. Database migration is explicit through `schema.sql`; no automatic destructive migration.

## Release policy

CI provides build/test artifacts; they are development artifacts. Publishing, code signing, updater verification/rollback, license compliance inventory and multi-platform real-game acceptance remain release gates. Do not bundle Minecraft game files, reference-client code or third-party cosmetics. Hera is not affiliated with Mojang or Microsoft.

## Mod browser and account setup

Open **Mods** in the running UI, create a Fabric/Forge profile and choose it as the target. Modrinth downloads work without a key; CurseForge requires a server-side API key. The browser stores real verified JARs in per-profile OPFS and provides file download actions. Native installs go directly to the profile game directory. See [provider setup, loaders and limits](docs/mods.md). Microsoft Client-ID setup is now explained and editable directly in **Account**; actual registration and service approval still require the owner's Microsoft account.
