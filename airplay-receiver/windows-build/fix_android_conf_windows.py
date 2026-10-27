"""OpenSSL 15-android.conf: detecta el clang del NDK comparando rutas con '/'.

En un anfitrion Windows perl devuelve rutas con '\\' (canonpath y IPC::Cmd), la comparacion
nunca coincide y OpenSSL cae a buscar un gcc inexistente. Normalizamos todo a '/'.
Se ejecuta desde la raiz del codigo de OpenSSL. Es idempotente.
"""
import re
from pathlib import Path

conf = Path("Configurations") / "15-android.conf"
text = conf.read_text(encoding="utf-8")

if "which_fwd" in text:
    print("15-android.conf ya adaptado")
    raise SystemExit(0)

nl = "\r\n" if "\r\n" in text else "\n"

# 1) la raiz del NDK con barras normales
needle = "$ndk = canonpath($ndk);"
if text.count(needle) != 1:
    raise SystemExit("no se encontro canonpath($ndk) una sola vez")
text = text.replace(needle, needle + nl + "            $ndk =~ s|\\\\|/|g;")

# 2) todas las busquedas de ejecutables devuelven rutas con barras normales
text = re.sub(r"\bwhich\(", "which_fwd(", text)

helper = (
    "    sub which_fwd {" + nl +
    "        my $found = which($_[0]);" + nl +
    "        $found =~ s|\\\\|/|g if defined $found;" + nl +
    "        return $found;" + nl +
    "    }" + nl + nl
)
anchor = "    sub android_ndk {"
if text.count(anchor) != 1:
    raise SystemExit("no se encontro 'sub android_ndk' una sola vez")
text = text.replace(anchor, helper + anchor)

conf.write_text(text, encoding="utf-8", newline="")
print("15-android.conf adaptado a rutas de Windows")
