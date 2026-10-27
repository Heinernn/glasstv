package dev.glasstv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/** Un unico resalte de cristal translucido que salta con muelle de un elemento a otro. */
final class GlassView extends View {
    private static final FloatPropertyCompat<GlassView> LEFT = new FloatPropertyCompat<GlassView>("left") {
        @Override public float getValue(GlassView v) { return v.gx; }
        @Override public void setValue(GlassView v, float f) { v.gx = f; v.invalidate(); }
    };
    private static final FloatPropertyCompat<GlassView> TOP = new FloatPropertyCompat<GlassView>("top") {
        @Override public float getValue(GlassView v) { return v.gy; }
        @Override public void setValue(GlassView v, float f) { v.gy = f; v.invalidate(); }
    };
    private static final FloatPropertyCompat<GlassView> WIDTH = new FloatPropertyCompat<GlassView>("width") {
        @Override public float getValue(GlassView v) { return v.gw; }
        @Override public void setValue(GlassView v, float f) { v.gw = f; v.invalidate(); }
    };
    private static final FloatPropertyCompat<GlassView> HEIGHT = new FloatPropertyCompat<GlassView>("height") {
        @Override public float getValue(GlassView v) { return v.gh; }
        @Override public void setValue(GlassView v, float f) { v.gh = f; v.invalidate(); }
    };
    private static final FloatPropertyCompat<GlassView> RADIUS = new FloatPropertyCompat<GlassView>("radius") {
        @Override public float getValue(GlassView v) { return v.gr; }
        @Override public void setValue(GlassView v, float f) { v.gr = f; v.invalidate(); }
    };

    private float gx, gy, gw, gh, gr;
    private boolean placed;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final SpringAnimation left, top, width, height, radius;

    GlassView(Context c) {
        super(c);
        setFocusable(false);
        fill.setColor(0x1CFFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Ui.dp(c, 1.5f));
        stroke.setColor(0x4DFFFFFF);
        left = spring(LEFT);
        top = spring(TOP);
        width = spring(WIDTH);
        height = spring(HEIGHT);
        radius = spring(RADIUS);
    }

    private SpringAnimation spring(FloatPropertyCompat<GlassView> prop) {
        SpringAnimation a = new SpringAnimation(this, prop);
        a.setSpring(new SpringForce().setStiffness(650f).setDampingRatio(0.72f));
        return a;
    }

    void moveTo(float cx, float cy, float w, float h, float r, boolean animate) {
        float l = cx - w / 2f;
        float t = cy - h / 2f;
        if (!placed || !animate) {
            left.cancel();
            top.cancel();
            width.cancel();
            height.cancel();
            radius.cancel();
            gx = l;
            gy = t;
            gw = w;
            gh = h;
            gr = r;
            placed = true;
            invalidate();
            return;
        }
        left.animateToFinalPosition(l);
        top.animateToFinalPosition(t);
        width.animateToFinalPosition(w);
        height.animateToFinalPosition(h);
        radius.animateToFinalPosition(r);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!placed) return;
        rect.set(gx, gy, gx + gw, gy + gh);
        canvas.drawRoundRect(rect, gr, gr, fill);
        canvas.drawRoundRect(rect, gr, gr, stroke);
    }
}
