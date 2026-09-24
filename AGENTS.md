# Big Orbit agent guide

## Role

Maintain a private Android owner console for Little Orbit with owl discipline: inspect the
trust boundary, trace the exact device/session identity, then change deliberately.

## Hard rules

1. Production code is Java 17 and XML Views; do not add Compose or Kotlin production code.
2. Keep the app free and free of ads, analytics, hosted push, and paid dependencies.
3. Never request or display relationship content, answers, notes, custom questions, precise
   locations, attachments, exports, passwords, MFA secrets, recovery codes after setup, or
   arbitrary database rows.
4. Every fresh device uses a hash-only terminal PIN plus password and `bootstrap`-domain P-256
   proof. Its restricted token never calls ordinary admin routes. Every admin data request then
   requires an active Little Orbit session bound to one approved key; challenges are one-use,
   short-lived, and domain-separated. First-owner MFA may be created, but existing MFA is preserved.
5. Store the device credential and access token only as Android Keystore-protected AES-GCM
   ciphertext. Never log request/response bodies, credentials, or signatures.
6. Production API traffic uses only the exact Little Orbit HTTPS authority. QA cleartext is
   limited to the fixed emulator smoke endpoint and `.smoke` package.
7. Operations are typed and allowlisted. Never add arbitrary command, SQL, path, URL, or
   model-tool inputs.
8. Notifications poll Little Orbit's authenticated endpoint with WorkManager. Never add FCM
   or another hosted broker; the public lock-screen version stays generic.
9. Release signing uses only `BIG_ORBIT_SIGNING_*`; never reuse Little Orbit's signer.
10. Keep modules below 500 logical lines, methods below 60 lines, inventory every Markdown file,
    update every affected document, and always review `ROADMAP.md`.

## Verification

```powershell
python scripts\check_versions.py
python scripts\check_docs.py
.\gradlew.bat :app:testDebugUnitTest :app:assembleSmoke :app:lintSmoke
.\gradlew.bat :app:lintRelease
```

Hardware claims require an actual enrolled device and a disposable Little Orbit smoke stack.
