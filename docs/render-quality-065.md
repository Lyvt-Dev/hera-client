# Visual integration quality pass — launcher 0.6.5 / mod 0.14.3

## Hera companion

Uses the actual camera's third-person state for shoulder visibility, including Free Look and Freecam. The model can remain visible with F1; the new Mit HUD ausblenden option restores HUD-linked hiding if desired. Captions still respect F1. Owner/world identity and alive-state checks reject stale poses before the next tick; a fresh session starts at the follow/shoulder target rather than interpolating out from inside the player.

Model JSON parsing and static mesh preparation run in a background task. Render-time pose calculation is shared by the base and optional glow pass. Static cube quaternions and mutable per-bone quaternion caches replace repeated per-draw quaternion construction. This is a source-level allocation/work reduction, not a measured FPS guarantee.

Clip changes blend from the last displayed pose over a configurable 0–500 ms interval. Initial and newly loaded sessions use the supplied pose directly; rapid clip changes capture the current blended pose instead of snapping to a rest pose. Animation neu starten restarts manual preview. All eight imported clips and original mesh remain. Shoulder mode still uses hover because the owner asset contains no seated clip. Legacy-style animations below remain stylistic additions, not full old-combat restoration.

Voice processing uses a bounded background executor. Opening, gain updates, stop/close calls and WAV decoding stay off the game tick/render thread. Disabling, muting or changing owner/world invalidates pending playback. Active volume follows the local voice slider and master volume on tick updates. Native audio-device timing and playback were not tested in-game.

## Crosshair, names and overlays

Custom Crosshair replaces only the Vanilla reticle texture call, preserving native attack indicators, spectator gates and 3D debug-crosshair behavior. Controlled third-person views hide the unrelated player aim reticle. The precise draw-call signature and remapped release annotation were inspected in the actual game/release JARs. Crosshair image import/storage performance is a separate remaining review item.

Own Nametag uses the actual camera state and offers an 8–64 block range, bounded by Vanilla's upstream 4,096 squared-distance label gate. Invisible/sneaking/HUD rules remain. Detached cameras suppress body hurt tilt/view bob, fire overlay and the stale player block-selection outline; this does not stop server-side damage or move/protect the player.

## Scoped item/fire rendering

Low Fire, Item Model/Old Animations and Item Physics wrap the original call in try/finally, rather than pairing separate HEAD/RETURN injections. That restores caller matrices after early cancellation or exceptions and samples module conditions once for the scope. This uses MixinExtras 0.5.0 already bundled by the required Fabric Loader 0.18.1; no new runtime download/dependency was added. MIT license reviewed from the installed artifact and retained at META-INF/licenses/MIT-MixinExtras.txt. No reference-client implementation was copied.

API contracts: [WrapMethod](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod) covers whole-method wrapping including cancellation; [WrapOperation](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation) preserves/chains the original texture operation. Runtime compatibility with arbitrary other mods is not claimed.

Item Model exposes optional left-hand X-position mirroring. Old Animations mirrors the added swing rotations for left-hand use and exposes strength. Item Physics exposes ground flattening and additional airborne tilt speed; it is still stylized rendering, not rigid-body simulation. Patterned Shield Status reuses its dye palette and nearest-dye result until the desired RGB changes; patterned shields remain limited to Vanilla dye colors.

## Evidence and limits

Gradle build and 116 tests passed. Tests cover animation sampling, angle wrap, initial/rapid/session transition behavior, quaternion-equivalence, defaults/bounds and dye matching. Direct item-wrapper tests simulate cancellation and exceptions and verify caller matrices/depth restore. ASM checks validate mapped render selectors, finally cleanup, actual camera-state use and call targets. Release-JAR WrapMethod/WrapOperation selectors were manually checked for intermediary remapping. These do not prove actual transformed-mixin application or rendered appearance.

No Minecraft launch, driver playback, shader test, visual acceptance or GPU/FPS benchmark was performed; owner testing is still requested. The remaining 69-feature quality scope is retained in feature-quality-review.md.
