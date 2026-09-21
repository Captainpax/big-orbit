param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,
    [string]$Apk = "app/build/outputs/apk/release/app-release.apk"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$apkPath = (Resolve-Path -LiteralPath (Join-Path $repoRoot $Apk)).Path
$adb = if ($env:ANDROID_HOME) {
    Join-Path $env:ANDROID_HOME "platform-tools/adb.exe"
} elseif ($env:ANDROID_SDK_ROOT) {
    Join-Path $env:ANDROID_SDK_ROOT "platform-tools/adb.exe"
} else {
    Join-Path $env:LOCALAPPDATA "Android/Sdk/platform-tools/adb.exe"
}
if (-not (Test-Path -LiteralPath $adb)) { throw "Android adb was not found." }

& $adb -s $Serial install -r $apkPath | Out-Host
if ($LASTEXITCODE -ne 0) { throw "The release APK could not be installed." }
& $adb -s $Serial logcat -c
& $adb -s $Serial shell am force-stop com.littleorbit.bigorbit
$launch = & $adb -s $Serial shell am start -W `
    -n com.littleorbit.bigorbit/.LoginActivity
if ($LASTEXITCODE -ne 0) { throw "The release activity could not be started." }
Start-Sleep -Seconds 2
$pidValue = (& $adb -s $Serial shell pidof com.littleorbit.bigorbit).Trim()
$fatal = & $adb -s $Serial logcat -d -v brief AndroidRuntime:E '*:S'
if (-not $pidValue -or ($fatal -match "com\.littleorbit\.bigorbit")) {
    $launch | Out-Host
    throw "The exact minified Big Orbit release did not survive cold launch."
}
[pscustomobject]@{
    Serial = $Serial
    Package = "com.littleorbit.bigorbit"
    ProcessId = $pidValue
    ColdLaunchPassed = $true
}
