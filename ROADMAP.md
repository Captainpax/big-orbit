# Big Orbit roadmap

The roadmap records implemented scope and remaining evidence. It does not turn an untested candidate into a release.

## 1.0.0 — Device-bound owner console

- [x] Remove privileged administration from the public Little Orbit web app.
- [x] Add independent signing, P-256 Android Keystore enrollment, device-bound sessions, typed operations, generic polling alerts, and metadata-only observability.
- [x] Publish code 2 after discarding the minified code-1 startup failure.
- [ ] Complete the owner-waived production two-device enrollment, cross-device revocation, alert, and recovery observations.

## 1.3.0 — Shared release train and terminal bootstrap

- [x] Align source version 1.3.0 and code 3 with Little Orbit's checked release-train contract.
- [x] Require every fresh device to use a one-use server-console PIN plus password and P-256 key proof; resume only within the restricted ten-minute setup capability, including fresh-key proof and credential rotation after a lost completion response.
- [x] Add first-owner TOTP QR enrollment, explicit recovery-code acknowledgement, existing-MFA preservation, protected setup storage, and fail-closed malformed-QR handling.
- [x] Expand the AI observatory with local schedule, reviewed-knowledge, public-context, reserve-capacity, weekly-arc, and run-state cards; keep all relationship content unavailable.
- [x] Make quiz regeneration a fixed allowlisted next-week operation and retain first-party WorkManager alerts.
- [x] Add a bounded Ubuntu CI workflow, grouped weekly dependency updates, version validation, and complete Markdown/link inventory.
- [ ] Exercise fresh, wrong-PIN, fifth-failure, expired, process-death, first-MFA, existing-MFA, offline, alert, revocation, large-text, and malformed-response paths on physical devices. The owner requested expedited publication and will perform these observations as live QA; they are not implied by emulator evidence.
- [x] Build, independently sign, inspect, and cold-launch the exact code-3 candidate on API 36 without a fatal exception or ANR.
- [ ] Publish code 3 only after Little Orbit 1.3.0 production migration and public compatibility verification. Never reuse a failed candidate code or bytes.
