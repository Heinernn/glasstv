package dev.glasstv.home;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

final class Ui {
    private static Typeface outfit;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static Typeface outfit(Context c) {
        if (outfit == null) {
            outfit = Typeface.createFromAsset(c.getAssets(), "fonts/outfit.ttf");
        }
        return outfit;
    }

    static void style(TextView t, float sp, int weight, int color) {
        t.setTypeface(outfit(t.getContext()));
        t.setFontVariationSettings("'wght' " + weight);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
    }
}
