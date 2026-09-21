# Big Orbit

Big Orbit is the free, open-source Android owner console for
[Little Orbit](https://github.com/Captainpax/littleorbit). It moves administration out of
the public website and onto an explicitly enrolled device with a non-exportable Android
Keystore key.

The verified 1.0.0 release candidate uses package `com.littleorbit.bigorbit`, version code 2,
and an independent pinned signer. It is not published until the two-device production enrollment
and revocation checks finish.

The app intentionally handles operational metadata only. It cannot browse relationship
notes, quiz answers, custom questions, precise locations, attachments, Smooch content, or
account exports.

## What is implemented

- Password plus TOTP/recovery authentication and in-app first-owner MFA setup.
- P-256 Android Keystore enrollment and signed, one-use server challenges.
- A separately encrypted 90-day device credential and rotating 30-minute API sessions.
- Action inbox with question-report decisions, thresholded quiz ratings, AI run/policy
  observability, encrypted-backup evidence, typed operation requests, service health,
  registration control, bounded account/session actions, security events, and explicit revocation
  for either the current device or any other enrolled/pending key.
- First-party WorkManager polling with a generic lock-screen notification. No Firebase,
  hosted push broker, advertising SDK, or analytics SDK is present.
- A side-by-side `smoke` build named **Big Orbit QA** with package
  `com.littleorbit.bigorbit.smoke` and isolated Little Orbit smoke-stack URL.

## Build

Requirements: JDK 17 and Android SDK 37.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleSmoke :app:lintSmoke
adb -s <serial> reverse tcp:18180 tcp:18180
```

The smoke package accepts only the fixed `http://127.0.0.1:18180/api` endpoint. The ADB
reverse tunnel keeps emulator and physical-device QA on the isolated local stack without
weakening the server's trusted-host policy.

Release builds deliberately require a signer that is independent from Little Orbit:

```text
BIG_ORBIT_SIGNING_STORE_FILE
BIG_ORBIT_SIGNING_STORE_PASSWORD
BIG_ORBIT_SIGNING_KEY_ALIAS
BIG_ORBIT_SIGNING_KEY_PASSWORD
```

Signer values and keystores must remain outside Git. An unsigned or debug-signed build is
never a production candidate.

Create a new local signer once (this refuses to overwrite an existing signer):

```powershell
.\scripts\create-local-signer.ps1
```

The release certificate is pinned independently through
`BIG_ORBIT_SIGNING_CERT_SHA256`. Build and verify one immutable candidate with:

```powershell
$env:BIG_ORBIT_SIGNING_ENV_FILE = "C:\protected\big-orbit-signing.env"
.\scripts\build-release.ps1
```

The verifier rejects the wrong package, version, or certificate and writes only public
hash/size/certificate metadata under the ignored `verification-output` directory.

## Visual baseline

The four sheets in [`docs/concepts`](docs/concepts) are high-resolution concept imagery,
not production screenshots. The current Java/XML interface follows their navy, lavender,
coral, amber, orbital-line, and soft-glow system while keeping large touch targets and
dynamic Android text.

![Big Orbit QA login on the API 36 tablet emulator](docs/screenshots/big-orbit-smoke-tablet.png)

This implementation capture is from the side-by-side smoke package on the 2560×1600 API 36
tablet emulator. It contains only simulated device information and no owner credentials.

## Server compatibility

Big Orbit 1.0.0 targets Little Orbit 1.2.0's `/v2/admin` API. There is no arbitrary command,
SQL, URL, or filesystem input in its operations contract. The public Little Orbit website
does not host an administrator panel.

Licensed under the [MIT License](LICENSE).
