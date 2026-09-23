param(
    [string]$PhoneUrl = "",
    [int]$KeepMs = 1800000
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ProfileDir = Join-Path $ProjectRoot "local-find-cws-chrome-profile\CanaryDebug"
$DraftDir = Join-Path $ProjectRoot "local-find-screenshots-draft"
$ExtensionDir = Join-Path $ProjectRoot "chrome-extension"
$LoaderPath = Join-Path $ProjectRoot "tools\load_extension_cdp_pipe.cjs"
$DiscoveryBridgePath = Join-Path $ProjectRoot "tools\local_find_discovery_bridge.cjs"

function Get-ShortPath([string]$Path) {
    $quotedPath = '"' + $Path.Replace('"', '""') + '"'
    $shortPath = cmd.exe /d /c ("for %I in (" + $quotedPath + ") do @echo %~sI")
    if (-not $shortPath) {
        throw "Could not resolve an ASCII short path for: $Path"
    }
    return $shortPath.Trim()
}

if (-not (Test-Path -LiteralPath (Join-Path $ExtensionDir "manifest.json"))) {
    throw "Local Find Chrome extension manifest was not found at: $(Join-Path $ExtensionDir "manifest.json")"
}
if (-not (Test-Path -LiteralPath $LoaderPath)) {
    throw "The Canary CDP loader was not found at: $LoaderPath"
}
if (-not (Test-Path -LiteralPath $DiscoveryBridgePath)) {
    throw "The local discovery bridge was not found at: $DiscoveryBridgePath"
}

New-Item -ItemType Directory -Force -Path $ProfileDir | Out-Null
New-Item -ItemType Directory -Force -Path $DraftDir | Out-Null

$ChromeCandidates = @(
    (Join-Path $env:LocalAppData "Google\Chrome SxS\Application\chrome.exe"),
    (Join-Path $env:ProgramFiles "Google\Chrome\Application\chrome.exe"),
    (Join-Path ${env:ProgramFiles(x86)} "Google\Chrome\Application\chrome.exe"),
    (Join-Path $env:LocalAppData "Google\Chrome\Application\chrome.exe")
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$ChromeExe = $ChromeCandidates | Select-Object -First 1

if (-not $ChromeExe) {
    $ChromeCommand = Get-Command "chrome.exe" -ErrorAction SilentlyContinue
    if ($ChromeCommand) {
        $ChromeExe = $ChromeCommand.Source
    }
}

if (-not $ChromeExe) {
    throw @"
Could not find chrome.exe.

Install Google Chrome or update this helper with the local chrome.exe path.
Checked:
- $env:ProgramFiles\Google\Chrome\Application\chrome.exe
- ${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe
- $env:LocalAppData\Google\Chrome\Application\chrome.exe
- chrome.exe from PATH
"@
}

$ExtensionShortPath = Get-ShortPath $ExtensionDir
$ProfileShortPath = Get-ShortPath $ProfileDir
$ChromeShortPath = Get-ShortPath $ChromeExe

Write-Host "Opening Chrome Canary with the supported CDP unpacked-extension loader..."
Write-Host "Chrome: $ChromeExe"
Write-Host "Profile: $ProfileDir (ASCII: $ProfileShortPath)"
Write-Host "Draft screenshots: $DraftDir"
Write-Host "Extension: $ExtensionDir (ASCII: $ExtensionShortPath)"
Write-Host "Loader: $LoaderPath"

$node = Get-Command "node.exe" -ErrorAction SilentlyContinue
if (-not $node) {
    throw "Node.js is required to load the unpacked extension through Chrome Canary CDP."
}

try {
    $bridgeHealthy = (Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:43789/health" -TimeoutSec 1).StatusCode -eq 200
} catch {
    $bridgeHealthy = $false
}
if (-not $bridgeHealthy) {
    Write-Host "Starting the Local Find discovery bridge..."
    Start-Process -FilePath $node.Source -ArgumentList @($DiscoveryBridgePath) -WorkingDirectory $ProjectRoot -WindowStyle Hidden
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        Start-Sleep -Milliseconds 300
        try {
            $bridgeHealthy = (Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:43789/health" -TimeoutSec 1).StatusCode -eq 200
        } catch {
            $bridgeHealthy = $false
        }
        if ($bridgeHealthy) { break }
    }
}
if (-not $bridgeHealthy) {
    throw "The Local Find discovery bridge did not start on http://127.0.0.1:43789."
}
Write-Host "Discovery bridge: http://127.0.0.1:43789"

$LoaderArgs = @(
    $LoaderPath,
    "--chrome", $ChromeShortPath,
    "--user-data-dir", $ProfileShortPath,
    "--extension", $ExtensionShortPath,
    "--result", (Join-Path $ProjectRoot "tools\cdp-extension-result.json"),
    "--open-url", "chrome://extensions",
    "--keep-ms", $KeepMs
)
if ($PhoneUrl) {
    $LoaderArgs += @("--phone-url", $PhoneUrl)
}

& $node.Source $LoaderArgs
if ($LASTEXITCODE -ne 0) {
    throw "Chrome Canary CDP loader failed with exit code $LASTEXITCODE."
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Confirm Local Find is loaded in chrome://extensions."
Write-Host "2. Pin the extension and open its popup."
Write-Host "3. If pairing, enable pairing mode on the phone before clicking Request Pairing."
Write-Host "4. Set English UI and capture screenshots manually."
Write-Host "5. Save drafts to $DraftDir"
