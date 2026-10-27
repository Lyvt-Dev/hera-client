# Hera capes

Launcher 0.1.8 / Fabric mod 0.7.0, Minecraft 1.21.11.

Open Capes in the launcher, choose a Fabric 1.21.11 profile and a connected Minecraft account, then select a PNG. Supported atlas sizes: 64x32, 128x64, 256x128 and 512x256, at most 1 MiB, RGB/RGBA, no animation. The preview crops the cape face from the atlas; it is a texture preview, not a simulated player model.

The native launcher validates/decodes/re-encodes the PNG and stores it in that UUID profile's config/hera/cape.json. Account tokens, passwords, arbitrary file paths and network uploads are not part of this flow. The file contains the account UUID, an enabled flag and the PNG. Disable or remove the cape from the same page. Other profiles have separate files.

Hera reads changes asynchronously about every two seconds. Unchanged files are checked by metadata only. It uses the cape solely on the local player with the matching account UUID. Minecraft's Skin Customization / Cape setting must be enabled; inspect in third person with F5. Disabling/removing restores the normal Minecraft cape. Elytra textures and remote players are unchanged.

Shared visibility was deferred in 0.1.8; the optional test service described below was added in 0.3.0.

Verification: PNG rejection and native persistence/toggle/removal tests; atlas/ownership unit tests; development startup loads the player mixin and all menu panels. Authenticated in-world cape rendering and live replacement still require manual gameplay acceptance.


## Optional shared test service — launcher 0.3.0 / mod 0.10.0

Standalone Node.js 22+ service in services/capes/server.mjs; no package installation/database needed. Start `node server.mjs --lan`, then configure the same LAN-IP origin in each launcher profile under Capes. The service ZIP includes a Windows start script and a German step-by-step README. It defaults to loopback without --lan; no firewall or router changes are automated.

Publishing is an explicit action after local PNG selection. The native launcher refreshes the existing Minecraft session, requests a nonce bound to UUID/action/PNG digest, submits the join proof solely to the fixed official Mojang endpoint, then sends the nonce and PNG to the cape service. The service confirms the player/UUID with Mojang hasJoined before writing. Nonces are one-use/60 seconds; no insecure UUID-only upload mode and no user tokens in React, mod files, Node logs or storage. Reads are public UUID lookups, uploads require Internet access to Mojang. PNG chunk CRC, dimensions, decompression limits, byte limits and bounded requests/rates are enforced. Server stores atomic UUID-named PNG files and retains them across restarts. Removal needs the same ownership proof.

Mod reads only public service configuration, queries at most eight loaded-player UUIDs per background batch, tracks up to 64 visible candidates, and uses digest hints to skip retransmitting unchanged PNGs. Bounded reads and deadlines prevent indefinitely stuck fetches. Image decode happens on the worker; at most one changed texture is uploaded per client tick. Cache and texture handles clear when leaving a world, on failures or after ten seconds without contact; the local owner's cape is independent and remains available. Official Minecraft capes and elytra are preserved. Local changes are not automatically republished; explicit publication/removal is separate from local display.

Automated protocol tests use an injected verifier only within Node test code; production always calls Mojang. No real account proof or two-client in-world acceptance was performed for this release, per the owner's request to test it personally. See services/capes/README.md for LAN configuration and limitations. No public HTTPS hosting is included.
