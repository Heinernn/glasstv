package dev.glasstv.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int ZONE_TILES = 0;
    private static final int ZONE_TOP = 1;
    private static final int ZONE_MENU = 2;

    private static final String PREFS = "home";
    private static final String KEY_LAST = "last_pkg";
    private static final float GLASS_PAD_DP = 5f;
    private static final int MENU_WIDTH_DP = 176;
    private static final int WHITE = 0xFFFFFFFF;

    private static final class InputItem {
        final String id;
        final String label;
        final boolean tuner;
        final int rank;
        final String pkg;

        InputItem(String id, String label, boolean tuner, int rank, String pkg) {
            this.id = id;
            this.label = label;
            this.tuner = tuner;
            this.rank = rank;
            this.pkg = pkg;
        }
    }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private final List<InputItem> inputs = new ArrayList<>();
    private final View[] pills = new View[3];

    private FrameLayout root;
    private GlowView glow;
    private RecyclerView rv;
    private GridLayoutManager lm;
    private AppAdapter adapter;
    private DockBand band;
    private GlassView glass;
    private LinearLayout pillRow;
    private LinearLayout menu;
    private TextView name;
    private TextView wifiLabel;

    private int columns;
    private int dockCount;
    private int zone = ZONE_TILES;
    private int sel;
    private int topSel;
    private int menuSel;
    private int screenW;
    private int screenH;
    private int cellH;
    private int topPad;
    private int bannerW;
    private int bannerH;
    private float scrollFade = 1f;
    private float nameVis;
    private int glowColor = Color.TRANSPARENT;
    private ValueAnimator nameAnim;
    private ValueAnimator glowAnim;
    private boolean stopped = true;

    private final Runnable reload = this::loadApps;

    private final BroadcastReceiver netReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateWifi(); }
    };
    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            ui.removeCallbacks(reload);
            ui.postDelayed(reload, 600);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        columns = Math.max(1, getResources().getInteger(R.integer.columns));
        buildUi();
        setContentView(root);
        updateWifi();
        loadApps();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    // ---------------------------------------------------------------- interfaz

    private void buildUi() {
        int padLR = dp(68);
        int cellW = (screenW - 2 * padLR) / columns;
        int inset = dp(8);
        bannerW = cellW - 2 * inset;
        bannerH = Math.round(bannerW * 3f / 5f);
        topPad = dp(16);
        cellH = topPad + bannerH + dp(40);
        int spacerH = screenH - dp(14) - cellH;

        root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.wallpaper);

        glow = new GlowView(this);
        glow.setCenter(dp(76) + dp(230), spacerH - dp(48), dp(430));
        root.addView(glow, match());

        rv = new RecyclerView(this) {
            @Override
            protected float getBottomFadingEdgeStrength() {
                return 0f;
            }
        };
        rv.setVerticalFadingEdgeEnabled(true);
        rv.setFadingEdgeLength(dp(128));
        lm = new GridLayoutManager(this, columns) {
            @Override
            protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State st, @NonNull int[] extra) {
                extra[0] = screenH;
                extra[1] = screenH;
            }
        };
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) { return position == 0 ? columns : 1; }
        });
        rv.setLayoutManager(lm);
        adapter = new AppAdapter(this, columns, spacerH, cellH, inset, topPad, bannerW, bannerH);
        rv.setAdapter(adapter);
        rv.setPadding(padLR, 0, padLR, 0);
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rv.setItemAnimator(null);
        rv.setFocusable(false);
        rv.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        band = new DockBand(this, columns);
        rv.addItemDecoration(band);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView r, int dx, int dy) { updateScrollFade(); }
            @Override public void onScrollStateChanged(@NonNull RecyclerView r, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && zone == ZONE_TILES && !apps.isEmpty()) {
                    scrollAndGlass(sel, true, false, 0);
                }
            }
        });
        root.addView(rv, match());

        name = new TextView(this);
        Ui.style(name, 44, 250, WHITE);
        name.setSingleLine(true);
        name.setAlpha(0f);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        np.leftMargin = dp(76);
        np.bottomMargin = screenH - spacerH + dp(20);
        root.addView(name, np);

        pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER_VERTICAL);
        String[] texts = {getString(R.string.inputs), getString(R.string.wifi), getString(R.string.settings)};
        int[] icons = {Icons.INPUT, Icons.WIFI, Icons.SETTINGS};
        for (int i = 0; i < 3; i++) {
            pills[i] = pill(icons[i], texts[i], i == 1);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            if (i > 0) lp.leftMargin = dp(12);
            pillRow.addView(pills[i], lp);
        }
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("H:mm");
        clock.setFormat12Hour("H:mm");
        Ui.style(clock, 22, 400, 0xE6FFFFFF);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.leftMargin = dp(22);
        pillRow.addView(clock, clp);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        pr.topMargin = dp(30);
        pr.rightMargin = dp(76);
        root.addView(pillRow, pr);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable mb = new GradientDrawable();
        mb.setColor(0xEB1D1D20);
        mb.setStroke(dp(1), 0x33FFFFFF);
        mb.setCornerRadius(dp(20));
        menu.setBackground(mb);
        menu.setVisibility(View.GONE);
        root.addView(menu, new FrameLayout.LayoutParams(dp(MENU_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START));

        glass = new GlassView(this);
        glass.setVisibility(View.INVISIBLE);
        root.addView(glass, match());
    }

    private LinearLayout pill(int icon, String text, boolean isWifi) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setPadding(dp(16), 0, dp(18), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x26FFFFFF);
        bg.setStroke(dp(1), 0x1FFFFFFF);
        bg.setCornerRadius(dp(19));
        p.setBackground(bg);

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(Icons.of(icon, 0xE6FFFFFF));
        p.addView(iv, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView t = new TextView(this);
        Ui.style(t, 14, 400, 0xF2FFFFFF);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(dp(230));
        t.setText(text);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.leftMargin = dp(8);
        p.addView(t, tp);
        if (isWifi) wifiLabel = t;
        return p;
    }

    // ------------------------------------------------------------------- datos

    private void loadApps() {
        final String[] dock = getResources().getStringArray(R.array.dock_apps);
        final String[] hidden = getResources().getStringArray(R.array.hidden_apps);
        final Context app = getApplicationContext();
        io.execute(() -> {
            final AppRepository.Loaded l = AppRepository.load(app, columns, dock, hidden);
            ui.post(() -> {
                if (!isDestroyed()) applyApps(l);
            });
        });
    }

    private void applyApps(AppRepository.Loaded l) {
        String keep = !apps.isEmpty() && sel < apps.size()
                ? apps.get(sel).pkg
                : getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST, null);
        apps.clear();
        apps.addAll(l.apps);
        dockCount = l.dockCount;
        band.dockCount = dockCount;
        sel = 0;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).pkg.equals(keep)) sel = i;
        }
        adapter.setData(apps);
        adapter.setSelected(zone == ZONE_TILES ? sel : -1, false, rv);
        afterLayout(() -> {
            syncGlass(false);
            updateNameGlow(false);
            updateScrollFade();
        });
    }

    private void updateWifi() {
        String base = networkName();
        wifiLabel.setText(base);
        if (base.equals(getString(R.string.wifi))) {
            io.execute(() -> {
                final String ssid = readSsidFromDump();
                if (ssid == null) return;
                ui.post(() -> {
                    if (isDestroyed()) return;
                    wifiLabel.setText(ssid);
                    relayoutPills();
                });
            });
        }
        relayoutPills();
    }

    private void relayoutPills() {
        pillRow.post(() -> {
            if (zone == ZONE_TOP) syncGlass(false);
        });
    }

    /** Con la ubicacion del sistema apagada Android oculta el SSID; con el permiso DUMP se lee de dumpsys. */
    private String readSsidFromDump() {
        Process p = null;
        try {
            p = new ProcessBuilder("dumpsys", "wifi").redirectErrorStream(true).start();
            Pattern pattern = Pattern.compile("mWifiInfo SSID: (.*?), BSSID");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String s = m.group(1).replace("\"", "");
                        return s.isEmpty() || s.equals("<unknown ssid>") ? null : s;
                    }
                }
            }
        } catch (IOException ignored) {
            // sin permiso DUMP se queda la etiqueta generica
        } finally {
            if (p != null) p.destroy();
        }
        return null;
    }

    private String networkName() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
        if (nc == null) return getString(R.string.no_network);
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return getString(R.string.ethernet);
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
            String ssid = wi == null ? null : wi.getSSID();
            if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>") && !ssid.equals("0x")) {
                return ssid.replace("\"", "");
            }
        }
        return getString(R.string.wifi);
    }

    // ---------------------------------------------------------------- ciclo

    @Override
    protected void onStart() {
        super.onStart();
        stopped = false;
        IntentFilter nf = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        nf.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(netReceiver, nf);
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pf.addDataScheme("package");
        registerReceiver(pkgReceiver, pf);
        updateWifi();
    }

    @Override
    protected void onStop() {
        stopped = true;
        try {
            unregisterReceiver(netReceiver);
            unregisterReceiver(pkgReceiver);
        } catch (IllegalArgumentException ignored) {
            // ya estaban dados de baja
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!stopped) goHome();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void goHome() {
        if (zone == ZONE_MENU) hideMenu();
        zone = ZONE_TILES;
        if (!apps.isEmpty()) selectTile(sel < columns ? sel : sel % columns, true);
    }

    // ---------------------------------------------------------------- mando

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean mine = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
                || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT
                || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BACK;
        if (k == KeyEvent.KEYCODE_SETTINGS) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) activatePill(2);
            return true;
        }
        if (!mine) return super.dispatchKeyEvent(e);
        if (e.getAction() == KeyEvent.ACTION_DOWN) handleKey(k, e.getRepeatCount());
        return true;
    }

    private static boolean isOk(int k) {
        return k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private void handleKey(int k, int repeat) {
        if (zone == ZONE_MENU) menuKey(k, repeat);
        else if (zone == ZONE_TOP) topKey(k, repeat);
        else tilesKey(k, repeat);
    }

    private void tilesKey(int k, int repeat) {
        if (apps.isEmpty()) return;
        if (k == KeyEvent.KEYCODE_DPAD_LEFT) moveTiles(0, -1);
        else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) moveTiles(0, 1);
        else if (k == KeyEvent.KEYCODE_DPAD_DOWN) moveTiles(1, 0);
        else if (k == KeyEvent.KEYCODE_DPAD_UP) moveTiles(-1, 0);
        else if (isOk(k) && repeat == 0) launchSelected();
        else if (k == KeyEvent.KEYCODE_BACK && sel >= columns) selectTile(sel % columns, true);
    }

    private void moveTiles(int dRow, int dCol) {
        int n = apps.size();
        int row = sel / columns;
        int col = sel % columns;
        int lastRow = (n - 1) / columns;
        int next = sel;
        if (dCol != 0) {
            int nc = col + dCol;
            if (nc < 0 || nc >= columns) return;
            next = row * columns + nc;
            if (next >= n) return;
        } else if (dRow > 0) {
            if (row >= lastRow) return;
            next = Math.min((row + 1) * columns + col, n - 1);
        } else if (dRow < 0) {
            if (row == 0) {
                enterTop();
                return;
            }
            next = (row - 1) * columns + col;
        }
        selectTile(next, true);
    }

    private void topKey(int k, int repeat) {
        if (k == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (topSel > 0) {
                topSel--;
                glassToView(pills[topSel], 4, -1, true);
            }
        } else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (topSel < pills.length - 1) {
                topSel++;
                glassToView(pills[topSel], 4, -1, true);
            }
        } else if (k == KeyEvent.KEYCODE_DPAD_DOWN || k == KeyEvent.KEYCODE_BACK) {
            if (!apps.isEmpty()) selectTile(sel, true);
        } else if (isOk(k) && repeat == 0) {
            activatePill(topSel);
        }
    }

    private void menuKey(int k, int repeat) {
        if (k == KeyEvent.KEYCODE_DPAD_UP) {
            if (menuSel > 0) {
                menuSel--;
                glassToMenuRow(true);
            }
        } else if (k == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (menuSel < inputs.size() - 1) {
                menuSel++;
                glassToMenuRow(true);
            }
        } else if (k == KeyEvent.KEYCODE_BACK) {
            closeMenu();
        } else if (isOk(k) && repeat == 0) {
            if (menuSel < inputs.size()) {
                InputItem it = inputs.get(menuSel);
                closeMenu();
                switchInput(it);
            }
        }
    }

    // ------------------------------------------------------------ seleccion

    private void selectTile(int idx, boolean animate) {
        zone = ZONE_TILES;
        sel = idx;
        adapter.setSelected(idx, animate, rv);
        scrollAndGlass(idx, animate, true, 0);
        updateNameGlow(animate);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LAST, apps.get(idx).pkg).apply();
    }

    private void enterTop() {
        zone = ZONE_TOP;
        topSel = 0;
        adapter.setSelected(-1, true, rv);
        glassToView(pills[topSel], 4, -1, true);
        updateNameGlow(true);
    }

    private void syncGlass(boolean animate) {
        if (zone == ZONE_TILES) {
            if (!apps.isEmpty()) scrollAndGlass(sel, animate, true, 0);
        } else if (zone == ZONE_TOP) {
            glassToView(pills[topSel], 4, -1, animate);
        } else {
            glassToMenuRow(animate);
        }
    }

    private int scrollOffset() {
        View spacer = lm.findViewByPosition(0);
        return spacer != null ? -spacer.getTop() : rv.computeVerticalScrollOffset();
    }

    /** Coloca el cristal sobre la tile y, si hace falta, desplaza la lista exactamente lo necesario. */
    private void scrollAndGlass(final int idx, boolean animate, final boolean plan, final int attempt) {
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(idx + 1);
        if (vh == null) {
            if (attempt < 3) {
                lm.scrollToPosition(idx + 1);
                afterLayout(() -> scrollAndGlass(idx, false, plan, attempt + 1));
            }
            return;
        }
        View v = vh.itemView;
        int dy = 0;
        if (plan) {
            int bottomLimit = screenH - dp(14);
            int topLimit = dp(96);
            if (idx < columns) dy = -scrollOffset();
            else if (v.getBottom() > bottomLimit) dy = v.getBottom() - bottomLimit;
            else if (v.getTop() < topLimit) dy = v.getTop() - topLimit;
        }
        float pad = dp(GLASS_PAD_DP);
        float cx = v.getLeft() + v.getWidth() / 2f;
        float cy = v.getTop() + topPad + bannerH / 2f - dy;
        glassTo(cx, cy, bannerW * TileView.SCALE + 2 * pad, bannerH * TileView.SCALE + 2 * pad,
                dp(10) * TileView.SCALE + pad, animate);
        if (dy != 0) {
            if (animate) rv.smoothScrollBy(0, dy, new DecelerateInterpolator(1.5f), 360);
            else rv.scrollBy(0, dy);
        }
    }

    private void glassTo(float cx, float cy, float w, float h, float r, boolean animate) {
        glass.moveTo(cx, cy, w, h, r, animate);
        glass.setVisibility(View.VISIBLE);
    }

    private void glassToView(View v, float padDp, float radiusDp, boolean animate) {
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        float pad = dp(padDp);
        float w = v.getWidth() + 2 * pad;
        float h = v.getHeight() + 2 * pad;
        float r = radiusDp < 0 ? h / 2f : dp(radiusDp);
        glassTo(loc[0] + v.getWidth() / 2f, loc[1] + v.getHeight() / 2f, w, h, r, animate);
    }

    private void glassToMenuRow(boolean animate) {
        if (menuSel < menu.getChildCount()) glassToView(menu.getChildAt(menuSel), 0, 12, animate);
    }

    private void afterLayout(final Runnable r) {
        final ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver o = root.getViewTreeObserver();
                if (o.isAlive()) o.removeOnGlobalLayoutListener(this);
                r.run();
            }
        });
        root.requestLayout();
    }

    // ---------------------------------------------------- nombre, brillo, fade

    private void updateScrollFade() {
        View spacer = lm.findViewByPosition(0);
        float off = spacer == null ? cellH * 2f : Math.max(0, -spacer.getTop());
        scrollFade = Math.max(0f, Math.min(1f, 1f - off / (0.6f * cellH)));
        applyAlphas();
    }

    private void applyAlphas() {
        name.setAlpha(nameVis * scrollFade);
        glow.setRadialStrength(nameVis * scrollFade);
    }

    private void updateNameGlow(boolean animate) {
        boolean show = zone == ZONE_TILES && !apps.isEmpty() && sel < dockCount;
        if (zone == ZONE_TILES && !apps.isEmpty()) tintGlow(apps.get(sel).glow, animate);
        if (show) name.setText(apps.get(sel).label);
        animateNameVis(show ? 1f : 0f, animate);
    }

    private void tintGlow(int target, boolean animate) {
        if (glowAnim != null) glowAnim.cancel();
        if (!animate || glowColor == Color.TRANSPARENT) {
            glowColor = target;
            glow.setGlowColor(target);
            return;
        }
        glowAnim = ValueAnimator.ofObject(new ArgbEvaluator(), glowColor, target);
        glowAnim.setDuration(300);
        glowAnim.addUpdateListener(a -> {
            glowColor = (int) a.getAnimatedValue();
            glow.setGlowColor(glowColor);
        });
        glowAnim.start();
    }

    private void animateNameVis(float to, boolean animate) {
        if (nameAnim != null) nameAnim.cancel();
        if (!animate) {
            nameVis = to;
            applyAlphas();
            return;
        }
        nameAnim = ValueAnimator.ofFloat(nameVis, to);
        nameAnim.setDuration(220);
        nameAnim.addUpdateListener(a -> {
            nameVis = (float) a.getAnimatedValue();
            applyAlphas();
        });
        nameAnim.start();
    }

    // ----------------------------------------------------------- acciones

    private void launchSelected() {
        try {
            startActivity(apps.get(sel).launchIntent());
        } catch (RuntimeException ex) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void activatePill(int i) {
        if (i == 0) {
            openMenu();
            return;
        }
        try {
            startActivity(new Intent(i == 1 ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ex) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void openMenu() {
        inputs.clear();
        inputs.addAll(readInputs());
        menu.removeAllViews();
        if (inputs.isEmpty()) menu.addView(menuRow(getString(R.string.no_inputs)));
        for (InputItem it : inputs) menu.addView(menuRow(it.label));
        menuSel = 0;
        zone = ZONE_MENU;

        int[] loc = new int[2];
        pills[0].getLocationInWindow(loc);
        int w = dp(MENU_WIDTH_DP);
        menu.setTranslationX(Math.max(dp(16), loc[0] + pills[0].getWidth() - w));
        menu.setTranslationY(loc[1] + pills[0].getHeight() + dp(10));
        menu.setAlpha(0f);
        menu.setVisibility(View.VISIBLE);
        menu.animate().alpha(1f).setDuration(160).start();
        afterLayout(() -> glassToMenuRow(false));
    }

    private View menuRow(String text) {
        TextView t = new TextView(this);
        Ui.style(t, 15, 400, WHITE);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(16), 0, dp(16), 0);
        t.setText(text);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        return t;
    }

    private void closeMenu() {
        hideMenu();
        zone = ZONE_TOP;
        glassToView(pills[topSel], 4, -1, true);
    }

    private void hideMenu() {
        menu.animate().cancel();
        menu.setVisibility(View.GONE);
    }

    private List<InputItem> readInputs() {
        List<InputItem> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        boolean tuner = false;
        for (TvInputInfo info : tim.getTvInputList()) {
            int type = info.getType();
            if (type == TvInputInfo.TYPE_TUNER) {
                if (!tuner) {
                    tuner = true;
                    out.add(new InputItem(info.getId(), getString(R.string.live_tv), true, 3, null));
                }
                continue;
            }
            if (!info.isPassthroughInput()) continue;
            if (type == TvInputInfo.TYPE_HDMI && info.getParentId() != null) continue;
            CharSequence custom = info.loadCustomLabel(this);
            CharSequence label = custom != null && custom.length() > 0 ? custom : info.loadLabel(this);
            out.add(new InputItem(info.getId(), String.valueOf(label), false,
                    type == TvInputInfo.TYPE_HDMI ? 0 : 2, null));
        }
        String receiver = getString(R.string.airplay_receiver);
        if (getPackageManager().getLeanbackLaunchIntentForPackage(receiver) != null) {
            out.add(new InputItem(null, getString(R.string.iphone), false, 1, receiver));
        }
        Collections.sort(out, (a, b) -> a.rank != b.rank ? a.rank - b.rank : a.label.compareToIgnoreCase(b.label));
        return out;
    }

    private void switchInput(InputItem it) {
        try {
            if (it.pkg != null) {
                Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(it.pkg);
                if (launch == null) {
                    Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            } else if (it.tuner) {
                openLiveTv();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(it.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (RuntimeException ex) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void openLiveTv() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException first) {
            for (AppEntry a : apps) {
                if (a.pkg.toLowerCase(Locale.ROOT).contains("livetv")) {
                    startActivity(a.launchIntent());
                    return;
                }
            }
            throw first;
        }
    }
}
