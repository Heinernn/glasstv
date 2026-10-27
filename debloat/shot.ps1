<#
.SYNOPSIS
  Presses remote keys over ADB, then pulls a screenshot from the TV.

.EXAMPLE
  .\shot.ps1 -Out shots\01.png -Keys DPAD_RIGHT,DPAD_DOWN

.NOTES
  Live TV, HDMI and DRM apps give empty screenshots (0 bytes): check `dumpsys activity activities | grep mFocusedApp` instead.
  Env: TV_HOST (required), TV_ADB.
#>
param(
    [Parameter(Mandatory = $true)][string]$Out,
    [string[]]$Keys = @()
)

$adb = if ($env:TV_ADB) { $env:TV_ADB } else { "adb" }
$dev = $env:TV_HOST
if (-not $dev) { throw "Set TV_HOST first, e.g. `$env:TV_HOST = '192.168.1.50:5555'" }

foreach ($k in $Keys) {
    & $adb -s $dev shell "input keyevent KEYCODE_$k"
    Start-Sleep -Milliseconds 700
}

# Capture on the device and pull: piping screencap through exec-out gets corrupted by vendor log noise.
& $adb -s $dev shell "screencap -p /data/local/tmp/shot.png" | Out-Null
$dir = Split-Path -Parent $Out; if ($dir) { New-Item -ItemType Directory -Force $dir | Out-Null }
& $adb -s $dev pull /data/local/tmp/shot.png $Out | Out-Null
& $adb -s $dev shell "rm /data/local/tmp/shot.png"

$size = (Get-Item $Out).Length
if ($size -eq 0) { Write-Host "Empty capture (0 bytes): protected app or HDMI / live TV input."; exit 1 }
Write-Host "$Out ($size bytes)"
