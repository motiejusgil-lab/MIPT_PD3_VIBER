param([switch]$UnitOnly)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & .\gradlew.bat verifyUnitTestCoverage :core:check :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Unit tests, coverage verification or APK build failed.' }
    if (-not $UnitOnly) {
        & .\gradlew.bat :app:connectedDebugAndroidTest --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Device UI tests failed. Check the connected emulator/device.' }
    }
    $taskArguments = @('-X', 'utf8', 'scripts/verify_results.py', '--output', 'build/verification-summary.json')
    if ($UnitOnly) { $taskArguments += '--unit-only' }
    & python @taskArguments
    if ($LASTEXITCODE -ne 0) { throw 'Saved test evidence verification failed.' }
} finally {
    Pop-Location
}
