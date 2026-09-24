<#
.SYNOPSIS
  Runs Gradle for the Local Find Android module through an ASCII directory
  junction, working around a Windows/Gradle worker-classpath bug that this
  repository's Chinese/full-width-bracket path name triggers.

.DESCRIPTION
  This repository's real path (…\local-find（找手机app）) contains non-ASCII
  characters. Gradle's test-worker classpath handoff on Windows corrupts when
  any path segment in that handoff is non-ASCII, which makes
  `testDebugUnitTest` fail with `ClassNotFoundException: GradleWorkerMain`
  even though `assembleRelease`/`bundleRelease`/`lintRelease` are unaffected.

  The fix is NOT to move or rename the repository (forbidden — this script
  only creates a directory *junction*, never a copy) and NOT to change
  Windows regional settings (forbidden). Instead this script always invokes
  Gradle through the pure-ASCII junction at D:\AIProjects\cache\lf-ascii,
  which must already point at this repository:

    New-Item -ItemType Junction -Path D:\AIProjects\cache\lf-ascii `
      -Target "D:\AIProjects\projects\local-find（找手机app）"

  It also pins GRADLE_USER_HOME to the short path D:\AIProjects\cache\lf-gradle
  (keeps the ~4GB dependency cache off C: and out of the long/non-ASCII repo
  path) and redirects Gradle/Java temp dirs to D: so nothing large lands on
  C:\Users\...\AppData\Local\Temp.

.PARAMETER Tasks
  Gradle tasks to run. Default: testDebugUnitTest lintRelease assembleRelease bundleRelease

.PARAMETER JunctionPath
  ASCII junction to build through. Default: D:\AIProjects\cache\lf-ascii

.PARAMETER GradleUserHome
  Short-path Gradle user home. Default: D:\AIProjects\cache\lf-gradle

.EXAMPLE
  .\tools\build-android.ps1
  .\tools\build-android.ps1 -Tasks testDebugUnitTest
#>
param(
    [string[]]$Tasks = @('testDebugUnitTest', 'lintRelease', 'assembleRelease', 'bundleRelease'),
    [string]$JunctionPath = 'D:\AIProjects\cache\lf-ascii',
    [string]$GradleUserHome = 'D:\AIProjects\cache\lf-gradle',
    [string]$LogDir = 'D:\AIProjects\cache\lf-ascii\android\build-logs'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $JunctionPath)) {
    throw "ASCII junction not found at $JunctionPath. Create it first with:`n" +
          "  New-Item -ItemType Junction -Path `"$JunctionPath`" -Target `"<repo path>`""
}
$item = Get-Item $JunctionPath
if ($item.LinkType -ne 'Junction') {
    throw "$JunctionPath exists but is not a Junction (LinkType=$($item.LinkType)). Refusing to build through it."
}

$androidDir = Join-Path $JunctionPath 'android'
if (-not (Test-Path (Join-Path $androidDir 'gradlew.bat'))) {
    throw "gradlew.bat not found under $androidDir — junction target does not look like this repo's android/ dir."
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
