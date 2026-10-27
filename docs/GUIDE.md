# GlassTV guide

*(Español: [GUIA.es.md](GUIA.es.md))*

## 1. Can this TV do it? Check before touching anything

Without ADB none of this is possible.

| Check | How | If it fails |
|---|---|---|
| It is Android TV / Google TV | Settings → About shows an Android version; there is a Play Store | No debloat, no launcher, no receiver |
| Developer options | Tap "Build number" seven times | No ADB |
| ADB over the network | Enable USB debugging, `adb connect IP:5555`; or Wireless debugging + `adb pair` | Try the other mode |
| SDK ≥ 26 | `getprop ro.build.version.sdk` | `INSTALL_FAILED_OLDER_SDK`: debloat only |
| APKs and HOME apps accepted | Try installing the launcher | Some China-market sets refuse; debloat only |
| CPU ABI | `getprop ro.product.cpu.abilist` | The receiver must be built for that ABI |
| RAM | `cat /proc/meminfo` | Under ~1 GB: keep the launcher minimal |

TVs running a proprietary OS (their settings show no Android version) cannot be used. An HDMI Android stick can stand in for them.

## 2. Debloat

1. `adb connect IP:5555`; accept the "Allow USB debugging?" prompt on the TV.
2. Ask what is plugged into the USB ports, and whether the network comes through a USB-Ethernet adapter (some sets drop it while USB debugging is on).
3. Back up into `backup/`: `pm list packages -f -u` (also `-e`, `-d`, `-s`, `-3`), `getprop`, `settings list global|secure|system`, and
   `cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` (and the same for `LEANBACK_LAUNCHER`).
   `getprop` contains the serial number and MAC: keep it private.
4. Classify packages by reading their components without launching them: `pm dump PKG | grep -oE "PKG/[A-Za-z0-9_.$]+"`.
5. Disable in small batches with `debloat/disable.ps1`, screenshot between batches (`debloat/shot.ps1`), reboot once at the end and compare
   `pm list packages -d` (some firmware re-enables packages at boot).
6. Never launch packages named factory / production / fac menu: on some boards opening one flips a factory flag.

Never disable: the `android` package, permission controller, package installer, ext services, module metadata, overlays,
`com.android.providers.*`, system UI, shell, phone, settings, the TV input and tuner services, Bluetooth remote services, the keyboard,
Play Store and Play Services. `disable.ps1` enforces a starting list; extend it per device.

## 3. Launcher

- Build: `launcher/build.ps1` (JDK 11, `ANDROID_HOME` with platform 33 + build-tools; Gradle comes from the included wrapper). Install: `launcher/install.ps1`.
- Configuration in `launcher/app/src/main/res/values/config.xml`: `columns`, `dock_apps` (package names, left to right), `hidden_apps`.
  Background: `res/drawable/wallpaper.xml` (a solid colour; replace it with a bitmap to use an image).
- The Wi-Fi name is read from `dumpsys wifi` using the `DUMP` permission granted over ADB, because Android hides the SSID from apps when the
  system location setting is off.
- Keys: Up/Down move exactly one row; Up from the dock goes to the top bar; the ends of the bar do not fall through to tiles; the last focused app is remembered.

### Taking over Home on Google TV

1. `cmd package set-home-activity` prints `Success` but changes nothing.
2. With the stock launcher (`com.google.android.apps.tv.launcherx`) disabled, the setup wizard (`com.google.android.tungsten.setupwraith`)
   takes the Home key through its `RecoveryActivity`. Disable both.
3. First run `dumpsys wifi | grep -E '^ID: [0-9]+ SSID|cname='`: networks created by the wizard are deleted when it is disabled. If one is in use, have
   the user forget it and join again before continuing.
4. Verify: press Home from another app and check `dumpsys activity activities | grep mFocusedApp`, then reboot and check again.
5. If Home ends up black: `debloat/restore-tv.ps1` re-enables the stock launcher and the wizard first.

### Remote keys that stop working

The stock launcher also answered some remote buttons. Capture the real key with `adb shell getevent -lt` and the window manager log while
the user presses it. On the tested set the Settings key (`KEYCODE_SETTINGS`, 176) is routed to a vendor receiver that starts the stock launcher's
dashboard activity, and crashes once that launcher is disabled. Options: (a) re-enable the stock package and disable only its home activity;
(b) an own accessibility key service (may cut key repeat, e.g. holding volume).

## 4. AirPlay receiver

AirPlay mirroring is encrypted with FairPlay and cannot be written from scratch, so GlassTV builds on
[android-airplay-server](https://github.com/jqssun/android-airplay-server). See [`airplay-receiver/README.md`](../airplay-receiver/README.md) for the patches and the build.

Setup over ADB:

```
adb shell appops set io.github.jqssun.airplay SYSTEM_ALERT_WINDOW allow
adb shell appops set io.github.jqssun.airplay RUN_IN_BACKGROUND allow
adb shell appops set io.github.jqssun.airplay RUN_ANY_IN_BACKGROUND allow
adb shell dumpsys deviceidle whitelist +io.github.jqssun.airplay
adb shell am start -n io.github.jqssun.airplay/.MainActivity
adb shell am broadcast -n io.github.jqssun.airplay/.SettingsReceiver -a io.github.jqssun.airplay.SET_SETTING --es key server_name --es value "Living room TV"
adb shell am broadcast -n io.github.jqssun.airplay/.SettingsReceiver -a io.github.jqssun.airplay.LIST_SETTINGS
```

Useful keys: `server_name`, `advertise_video`, `fill_wide`, `boot_auto_start`, `require_pin`, `h265_enabled`, `max_fps`.
Disable any other AirPlay receiver on the TV so two are not advertised.

Check from a PC that the TV is advertised: send an mDNS PTR query for `_airplay._tcp.local` and `_raop._tcp.local` to `224.0.0.251:5353`
**from the LAN interface** (a VirtualBox adapter will otherwise swallow it) and read `features=`. Check again after a reboot without opening the app.

Limits: no DRM content; signed links can expire (HTTP 410); mirroring stops when the iPhone locks; a portrait iPhone leaves side bars.

## 5. Privacy while debugging

Receiver logs contain the URL of every cast video. Use `airplay-receiver/windows-build/cap.sh`: it runs on the TV and drops every line with a URL
before anything reaches the PC. Tell the user before capturing and delete what you saved afterwards. Screenshots can show personal content.

## 6. Closing

Turn USB / network debugging off in Developer options, revoke the debugging authorisations and reboot. Port 5555 is plaintext. Verify it refuses
connections: only "Connection refused" proves it is closed ("No route to host" proves nothing).
