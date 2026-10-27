<#
.SYNOPSIS
  Builds the launcher, installs it on the TV and opens it.

.EXAMPLE
  $env:TV_HOST = "192.168.1.50:5555"; .\install.ps1 [-NoBuild]

.NOTES
  Env: TV_HOST (required), TV_ADB (default "adb"). The DUMP permission lets the launcher read the Wi-Fi name
  when the system location setting is off (Android hides the SSID otherwise); without it the pill just says "Wi-Fi".
#>
param([switch]$NoBuild)

$adb = if ($env:TV_ADB) { $env:TV_ADB } else { "adb" }
$dev = $env:TV_HOST
if (-not $dev) { throw "Set TV_HOST first, e.g. `$env:TV_HOST = '192.168.1.50:5555'" }
$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
$pkg = "dev.glasstv.home"

if (-not $NoBuild) {
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) { throw "Build failed." }
}

& $adb connect $dev | Out-Null
& $adb -s $dev install -r $apk
if ($LASTEXITCODE -ne 0) { throw "Install failed." }
& $adb -s $dev shell "pm grant $pkg android.permission.ACCESS_FINE_LOCATION"
& $adb -s $dev shell "pm grant $pkg android.permission.DUMP"
& $adb -s $dev shell "am start -n $pkg/.MainActivity"
