# Performance review — mod 0.7.0

This is a source/algorithm audit and targeted measurement, not a guarantee of unchanged FPS or a full gameplay benchmark. Rendering extra outlines, models, text and glow has CPU/GPU cost. Different scenes, hardware, other mods, chunk loading and GC affect frame times.

| Features reviewed | Work removed or bounded | Remaining behavior / cost |
| --- | --- | --- |
| Item/player/mob ESP, hitboxes, TNT | nearest 128 candidate selection moved from every frame to every second client tick; bounded top-K heap instead of sorting all matching entities; live frame interpolation retained; glow shares this bounded selection | Entity enumeration remains on the game thread, 10 times/second while relevant features run; extra glow still invokes Minecraft's outline rendering |
| ESP boxes/tracers | eight reusable projected corners per box instead of repeatedly projecting vertices on each edge; reusable vectors/clipping output; homogeneous offscreen rejection | Visible line primitives still need to be rendered |
| Block/container ESP | no initial queue containing all chunk sections; resumable cell cursor; at most 64 section inspections and 4,096 cells per tick, cooperative 1 ms time target; loaded chunks only; stable prior scan retained | A single vanilla operation cannot be preempted; large scans can take longer to finish |
| Item selection/highlighter | registry IDs resolved to item identity set only when selection changes; no per-frame registry-ID string conversion | Slot/model drawing remains vanilla |
| HUD FPS, coordinates, ping, memory, clock, direction, speed, biome, server | sample only requested/visible modules, 4 Hz; reuse text/width snapshots; calculate dimensions once per module/frame | Values update at the existing 4 Hz rate |
| Keystrokes, armor HUD | reuse key/percentage text and slot arrays; preserve live input display | Four armor item models still render normally |
| TNT/block labels, UI surfaces | reuse common fuse labels, bounded label caches and prebuilt sprite identifiers; clear font-width caches on resource reload | Block distance labels are sampled over five ticks; no persistent unbounded text cache |
| Fast Items | standalone-mod lookup once; avoid geometry classification when chosen options do not need it; stop further flatness work after a non-flat layer | Special/dynamic item models retain defensive checks; no unsafe model cache introduced |
| D-Tap | at most four placement candidates per step with cooperative 1 ms target; reuse target/self damage estimates for suitability and scoring; no repeated exposure raycasts while waiting for crosshair/cooldown | Uses the first suitable candidate group; search may span ticks, aborts if target changes block, and rechecks before actions; no PvP/anti-cheat guarantee |
| Crystal-Totem / Auto-Totem | reuse nine-slot scratch array, remove stream allocation; existing bounded 9/36-slot loops, cooldown and vanilla packet synchronization retained | Actions themselves and server processing have cost |
| Zoom, Free Look, Freecam | inspected constant-size hooks; no scene scans/network/file work; skip zero-motion freecam vector updates | Vanilla camera distance clipping and actual rendered scene remain |
| No Fog, Time/Weather Changer, Full Bright, hide own armor | inspected early-gated constant-time hooks; disabled armor override exits before additional lookups; default options object no longer allocated on fallback calls | Engine render work remains; no measured FPS gain claimed for these hooks |
| Local capes | file work off game thread; metadata poll avoids repeated JSON/base64 reads; texture decode only after change, bounded dimensions; cache local SkinTextures override; remote players exit before UUID/string work | A changed texture must still decode/upload once; not yet accepted in authenticated gameplay |

## Isolated clipping measurement

Run scripts/benchmark-overlays.ps1 with the workspace JDK 21. It compares Hera 0.6.1's previous own clipping implementation against clipInto, using identical deterministic coordinates, five warmup passes, one million measured segments, three separate JVM trials per variant, and fixed 256 MiB heaps. ThreadMXBean measures thread-allocated bytes. This excludes Minecraft rendering, entity collection, GPU work and game frame timing.

Observed here: before 71.04 / 54.54 / 46.05 ns per segment, after 28.78 / 26.54 / 28.71 ns. Allocation before 59.17 bytes/segment, after reported 0.00. All checksums: 1510759388.464. These numbers are not FPS results or a universal speedup claim.

## Acceptance still needed

Compare the same authenticated world, camera path, render distance, JVM and mod set after warmup, feature by feature and combined. Record frame-time percentiles and allocations in entity-heavy, item-heavy and container-dense scenes. Exercise toggles, world changes, resource reloads, and long sessions. No such comparative gameplay capture has been observed by the agent, and adapters remain installation-tested rather than certified supported.
