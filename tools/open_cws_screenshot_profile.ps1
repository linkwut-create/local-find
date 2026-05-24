$ErrorActionPreference = "Stop"

$ProfileDir = "D:\local-find-cws-chrome-profile"
$DraftDir = "D:\local-find-screenshots-draft"
$ExtensionDir = "D:\local-find\chrome-extension"

if (-not (Test-Path -LiteralPath (Join-Path $ExtensionDir "manifest.json"))) {
    throw "Local Find Chrome extension manifest was not found at: $(Join-Path $ExtensionDir "manifest.json")"
}

New-Item -ItemType Directory -Force -Path $ProfileDir | Out-Null
New-Item -ItemType Directory -Force -Path $DraftDir | Out-Null

$ChromeCandidates = @(
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

$ChromeArgs = @(
    "--user-data-dir=$ProfileDir",
    "--load-extension=$ExtensionDir",
    "--disable-extensions-except=$ExtensionDir",
    "--new-window",
    "chrome://extensions"
)

Write-Host "Opening Chrome for Local Find Chrome Web Store screenshot capture..."
Write-Host "Chrome: $ChromeExe"
Write-Host "Profile: $ProfileDir"
Write-Host "Draft screenshots: $DraftDir"
Write-Host "Extension: $ExtensionDir"

Start-Process -FilePath $ChromeExe -ArgumentList $ChromeArgs

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Enable Developer mode if needed."
Write-Host "2. Confirm Local Find is loaded."
Write-Host "3. Pin extension."
Write-Host "4. Open popup."
Write-Host "5. Set English UI."
Write-Host "6. Capture screenshots manually."
Write-Host "7. Save drafts to D:\local-find-screenshots-draft\"
