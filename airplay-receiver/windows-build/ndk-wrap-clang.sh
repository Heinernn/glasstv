#!/usr/bin/env bash
# Save as "armv7a-linux-androideabi23-clang" (no extension) in a folder placed first on PATH.
# NDK clang wrapper that survives long command lines: Windows limits CreateProcess to 32767 characters and the
# link line of a large library exceeds it. Long invocations pass their arguments through a response file (@file).
# Needs NDK_BIN_DIR (MSYS form, e.g. /d/tools/sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/windows-x86_64/bin).
bin_dir=${NDK_BIN_DIR:?set NDK_BIN_DIR to the NDK llvm bin folder}
target=armv7a-linux-androideabi23

if [ "$1" = "-cc1" ]; then
    exec "$bin_dir/clang.exe" "$@"
fi

total=0
for a in "$@"; do
    total=$((total + ${#a} + 3))
done

if [ "$total" -lt 12000 ]; then
    exec "$bin_dir/clang.exe" --target=$target "$@"
fi

rsp=$(mktemp "${TMPDIR:-/tmp}/clang-args.XXXXXX")
for a in "$@"; do
    esc=${a//\\/\\\\}
    esc=${esc//\"/\\\"}
    printf '"%s"\n' "$esc" >> "$rsp"
done
rsp_win=$(cygpath -m "$rsp")
"$bin_dir/clang.exe" --target=$target "@$rsp_win"
rc=$?
rm -f "$rsp"
exit $rc
