package dev.glasstv.home;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;

final class AppEntry {
    final String label;
    final String pkg;
    final ComponentName component;
    Bitmap banner;
    int glow = 0xFF6E86FF;

    AppEntry(String label, String pkg, ComponentName component) {
        this.label = label;
        this.pkg = pkg;
        this.component = component;
    }

    Intent launchIntent() {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.LEANBACK_LAUNCHER")
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
}
