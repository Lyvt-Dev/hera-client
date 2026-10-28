# Hera interface system — 0.5.0 / mod 0.12.0

## Architecture reviewed

The launcher is React/TypeScript, Vite and Tauri. `App.tsx` owns page navigation, the
selected profile and launch state. Page components reuse `ui.tsx`, `ProfileList`
and the existing typed `bridge.ts` boundary. Rust owns UUID profile directories,
account secrets, installers and process lifecycle. This change does not move any
native access or credentials into the browser. Existing browser preview profiles
are local preview data, not authenticated launcher accounts.

The Fabric 1.21.11 mod uses Minecraft Screen/ClickableWidget input and DrawContext
rendering. HeraTheme provides Nunito text, cached scale-aware glyph atlases and
coverage-mask rounded corners. ModuleScreen embeds settings collected by
ModuleSettingsScreen. HudRenderer shares measurement with HudEditorScreen;
HudConfig stores schema-2 per-profile layouts and presets. FeatureConfig stores
gameplay settings separately. The local and shared cape systems and Windows
media bridge are independent of this UI work.

## Foundations

| Role | Launcher token | Ingame token | Value |
| --- | --- | --- | --- |
| Canvas | `--bg` | `BG` | #101216 |
| Surface | `--surface` | `PANEL` | #171a20 |
| Hover/input group | `--raised` | `RAISED` | #20242b |
| Divider | `--line` | `LINE` | #2c313a |
| Text | `--text` | `TEXT` | #f4f2ef |
| Secondary | `--muted` | `MUTED` | #a9adb5 |
| Hera orange | `--accent` | `ACCENT` | #f49b63 |
| Selected | `--accent-soft` | `SELECTED` | #30271f |

`design-system.css` is the launcher component layer over existing structural
styles. `HeraTheme.java` is the mod's semantic foundation. Colors have the same
roles, while dimensions follow each platform's units. Typography uses the
already bundled OFL Nunito assets, with explicit heading/body/secondary levels.
No new dependency, reference implementation, icon pack or image was imported.
Existing Pumpkin branding, licensed font files and world artwork are retained.
Spacing follows 4/8/12/16/24/32 where practical; compact game controls retain
Minecraft GUI units. Corners are 5–8 for controls, 12 for desktop workspaces.
Desktop transitions and game button hover transitions take 160ms; no blur pass,
pulsing decoration or per-frame font rasterization is added. Launcher reduced
motion preference remains supported. The game hover transition is independent
of the existing HUD keystroke animation preference.

## Interaction changes

- Search and category results are cached on input changes rather than rebuilt
  in every render. Empty results clear stale module settings and activation controls.
- Module rows have a title, description, state switch, focus highlight and tooltip.
  Space toggles a focused row; Enter selects it. Ctrl+F focuses search, Escape
  clears a nonempty focused search before the ordinary close/save action.
- Detail rows distinguish labels from values. Continuous HUD appearance settings
  use a reusable HeraSlider, with drag and vanilla keyboard slider navigation.
  Long values are bounded by the detail pane instead of overwriting their label.
- Selection is reconciled when the active-only category loses a disabled module.
  Scrollbars show list/detail position. Existing cyclic choices remain cyclic;
  this release does not claim every option is a dropdown or slider.
- Appearance edits invalidate cached HUD measurements immediately. Full settings
  pages disable unavailable previous/next controls.
- HUD Studio lists enabled state separately from selection, has wider element
  labels and orange alignment guides. Clicking the palette background no longer
  drags an obscured HUD element. Drag, anchors, scale, undo/redo, presets and save
  retain the existing implementation.
- New HUD modules can follow the global Hera accent. Old serialized modules
  without this field retain their custom accent; choosing a custom accent opts
  out. This intentionally preserves existing appearance and enabled states.
- Launcher pages share Nunito, stronger hierarchy, deliberate list density,
  accessible focus colors, aligned navigation and controls, calm empty states,
  responsive settings groups and a more prominent launch action. Existing
  single-launch guards, error/progress feedback and explicit profile actions
  remain connected to the same implementation.

## Verification boundaries

Automated checks and publication results are recorded in status.md. Geometry
tests cover pane/footer separation; slider tests cover bounded quantization;
configuration tests cover preservation of legacy accents. These do not establish
gameplay, GPU performance or visual acceptance.

Browser screenshots inspect the actual React preview, not native authentication
or a running Minecraft instance. Native-only cape/account actions cannot be
certified from that preview. No new game launch or multiplayer test is claimed.
Existing custom HUD positions, colors and scales are preserved, so an old layout
may intentionally look different from the new defaults.
