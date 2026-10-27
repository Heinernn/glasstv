# Guía de GlassTV

*(English: [GUIDE.md](GUIDE.md))*

## 1. ¿Puede esta TV? Comprueba antes de tocar nada

Sin ADB no se puede hacer nada de esto.

| Comprobación | Cómo | Si falla |
|---|---|---|
| Es Android TV / Google TV | Ajustes → Acerca de muestra una versión de Android; hay Play Store | Sin debloat, ni launcher, ni receptor |
| Opciones de desarrollador | Pulsar siete veces "Número de compilación" | Sin ADB |
| ADB por red | Activar depuración USB, `adb connect IP:5555`; o Depuración inalámbrica + `adb pair` | Probar el otro modo |
| SDK ≥ 26 | `getprop ro.build.version.sdk` | `INSTALL_FAILED_OLDER_SDK`: solo debloat |
| Acepta APK y apps HOME | Probar a instalar el launcher | Algunos equipos del mercado chino lo rechazan; solo debloat |
| ABI de la CPU | `getprop ro.product.cpu.abilist` | El receptor debe compilarse para esa ABI |
| RAM | `cat /proc/meminfo` | Menos de ~1 GB: launcher mínimo |

Las TV con sistema propio (sus ajustes no muestran versión de Android) no sirven. Un stick Android por HDMI puede ocupar su lugar.

## 2. Debloat

1. `adb connect IP:5555`; acepta en la TV el aviso "¿Permitir depuración USB?".
2. Pregunta qué hay en los USB y si la red llega por un adaptador USB-Ethernet (algunos equipos lo cortan con la depuración USB activa).
3. Copia en `backup/`: `pm list packages -f -u` (y `-e`, `-d`, `-s`, `-3`), `getprop`, `settings list global|secure|system` y
   `cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` (y lo mismo con `LEANBACK_LAUNCHER`).
   `getprop` contiene el número de serie y la MAC: mantenlo privado.
4. Clasifica los paquetes leyendo sus componentes sin lanzarlos: `pm dump PKG | grep -oE "PKG/[A-Za-z0-9_.$]+"`.
5. Desactiva en lotes pequeños con `debloat/disable.ps1`, haz captura entre lotes (`debloat/shot.ps1`), reinicia una vez al final y compara
   `pm list packages -d` (algún firmware reactiva paquetes al arrancar).
6. No lances paquetes llamados factory / production / fac menu: en algunas placas abrir uno activa una bandera de fábrica.

No desactives nunca: el paquete `android`, permission controller, package installer, ext services, module metadata, overlays,
`com.android.providers.*`, system UI, shell, phone, settings, los servicios de entradas y sintonizador de TV, los del mando Bluetooth, el teclado,
Play Store y Play Services. `disable.ps1` aplica una lista inicial; amplíala por dispositivo.

## 3. Launcher

- Compilar: `launcher/build.ps1` (JDK 11, `ANDROID_HOME` con plataforma 33 y build-tools; Gradle viene en el wrapper incluido). Instalar: `launcher/install.ps1`.
- Configuración en `launcher/app/src/main/res/values/config.xml`: `columns`, `dock_apps` (paquetes, de izquierda a derecha), `hidden_apps`.
  Fondo: `res/drawable/wallpaper.xml` (color liso; cámbialo por un bitmap para usar una imagen).
- El nombre del Wi-Fi se lee de `dumpsys wifi` con el permiso `DUMP` concedido por ADB, porque Android oculta el SSID a las apps cuando la
  ubicación del sistema está apagada.
- Teclas: Arriba/Abajo mueven exactamente una fila; Arriba desde el dock va a la barra superior; los extremos de la barra no caen a las tiles; se recuerda la última app.

### Tomar el Home en Google TV

