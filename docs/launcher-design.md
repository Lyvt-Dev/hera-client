# Hera launcher visual system — 0.1.2

The owner requested Nunito and the visual language of their Discord-bot project at `C:/Users/baosi/Documents/HeraClient`. That project was inspected read-only; its files were not edited.

## Interface

- Charcoal background and panels, warm orange primary actions, rounded 10/14/18px surfaces, larger Nunito typography and visible blue focus rings.
- Shared Hera pumpkin identity with Minecraft-specific copy and an original voxel-world hero. The home page shows actual profile count, selected profile and account state, plus working profile/mod/download actions.
- Sidebar profile switching is available across pages. The selected profile UUID is persisted locally; passwords, codes and tokens are not persisted in UI state.
- Profile cards, mod search/results, forms, account page, downloads and settings share the same tokens. Legacy violet settings remain readable and display using the new warm theme; new installs default to orange. Cyan remains selectable.
- Desktop layout and narrow-screen CSS are provided. Browser QA verified Nunito's loaded state, successful local image loading, no horizontal overflow at the observed viewport, profile switching after reload, live Modrinth results, modal opening/Escape cancellation and the color selector. No claim is made of a physical mobile-device test.

## Assets and licensing

- `public/fonts/nunito-variable.ttf`: copied unchanged from the owner's reference project. Copyright 2014 The Nunito Project Authors. The accompanying full SIL Open Font License 1.1 is preserved as `public/fonts/OFL-Nunito.txt` and included in the built frontend. No remote font service is required.
- `public/images/hera-avatar.png`: the owner's existing Hera avatar. The reference project's design documentation identifies it as a user-supplied asset. Reused for the same Hera identity as requested.
- `public/images/minecraft-world.png`: new generated illustration created with the built-in image-generation tool for this task; original output retained under Codex generated_images and the selected asset copied into the project. No official Minecraft screenshot or NoRisk/LiquidBounce asset was copied.

The redesign does not certify gameplay support or resolve a server-side Minecraft app approval. See `status.md` and `app-approval-de.md` for the separate authentication evidence and current limitations.
