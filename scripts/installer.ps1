param([string]$Repository = 'Lyvt-Dev/hera-client', [string]$Notes = 'Hera Client update')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if ($Repository -and $Repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') { throw 'Repository als Benutzer/Repository angeben.' }
if (Test-Path '.local/cargo/bin/cargo.exe') {
    $env:RUSTUP_HOME = Join-Path $projectRoot '.local/rustup'
    $env:CARGO_HOME = Join-Path $projectRoot '.local/cargo'
    $env:PATH = (Join-Path $env:CARGO_HOME 'bin') + ';' + $env:PATH
}
if (!$env:TAURI_SIGNING_PRIVATE_KEY) {
    $key = Join-Path $projectRoot '.local/signing/hera.key'
    if (!(Test-Path -LiteralPath $key)) { throw 'TAURI_SIGNING_PRIVATE_KEY setzen. Den ursprünglichen Signaturschlüssel verwenden.' }
    $env:TAURI_SIGNING_PRIVATE_KEY = $key
    $env:TAURI_SIGNING_PRIVATE_KEY_PASSWORD = ''
}
if ($Repository) { $env:HERA_UPDATE_URL = "https://github.com/$Repository/releases/latest/download/latest.json" }
else { $env:HERA_UPDATE_URL = ''; Write-Warning 'Ohne Repository wird ein Installer ohne Online-Update-Adresse gebaut.' }
npm run build -w '@hera/shared-types'
if ($LASTEXITCODE) { throw 'Shared build failed' }
npm run bundle -w '@hera/launcher' -- --bundles nsis
if ($LASTEXITCODE) { throw 'Installer build failed' }
$config = Get-Content 'apps/launcher/src-tauri/tauri.conf.json' -Raw | ConvertFrom-Json
$installer = Get-ChildItem 'apps/launcher/src-tauri/target/release/bundle/nsis' -Filter "*_$($config.version)_x64-setup.exe" | Select-Object -First 1
if (!$installer) { throw 'Installer nicht gefunden' }
New-Item -ItemType Directory -Force 'releases' | Out-Null
$assetName = $installer.Name.Replace(' ', '.')
Copy-Item -LiteralPath $installer.FullName -Destination (Join-Path 'releases' $assetName)
Copy-Item -LiteralPath ($installer.FullName + '.sig') -Destination (Join-Path 'releases' ($assetName + '.sig'))
Get-FileHash -Algorithm SHA256 -LiteralPath $installer.FullName | ForEach-Object { "$($_.Hash)  $assetName" } | Set-Content 'releases/SHA256SUMS.txt'
if ($Repository) {
    $tag = 'v' + $config.version
    $manifest = @{
        version = $config.version
        notes = $Notes
        pub_date = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
        platforms = @{'windows-x86_64' = @{
            signature = (Get-Content -LiteralPath ($installer.FullName + '.sig') -Raw).Trim()
            url = "https://github.com/$Repository/releases/download/$tag/$assetName"
        }}
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content 'releases/latest.json' -Encoding utf8NoBOM
}
Write-Host "Installer und Signatur liegen in $projectRoot/releases. Es wurde nichts veröffentlicht."
