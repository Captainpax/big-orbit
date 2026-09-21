# Release process

1. Run debug unit tests, smoke assembly, smoke lint, and release lint.
2. Enroll `com.littleorbit.bigorbit.smoke` against the disposable Little Orbit smoke stack.
3. Exercise first enrollment, returning password/MFA login, background credential rotation,
   notification permission denial, alert polling, local sign-out races, report resolution,
   registration and account/session controls, job retry, current-device revocation, and revocation
   of a second enrolled or pending device from the inventory.
4. Create the independent signer once with `./scripts/create-local-signer.ps1`, protect and
   back it up outside Git, then build with its generated environment file:
   `./scripts/build-release.ps1 -SigningEnvironment C:\protected\big-orbit-signing.env`.
5. Inspect the manifest for package `com.littleorbit.bigorbit`, version code 2, version 1.0.0,
   HTTPS-only traffic, disabled backup, and no QA label or endpoint.
6. Commit the final release-preparation tree before the last build. Android embeds source-control
   provenance, so any later commit can produce different APK bytes even when app code is unchanged.
7. Install and cold-launch the exact minified APK on an Android device with
   `-DeviceSerial <adb-serial>`. Treat any startup crash as a failed candidate.
8. From that clean commit, retain the generated
   `verification-output/big-orbit-1.0.0.json`, create the release tag, and publish only the exact
   APK bytes it describes. The verifier pins the independent release certificate and rejects the
   wrong package or version. A failed candidate's version code and bytes are not reused.

No release or hardware validation is implied by the presence of this guide.

## 1.0.0 candidate contract

- Package: `com.littleorbit.bigorbit`; version `1.0.0`; version code `2`.
- Certificate SHA-256: `04dc3502933faaa99dfd6641acc52b2bd71c9895087cb3060e3ce92dd8406f8c`.
- The artifact contains no smoke package, label, endpoint, signing metadata, or bundled QA APK.

The exact size and APK SHA-256 come from the verifier run on the final tagged preparation commit;
they are recorded in its ignored verification JSON, the GitHub release assets, and the later
rollout-evidence commit. Publish those bytes only after the production API exposes device-bound
`/v2/admin`, both recovery devices enroll, and cross-device revocation succeeds.

Version code 1 was signed but failed its exact release-mode launch check because R8 removed the
reflectively created WorkManager Room database constructor. It was never published and is not
reused. Code 2 keeps that constructor and makes a real minified-device launch part of the release
gate.
