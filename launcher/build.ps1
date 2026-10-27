<#
.SYNOPSIS
  Builds the launcher (debug APK): launcher\app\build\outputs\apk\debug\app-debug.apk

.NOTES
  Needs: JDK 11 (JAVA_HOME), Android SDK with platform 33 + build-tools (ANDROID_HOME) and Gradle 7.5.x
  (GRADLE_BIN, default "gradle" on PATH). Android Gradle Plugin 7.4.2 runs on JDK 11; newer JDKs need a newer plugin.
#>
if (-not $env:ANDROID_HOME) { throw "Set ANDROID_HOME to your Android SDK folder." }
$gradle = if ($env:GRADLE_BIN) { $env:GRADLE_BIN } else { "gradle" }

$sdk = $env:ANDROID_HOME -replace '\\', '\\'
Set-Content -Path (Join-Path $PSScriptRoot "local.properties") -Value "sdk.dir=$sdk" -Encoding ascii

Push-Location $PSScriptRoot
try {
    & $gradle assembleDebug --no-daemon --console=plain
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
