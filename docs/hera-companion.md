# Hera Companion — mod 0.13.1

Right Shift → Hera Companion. Disabled by default. Automatic mode follows the player and moves to the right shoulder after five seconds standing still. Follow and shoulder modes can be pinned in settings. The shoulder sprite is hidden in first person. State resets on world change/disconnect; no server entities or custom packets are sent.

This first implementation uses two original high-resolution anime billboard sprites, smooth tick interpolation, idle bobbing and a block-occlusion raycast. It floats behind the player; it does not walk with ground pathfinding, collide with terrain, or use a fully rigged 3D model. One sprite draw and one raycast per visible frame; no entity scan or per-frame image generation. No measured FPS claim.

Three optional English voice clips with German captions: greeting, following and sitting on shoulder. Voice defaults off; a volume slider and Minecraft master volume apply. Audio uses local Java Sound, a daemon worker, bounded clips and a twelve-second speech cooldown; it does not access a microphone or online speech service. Captions remain available without an audio device. The clips were synthesized locally with installed Microsoft Zira Desktop; no third-party character or recorded performer was copied. Illustrations were generated specifically for Hera using Imagegen, rather than imported from an anime or another client. No new external code dependencies.

Implemented: module/settings persistence, follow/shoulder state, sprite projection/occlusion, captions and WAV playback. Automated validation and actual game-testing are documented separately in status.md. Runtime appearance, shoulder alignment, audio-device compatibility and gameplay performance still require owner testing.
