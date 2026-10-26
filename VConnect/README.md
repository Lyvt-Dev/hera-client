# VConnect

Requires Node.js 24. Run `npm run dev` and open **http://localhost:4173**.

The app uses SQLite at `.local/vconnect.sqlite`, created/migrated automatically. Keep `.local/` private and persistent: it contains account hashes, public content, uploaded images and backups and is excluded from Git. No secrets or account records are bundled. Back up with SQLite's backup API; WAL is enabled.

Register an email, unique @username and password (12–256 characters). Login accepts email or username. Profile names are display names; handles are unique regardless of case. Existing profiles retain their display names and receive stable handles.

Art is a shared artist channel with image posts. Design is a programmer channel with projects and escaped fenced code blocks. Profile editing includes own avatar/banner upload, cropping and a live design preview. PNG/JPEG/WebP originals may be up to 8 MB; server storage contains validated, metadata-free raster PNGs. Media is publicly visible where the user posts it.

The seven-stage tour can be opened from the header star or sidebar help area; progress is private to the account. Recommendations use interests, languages, collab formats and swipe feedback; the Neueste feed remains chronological.

Development includes six labeled fictional swipe profiles. Set VCONNECT_DEMO=0 to hide them. NODE_ENV=production disables them by default. npm run dev watches the backend and browser assets refresh automatically, deferring refresh while editing. VCONNECT_LIVE_RELOAD=0 disables that development behavior.

Production requires HTTPS, PUBLIC_ORIGIN equal to the public origin, a reverse proxy preserving Host, and a persistent Node host/disk. Configure HOST, PORT or VCONNECT_DATABASE on the server when needed. Static hosting alone cannot run the SQLite backend. Runtime configuration belongs on the server, not in frontend account flows.

Validation: `npm run check`, `npm test`, `npm run build`. See docs/status.md for current implementation and browser verification, and docs/security.md for the SQL/media/auth review and remaining release work.
