# Contributing

Please open an issue before changing an authentication, notification, privacy, or release
boundary. Keep changes small, include retry/expiry/unauthorized tests, and state exactly what
was verified.

Run version and documentation checks, debug unit tests, smoke assembly, and smoke lint before proposing a change:

```powershell
python scripts\check_versions.py
python scripts\check_docs.py
.\gradlew.bat :app:testDebugUnitTest :app:assembleSmoke :app:lintSmoke
```

Do not
commit `local.properties`, keystores, credentials, API responses, screenshots containing
real operational data, bootstrap PINs/tokens, authenticator QR codes, recovery codes, or generated build directories.

Authentication changes require expiry, fifth-failure, replay, process-death, malformed-response,
existing-MFA, unauthorized-route, and device-revocation coverage. Operational UI changes must
remain metadata-only. Update every affected document in [`docs/DOCUMENTATION-MAP.md`](docs/DOCUMENTATION-MAP.md)
and review [`ROADMAP.md`](ROADMAP.md).

Security-sensitive reports should follow [`SECURITY.md`](SECURITY.md).
