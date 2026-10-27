# AirPlay receiver

GlassTV does not reimplement AirPlay (FairPlay-encrypted). It patches and builds
[android-airplay-server](https://github.com/jqssun/android-airplay-server) (GPL-3.0, based on [UxPlay](https://github.com/FDH2/UxPlay)).
Everything in this folder is distributed under the [GPL-3.0](LICENSE-GPL-3.0.txt).

## What changed upstream (version 0.0.36)

| File | Change |
|---|---|
| `patches/0010-hls-master-query-string-and-direct-http-masters.patch` | Serve the HLS master playlist when the URL carries a query string; return the playlist unchanged when there is nothing to rewrite (it returned uninitialised memory); play plain `http(s)` master URLs directly. Apply it after the project's own patches in `app/src/main/cpp/patches/UxPlay`. Upstream issue #79. |
| `windows-build/project-changes.diff` | Kotlin changes: mirroring fills wide frames (`fill_wide` setting, Menu key cycles fit / fill / stretch); a video played from its own URL keeps playing when the sender disconnects (iPhone locked). Also pins the ABI to `armeabi-v7a`. |
| `windows-build/` | Everything needed to build on Windows (below). |

If you only need mirroring and music, the unpatched F-Droid release works; the patch is what makes the in-app AirPlay button play video.

## Building on Windows

The project assumes Linux. Adapt a checkout with `python windows-build/patch-airplay-windows.py <checkout>` (idempotent; it applies the
OpenSSL, FFmpeg and CMake changes), then run `windows-build/build-airplay.ps1` with `TV_TOOLS` pointing at a folder with the layout described
at the top of that script. The pieces:

- OpenSSL: `building_env.py` runs bash instead of `cmd`; `15-android.conf` path comparison normalised; no CMake regeneration in the install step
  (ninja holds its files open on Windows); built **static** because the `libcrypto.so` link line exceeds Windows' 32,767-character limit.
- FFmpeg: `configure` through `sh`, `clang.exe --target=...` instead of the `.cmd` wrappers, `--host-cc=gcc`, `pwd -W` so `make` gets `D:/...` paths.
- `ndk-wrap-clang.sh`: `clang` wrapper that passes very long argument lists through a response file.
- Perl: Git's MSYS perl (OpenSSL refuses native Windows perl) plus a few pure-perl modules copied from a full distribution (`PERL5LIB`).
- If a build stops with `libavutil/samplefmt.h not found`, run `make install-libavutil-headers` in FFmpeg's build directory by hand.

For a 64-bit TV change `allAbis` in `app/build.gradle.kts` to `arm64-v8a` and rebuild from a clean native cache.

## Debugging without leaking what was cast

`windows-build/cap.sh` runs on the TV (`adb push` it to `/data/local/tmp`, run with `sh`): it keeps only the receiver's log tags and drops every
line containing `http://` or `https://`, so no video address ever reaches the PC.
