package dev.glasstv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
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
    private final Paint tint = new Paint();
    private final Paint grain = new Paint();
    private float cx, cy, radius;
    private int color = Color.TRANSPARENT;

    GlowView(Context c) {
        super(c);
        setFocusable(false);
        radial.setAlpha(0);
        // Grano muy sutil (evita las bandas del degradado en pantallas grandes).
        Bitmap n = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        java.util.Random r = new java.util.Random(7);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                int a = r.nextInt(4);
                n.setPixel(x, y, (a << 24) | (r.nextBoolean() ? 0xFFFFFF : 0x000000));
            }
        }
        grain.setShader(new BitmapShader(n, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
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
        // Segundo tono: el mismo color, mucho mas oscuro, en vez de negro puro abajo.
        int dark = darker(rgb, 0.28f);
        if (getHeight() > 0) {
            ambient.setShader(new LinearGradient(0, 0, 0, getHeight(),
                    new int[] {0x7A000000 | rgb, 0x40000000 | rgb, 0xB0000000 | dark},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            // Resplandor azul noche abajo a la derecha: da profundidad al fondo.
            tint.setShader(new RadialGradient(getWidth() * 0.9f, getHeight() * 1.0f, getWidth() * 0.6f,
                    new int[] {0x5A14264F, 0x00000000}, null, Shader.TileMode.CLAMP));
        }
        if (radius > 0) {
            radial.setShader(new RadialGradient(cx, cy, radius,
                    new int[] {0x90000000 | rgb, 0x40000000 | rgb, rgb},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }
    }

    private static int darker(int rgb, float f) {
        int r = Math.round(((rgb >> 16) & 0xFF) * f);
        int g = Math.round(((rgb >> 8) & 0xFF) * f);
        int b = Math.round((rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), ambient);
        canvas.drawRect(0, 0, getWidth(), getHeight(), tint);
        if (radius > 0 && radial.getAlpha() > 0) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), radial);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), grain);
    }
}
