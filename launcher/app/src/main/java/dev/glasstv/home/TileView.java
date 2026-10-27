package dev.glasstv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Icono de app estilo tvOS: 5:3, esquinas redondas, al enfocar se eleva, se inclina y pasa un brillo. */
final class TileView extends FrameLayout {
    static final float SCALE = 1.12f;
    static final float CORNER_DP = 14f;

    private final FrameLayout card;
    private final ImageView banner;
    private final View shine;
    private final TextView label;
    private final int topPad;
    private final int bannerW;
    private final int bannerH;
    private final float lift;

    TileView(Context c, int inset, int topPad, int bannerW, int bannerH) {
        super(c);
        this.topPad = topPad;
        this.bannerW = bannerW;
        this.bannerH = bannerH;
        lift = Ui.dp(c, 22);
        setClipChildren(false);
        setClipToPadding(false);
        setCameraDistance(c.getResources().getDisplayMetrics().density * 9000f);

        card = new FrameLayout(c);
        final float radius = Ui.dp(c, CORNER_DP);
        card.setBackgroundColor(0xFF26262A);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);

        banner = new ImageView(c);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(banner, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        shine = new View(c);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x00FFFFFF, 0x5CFFFFFF, 0x00FFFFFF}));
        shine.setAlpha(0f);
        card.addView(shine, new LayoutParams(Math.round(bannerW * 0.55f), LayoutParams.MATCH_PARENT));

        LayoutParams cp = new LayoutParams(bannerW, bannerH, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        cp.topMargin = topPad;
        addView(card, cp);

        label = new TextView(c);
        Ui.style(label, 12.5f, 400, Color.WHITE);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        label.setSingleLine(true);
        label.setAlpha(0f);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP);
        lp.topMargin = topPad + bannerH + Ui.dp(c, 16);
        lp.leftMargin = inset;
        lp.rightMargin = inset;
        addView(label, lp);
    }

    void bind(AppEntry e) {
        banner.setImageBitmap(e.banner);
        label.setText(e.label);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        setPivotX(w / 2f);
        setPivotY(topPad + bannerH / 2f);
    }

    /** tilt: grados de giro lateral al enfocar (cada icono mira un poco hacia el centro de la pantalla). */
    void setLook(boolean on, boolean animate, float tilt) {
        float s = on ? SCALE : 1f;
        float a = on ? 1f : 0f;
        float rot = on ? tilt : 0f;
        float z = on ? lift : 0f;
        animate().cancel();
        card.animate().cancel();
        label.animate().cancel();
        if (animate) {
            animate().scaleX(s).scaleY(s).rotationY(rot).setDuration(240).setInterpolator(new DecelerateInterpolator());
            card.animate().translationZ(z).setDuration(240);
            label.animate().alpha(a).setDuration(220);
            if (on) sweepShine();
        } else {
            setScaleX(s);
            setScaleY(s);
            setRotationY(rot);
            card.setTranslationZ(z);
            label.setAlpha(a);
            shine.setAlpha(0f);
        }
    }

    private void sweepShine() {
        shine.animate().cancel();
        shine.setTranslationX(-shine.getLayoutParams().width);
        shine.setAlpha(1f);
        shine.animate().translationX(bannerW).setDuration(700)
                .withEndAction(() -> shine.setAlpha(0f));
    }
}