1. `cmd package set-home-activity` imprime `Success` pero no cambia nada.
2. Con el launcher de serie (`com.google.android.apps.tv.launcherx`) desactivado, el asistente de configuración
   (`com.google.android.tungsten.setupwraith`) se queda con la tecla Home mediante su `RecoveryActivity`. Desactiva ambos.
3. Antes ejecuta `dumpsys wifi | grep -E '^ID: [0-9]+ SSID|cname='`: las redes que creó el asistente se borran al desactivarlo. Si una está en uso, pide
   que la olviden y la vuelvan a añadir antes de continuar.
4. Verifica: pulsa Home desde otra app y mira `dumpsys activity activities | grep mFocusedApp`; reinicia y repite.
5. Si Home queda en negro: `debloat/restore-tv.ps1` reactiva primero el launcher de serie y el asistente.

### Teclas del mando que dejan de funcionar

El launcher de serie también atendía algunos botones. Captura la tecla real con `adb shell getevent -lt` y el registro del gestor de ventanas mientras
el usuario la pulsa. En el equipo probado la tecla Ajustes (`KEYCODE_SETTINGS`, 176) se enruta a un receptor del fabricante que arranca la actividad
de panel del launcher de serie, y se cae al desactivarlo. Opciones: (a) reactivar el paquete de serie y desactivar solo su actividad de inicio;
(b) un servicio de accesibilidad propio que intercepte la tecla (puede cortar la repetición, por ejemplo al mantener el volumen).

## 4. Receptor AirPlay

La duplicación AirPlay va cifrada con FairPlay y no se puede escribir desde cero, así que GlassTV se apoya en
[android-airplay-server](https://github.com/jqssun/android-airplay-server). Los parches y la compilación están en [`airplay-receiver/README.md`](../airplay-receiver/README.md).

Preparación por ADB:

```
adb shell appops set io.github.jqssun.airplay SYSTEM_ALERT_WINDOW allow
adb shell appops set io.github.jqssun.airplay RUN_IN_BACKGROUND allow
adb shell appops set io.github.jqssun.airplay RUN_ANY_IN_BACKGROUND allow
adb shell dumpsys deviceidle whitelist +io.github.jqssun.airplay
adb shell am start -n io.github.jqssun.airplay/.MainActivity
adb shell am broadcast -n io.github.jqssun.airplay/.SettingsReceiver -a io.github.jqssun.airplay.SET_SETTING --es key server_name --es value "TV del salon"
adb shell am broadcast -n io.github.jqssun.airplay/.SettingsReceiver -a io.github.jqssun.airplay.LIST_SETTINGS
```

Claves útiles: `server_name`, `advertise_video`, `fill_wide`, `boot_auto_start`, `require_pin`, `h265_enabled`, `max_fps`.
Desactiva cualquier otro receptor AirPlay de la TV para que no se anuncien dos.

Comprueba desde un PC que la TV se anuncia: envía una consulta mDNS PTR de `_airplay._tcp.local` y `_raop._tcp.local` a `224.0.0.251:5353`
**por la interfaz de la LAN** (un adaptador de VirtualBox se la tragaría) y lee `features=`. Repite tras reiniciar sin abrir la app.

Límites: sin contenido con DRM; los enlaces firmados pueden caducar (HTTP 410); la duplicación se detiene al bloquear el iPhone; un iPhone en vertical deja franjas laterales.

## 5. Privacidad al depurar

Los registros del receptor contienen la dirección de cada vídeo enviado. Usa `airplay-receiver/windows-build/cap.sh`: se ejecuta en la TV y descarta toda línea con una URL
antes de que llegue nada al PC. Avisa al usuario antes de capturar y borra lo guardado después. Las capturas de pantalla pueden mostrar contenido personal.

## 6. Cierre

Apaga la depuración USB / de red en Opciones de desarrollador, revoca las autorizaciones de depuración y reinicia. El puerto 5555 va sin cifrar. Comprueba
que rechaza conexiones: solo "Connection refused" prueba que está cerrado ("No route to host" no prueba nada).
