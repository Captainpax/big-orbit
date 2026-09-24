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

## 1.3.0 candidate gate

The current source intends version 1.3.0 code 3 and must connect only to a deployed compatible Little Orbit 1.3.0 API. Before signing, verify the shared release-train contract in both repositories. After signing, cold-launch the exact minified APK, complete both first-owner and existing-MFA device flows, inspect generic notifications and metadata-only observability, and verify malformed QR recovery. Record exact size, SHA-256, certificate SHA-256, commit, and device evidence in a new immutable release record. Until then, do not change the stable download or tag from 1.0.0 code 2.

## Published 1.0.0 identity

- Package: `com.littleorbit.bigorbit`; version `1.0.0`; version code `2`.
- Size: 2,475,090 bytes.
- SHA-256: `ef4c10509709504394078835d1b74db44788999fff65dfb0f68e8bee7821a824`.
- Certificate SHA-256: `04dc3502933faaa99dfd6641acc52b2bd71c9895087cb3060e3ce92dd8406f8c`.
- The artifact contains no smoke package, label, endpoint, signing metadata, or bundled QA APK.

The exact APK and verification JSON are published at
[`v1.0.0`](https://github.com/Captainpax/big-orbit/releases/tag/v1.0.0). The production Little
Orbit API exposes only device-bound `/v2/admin`; the legacy web and `/v1/admin` routes return 404.
The owner waived production two-device enrollment, cross-device revocation, alert, and recovery
observation as publication gates and will report that QA later. Those checks are not treated as
passed by this release record.

Version code 1 was signed but failed its exact release-mode launch check because R8 removed the
reflectively created WorkManager Room database constructor. It was never published and is not
reused. Code 2 keeps that constructor and makes a real minified-device launch part of the release
gate.

The signer remains protected outside Git on the release host. The owner waived a durable off-host
signer backup until the replacement server is available; that recovery gap remains open.
