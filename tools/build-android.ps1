<#
.SYNOPSIS
  Runs Gradle for the Local Find Android module through a pure-ASCII `subst`
  drive, working around a Windows/Gradle path-handling bug that this
  repository's Chinese/full-width-bracket path name triggers.

.DESCRIPTION
  This repository's real path (…\local-find（找手机app）) contains non-ASCII
  characters. `testDebugUnitTest`'s worker process reads its classpath from a
  temp file Gradle writes; on this repo's path, that file's entries get
  parsed back into the original non-ASCII path, and the worker then fails
  with `ClassNotFoundException` on the test class it should run (2026-09-24
  finding — an earlier attempt using a directory *junction* at
  D:\AIProjects\cache\lf-ascii did NOT fix this: Windows/Java transparently
  resolve a junction back to its real target when canonicalizing the project
  path, so Gradle's worker classpath ends up with the same non-ASCII path
  either way).

  `subst` (a drive-letter-level DOS device mapping, distinct from an NTFS
  junction) does not get resolved back to the real path the same way, and
  running the full `testDebugUnitTest lintRelease assembleRelease
  bundleRelease` chain through a subst'd drive is confirmed to work
  end-to-end (BUILD SUCCESSFUL, real test pass counts in the JUnit XML
  reports, not just the assemble/bundle tasks that already worked either
  way).

  The fix is NOT to move or rename the repository and NOT to change Windows
  regional settings — both stay off the table. This script only creates a
  `subst` mapping (auto-created if missing, reused if already correct,
  never silently overwritten if it points somewhere else), and only ever
  points it at this repository.

  It also pins GRADLE_USER_HOME to the short path D:\AIProjects\cache\lf-gradle
  (keeps the ~4GB dependency cache off C: and out of the long/non-ASCII repo
  path) and redirects Gradle/Java temp dirs to D: so nothing large lands on
  C:\Users\...\AppData\Local\Temp.

  The subst mapping is session-scoped (gone on reboot, or remove it early
  with `subst <DriveLetter> /D`); this script does not remove it after
  running, so repeated calls in the same session/session-chain don't pay the
  mapping cost twice.

.PARAMETER Tasks
  Gradle tasks to run. Default: testDebugUnitTest lintRelease assembleRelease bundleRelease

.PARAMETER DriveLetter
  Drive letter (with colon) to subst this repo onto. Default: X:

.PARAMETER GradleUserHome
  Short-path Gradle user home. Default: D:\AIProjects\cache\lf-gradle

.EXAMPLE
  .\tools\build-android.ps1
  .\tools\build-android.ps1 -Tasks testDebugUnitTest
#>
param(
    [string[]]$Tasks = @('testDebugUnitTest', 'lintRelease', 'assembleRelease', 'bundleRelease'),
    [string]$DriveLetter = 'X:',
    [string]$GradleUserHome = 'D:\AIProjects\cache\lf-gradle',
    [string]$LogDir = 'D:\AIProjects\cache\lf-gradle\build-logs'
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repoMarker = Join-Path $repoRoot 'android\gradlew.bat'

# Deliberately do NOT parse `subst`'s console listing to check the existing
# mapping's target: this repo's path is non-ASCII and `subst`'s output comes
# back through the console's system codepage, which mojibakes it — string
# matching against it is exactly the class of bug this whole script exists
# to work around. Instead, if the drive letter is already in use, verify it
# by content: the same file (gradlew.bat) must exist and be byte-identical.
if (Test-Path "$DriveLetter\") {
    $mappedMarker = Join-Path $DriveLetter 'android\gradlew.bat'
    $same = (Test-Path $mappedMarker) -and
        ((Get-FileHash $mappedMarker -Algorithm SHA256).Hash -eq (Get-FileHash $repoMarker -Algorithm SHA256).Hash)
    if (-not $same) {
        throw "$DriveLetter is already in use and does not look like this repo (gradlew.bat missing or content differs) — not touching it. Pick a different -DriveLetter or remove that mapping yourself first (subst $DriveLetter /D)."
    }
    Write-Output "[build-android] $DriveLetter already mapped to this repo (verified by file content), reusing it."
} else {
    Write-Output "[build-android] mapping $DriveLetter -> $repoRoot"
    & subst $DriveLetter $repoRoot
    if ($LASTEXITCODE -ne 0) {
        throw "subst $DriveLetter `"$repoRoot`" failed with exit code $LASTEXITCODE"
    }
}

$androidDir = Join-Path $DriveLetter 'android'
if (-not (Test-Path (Join-Path $androidDir 'gradlew.bat'))) {
    throw "gradlew.bat not found under $androidDir — subst mapping does not look like this repo's android/ dir."
}

if (-not (Test-Path $GradleUserHome)) {
    New-Item -ItemType Directory -Path $GradleUserHome -Force | Out-Null
}
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}

# Keep every temp/cache path on D:, never C:.
$env:GRADLE_USER_HOME = $GradleUserHome
$env:TEMP = 'D:\AIProjects\cache\lf-gradle\.tmp'
$env:TMP = $env:TEMP
if (-not (Test-Path $env:TEMP)) {
    New-Item -ItemType Directory -Path $env:TEMP -Force | Out-Null
}

Push-Location $androidDir
try {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $logPath = Join-Path $LogDir "build-$stamp.log"
    Write-Output "[build-android] cwd=$(Get-Location) GRADLE_USER_HOME=$env:GRADLE_USER_HOME"
    Write-Output "[build-android] tasks: $($Tasks -join ' ')"
    Write-Output "[build-android] log: $logPath"

    # Do not merge stderr with *>&1 here: PowerShell 5.1 wraps each native
    # stderr line in a NativeCommandError, which aborts this pipeline even on
    # a successful (warnings-only) Gradle run. gradlew's own stderr is
    # already visible to the caller; only stdout is teed to the log file.
    $gradleArgs = @('--no-daemon') + $Tasks
    & .\gradlew.bat @gradleArgs | Tee-Object -FilePath $logPath
    $exitCode = $LASTEXITCODE

    Write-Output "[build-android] exit code: $exitCode"
    exit $exitCode
} finally {
    Pop-Location
}
