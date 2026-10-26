# Security review: VConnect

Review scope: store.mjs, transport.mjs, media.mjs, recommendations.mjs and the frontend renderers. This is a code review and local regression suite, not an independent penetration test or a claim that the whole service is production-ready.

## SQL injection
All request-derived values are passed as SQLite bound parameters via prepared statements. Schema, migration, trigger, transaction and ordering SQL is constant. There are no request-provided SQL identifiers, SQL fragments or database/file paths. Rejected identifiers do not generate raw database errors in HTTP responses.

Regression tests send quote/UNION/DROP/OR payloads through login identifiers, passwords, profile names/bios, post bodies, interaction kinds/targets, media identifiers, spoofed user IDs and channel values. Assertions verify authentication is not bypassed, stored content remains literal, other accounts remain unchanged and SQLite integrity/tables survive. No injectable query path was found within this reviewed scope.

## Usernames
Handles are normalized with Unicode NFKC and lowercase, then restricted to 3–24 ASCII letters/numbers/underscores. A case-insensitive UNIQUE index makes concurrent requests race-safe; database triggers reject invalid/null handles. Profile updates check conflicts and write the username and profile/media changes transactionally. A concurrent-registration test proves only one case-variant request succeeds. Display names are intentionally separate from unique @handles.

Legacy profiles retain their display names. Stable handles are assigned once; colliding names receive numeric suffixes. No accounts or credentials are merged using an unverified email address.

## Authentication and request boundaries
Passwords use scrypt and unique salts. Raw session IDs exist only in HttpOnly cookies; SQLite stores SHA-256 session lookups. APIs derive the actor from the session, never a request user_id. Mutation origins and hosts are checked, auth requests are throttled, and body sizes are bounded. Production requires HTTPS PUBLIC_ORIGIN and uses Secure cookies. Public state omits emails, password hashes and sessions.

## Media and text
Browser-selected raster images are cropped/resized on canvas. Server validation does not trust the client: PNG dimensions/CRC/filter bytes/decoded length are checked under strict bounds, and only essential re-encoded PNG chunks are stored. SVG/HTML/animated and malformed payloads are rejected. User profile image references cannot be supplied to steal or delete another account's media. Public media uses UUID lookup, image/png and nosniff, with no arbitrary filesystem paths exposed.

Profile text, captions, names and code blocks are escaped. Image sources are restricted to generated local media paths or local PNG previews. Profile/social URLs require HTTP(S) without embedded credentials. User code is displayed, not executed.

## Actual remaining work
Email verification and account recovery, deletion/export flows, reporting/moderation, production proxy/HTTPS operations, backup policy and broader load/abuse testing remain outstanding. The current complete-state API is intentionally bounded to the latest 200 posts and is not a Twitter-scale infrastructure claim.
