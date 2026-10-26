# VConnect status

## Implemented
Independent Node.js 24 / SQLite social website. Locally bundled Nunito (SIL OFL license included); original UI code, SVG icons, geometric designs and test image generators. No third-party UI implementation copied.

- Dedicated login/register pages; email or unique @username login, scrypt password hashes, random per-account salts, HttpOnly SameSite sessions (only hashes stored in SQLite), production Secure cookies, same-origin mutation checks, allowed-host checks, request limits and bounded upload bodies.
- Case-insensitive unique usernames: NFKC normalization, ASCII handles, SQLite UNIQUE index and format triggers. Display names remain separate and may repeat. Existing accounts receive stable handles without losing display names or posts.
- Four-tab profile studio with live preview: name, username, role, pronouns, bio, interests, languages, stream times, collab formats, six themes, eight avatar choices, three banner patterns and four social links.
- Own avatar/banner upload from PNG/JPEG/WebP, crop/zoom/position preview, atomic save and removal. Canonical raster PNGs are dimension/CRC/decompression-validated, re-encoded without metadata and stored as account-owned SQLite BLOBs under generated UUID media URLs. No user-controlled filesystem paths.
- Art is a shared artist channel; Design is a shared programmer channel. Explicit channel selection, image posts with optional alt text and escaped fenced code blocks. Flow navigation removed. No fake member counts, activity, mutual matches or bot posts.
- Larger, quieter feed layout, streamlined right sidebar, animated Posten action and keyboard shortcut, actual likes/bookmarks, full profile viewing, saved Connections and copying text/image links.
- Discovery deck supports direct drag/tilt, direction stamps, layered entry/exit animations, keyboard arrows, single-swipe undo and reduced motion. Fictional demo profiles are clearly labeled and disabled by default in production.
- Deterministic recommendation/feed ranking in recommendations.mjs: shared interests/languages/collab formats, positive and negative swipe feedback, one exploration slot per four cards, recency and connection affinity with consecutive-author diversity. No fabricated match percentages, pronoun-based ranking or sensitive inference.
- Seven-step contextual tutorial: role choice, profile design, interests/links, isolated swipe practice, actual discovery, Connections, posting and bookmarks. Server-persisted pause/resume/completion/replay; no forced public tutorial posts.
- One primary development preview at http://localhost:4173. Node watch restarts the backend; development-only SSE refreshes static code and waits while a form/dialog is active. Production disables reload routes. Hash navigation survives refresh.

## Automated-tested
npm run check, npm test, npm run build: 16 passing test cases. Coverage includes SQL injection payloads at store and HTTP boundaries, authentication and ownership, simultaneous/case/Unicode username collisions, database-enforced uniqueness, legacy migrations, PNG corruption/metadata/size/active-content rejection, atomic media replacement/removal, shared channel and code/image posts, profile/tutorial persistence, recommendation ranking/feedback/diversity, CSRF/host/body-size rejection and escaping stored text/code.

## Browser-tested
Observed profile role/design changes and live preview; uploaded/cropped/saved avatar and banner and reloaded them; real drag swipe, next-card advancement and saved Connection; tutorial pause, resume through all seven stages and persisted completion; Art image upload, alt text and publication; Design codeblock publication with Art post excluded. A discovered asynchronous image-draft race was corrected and the successful upload retested. Larger channel layout and Flow removal inspected. Physical-device/mobile end-to-end testing remains unperformed.

## Game-tested
Not applicable; no Minecraft adapter, native or mod changes.

## Data and release
The owner selected the newer localhost:4174 preview data. Its consistent SQLite backup was promoted to the primary database; the previous main database is retained privately in .local/backups. Credentials from different databases were not merged. Existing user posts and profile images were preserved. The old preview address redirects to the primary during this session. .local, databases, account records, uploads and backups are excluded from Git.

No public deployment. Git delivery is recorded in the repository history. A website change does not require a Hera Windows updater release. Public launch still requires email verification/recovery, moderation/reporting, account/data deletion, operational backups, legal/privacy content and HTTPS hosting on a persistent Node host. No comment threads, real-time messaging, mutual-match notifications or external security audit are implemented/claimed. Static Sites hosting cannot run this persistent Node/SQLite backend.
