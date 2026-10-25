# Mods: Modrinth, CurseForge, Fabric and Forge

## Using the browser

Open **Mods**, create a Fabric or Forge profile for Minecraft 1.21.1 and select it as the target. Search Modrinth or CurseForge, choose a compatible release and add it. Required dependency releases are resolved first. Wrong loaders/game versions, pinned-version conflicts, declared incompatibilities and filename collisions stop installation. Optional dependencies are not installed automatically. Cross-platform project identity and all possible runtime incompatibilities cannot be inferred reliably from provider metadata.

The web preview downloads actual JAR bytes, checks size and SHA-1, and stores files plus a manifest in origin-private browser storage under `hera-profiles/<UUID>/mods/`. These are real downloads, not placeholder profile entries. Each installed file has a **Datei herunterladen** action for transfer to a Minecraft instance. Browser storage can be cleared by the user/browser; it is independent of the native game directory. The browser cannot launch Java or silently write into arbitrary desktop folders.

The desktop app resolves provider metadata independently in Rust, verifies downloads again and stages the complete set before committing files to `<Hera app data>/profiles/<UUID>/mods/`. Profile metadata is outside `mods`, in `hera-mods.json`. It rejects modifications while Minecraft runs. Re-adding the same recorded version is idempotent. Updating/removing managed mods and automatic modpack conflict resolution are not part of this change. Profile duplication currently duplicates configuration, not installed files.

## Loaders

Vanilla cannot accept mods. Fabric and Forge are separate immutable profile loaders. Current pinned loaders are Fabric **0.16.14** and Forge **52.1.20**, targeting **1.21.1**. Their launcher adapters are experimental until authenticated game launch/gameplay are verified.

Fabric downloads official profile metadata and checksum-verified Maven libraries, merges JVM/game arguments and starts KnotClient. Forge downloads the official installer from the Forge Maven with its published checksum, prepares a separate runtime under `runtimes/forge-1.21.1-52.1.20`, then executes that verified installer with Java 21. No processor or executable from a mod project is run by the launcher. Official installer output is validated against generated version metadata and library hashes. Forge installation may take several minutes; installer logs live inside that runtime. A corrupt generated Forge artifact currently reports an explicit repair error; automatic replay of its processor pipeline remains future work.

No NoRisk or other launcher code is copied. Mods remain third-party code and are fetched from provider URLs; authors' licenses and distribution restrictions apply. No downloaded game/mod JAR is bundled in Hera releases.

## CurseForge key and gateway

The [official CurseForge API](https://docs.curseforge.com/rest-api/) requires an API key. There is no configured key in this project. Get one for your own application via the official CurseForge developer console under its applicable terms. Never put it in React, a VITE-prefixed variable or the desktop binary.

For browser development, copy `apps/launcher/.env.example` to `apps/launcher/.env.local`, put the key there, then restart `npm run dev`. Only Vite's Node middleware reads it. For a native development gateway:

```powershell
$env:CURSEFORGE_API_KEY='YOUR_SERVER_SIDE_KEY'
npm run catalog
```

Then run the desktop app in an environment with:

```powershell
$env:HERA_CATALOG_URL='http://127.0.0.1:3101/api/catalog'
./scripts/desktop.ps1
```

Production requires an operated HTTPS gateway with server-side key storage and rate limiting. The main Fastify backend also exposes the same `/api/catalog/*` routes. The local gateway binds loopback only. The standalone packaged launcher does not start a Node service itself. `HERA_CATALOG_URL` is public endpoint configuration; the API key never leaves the gateway. Requests honor author distribution opt-out and unavailable download URLs without inventing CDN links. The browser uses a bounded, hash-verified download relay for CurseForge CDN files where direct CORS cannot be relied upon.

## Microsoft Client-ID

In **Account**, Hera now explains registration and accepts the public client ID without a rebuild. Native configuration is saved in SQLite; browser configuration is separate localStorage. Existing accounts must be logged out before changing the app identity. A changed ID cancels an outstanding device-code flow. A build-time `HERA_MICROSOFT_CLIENT_ID` remains an optional default.

The user currently has no confirmed Entra directory/application registration, so no ID was created. Creating a tenant, accepting Microsoft terms and granting application access require the owner's account and decisions. See [Microsoft setup](microsoft-setup.md). A syntactically valid UUID is not evidence of a registered or Minecraft-approved application; actual login remains unverified.
