package dev.glasstv.home;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SPACER = 0;
    private static final int TYPE_TILE = 1;
    private static final int TYPE_FILLER = 2;

    private final Context ctx;
    private final int columns;
    private final int dockCols;
    private final float dockShift;
    private final int spacerHeight;
    private final int cellHeight;
    private final int inset;
    private final int topPad;
    private final int bannerW;
    private final int bannerH;
    private final List<AppEntry> apps = new ArrayList<>();
    private int selected = -1;
    private boolean intro = true;
    private int dockCount;

    AppAdapter(Context ctx, int columns, int dockCols, float dockShift, int spacerHeight, int cellHeight,
               int inset, int topPad, int bannerW, int bannerH) {
        this.ctx = ctx;
        this.columns = columns;
        this.dockCols = dockCols;
        this.dockShift = dockShift;
        this.spacerHeight = spacerHeight;
        this.cellHeight = cellHeight;
        this.inset = inset;
        this.topPad = topPad;
        this.bannerW = bannerW;
        this.bannerH = bannerH;
    }

    /** Hay un hueco invisible tras el dock para que la primera fila de la rejilla empiece en la fila siguiente. */
    private boolean hasFiller() {
        return !apps.isEmpty() && columns > dockCount;
    }

    /** Posicion en el RecyclerView de la app con ese indice. */
    int posOf(int index) {
        return index + 1 + (hasFiller() && index >= dockCount ? 1 : 0);
    }

    private int indexOf(int position) {
        return position - 1 - (hasFiller() && position > dockCount + 1 ? 1 : 0);
    }

    int spanSize(int position) {
        if (position == 0) return columns;
        if (hasFiller() && position == dockCount + 1) return columns - dockCount;
        return 1;
    }

    boolean isFiller(int position) {
        return hasFiller() && position == dockCount + 1;
    }

    void setData(List<AppEntry> list, int dockCount) {
        apps.clear();
        apps.addAll(list);
        this.dockCount = dockCount;
        notifyDataSetChanged();
    }

    void setSelected(int index, boolean animate, RecyclerView rv) {
        int old = selected;
        selected = index;
        refreshLook(rv, old, animate);
        refreshLook(rv, index, animate);
    }

    private void refreshLook(RecyclerView rv, int index, boolean animate) {
        if (index < 0) return;
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(posOf(index));
        if (vh != null && vh.itemView instanceof TileView) {
            ((TileView) vh.itemView).setLook(index == selected, animate, tilt(index));
        }
    }

    /** Giro lateral (grados): los iconos de la izquierda miran a la derecha y viceversa. */
    private float tilt(int index) {
        float col = index < dockCount
                ? index + (columns - dockCols) / 2f
                : (index - dockCount) % columns;
        float frac = (col + 0.5f) / columns - 0.5f;
        return frac * 8f;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_SPACER;
        return isFiller(position) ? TYPE_FILLER : TYPE_TILE;
    }

    @Override
    public int getItemCount() {
        return apps.size() + 1 + (hasFiller() ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == TYPE_SPACER) {
            v = new View(ctx);
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacerHeight));
        } else if (viewType == TYPE_FILLER) {
            v = new View(ctx);
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        } else {
            v = new TileView(ctx, inset, topPad, bannerW, bannerH);
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight));
        }
        return new RecyclerView.ViewHolder(v) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0 || isFiller(position)) return;
        int idx = indexOf(position);
        TileView t = (TileView) holder.itemView;
        t.bind(apps.get(idx));
        t.setLook(idx == selected, false, tilt(idx));
        t.setTranslationX(idx < dockCount ? dockShift : 0f);
        t.setAlpha(1f);
        t.setTranslationY(0f);
        if (intro && idx < columns * 3) {
            long delay = 40L * ((idx / columns) * 2 + idx % columns);
            t.setAlpha(0f);
            t.setTranslationY(ctx.getResources().getDisplayMetrics().density * 24f);
            ObjectAnimator a = ObjectAnimator.ofFloat(t, View.ALPHA, 0f, 1f);
            ObjectAnimator y = ObjectAnimator.ofFloat(t, View.TRANSLATION_Y, t.getTranslationY(), 0f);
            a.setStartDelay(delay); y.setStartDelay(delay);
            a.setDuration(380); y.setDuration(380);
            a.start(); y.start();
            if (idx == columns * 3 - 1 || idx == apps.size() - 1) intro = false;
        }
    }
}
