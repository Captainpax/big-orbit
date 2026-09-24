# Privacy boundary

Big Orbit may display account status, global question text, K-anonymous rating aggregates,
explicitly consented sanitized product reviews, model/prompt provenance, public weekly and daily
theme labels, reviewed-knowledge counts, public-context freshness, reserve capacity, operation
status, backup hashes, and enrolled-device metadata.

It does not receive relationship IDs with content, couple membership details, quiz answers,
notes, custom questions, Smooch content, coordinates, attachments, exports, or raw attributable
quiz feedback. It has no tracking or hosted notification identifier.

The device credential, short access token, and unfinished bootstrap token are encrypted separately
with an AES-GCM key held by Android Keystore. The P-256 private key is non-exportable. The app
never persists the password, terminal PIN, authenticator secret, or recovery codes. A first-owner
authenticator QR is protected from screenshots; recovery codes are shown once and only the owner
can choose where to keep them. Malformed QR bytes fail closed to a recoverable setup state.

The server stores only a domain-separated hash of the eight-digit terminal PIN. It expires after
ten minutes, is consumed once, is invalidated after five failed attempts, and invalidates every
still-live setup capability when the console issues a new PIN. The restricted bootstrap token can
prove the pending device and confirm first-owner MFA only; after a lost completion response, it
can re-prove the same key and rotate replacement credentials during the same ten-minute window.
It cannot read owner metadata or queue operations. Pending devices are revoked on expiry.

Android cloud backup and device transfer are disabled. Local sign-out removes the short access
token and stops background polling until the owner signs in with password and MFA again; it
retains the encrypted 90-day device credential so the already enrolled key does not need
re-enrollment. Server revocation disables the selected credential and every bound session.
Revoking the current device also deletes its local credential and P-256 key; revoking another
device leaves its now-useless private key on that device.
