# Feature parity and acceptance criteria

Statuses are implementation scope, not claims of production completion. Actual verification is recorded in [status.md](status.md).

| Phase | Capability | Present scope | Acceptance gate |
|---|---|---|---|
| 1 | Monorepo / strict types | Implemented | npm ci, check, tests and build pass from checkout |
| 1 | Original shell / design | Implemented subset | keyboard navigation, modal focus, reduced motion, readable at 850×640; no fake data |
| 1 | Persistent settings | Browser + native SQLite | change/restart, invalid input rejected, storage errors visible |
| 2 | Profiles | CRUD, duplicate, JSON import/export, isolated UUID paths | import cannot escape root, restart preserves data, deletion retains game files |
| 2 | Microsoft accounts | Native Microsoft window + authorization code/PKCE, refresh, ownership, keychain, account list/logout | successful real account login, renewal, denied/cancelled flow; no tokens in DB or logs |
| 2 | Vanilla install | Experimental 1.21.1 x86_64 | clean install, all official hashes, repair corruption, offline error, real launch |
| 2 | Process control | Native spawn/exit/stop | one game only, actual exit observed, Java mismatch shown, no shell interpolation |
| 2 | Runtime | PATH / JAVA_HOME detection + profile path | Java 21 check, missing runtime error; automatic managed install still planned |
| 2 | Version catalog | Live official metadata | network failure visible; catalog never implies tested adapter |
| 2 | Fabric/Forge | Experimental 1.21.1 adapters; real installation tests passed | independent per-version install/start tests; no support badges before evidence |
| 2 | Download UX | File-count progress, retries/cache repair | trustworthy counts; speed, ETA, cancellation and range resume still planned |
| 3 | Fabric client | Independent 1.21.1 source | real Fabric build and world-load, runs without Hera launcher |
| 3 | ClickGUI | Search, toggles, keybind, persisted config | actual state changes visible after world/game restart |
| 3 | HUD editor | Drag, scale, grid, normalized placement, two colors/background toggle/reset | module stays visible at resized resolution, persistence failure visible |
| 3 | First HUD modules | FPS, coordinates, ping, RAM, clock, direction, velocity, biome, server | each value cross-checked in actual game; zero made-up metrics |
| 3 | Remaining HUD modules | Planned | CPS, armor, potions, keystrokes, sprint/sneak, chunk, player, inventory, durability, session stats, reach info, crosshair, bossbar, scoreboard, tablist, chat, waypoints, screenshots, notifications each with game tests |
| 3 | Advanced editor | Planned | anchors/guides, fonts, alpha/color picker, per-module controls, import/export/presets tested |
| 4 | Performance | No boost claims | fixed world/seed/settings/hardware, warmed recordings, p50/p95/p99 frametimes, measured before/after |
| 4 | Modrinth / CurseForge | Search, release selection, required dependencies, profile install; CurseForge requires server key | official API, transitive deps, compatibility/conflicts, hashes, backup/restore; server-only CurseForge key |
| 4 | Cosmetics / skin 3D | Planned | own assets, transforms, visibility privacy, animated rendering, drag/zoom and skin UV verification |
| 5 | Backend settings sync | API source + schema | authenticated identity, CAS conflict, persistent PostgreSQL restart and Redis failure integration tests |
| 5 | Friends / chat / presence | Planned | friendship ACL, privacy, blocked users, message limit, reconnect, moderation and secure WebSockets |
| 5 | Hosted worlds | Planned infrastructure | private IP protection, invite expiry, ACL, reconnect, abuse/bandwidth budgets and actual multiplayer test |
| 5 | Moments | Planned infrastructure | screenshots, opt-in audience, upload sanitation, delete/report, reactions/comments authorization |
| 4–5 | Arcade / Discord / servers | Planned | actual working games/IPC/pings, optional background CPU budget |
| 6 | CI artifacts | Build/test workflows | green matrix from clean checkout, artifacts individually verified |
| 6 | Installers / updater | Planned release hardening | signed EXE/AppImage/DMG and update manifests, rollback/recovery, three channels tested |
| 6 | Security / licensing | Initial boundaries | dependency/license review, auth abuse tests, signed provenance, EULA/Microsoft/third-party terms sign-off |

NeoForge, legacy 1.8.9, mod updates/removal, modpack import/export and full runtime conflict analysis remain planned. See [mods.md](mods.md) for current provider/setup limits.
