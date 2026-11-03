package dev.glasstv.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Atajos del control remoto que funcionan desde cualquier app: el boton YouTube abre YouTube y el boton APPS
 * abre este launcher. Hay que activarlo en Ajustes > Accesibilidad.
 */
public final class KeyService extends AccessibilityService {
    private static final int KEY_YOUTUBE = KeyEvent.KEYCODE_BUTTON_3;
    private static final int KEY_APPS = KeyEvent.KEYCODE_ALL_APPS;

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code != KEY_YOUTUBE && code != KEY_APPS) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            if (code == KEY_YOUTUBE) openYoutube();
            else openHome();
        }
        return true;
    }

    private void openYoutube() {
        String pkg = getString(R.string.youtube_pkg);
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void openHome() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setPackage(getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override public void onInterrupt() {}
}
