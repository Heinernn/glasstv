package dev.glasstv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Banda redondeada, algo mas clara que el fondo, detras de la fila del dock. */
final class DockBand extends RecyclerView.ItemDecoration {
    int dockCount;
    boolean filler;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float padX;
    private final float radius;
    private final int gap;
    private final int columns;

    DockBand(Context c, int columns) {
        this.columns = columns;
        padX = Ui.dp(c, 20);
        radius = Ui.dp(c, 28);
        gap = Ui.dp(c, 22);
        fill.setColor(0x24FFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Ui.dp(c, 1));
        stroke.setColor(0x33FFFFFF);
    }

    @Override
    public void getItemOffsets(@NonNull Rect out, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        int first = dockCount + 1 + (filler ? 1 : 0);
        if (pos >= first && pos < first + columns) out.top = gap;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        View first = null;
        View last = null;
        int firstPos = Integer.MAX_VALUE;
        int lastPos = -1;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(v);
            if (pos >= 1 && pos <= dockCount) {
                if (pos < firstPos) { firstPos = pos; first = v; }
                if (pos > lastPos) { lastPos = pos; last = v; }
            }
        }
        if (first == null || last == null) return;
        rect.set(first.getLeft() + first.getTranslationX() - padX, first.getTop(),
                last.getRight() + last.getTranslationX() + padX, first.getBottom());
        c.drawRoundRect(rect, radius, radius, fill);
        c.drawRoundRect(rect, radius, radius, stroke);
    }
}
