# Hera 0.6.0 / Fabric mod 0.13.0

Target: Minecraft 1.21.11, Fabric. Newly added modules default off. Existing
profiles, gameplay settings, local/shared capes and custom HUD layouts remain.
No new dependency or third-party implementation/assets were imported.

## CurseForge

The existing official-API gateway now has native, persisted publisher configuration
and a connection probe under Settings → Advanced catalog settings. The publisher
key stays in the Node service, never in React, SQLite, the game or release assets.
The saved gateway is used for searches, version metadata and dependency resolution
during native installation. API actions are allowlisted. HTTPS is required except
loopback HTTP; credentials and query/fragment parameters are rejected in gateway
configuration. Downloads retain compatibility, filename, origin, hash and author
distribution checks. Listings now filter incompatible provider results and obey
CurseForge pagination limits.

The owner confirmed no API key exists yet. Therefore real authenticated CurseForge
search/download is **not live-tested and cannot work until a key is configured**.
Obtain one from CurseForge for Studios, then set `CURSEFORGE_API_KEY` only in the
gateway process environment. Run `npm run catalog` from the repository. The local
gateway address is `http://127.0.0.1:3101/api/catalog`. Save that address in advanced
catalog settings and use Connection test. A configured-key result checks presence,
not approval/validity of the key at CurseForge. Production needs an operated HTTPS
gateway; no hosting or developer key is bundled.

Official authentication and distribution contract:
https://docs.curseforge.com/rest-api/ — `x-api-key`, file availability and
`allowModDistribution` are respected; no scraping/download URL guessing fallback.

## Mod features and controls

Use Right Shift. HUD modules share appearance sliders and HUD Studio placement.
Features with no additional parameter have an activation control only.

