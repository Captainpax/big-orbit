# Big Orbit 1.3.0 release verification — 2026-09-23

This covers local implementation verification, the exact signed/emulator-tested artifact, Little Orbit production compatibility, and publication. It is not a physical-device claim.

Verified:

- The version checker agrees on Big Orbit 1.3.0 code 3 and the shared Little Orbit 1.3.0 release train.
- Debug Java compilation and unit tests pass, including the purpose-separated bootstrap challenge contract.
- The sibling Little Orbit disposable PostgreSQL suite passes all three bootstrap/reserve cases after the final refactor, including one-use PIN consumption, pending-device proof, first-owner TOTP enablement, ordinary session issuance, signed recovery after a lost completion response, and invalidation of old live setup capabilities by a new PIN. Its exact temporary database was then dropped.
- Enrollment QR decoding now treats malformed data as a recoverable unavailable state instead of allowing an activity crash.
- Debug unit tests, debug lint/assembly, and smoke lint/assembly pass together: 90 Gradle tasks completed successfully. The compiler reports the existing non-fatal deprecated-API note in `ConsoleActivity`.
- The version and complete Markdown/link inventory checks pass in the final repository pass recorded before handoff.
- The exact minified APK from source commit `e2d26b0` is package `com.littleorbit.bigorbit`, version 1.3.0 code 3, 2,485,190 bytes, SHA-256 `deccc454920c08793b3ce56b570b5c049343a48a7008984d7de0f91713ea8a5b`, and independent certificate SHA-256 `04dc3502933faaa99dfd6641acc52b2bd71c9895087cb3060e3ce92dd8406f8c`.
- Independent inspection confirms the APK is non-debuggable, targets API 36, disables backup and cleartext traffic, and contains none of the checked smoke package, QA label, or loopback endpoint markers.
- The exact APK fresh-installed beside Little Orbit smoke on an API 36 emulator, resolved `LoginActivity`, cold-launched with a live process, rendered the terminal-PIN/MFA screen, and produced no matching fatal exception or ANR.

Production compatibility and publication:

- Little Orbit production migrated through 0030 and 0031 to head 0031, reported API version 1.3.0, and exposed only the device-bound `/v2/admin` surface. The compatibility floor remains phone code 23 with no forced-update deadline.
- The exact immutable [Big Orbit 1.3.0 release](https://github.com/Captainpax/big-orbit/releases/tag/v1.3.0) was published at `2026-09-24T02:23:00Z`. Its GitHub asset byte count and digest match the inspected artifact above.
- Little Orbit published its matching immutable API record at `2026-09-24T02:33:46.511784Z` after a passing coordinated encrypted backup/restore drill. Public readiness, release metadata, complete and ranged APK downloads, patch notes, RSS, and rollout logs were verified from the Little Orbit release host.
- The first hosted Android job failed before Gradle because the setup action requested a removed legacy SDK tools package. The bounded workflow now pins Ubuntu 24.04 and verifies its preinstalled Android 37 platform and Build Tools 37.0.0 before running unit tests, lint, and debug assembly.

Deferred physical-device evidence:

- No physical fresh-device, returning-device, expiry, fifth-failure, process-death, large-text, notification, or revocation matrix has run for this release.
- No physical code-3 cold launch or fresh/returning enrollment flow has run; the owner requested expedited publication and will perform those observations as live QA.
