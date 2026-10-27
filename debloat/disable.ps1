<#
.SYNOPSIS
  Disables packages one by one (pm disable-user), refusing anything on the protected list, and records what it disabled.

.EXAMPLE
  $env:TV_HOST = "192.168.1.50:5555"
  .\disable.ps1 com.vendor.analytics com.vendor.demo

.NOTES
  Env: TV_HOST (required, ip:port), TV_ADB (adb path, default "adb"), TV_BACKUP (folder for disabled-by-us.txt, default .\backup).
#>
param(
    [Parameter(Mandatory = $true, ValueFromRemainingArguments = $true)][string[]]$Packages
)

$adb = if ($env:TV_ADB) { $env:TV_ADB } else { "adb" }
$dev = $env:TV_HOST
if (-not $dev) { throw "Set TV_HOST first, e.g. `$env:TV_HOST = '192.168.1.50:5555'" }
$backup = if ($env:TV_BACKUP) { $env:TV_BACKUP } else { Join-Path (Get-Location) "backup" }
New-Item -ItemType Directory -Force $backup | Out-Null
$listFile = Join-Path $backup "disabled-by-us.txt"

# Never disabled: Android core and what a TV needs to work. Extend it per device after reading `dumpsys package`.
$protected = @(
    '^android$',
    '^com\.android\.(permissioncontroller|packageinstaller|systemui|shell|phone|settings|tv\.settings.*|ext\..*|modulemetadata|bluetooth.*|inputdevices|keychain|location\.fused|se|vpndialogs|networkstack.*|wifi\.resources|providers\.(media|media\.module|downloads|settings.*|tv|telephony|partnerbookmarks))$',
    '^com\.google\.android\.(permissioncontroller|packageinstaller|ext\.(services|shared)|modulemetadata|overlay\.modules.*|gms|gsf|webview|inputmethod\.latin|tv\.remote\.service|tv\.frameworkpackagestubs)$',
    '^com\.android\.vending$',
    '^com\.mediatek\.(tvinput|tv\.service|network)$',
    'factory', 'factorytest', 'factorymenu'
)

function Invoke-Adb { param([string]$cmd) & $adb -s $dev shell $cmd }

foreach ($p in $Packages) {
    if ($protected | Where-Object { $p -match $_ }) { Write-Host "REFUSED (protected): $p"; continue }
    if (-not (Invoke-Adb "pm list packages $p" | Where-Object { $_ -eq "package:$p" })) { Write-Host "NOT FOUND: $p"; continue }
    Write-Host "$p -> $(Invoke-Adb "pm disable-user --user 0 $p")"
}

$now = (Invoke-Adb "pm list packages -d") -replace 'package:', '' | ForEach-Object { $_.Trim() }
foreach ($p in $Packages) {
    if ($now -contains $p) {
        if (-not (Test-Path $listFile) -or -not (Select-String -Path $listFile -SimpleMatch -Pattern $p -Quiet)) {
            [IO.File]::AppendAllText($listFile, "$p`n", (New-Object Text.UTF8Encoding($false)))
        }
        Write-Host "VERIFIED disabled: $p"
    }
}
