package dev.glasstv.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AppRepository {
    private static final int BANNER_W = 400;
    private static final int BANNER_H = 240;

    static final class Loaded {
        final List<AppEntry> apps;
        final int dockCount;

        Loaded(List<AppEntry> apps, int dockCount) {
            this.apps = apps;
            this.dockCount = dockCount;
        }
    }

    private AppRepository() {}

    static Loaded load(Context ctx, int columns, String[] dockPackages, String[] hiddenPackages) {
        java.util.Set<String> hidden = new java.util.HashSet<>(java.util.Arrays.asList(hiddenPackages));
        PackageManager pm = ctx.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.LEANBACK_LAUNCHER");

        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (ai.packageName.equals(ctx.getPackageName()) || byPackage.containsKey(ai.packageName)
                    || hidden.contains(ai.packageName)) continue;
            AppEntry e = new AppEntry(String.valueOf(ri.loadLabel(pm)), ai.packageName,
                    new ComponentName(ai.packageName, ai.name));
            e.banner = render(pm, ai);
            e.glow = sampleGlow(e.banner);
            byPackage.put(ai.packageName, e);
        }

        List<AppEntry> dock = new ArrayList<>();
        for (String pkg : dockPackages) {
            AppEntry e = byPackage.get(pkg);
            if (e != null && dock.size() < columns) {
                dock.add(e);
                byPackage.remove(pkg);
            }
        }

        final Collator collator = Collator.getInstance(new Locale("es", "ES"));
        collator.setStrength(Collator.PRIMARY);
        List<AppEntry> rest = new ArrayList<>(byPackage.values());
        rest.sort((a, b) -> collator.compare(a.label, b.label));

        while (dock.size() < columns && !rest.isEmpty()) {
            dock.add(rest.remove(0));
        }

        List<AppEntry> all = new ArrayList<>(dock);
        all.addAll(rest);
        return new Loaded(all, dock.size());
    }

    private static Bitmap render(PackageManager pm, ActivityInfo ai) {
        Bitmap bmp = Bitmap.createBitmap(BANNER_W, BANNER_H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Drawable banner = ai.loadBanner(pm);
        if (banner != null) {
            c.drawColor(0xFF1F1F23);
            int iw = banner.getIntrinsicWidth() > 0 ? banner.getIntrinsicWidth() : 320;
            int ih = banner.getIntrinsicHeight() > 0 ? banner.getIntrinsicHeight() : 180;
            float scale = Math.max(BANNER_W / (float) iw, BANNER_H / (float) ih);
            int dw = Math.round(iw * scale);
            int dh = Math.round(ih * scale);
            int left = (BANNER_W - dw) / 2;
            int top = (BANNER_H - dh) / 2;
            banner.setBounds(left, top, left + dw, top + dh);
            banner.draw(c);
            return bmp;
        }
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, 0, BANNER_W, BANNER_H, 0xFF3A3A44, 0xFF1B1B20, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, BANNER_W, BANNER_H, p);
        Drawable icon = ai.loadIcon(pm);
        int size = 140;
        int left = (BANNER_W - size) / 2;
        int top = (BANNER_H - size) / 2;
        icon.setBounds(left, top, left + size, top + size);
        icon.draw(c);
        return bmp;
    }

    /** Color dominante y saturado del banner, para el brillo ambiental. */
    static int sampleGlow(Bitmap src) {
        Bitmap s = Bitmap.createScaledBitmap(src, 16, 9, true);
        float[] hsv = new float[3];
        float r = 0, g = 0, b = 0, w = 0;
        for (int y = 0; y < s.getHeight(); y++) {
            for (int x = 0; x < s.getWidth(); x++) {
                int p = s.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                Color.colorToHSV(p, hsv);
                if (hsv[2] < 0.25f || hsv[1] < 0.2f) continue;
                float wt = hsv[1] * hsv[2];
                r += Color.red(p) * wt;
                g += Color.green(p) * wt;
                b += Color.blue(p) * wt;
                w += wt;
            }
        }
        s.recycle();
        if (w < 0.5f) return 0xFF6E86FF;
        Color.colorToHSV(Color.rgb(Math.round(r / w), Math.round(g / w), Math.round(b / w)), hsv);
        hsv[1] = Math.max(hsv[1], 0.6f);
        hsv[2] = Math.max(hsv[2], 0.9f);
        return Color.HSVToColor(hsv);
    }
}
