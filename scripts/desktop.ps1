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
npm run desktop
exit $LASTEXITCODE
