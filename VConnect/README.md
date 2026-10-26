# VConnect

Requires Node.js 24. Run `npm run dev`, then open http://127.0.0.1:4173.

SQLite is created automatically at `.local/vconnect.sqlite`. Keep this directory private, persistent and excluded from Git and deployment archives. Back up SQLite with its supported backup API or while the server is stopped; WAL is enabled.

Register through the account button, then choose Konto erstellen. Passwords require 12 characters. Profiles, posts and interactions persist across browser/server restarts. Existing data from the earlier browser-local prototype is not automatically imported.

Local development includes six clearly labeled fictional swipe-test profiles. Set VCONNECT_DEMO=0 to disable them. NODE_ENV=production disables them by default. Production requires HTTPS, PUBLIC_ORIGIN set to the exact public origin and a persistent Node host; Secure cookies are then enabled. Configure HOST/PORT for the chosen reverse proxy. No credentials are bundled. Do not expose the development preview as a production service.

Validation: npm run check, npm test, npm run build. See docs/status.md for implemented behavior and release gaps.
