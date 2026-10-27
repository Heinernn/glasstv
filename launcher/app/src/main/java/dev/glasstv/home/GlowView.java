package dev.glasstv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Fondo ambiental sin blur: un degradado vertical con el color de la app enfocada, y debajo del nombre
 * un halo radial que solo se ve mientras el foco esta en el dock.
 */
final class GlowView extends View {
    private final Paint ambient = new Paint();
    private final Paint radial = new Paint();
    private float cx, cy, radius;
    private int color = Color.TRANSPARENT;

    GlowView(Context c) {
        super(c);
        setFocusable(false);
        radial.setAlpha(0);
    }

    void setCenter(float cx, float cy, float radius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        rebuild();
    }

    void setGlowColor(int c) {
        color = c;
        rebuild();
        invalidate();
    }

    /** 0 oculta el halo del nombre, 1 lo muestra entero. */
    void setRadialStrength(float s) {
        radial.setAlpha(Math.round(255 * Math.max(0f, Math.min(1f, s))));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuild();
    }

    private void rebuild() {
        int rgb = color & 0x00FFFFFF;
        if (getHeight() > 0) {
            ambient.setShader(new LinearGradient(0, 0, 0, getHeight(),
                    0x38000000 | rgb, 0x0C000000 | rgb, Shader.TileMode.CLAMP));
        }
        if (radius > 0) {
            radial.setShader(new RadialGradient(cx, cy, radius,
                    new int[] {0x60000000 | rgb, 0x28000000 | rgb, rgb},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), ambient);
        if (radius > 0 && radial.getAlpha() > 0) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), radial);
        }
    }
}
