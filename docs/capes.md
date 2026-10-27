# Local Hera capes

Launcher 0.1.8 / Fabric mod 0.7.0, Minecraft 1.21.11.

Open Capes in the launcher, choose a Fabric 1.21.11 profile and a connected Minecraft account, then select a PNG. Supported atlas sizes: 64x32, 128x64, 256x128 and 512x256, at most 1 MiB, RGB/RGBA, no animation. The preview crops the cape face from the atlas; it is a texture preview, not a simulated player model.

The native launcher validates/decodes/re-encodes the PNG and stores it in that UUID profile's config/hera/cape.json. Account tokens, passwords, arbitrary file paths and network uploads are not part of this flow. The file contains the account UUID, an enabled flag and the PNG. Disable or remove the cape from the same page. Other profiles have separate files.

Hera reads changes asynchronously about every two seconds. Unchanged files are checked by metadata only. It uses the cape solely on the local player with the matching account UUID. Minecraft's Skin Customization / Cape setting must be enabled; inspect in third person with F5. Disabling/removing restores the normal Minecraft cape. Elytra textures and remote players are unchanged.

There is no shared cape service, public upload, account-wide synchronization or visibility to other players in this release. These were explicitly deferred by the owner.

Verification: PNG rejection and native persistence/toggle/removal tests; atlas/ownership unit tests; development startup loads the player mixin and all menu panels. Authenticated in-world cape rendering and live replacement still require manual gameplay acceptance.
