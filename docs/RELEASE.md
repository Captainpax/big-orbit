# Release process

1. Run debug unit tests, smoke assembly, smoke lint, release lint, and instrumentation tests.
2. Enroll `com.littleorbit.bigorbit.smoke` against the disposable Little Orbit smoke stack.
3. Exercise first enrollment, returning password/MFA login, background credential rotation,
   notification permission denial, alert polling, job retry, and device revocation.
4. Build release only with the independent `BIG_ORBIT_SIGNING_*` values.
5. Inspect the manifest for package `com.littleorbit.bigorbit`, version code 1, version 1.0.0,
   HTTPS-only traffic, disabled backup, and no QA label or endpoint.
6. Verify the APK signature, record its SHA-256 and byte count, and publish those exact immutable
   bytes. A failed candidate's version code and bytes are not reused.

No release or hardware validation is implied by the presence of this guide.
