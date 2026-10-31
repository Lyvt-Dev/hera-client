# HUD quality pass — launcher 0.6.3 / mod 0.14.1

This pass improves shared behavior used by every one of the 31 HUD elements. It does not certify all gameplay/presentation modules or replace in-game acceptance.

- Rendering fits the requested scale to the current viewport when necessary. Saved scale, position and anchors are not overwritten. HUD Studio hitboxes, selection outlines, dragging and snapping use the same visible dimensions; an adapted scale is identified in the inspector.
- Text-based HUD modules expose Max. Textbreite under the existing advanced appearance controls (80–640 logical pixels, default 320). Existing configurations receive the default. Long text is ellipsized at codepoint boundaries, controls are flattened to a single line, and font measurements are bounded to a 4,096-codepoint prefix with logarithmic prefix search. Ordinary supported Smooth Font labels use the cached face directly without per-measurement Text wrappers.
- Title/actionbar content and active/expired transitions invalidate only the corresponding cached sample immediately. Other values retain their existing 4 Hz sampling; title/actionbar fade opacity is calculated per frame. Titles use the actual titleFadeInTicks/titleStayTicks/titleFadeOutTicks fields, verified in the mapped game JAR; actionbar uses the actual Vanilla 20-tick fade duration. Empty subtitle rows are omitted. This is not a claim of preserving every styled server text effect.
- Spotify title/artist fitting respects the selected font. Resource/config invalidation clears the media label cache so labels can be remeasured.
- HUD matrix restoration is guarded with finally for all HUD elements.

Automated checks cover viewport bounds across left/right/center anchors and small/large windows without modifying stored settings, long and supplementary-Unicode labels with bounded measurement counts, width migration/clamping, title/actionbar fade curves and actual Vanilla shadow field names/types. These checks do not prove renderer appearance, shader compatibility or GPU/FPS performance. No new game launch was performed; owner testing remains requested.
