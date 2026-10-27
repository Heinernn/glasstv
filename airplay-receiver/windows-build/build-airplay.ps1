<#
.SYNOPSIS
  Builds the android-airplay-server receiver (with patch 0010) on a Windows host for a 32-bit ARM TV.

.DESCRIPTION
  The project assumes a Linux/macOS host; see ..\README.md for what had to be adapted. Expected layout under TV_TOOLS:
    jdk17\<jdk folder>   Temurin 17
    sdk\                 Android SDK: platform 37, build-tools, ndk 27.0.12077973, cmake 3.22.1
    strawberry\c\bin     only gcc is used (FFmpeg's configure wants a host compiler)
    perl-extra\          Locale\Maketext\Simple.pm, Pod\, ExtUtils\ copied from a full Perl (Git's perl lacks them)
    ndk-wrap\            ndk-wrap-clang.sh saved as armv7a-linux-androideabi23-clang
    src\airplay\         checkout of jqssun/android-airplay-server with the patches applied

.EXAMPLE
  $env:TV_TOOLS = "D:\tvtools"; .\build-airplay.ps1
#>
$tools = $env:TV_TOOLS
if (-not $tools) { throw "Set TV_TOOLS to the folder described above." }

$jdk = (Get-ChildItem "$tools\jdk17" -Directory | Select-Object -First 1).FullName
$ndkVersion = "27.0.12077973"
$env:JAVA_HOME = $jdk
# sh, patch, perl, sort... come from Git for Windows and must precede the Windows tools of the same name
$env:Path = "$tools\ndk-wrap;C:\Program Files\Git\usr\bin;$tools\strawberry\c\bin;$env:JAVA_HOME\bin;$env:Path;$tools\sdk\ndk\$ndkVersion\prebuilt\windows-x86_64\bin"

# OpenSSL's Configure runs under MSYS perl, which splits PERL5LIB on ':' and so needs the /d/... form
$drive = $tools.Substring(0, 1).ToLower()
$msysTools = "/$drive" + $tools.Substring(2).Replace('\', '/')
$env:PERL5LIB = "$msysTools/perl-extra"
$env:MSYS2_ENV_CONV_EXCL = "PERL5LIB"
$env:NDK_BIN_DIR = "$msysTools/sdk/ndk/$ndkVersion/toolchains/llvm/prebuilt/windows-x86_64/bin"

$env:ANDROID_HOME = "$tools\sdk"
$env:ANDROID_SDK_ROOT = "$tools\sdk"
$env:GRADLE_USER_HOME = "$tools\gradle-home"

Push-Location "$tools\src\airplay"
try {
    & .\gradlew.bat assembleRelease --no-daemon --console=plain
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
