# Privacy boundary

Big Orbit may display account status, global question text, K-anonymous rating aggregates,
explicitly consented sanitized product reviews, model/prompt provenance, operation status,
backup hashes, and enrolled-device metadata.

It does not receive relationship IDs with content, couple membership details, quiz answers,
notes, custom questions, Smooch content, coordinates, attachments, exports, or raw attributable
quiz feedback. It has no tracking or hosted notification identifier.

The device credential and short access token are encrypted with an AES-GCM key held by Android
Keystore. The P-256 private key is non-exportable. Android cloud backup and device transfer are
disabled. Local sign-out removes only the short access token so the already enrolled device can
prove itself again; it retains the encrypted 90-day device credential. Server revocation disables
the credential and every bound session, then Big Orbit deletes its local credential and P-256 key.
