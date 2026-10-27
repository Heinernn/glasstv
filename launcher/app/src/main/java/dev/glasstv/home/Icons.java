package dev.glasstv.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Iconos de la barra superior dibujados en un lienzo de 24x24. */
final class Icons {
    static final int INPUT = 0;
    static final int WIFI = 1;
    static final int SETTINGS = 2;

    private Icons() {}

    static Drawable of(int kind, int color) {
        return new Glyph(kind, color);
    }

    private static final class Glyph extends Drawable {
        private final int kind;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF arc = new RectF();

        Glyph(int kind, int color) {
            this.kind = kind;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setStrokeWidth(2.2f);
            stroke.setColor(color);
            fill.setColor(color);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            c.save();
            c.translate(b.left, b.top);
            float s = b.width() / 24f;
            c.scale(s, s);
            switch (kind) {
                case INPUT: drawInput(c); break;
                case WIFI: drawWifi(c); break;
                default: drawGear(c); break;
            }
            c.restore();
        }

        private void drawInput(Canvas c) {
            path.reset();
            path.moveTo(12, 5);
            path.lineTo(19, 5);
            path.lineTo(19, 19);
            path.lineTo(12, 19);
            c.drawPath(path, stroke);
            c.drawLine(4, 12, 15, 12, stroke);
            path.reset();
            path.moveTo(11, 8);
            path.lineTo(15, 12);
            path.lineTo(11, 16);
            c.drawPath(path, stroke);
        }

        private void drawWifi(Canvas c) {
            for (float r : new float[] {5f, 9f, 13f}) {
                arc.set(12 - r, 19 - r, 12 + r, 19 + r);
                c.drawArc(arc, 225, 90, false, stroke);
            }
            c.drawCircle(12, 19, 1.8f, fill);
        }

        private void drawGear(Canvas c) {
            c.drawCircle(12, 12, 6.5f, stroke);
            Paint tooth = new Paint(stroke);
            tooth.setStrokeCap(Paint.Cap.BUTT);
            tooth.setStrokeWidth(3.4f);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                float cos = (float) Math.cos(a);
                float sin = (float) Math.sin(a);
                c.drawLine(12 + cos * 8.4f, 12 + sin * 8.4f, 12 + cos * 10.6f, 12 + sin * 10.6f, tooth);
            }
        }

        @Override public void setAlpha(int alpha) { stroke.setAlpha(alpha); fill.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { stroke.setColorFilter(cf); fill.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
