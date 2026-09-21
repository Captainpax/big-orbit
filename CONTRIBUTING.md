# Contributing

Please open an issue before changing an authentication, notification, privacy, or release
boundary. Keep changes small, include retry/expiry/unauthorized tests, and state exactly what
was verified.

Run the debug unit tests, smoke assembly, and smoke lint before proposing a change. Do not
commit `local.properties`, keystores, credentials, API responses, screenshots containing
real operational data, or generated build directories.

Security-sensitive reports should follow [`SECURITY.md`](SECURITY.md).
