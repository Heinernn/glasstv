#!/system/bin/sh
# Captura eventos del receptor AirPlay y descarta TODA linea que contenga una direccion http(s) antes de salir de la TV.
logcat -c
timeout 900 logcat -v threadtime | grep --line-buffered -E 'AirPlayService|AirPlayNative|ExoPlayer|MediaCodec|ACodec' | grep --line-buffered -v -E 'https?:'
