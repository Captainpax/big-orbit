# Release process

1. Run version/documentation checks, debug unit tests, smoke assembly, smoke lint, and release lint.
2. Enroll `com.littleorbit.bigorbit.smoke` against the disposable Little Orbit smoke stack.
3. Exercise fresh password/PIN bootstrap, wrong PIN through the fifth attempt, expiry, setup
   process death/resume, lost-completion-response credential recovery, first-owner QR/TOTP,
   existing-MFA preservation, returning password/MFA
   login, background credential rotation,
   notification permission denial, alert polling, local sign-out races, report resolution,
   registration and account/session controls, job retry, current-device revocation, and revocation
   of a second enrolled or pending device from the inventory.
4. Create the independent signer once with `./scripts/create-local-signer.ps1`, protect and
   back it up outside Git, then build with its generated environment file:
   `./scripts/build-release.ps1 -SigningEnvironment C:\protected\big-orbit-signing.env`.
5. Inspect the manifest for package `com.littleorbit.bigorbit` and the exact candidate version/code
   declared by `VERSION`, `release-train.json`, and Gradle. Require HTTPS-only traffic, disabled
   backup, and no QA label, endpoint, signer metadata, or bundled smoke artifact.
6. Commit the final release-preparation tree before the last build. Android embeds source-control
   provenance, so any later commit can produce different APK bytes even when app code is unchanged.
7. Install and cold-launch the exact minified APK on an Android device with
   `-DeviceSerial <adb-serial>`. Treat any startup crash as a failed candidate.
8. From that clean commit, retain the generated versioned verification JSON, create the release
   tag, and publish only the exact
   APK bytes it describes. The verifier pins the independent release certificate and rejects the
   wrong package or version. A failed candidate's version code and bytes are not reused.

No release or hardware validation is implied by the presence of this guide.

## Published 1.3.0 identity

Big Orbit 1.3.0 code 3 connects only to the deployed compatible Little Orbit 1.3.0 API. The shared release-train contract passed in both repositories, and the exact minified APK was independently inspected and cold-launched on API 36 before publication. The immutable record retains its exact size, SHA-256, certificate SHA-256, source commit, production compatibility, and explicitly deferred physical-device evidence.

- Package: `com.littleorbit.bigorbit`; version `1.3.0`; version code `3`.
- Size: 2,485,190 bytes.
- SHA-256: `deccc454920c08793b3ce56b570b5c049343a48a7008984d7de0f91713ea8a5b`.
- Certificate SHA-256: `04dc3502933faaa99dfd6641acc52b2bd71c9895087cb3060e3ce92dd8406f8c`.
- The artifact contains no smoke package, label, endpoint, signing metadata, or bundled QA APK.

The exact APK and verification JSON are published at
[`v1.3.0`](https://github.com/Captainpax/big-orbit/releases/tag/v1.3.0). The production Little
Orbit API exposes only device-bound `/v2/admin`; the legacy web and `/v1/admin` routes return 404.
The owner deferred physical fresh/returning enrollment, cross-device revocation, alert, recovery,
and large-text observation to live QA. Those checks are not treated as passed by this release record.

Version code 1 was signed but failed its exact release-mode launch check because R8 removed the
reflectively created WorkManager Room database constructor. It was never published and is not
reused. Code 2 keeps that constructor and makes a real minified-device launch part of the release
gate.

The signer remains protected outside Git on the release host. The owner waived a durable off-host
signer backup until the replacement server is available; that recovery gap remains open.
