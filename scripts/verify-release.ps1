param(
    [Parameter(Mandatory = $true)]
    [string]$Apk,
    [string]$ExpectedCertificateSha256 = $env:BIG_ORBIT_SIGNING_CERT_SHA256,
    [string]$OutputDirectory = "verification-output"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
if ([string]::IsNullOrWhiteSpace($ExpectedCertificateSha256)) {
    throw "Set BIG_ORBIT_SIGNING_CERT_SHA256 to the pinned release certificate digest."
}
if (-not $apkPath.StartsWith($repoRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "The release APK must be built from this Big Orbit checkout."
}
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) {
    $localProperties = Join-Path $repoRoot "local.properties"
    $sdkLine = if (Test-Path -LiteralPath $localProperties) {
        Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match '^sdk\.dir=' } |
            Select-Object -First 1
    }
    if ($sdkLine) {
        $sdkRoot = ($sdkLine.Split('=', 2)[1]).Replace('\:', ':').Replace('\\', '\')
    }
}
if (-not $sdkRoot -and $env:LOCALAPPDATA) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not $sdkRoot -or -not (Test-Path -LiteralPath $sdkRoot)) {
    throw "Set ANDROID_HOME or ANDROID_SDK_ROOT to an installed Android SDK."
}
$sdkRoot = (Resolve-Path -LiteralPath $sdkRoot).Path
$buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkRoot "build-tools") -Directory |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw "Android build-tools are unavailable." }
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
$apkanalyzer = Join-Path $sdkRoot "cmdline-tools\latest\bin\apkanalyzer.bat"
if (-not (Test-Path -LiteralPath $apkanalyzer)) {
    $apkanalyzer = (Get-Command apkanalyzer.bat -ErrorAction Stop).Source
}
$verification = & $apksigner verify --verbose --print-certs $apkPath 2>&1
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed." }
$signerCount = $verification | Where-Object {
    $_ -match '^Number of signers:'
} | Select-Object -First 1
if (-not $signerCount -or ($signerCount -split ':', 2)[1].Trim() -ne "1") {
    throw "The APK must have exactly one signer."
}
$digestLine = $verification | Where-Object {
    $_ -match '^(Signer #1|V[0-9.]+ Signer): certificate SHA-256 digest:'
} | Select-Object -First 1
if (-not $digestLine) { throw "The APK signer digest is unavailable." }
if ($digestLine -notmatch 'digest:\s*([0-9a-fA-F]{64})$') {
    throw "The APK signer digest is malformed."
}
$certificate = $Matches[1].ToLowerInvariant()
$expected = ($ExpectedCertificateSha256 -replace '[^0-9a-fA-F]', '').ToLowerInvariant()
if ($expected.Length -ne 64 -or $certificate -ne $expected) {
    throw "The APK signer does not match the pinned Big Orbit release certificate."
}
$packageName = (& $apkanalyzer manifest application-id $apkPath).Trim()
$versionCode = [int](& $apkanalyzer manifest version-code $apkPath).Trim()
$versionName = (& $apkanalyzer manifest version-name $apkPath).Trim()
if ($packageName -ne "com.littleorbit.bigorbit" -or $versionCode -ne 3 -or
        $versionName -ne "1.3.0") {
    throw "The release package or version metadata is not Big Orbit 1.3.0."
}
$outputPath = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory))
$prefix = $repoRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar
if (-not $outputPath.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Verification output must stay inside this checkout."
}
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$item = Get-Item -LiteralPath $apkPath
$metadata = [ordered]@{
    package = $packageName
    version_name = $versionName
    version_code = $versionCode
    bytes = $item.Length
    sha256 = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
    certificate_sha256 = $certificate
    verified_at = (Get-Date).ToUniversalTime().ToString("o")
}
$metadataPath = Join-Path $outputPath "big-orbit-1.3.0.json"
$metadata | ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding utf8
$metadata | ConvertTo-Json
