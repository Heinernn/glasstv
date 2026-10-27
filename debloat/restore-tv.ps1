<#
.SYNOPSIS
  Re-enables everything disable.ps1 disabled and gives Home back to the stock launcher.

.NOTES
  Env: TV_HOST (required), TV_ADB, TV_BACKUP (folder holding disabled-by-us.txt, default .\backup),
  STOCK_LAUNCHER (default Google TV: com.google.android.apps.tv.launcherx), CUSTOM_LAUNCHER (default dev.glasstv.home).
#>
$adb = if ($env:TV_ADB) { $env:TV_ADB } else { "adb" }
$dev = $env:TV_HOST
if (-not $dev) { throw "Set TV_HOST first, e.g. `$env:TV_HOST = '192.168.1.50:5555'" }
$backup = if ($env:TV_BACKUP) { $env:TV_BACKUP } else { Join-Path (Get-Location) "backup" }
$listFile = Join-Path $backup "disabled-by-us.txt"
$stock = if ($env:STOCK_LAUNCHER) { $env:STOCK_LAUNCHER } else { "com.google.android.apps.tv.launcherx" }
$custom = if ($env:CUSTOM_LAUNCHER) { $env:CUSTOM_LAUNCHER } else { "dev.glasstv.home" }

& $adb connect $dev | Out-Null

# Stock launcher first, so Home never ends up with nothing to open.
foreach ($p in $stock, 'com.google.android.tungsten.setupwraith') {
    & $adb -s $dev shell "pm enable --user 0 $p"
}
if (Test-Path $listFile) {
    Get-Content $listFile | ForEach-Object { $_.Trim([char]0xFEFF).Trim() } | Where-Object { $_ } | ForEach-Object {
        & $adb -s $dev shell "pm enable --user 0 $_"
    }
}
& $adb -s $dev shell "pm clear-package-preferred-activities $custom" 2>$null

Write-Host "`nStill disabled:"
& $adb -s $dev shell "pm list packages -d"
Write-Host "`nReboot the TV to finish."
