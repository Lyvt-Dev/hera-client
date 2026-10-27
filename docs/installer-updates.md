# Windows installer and launcher updates

Hera uses a per-user NSIS setup (German/English, shortcuts and uninstaller). The installer downloads Microsoft WebView2 when required. Java 21 and Minecraft game files are separate: choose Java in a profile and use Install/Repair to download and verify game files. The installer does not include a Minecraft license, account tokens, or the game assets.

The Downloads page includes a native Hera updater with explicit check/install actions, byte progress, release notes and errors. It uses the official Tauri updater plugin (Apache-2.0 OR MIT). Installation verifies the update signature and refuses to run while Minecraft or another installation is active. Update transport is HTTPS. Browser preview cannot install executable updates.

## Build locally (PowerShell 7)

```powershell
./scripts/installer.ps1 -Repository 'OWNER/REPOSITORY' -Notes 'Release notes'
```

The default repository is `Lyvt-Dev/hera-client`. Pass `-Repository ''` to disable online updates for a build. A repository name alone is insufficient; use the full owner/name pair. The script sets the build-time `HERA_UPDATE_URL`, builds the installer, copies it and its signature to `releases/`, and generates a static `latest.json` for the Windows x86_64 updater. It does not upload or publish anything. After checks and a source push, `./scripts/publish-release.ps1` publishes the validated local artifacts using Git Credential Manager; credentials remain in memory and are never printed. Until that repository has a public release containing `latest.json`, checking for updates will show an HTTP error rather than falsely report that Hera is current.

Before each release, increment the matching version in Tauri configuration, Cargo.toml and package manifests. The updater offers only newer versions. Upload the setup EXE, its `.sig`, `latest.json` and `SHA256SUMS.txt` to the matching GitHub release `vVERSION`. Release downloads and metadata must be publicly readable for anonymous launcher users. Never embed a GitHub token in the client.

The initial local signing key is in ignored `.local/signing/hera.key`; its public key is in `tauri.conf.json`. Keep a private backup of the original key. Do not commit or upload the private key or generation log. Losing it prevents updates for installed clients that trust this public key. CI must use the same key via `TAURI_SIGNING_PRIVATE_KEY` and `TAURI_SIGNING_PRIVATE_KEY_PASSWORD` secrets. The updater signature is separate from Windows Authenticode; this development installer has no publisher certificate.

## Verification limits

Compile/unit checks and installer generation are recorded in `status.md`. An actual installed-version-to-newer-version upgrade requires two published versions on the configured release channel. It cannot be claimed from building one installer. No GitHub release has been published by this script.

References: [Tauri updater](https://v2.tauri.app/plugin/updater/), [Windows installer](https://v2.tauri.app/distribute/windows-installer/).

The Windows workflow also publishes tagged releases after tests/build. GitHub Actions requires the original signing key in its encrypted signing secrets; locally signed releases use the key on this machine. Never commit the private key. Source changes are pushed after validation according to the owner preference recorded in AGENTS.md.

Release QA: install PyNaCl 1.6.2 into ignored .local/release-tools, then run `python scripts/verify-release.py`. It downloads the public Windows asset and verifies the trusted-comment and binary signatures, including a tamper rejection check, without installing anything. GitHub-normalized asset names use `Hera.Client_VERSION_x64-setup.exe`.

When the workspace contains unrelated local commits, publish only the verified Hera commit from the release branch/reference: `./scripts/publish-release.ps1 -Revision SOURCE_COMMIT`. The script verifies that this exact revision is on origin/main before creating the release; it does not push unrelated local commits.

## 0.1.5 setup

New profiles default to Hera / Fabric 1.21.11. Hera 0.6.0 and the matching Fabric API are embedded and hash-checked during install/repair. Other loader/game combinations do not receive this version-specific payload. Native loader catalogs choose releases actually published for the selected Minecraft version. Windows x64 Java setup uses verified Mojang runtime downloads; custom Java paths remain available. Installation tests do not establish gameplay support. Installer updater signatures are distinct from Windows Authenticode certification.

0.1.7 bundles Hera 0.6.1 (owner logo, native skin head and packet synchronization cleanup), and fixes mod catalog rejection/filtering of game versions other than 1.21.1. Mod downloads are still checked against the selected game/loader and published hashes.

0.1.8 bundles Hera 0.7.0, local cape controls and the performance pass documented in capes.md / performance.md. No shared cape server is configured or advertised.
