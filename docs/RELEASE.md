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
5. Inspect the manifest for package `com.littleorbit.bigorbit`, version code 1, version 1.0.0,
   HTTPS-only traffic, disabled backup, and no QA label or endpoint.
6. Retain the generated `verification-output/big-orbit-1.0.0.json`, then publish the exact APK
   bytes it describes. The verifier pins the independent release certificate and rejects the
   wrong package or version. A failed candidate's version code and bytes are not reused.

No release or hardware validation is implied by the presence of this guide.

## Verified 1.0.0 candidate

- Package: `com.littleorbit.bigorbit`; version `1.0.0`; version code `1`.
- Size: 2,390,730 bytes.
- SHA-256: `8a5bf42ae8c2f9786f880f648ff5c58fbfdf9ba62d87e612c3ff3c19b705f2b6`.
- Certificate SHA-256: `04dc3502933faaa99dfd6641acc52b2bd71c9895087cb3060e3ce92dd8406f8c`.
- The artifact contains no smoke package, label, endpoint, signing metadata, or bundled QA APK.

This identity is a verified candidate, not publication evidence. Publish these exact bytes only
after the production API exposes device-bound `/v2/admin`, both recovery devices enroll, and
cross-device revocation succeeds.
