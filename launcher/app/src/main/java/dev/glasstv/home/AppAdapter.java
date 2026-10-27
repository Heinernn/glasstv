package dev.glasstv.home;

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

    private final Context ctx;
    private final int columns;
    private final int spacerHeight;
    private final int cellHeight;
    private final int inset;
    private final int topPad;
    private final int bannerW;
    private final int bannerH;
    private final List<AppEntry> apps = new ArrayList<>();
    private int selected = -1;

    AppAdapter(Context ctx, int columns, int spacerHeight, int cellHeight, int inset, int topPad,
               int bannerW, int bannerH) {
        this.ctx = ctx;
        this.columns = columns;
        this.spacerHeight = spacerHeight;
        this.cellHeight = cellHeight;
        this.inset = inset;
        this.topPad = topPad;
        this.bannerW = bannerW;
        this.bannerH = bannerH;
    }

    void setData(List<AppEntry> list) {
        apps.clear();
        apps.addAll(list);
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
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(index + 1);
        if (vh != null && vh.itemView instanceof TileView) {
            ((TileView) vh.itemView).setLook(index == selected, animate, tilt(index));
        }
    }

    /** Giro lateral (grados): las columnas de la izquierda miran a la derecha y viceversa. */
    private float tilt(int index) {
        float frac = ((index % columns) + 0.5f) / columns - 0.5f;
        return frac * 8f;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_SPACER : TYPE_TILE;
    }

    @Override
    public int getItemCount() {
        return apps.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == TYPE_SPACER) {
            v = new View(ctx);
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacerHeight));
        } else {
            v = new TileView(ctx, inset, topPad, bannerW, bannerH);
            v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight));
        }
        return new RecyclerView.ViewHolder(v) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0) return;
        TileView t = (TileView) holder.itemView;
        t.bind(apps.get(position - 1));
        t.setLook(position - 1 == selected, false, tilt(position - 1));
    }
}