| Requested feature | Implemented behavior / control |
| --- | --- |
| Weavy Capes | Eight articulated textured cape strips, continuous pose/age-driven flutter; strength slider. Preserves vanilla eligibility and elytra exclusion. Cosmetic movement, not a cloth collision simulation. |
| Custom Crosshair | 32×32 transparent painting canvas, brush, right-click eraser, color, undo and clear. Save to the existing per-profile PNG. Preview texture updates only when edited and is released when the screen closes. Existing PNG import remains. |
| Potion Status | Existing effects HUD renamed with migration; real effect icons, names/amplifiers and remaining time. Vanilla effect overlay suppressed only when the replacement is enabled. |
| Scoreboard | Received sidebar/team-color objective, up to 15 sorted non-hidden entries, optional scores; movable/scalable HUD replaces vanilla sidebar only while enabled. |
| Action Bar | Received vanilla overlay text/lifetime, movable/scalable HUD; no fabricated messages. |
| Chat Heads | Player text sprites for exact messages with a known sender profile. Keeps ordinary chat insertion, signature arguments and other mods' cancellation handling; no guessed sender names. |
| Clear Lava | Removes lava fog and the fire overlay while submerged locally; does not alter lava blocks or damage. |
| Arrow Trail | END_ROD particle trail for moving loaded arrows, bounded scan/range and at most 48 particles per sample. Local visual only. |
| Item Model | First-person item scale and X/Y/Z offsets; matrix state restored after submission. |
| Combo Counter | Received damage with local attacker and player target; resets on incoming damage, target changes, disconnect or a 3-second gap. Server attribution is required. |
| Item Counter | Up to eight selected registry items, actual player inventory counts including offhand; dedicated searchable selection. |
| Day Counter | Received world time-of-day, with local world fallback before first update. Uses server day data independently of visual Time Changer. |
| Uptime | Existing connection/session timer renamed with migration. |
| TPS | Smoothed estimate from world-time packet deltas, capped at 20. Displays `≈` and unavailable/stale states; not authoritative server tick profiling. |
| Resource Pack Display | Names of enabled resource pack profiles, capped at six lines. |
| Saturation HUD | Client-held server-updated food and saturation values. |
| Paperdoll | Actual local player model rendered in movable/scalable HUD. |
| Helditem | Main-hand item name and up to seven actual enchantment lines; replaces vanilla held-item label only while enabled. |
| Item Changes | Sampled inventory count deltas, up to six entries, five-second lifetime; first sample is a baseline rather than invented gains. |
| Titles | Actual title/subtitle and remaining lifetime; shared HUD positioning/scale instead of vanilla title rendering while enabled. |
| Block Outlines | Targeted block outline shape projected using the game camera; color and thickness controls. Vanilla outline suppressed while enabled. |
| Hit Color | Custom living-entity hurt tint, with default hurt overlay suppressed for that tint. |
| Jump Reset | HUD timing cue when hurt and grounded. Does not jump or send combat actions. |
| Toggle Sprint | Bind the new Hera toggle key in Minecraft Controls. Uses normal sprint input, leaving vanilla sprint eligibility checks in place. |
| Old Animations | Additional classic-style first-person item swing. Cosmetic legacy swing style; does not restore old combat/blocking mechanics or promise complete 1.7/1.8 animation parity. |
| No Hurt Cam | Suppresses vanilla hurt camera tilt. |
| Low Fire | Lowers the first-person fire overlay; configurable amount. |
| Shield Status | HUD and own first-person shield tint with customizable ready/blocking/cooldown colors. Patterned shields use the closest vanilla dye for their base while retaining pattern layers. |
| No Advancement | Suppresses advancement toasts; achievement state is unchanged. |
| Particle Settings | Search registry particle types; selected types are blocked at the local particle creation boundary, up to 128 choices. |
| Item Physics | Grounded drops lie flat with a stable age; airborne drops tumble. Uses actual entity collision/ground state, not a new rigid-body simulation. Hera Fast Items yields its billboard transform while this is active. |
| Clear GUI | Removes the base in-world menu dimming background. Individual screen textures/other mods' backgrounds or blur are not universally removed. |
| Way Points | Up to 32 named current-position points, isolated by server/world name and dimension; on-screen distance labels. Manager supports removal and persistence. Optional key binding in Minecraft Controls. |
| Shulker Preview | Bounded 27-slot item tooltip from the item's received CONTAINER component. No hidden server/container inventory queries. |

### Invisible players / vanish

Hitboxes has an explicit switch for invisible **loaded** entities. Player hitboxes
can include invisible player entities the server has sent. A server-side vanish
plugin that withholds/removes the player entity cannot be reconstructed by this
client. The module description states this; no fake entities or tab-list guesses.

### Data and performance

Feature options stay in schema-2 `features.json`; HUD layouts remain schema-2.
`Effekte` → `Potion Status` and `Session` → `Uptime` migrate in normal load and
preset restore. Counts, scoreboard sorting, pack names and text measurements use
the existing HUD sample cache. Arrow work is limited to 1024 examined entities per
sample, 64-block range and 48 emitted particles. Cape mesh is cached, pixel-editor
history bounded to 32 strokes, inventory changes bounded to six entries. No new
network authentication, packet spoofing, anti-cheat bypass, shader or framebuffer
blur pass was added. Opt-in rendering necessarily has a cost; no zero-FPS-impact
claim or measured FPS guarantee is made.

## Verification

See `status.md` for final build/test counts and publication results. Automated
tests cover pixel stroke/undo math, option bounds and persistence, old HUD name
migration, TPS math, official API header/pagination/filter boundaries and native
gateway URL/persistence. An ASM test checks the new mixins' method selectors against
the actual mapped Minecraft JAR without launching the game. This is not a runtime
mixin application, visual acceptance or multiplayer behavior test.

No new Minecraft launch, live CurseForge download, multiplayer vanish test, GPU
visual test or enabled/disabled FPS benchmark is claimed. The owner requested to
test the game personally. In particular cape seams/pose, legacy swing appearance,
fire overlay placement and cross-mod renderer compatibility require that testing.
