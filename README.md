# GlassTV

[![Build launcher](https://github.com/Heinernn/glasstv/actions/workflows/build.yml/badge.svg)](https://github.com/Heinernn/glasstv/actions/workflows/build.yml)

**Debloat an Android TV, give it a glass-style Apple TV home screen, and AirPlay to it from an iPhone.**
*(Español: [README.es.md](README.es.md))*

<p align="center">
  <img src="docs/img/home.png" width="49%" alt="Home screen: dock with the focused app, glass highlight, top bar">
  <img src="docs/img/focus.png" width="49%" alt="Another app focused: the ambient background takes its colour">
</p>

GlassTV is three pieces that work together on a stock Android TV / Google TV, all driven over ADB (no root):

| Piece | What it does |
|---|---|
| [`debloat/`](debloat) | Backs the TV up, disables telemetry / vendor content / preinstalled apps one by one (never uninstalls), refuses a protected list, and can undo everything. |
| [`launcher/`](launcher) | A lightweight native launcher (Java, ~50 MB RAM, no blur, no video): dock band, app grid, a single translucent glass highlight that springs between tiles, ambient background tinted with the focused app, clock, Wi-Fi / Settings pills and an Inputs menu (HDMI, AV, live TV, iPhone). Deterministic D-pad navigation. |
| [`airplay-receiver/`](airplay-receiver) | Patches and Windows build notes for the open-source [android-airplay-server](https://github.com/jqssun/android-airplay-server): screen mirroring, music and in-app AirPlay video, advertised to iPhones as soon as the TV is on. |

Tested on a Xiaomi MiTV-MOSR2 (Google TV on Android 11, 1.4 GB RAM, 32-bit ARM).

## Engineering notes (the interesting parts)

- **Taking over Home on Google TV.** `cmd package set-home-activity` prints `Success` and does nothing. Home only moved to the custom
  launcher after disabling the stock launcher *and* the setup wizard that hijacks the Home intent. Disabling that wizard makes Android
  delete the Wi-Fi networks it created, so the tooling checks which saved network belongs to it first.
- **A dead remote button explained.** After disabling the stock launcher the remote's Settings key stopped working. Tracing
  `KEYCODE_SETTINGS` through the window manager logs showed a vendor `GlobalKeyReceiver` crashing on an explicit start of the stock
  launcher's dashboard activity ("process is bad"). The write-up lists two fixes and their trade-offs.
- **In-app AirPlay video returned 404.** The receiver served the HLS master playlist only for the exact path `/master.m3u8`; iOS appends a
  query string. Patch `0010` matches on the path, fixes an uninitialised buffer in the playlist rewrite
  (upstream issue [#79](https://github.com/jqssun/android-airplay-server/issues/79)) and plays plain `http(s)` masters directly, as an Apple TV does.
  Direct videos also keep playing when the iPhone locks and drops its connections.
- **Cross-building OpenSSL + FFmpeg for Android from Windows.** The project assumes Linux. Fixes: bash instead of `cmd`, path
  normalisation in OpenSSL's Android config, a `clang` wrapper that passes long argument lists through a response file
  (Windows' 32,767-character limit broke the `libcrypto.so` link), static OpenSSL, `pwd -W` paths for FFmpeg's `make`, a perl whose
  missing modules are supplied separately. See [`airplay-receiver/windows-build`](airplay-receiver/windows-build).
- **Privacy while debugging.** Receiver logs contain the URL of every cast video. `cap.sh` runs on the TV and drops every line that
  contains a URL before anything reaches the PC.
- **Mirroring that fills the screen.** Wide frames (an iPhone in landscape) are scaled to fill a 16:9 screen, cropping at most 35 %; the
  Menu key cycles fit / fill / stretch.

## Quick start

```powershell
$env:TV_HOST = "192.168.1.50:5555"      # your TV, ADB over the network
cd debloat
.\disable.ps1 com.vendor.analytics      # one package at a time, protected list enforced
.\restore-tv.ps1                        # undo everything
cd ..\launcher
.\install.ps1                           # build, install, open (needs JDK 11 and Android SDK 33; Gradle comes from the included wrapper)
```

Read [docs/GUIDE.md](docs/GUIDE.md) first: it explains how to check that a TV can do this at all, the back-up step, the Home takeover, and
the AirPlay receiver.

## Limits

- Needs Android TV with ADB access. TVs with a proprietary OS cannot be debloated or have apps installed.
- AirPlay receivers that are not Apple's cannot play DRM content (Apple TV+, Disney+, Prime Video); mirroring shows black for those apps.
- Links signed for the iPhone's session may be refused (HTTP 410/400). Mirroring stops when the iPhone locks (iOS stops sending frames,
  same as on an Apple TV); the in-app AirPlay button does not.
- **Disabling packages can leave a TV unable to boot** if a core package is included. Back up first, disable in small batches, reboot to check.
  You do this at your own risk.

## License and credits

- Launcher, debloat scripts and documentation: [MIT](LICENSE).
- `airplay-receiver/`: patches to GPL-3.0 software, distributed under [GPL-3.0](airplay-receiver/LICENSE-GPL-3.0.txt).
  Upstream: [android-airplay-server](https://github.com/jqssun/android-airplay-server) and [UxPlay](https://github.com/FDH2/UxPlay).
- Font: [Outfit](https://github.com/google/fonts/tree/main/ofl/outfit), SIL Open Font License.
- Not affiliated with or endorsed by Apple, Google, Xiaomi or Netflix. "Apple TV" and "AirPlay" are trademarks of Apple Inc.;
  the home screen is an independent design and uses no Apple assets.
