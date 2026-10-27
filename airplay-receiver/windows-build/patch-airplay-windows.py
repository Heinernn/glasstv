"""Adapta el proyecto android-airplay-server para compilarlo desde un anfitrion Windows.

El proyecto asume Linux/macOS: OpenSSL usa cmd en lugar de bash para Android y FFmpeg
busca gcc y clang sin extension. Es idempotente: se puede ejecutar varias veces.
"""
import sys
from pathlib import Path

SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else sys.exit("usage: patch-airplay-windows.py <path to android-airplay-server checkout>")
CPP = SRC / "app" / "src" / "main" / "cpp"


def edit(path, pairs):
    text = path.read_text(encoding="utf-8")
    nl = "\r\n" if "\r\n" in text else "\n"
    for old, new in pairs:
        old = old.replace("\n", nl)
        new = new.replace("\n", nl)
        if new and new in text:
            continue
        if text.count(old) == 0:
            print(f"  omitido en {path.name} (ya aplicado o el texto cambio):\n{old[:80]}")
            continue
        if text.count(old) != 1:
            raise SystemExit(f"{path.name}: esperaba 1 coincidencia y hay {text.count(old)} para:\n{old}")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8", newline="")
    print("OK", path.name)


edit(CPP / "third_party" / "openssl-cmake" / "scripts" / "building_env.py", [
    (
        "env_sep = ';' if args.os == 'WIN32' else ':'",
        "# windows host cross-compiling for android: bash (msys) is still needed and PATH uses ';'\n"
        "win_host = os.name == 'nt'\n"
        "env_sep = ';' if (args.os == 'WIN32' or win_host) else ':'",
    ),
    (
        "    for k, v in pattern.findall(content):\n"
        "        add_env(k, v.replace(\"\\\\\\\"\", \"\\\"\"))",
        "    for k, v in pattern.findall(content):\n"
        "        v = v.replace(\"\\\\\\\"\", \"\\\"\")\n"
        "        if k == 'PATH' and win_host:\n"
        "            # the cmake file joins directories with ':', which clashes with drive letters\n"
        "            v = v.replace('/:', '/;')\n"
        "        add_env(k, v)",
    ),
    (
        "            v = v.replace('/:', '/;')\n",
        "            v = v.replace('/:', '/;')\n"
        "        if k in ('ANDROID_NDK', 'ANDROID_NDK_ROOT') and win_host:\n"
        "            # msys perl sees PATH as /e/...: keep the ndk root in that form so path comparisons match\n"
        "            v = re.sub(r'^([A-Za-z]):/', lambda m: '/' + m.group(1).lower() + '/', v)\n",
    ),
    (
        "else:\n"
        "    proc = Popen(cmd_line, env=env, cwd=args.cwd, shell=True)\n"
        "    proc.communicate()",
        "elif win_host:\n"
        "    proc = Popen(['bash', '-c', cmd_line], env=env, cwd=args.cwd)\n"
        "    proc.communicate()\n"
        "else:\n"
        "    proc = Popen(cmd_line, env=env, cwd=args.cwd, shell=True)\n"
        "    proc.communicate()",
    ),
])

edit(CPP / "cmake" / "BuildFFmpeg.cmake", [
    (
        '    set(_ffmpeg_make "${ANDROID_NDK}/prebuilt/windows-x86_64/bin/make.exe")\n',
        '    set(_ffmpeg_make "${ANDROID_NDK}/prebuilt/windows-x86_64/bin/make.exe")\n'
        "    # configure only compiles (never runs) a host test file: use the ndk clang against the ndk sysroot\n"
        '    set(_ffmpeg_hostcc "--host-cc=${_ffmpeg_tc}/clang.exe --target=x86_64-linux-android${AIRPLAY_ANDROID_API} '
        '--sysroot=${CMAKE_SYSROOT}")\n',
    ),
    (
        "    set(_ffmpeg_make make)\nendif()",
        '    set(_ffmpeg_make make)\n    set(_ffmpeg_hostcc "")\nendif()',
    ),
    (
        '        "--cxx=${_ffmpeg_cxx}"\n',
        '        "--cxx=${_ffmpeg_cxx}"\n        "${_ffmpeg_hostcc}"\n',
    ),
])

# el paso de instalacion de OpenSSL regenera build.ninja mientras ninja corre; en Windows ninja tiene esos
# archivos abiertos ("failed recompaction: Permission denied"). Las librerias importadas tienen ruta fija: no hace falta.
edit(CPP / "third_party" / "openssl-cmake" / "cmake" / "BuildOpenSSL.cmake", [
    (
        "        COMMAND ${CMAKE_COMMAND} -G ${CMAKE_GENERATOR} ${CMAKE_BINARY_DIR}                    # force CMake-reload\n",
        "",
    ),
])

# OpenSSL estatico: el enlazado de libcrypto.so supera el limite de linea de comandos de Windows (32767)
edit(CPP / "CMakeLists.txt", [
    (
        'set(BUILD_OPENSSL ON CACHE BOOL "" FORCE)\n',
        'set(BUILD_OPENSSL ON CACHE BOOL "" FORCE)\n'
        'set(OPENSSL_USE_STATIC_LIBS ON CACHE STRING "" FORCE)\n'
        'set(OPENSSL_MODULES "no-shared" CACHE STRING "" FORCE)\n',
    ),
])

# el make nativo del NDK no entiende rutas /e/...: configure debe escribir la ruta de fuentes como E:/...
edit(CPP / "third_party" / "ffmpeg" / "configure", [
    (
        '    source_path=$(cd $(dirname "$0"); pwd)\n',
        '    source_path=$(cd $(dirname "$0"); pwd -W 2>/dev/null || pwd)\n',
    ),
])
