param([switch]$LiveDownloads)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (Test-Path -LiteralPath '.local\cargo\bin\cargo.exe') {
    $env:CARGO_HOME = Join-Path $projectRoot '.local\cargo'
    $env:RUSTUP_HOME = Join-Path $projectRoot '.local\rustup'
    $env:PATH = "$env:CARGO_HOME\bin;$env:PATH"
}
$taskJdk = Get-ChildItem -LiteralPath '.local\jdk' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($taskJdk) { $env:JAVA_HOME = $taskJdk.FullName; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
foreach ($taskCheck in @('check', 'test', 'build')) {
    npm run $taskCheck
    if ($LASTEXITCODE -ne 0) { throw "npm $taskCheck failed" }
}
cargo test --locked --manifest-path apps/launcher/src-tauri/Cargo.toml
if ($LASTEXITCODE -ne 0) { throw 'Native tests failed' }
if ($LiveDownloads) {
    cargo test --locked --manifest-path apps/launcher/src-tauri/Cargo.toml official_install_smoke -- --ignored
    if ($LASTEXITCODE -ne 0) { throw 'Live installation test failed' }
}
$taskGradle = Join-Path $projectRoot '.local\gradle\gradle-8.14.3\bin\gradle.bat'
if (Test-Path -LiteralPath $taskGradle) {
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.local\gradle-cache'
    & $taskGradle -p minecraft build --no-daemon --max-workers=2
} else { gradle -p minecraft build --no-daemon --max-workers=2 }
if ($LASTEXITCODE -ne 0) { throw 'Fabric build failed' }
