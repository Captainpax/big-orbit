param(
    [string]$SigningEnvironment = $env:BIG_ORBIT_SIGNING_ENV_FILE,
    [string]$DeviceSerial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not $SigningEnvironment) {
    throw "Set BIG_ORBIT_SIGNING_ENV_FILE to the protected signer environment file."
}
$environmentPath = (Resolve-Path -LiteralPath $SigningEnvironment).Path
$allowed = @(
    "BIG_ORBIT_SIGNING_STORE_FILE",
    "BIG_ORBIT_SIGNING_STORE_PASSWORD",
    "BIG_ORBIT_SIGNING_KEY_ALIAS",
    "BIG_ORBIT_SIGNING_KEY_PASSWORD",
    "BIG_ORBIT_SIGNING_CERT_SHA256"
)
$found = @{}
foreach ($line in Get-Content -LiteralPath $environmentPath) {
    if (-not $line -or $line.StartsWith("#")) { continue }
    $parts = $line.Split('=', 2)
    if ($parts.Count -ne 2 -or $parts[0] -notin $allowed) {
        throw "The signing environment contains an unexpected key."
    }
    $found[$parts[0]] = $parts[1]
}
foreach ($key in $allowed) {
    if (-not $found.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($found[$key])) {
        throw "The signing environment is incomplete."
    }
    [Environment]::SetEnvironmentVariable($key, $found[$key], "Process")
}
$store = (Resolve-Path -LiteralPath $found["BIG_ORBIT_SIGNING_STORE_FILE"]).Path
[Environment]::SetEnvironmentVariable("BIG_ORBIT_SIGNING_STORE_FILE", $store, "Process")
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Big Orbit release build failed." }
    & (Join-Path $PSScriptRoot "verify-release.ps1") `
        -Apk "app/build/outputs/apk/release/app-release.apk" `
        -ExpectedCertificateSha256 $found["BIG_ORBIT_SIGNING_CERT_SHA256"]
    if ($LASTEXITCODE -ne 0) { throw "Big Orbit release verification failed." }
    if ($DeviceSerial) {
        & (Join-Path $PSScriptRoot "verify-release-launch.ps1") `
            -Apk "app/build/outputs/apk/release/app-release.apk" `
            -Serial $DeviceSerial
    }
}
finally {
    Pop-Location
}
