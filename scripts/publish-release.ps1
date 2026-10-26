param([string]$Repository = 'Lyvt-Dev/hera-client', [string]$Revision = 'HEAD')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
if ($Repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') { throw 'Repository als Benutzer/Repository angeben.' }
$config = Get-Content 'apps/launcher/src-tauri/tauri.conf.json' -Raw | ConvertFrom-Json
$manifest = Get-Content 'releases/latest.json' -Raw | ConvertFrom-Json
if ($manifest.version -ne $config.version) { throw 'Installer/Manifest zuerst für die aktuelle Version bauen.' }
$tag = 'v' + $config.version
$setup = "Hera.Client_$($config.version)_x64-setup.exe"
$files = @($setup, "$setup.sig", 'SHA256SUMS.txt', 'latest.json')
foreach ($name in $files) { if (!(Test-Path -LiteralPath (Join-Path 'releases' $name))) { throw "Release-Datei fehlt: $name" } }
$hash = (Get-FileHash -LiteralPath (Join-Path 'releases' $setup) -Algorithm SHA256).Hash
if (!(Get-Content releases/SHA256SUMS.txt -Raw).Contains($hash)) { throw 'Installer-Prüfsumme stimmt nicht.' }
if ($manifest.platforms.'windows-x86_64'.signature -ne (Get-Content -LiteralPath (Join-Path 'releases' "$setup.sig") -Raw).Trim()) { throw 'Update-Signatur stimmt nicht mit dem Manifest überein.' }
$expectedUrl = "https://github.com/$Repository/releases/download/$tag/$([Uri]::EscapeDataString($setup))"
if ($manifest.platforms.'windows-x86_64'.url -ne $expectedUrl) { throw 'Manifest verweist auf ein anderes Repository.' }
$env:GIT_TERMINAL_PROMPT = '0'
# The credential stays in process memory and is never printed or written to disk.
$credentialLines = "protocol=https`nhost=github.com`n`n" | git credential fill
if ($LASTEXITCODE) { throw 'Bitte zuerst GitHub über Git Credential Manager anmelden.' }
$tokenLine = $credentialLines | Where-Object { $_.StartsWith('password=') } | Select-Object -First 1
if (!$tokenLine) { throw 'GitHub-Anmeldung fehlt.' }
$headers = @{ Authorization = "Bearer $($tokenLine.Substring(9))"; Accept = 'application/vnd.github+json'; 'X-GitHub-Api-Version' = '2022-11-28' }
$api = "https://api.github.com/repos/$Repository"
$commit = (git rev-parse --verify "$Revision`^{commit}").Trim()
if ($LASTEXITCODE) { throw 'Commit fehlt.' }
$remote = git ls-remote origin refs/heads/main
if (!$remote -or !($remote -match $commit)) { throw 'Den aktuellen Commit zuerst nach origin/main pushen.' }
$release = $null
try { $release = Invoke-RestMethod -Uri "$api/releases/tags/$tag" -Headers $headers } catch { if ([int]$_.Exception.Response.StatusCode -ne 404) { throw } }
if ($release -and !$release.draft) { throw 'Dieser Release ist bereits veröffentlicht. Eine neue Version verwenden.' }
if (!$release) {
    $body = @{ tag_name = $tag; target_commitish = $commit; name = "Hera $($config.version)"; body = $manifest.notes; draft = $true; prerelease = $false } | ConvertTo-Json
    $release = Invoke-RestMethod -Method Post -Uri "$api/releases" -Headers $headers -ContentType 'application/json' -Body $body
}
foreach ($name in $files) {
    if ($release.assets | Where-Object name -EQ $name) { throw "Datei existiert im Entwurf bereits: $name. Entwurf prüfen, bevor erneut veröffentlicht wird." }
    $uri = "https://uploads.github.com/repos/$Repository/releases/$($release.id)/assets?name=$([Uri]::EscapeDataString($name))"
    Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType 'application/octet-stream' -InFile (Join-Path 'releases' $name) | Out-Null
}
$published = Invoke-RestMethod -Method Patch -Uri "$api/releases/$($release.id)" -Headers $headers -ContentType 'application/json' -Body '{"draft":false}'
Write-Output "Veröffentlicht: $($published.html_url)"
Remove-Variable credentialLines, tokenLine, headers -ErrorAction SilentlyContinue
