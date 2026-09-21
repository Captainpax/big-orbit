param(
    [string]$OutputDirectory = (
        Join-Path $env:LOCALAPPDATA "LittleOrbitSecrets\big-orbit"
    )
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
[IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$secretRoot = (Resolve-Path -LiteralPath $OutputDirectory).Path
$repoPrefix = $repoRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar
if ($secretRoot.StartsWith($repoPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "The Big Orbit signer must be stored outside the repository."
}

$keystore = Join-Path $secretRoot "big-orbit-release.p12"
$environment = Join-Path $secretRoot "big-orbit-signing.env"
$certificate = Join-Path $secretRoot "big-orbit-release-cert.der"
if ((Test-Path -LiteralPath $keystore) -or (Test-Path -LiteralPath $environment)) {
    throw "Refusing to overwrite an existing Big Orbit signer."
}

$bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(36)
$password = [Convert]::ToBase64String($bytes).
    Replace('+', '-').Replace('/', '_').TrimEnd('=')
$alias = "big-orbit-release"
[Environment]::SetEnvironmentVariable(
    "BIG_ORBIT_KEYTOOL_PASSWORD", $password, "Process"
)
try {
    & keytool.exe -genkeypair -noprompt -storetype PKCS12 `
        -keystore $keystore '-storepass:env' BIG_ORBIT_KEYTOOL_PASSWORD `
        '-keypass:env' BIG_ORBIT_KEYTOOL_PASSWORD -alias $alias -keyalg RSA `
        -keysize 4096 -sigalg SHA256withRSA -validity 10000 `
        -dname "CN=Big Orbit Release, OU=Little Orbit, O=Little Orbit, C=US"
    if ($LASTEXITCODE -ne 0) { throw "Big Orbit signer generation failed." }
    & keytool.exe -exportcert -keystore $keystore -storetype PKCS12 `
        '-storepass:env' BIG_ORBIT_KEYTOOL_PASSWORD -alias $alias -file $certificate
    if ($LASTEXITCODE -ne 0) { throw "Big Orbit certificate export failed." }
    $digest = (Get-FileHash -LiteralPath $certificate -Algorithm SHA256).
        Hash.ToLowerInvariant()
    $lines = @(
        "BIG_ORBIT_SIGNING_STORE_FILE=$keystore",
        "BIG_ORBIT_SIGNING_STORE_PASSWORD=$password",
        "BIG_ORBIT_SIGNING_KEY_ALIAS=$alias",
        "BIG_ORBIT_SIGNING_KEY_PASSWORD=$password",
        "BIG_ORBIT_SIGNING_CERT_SHA256=$digest"
    )
    [IO.File]::WriteAllLines(
        $environment, $lines, [Text.UTF8Encoding]::new($false)
    )
    foreach ($path in @($keystore, $environment)) {
        $acl = Get-Acl -LiteralPath $path
        $acl.SetAccessRuleProtection($true, $false)
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $rule = [Security.AccessControl.FileSystemAccessRule]::new(
            $identity, "FullControl", "Allow"
        )
        $acl.SetAccessRule($rule)
        Set-Acl -LiteralPath $path -AclObject $acl
    }
    Write-Output "Big Orbit signer created outside Git."
    Write-Output "Signing environment: $environment"
    Write-Output "Certificate SHA-256: $digest"
}
finally {
    [Environment]::SetEnvironmentVariable(
        "BIG_ORBIT_KEYTOOL_PASSWORD", $null, "Process"
    )
    if (Test-Path -LiteralPath $certificate) {
        Remove-Item -LiteralPath $certificate -Force
    }
}
