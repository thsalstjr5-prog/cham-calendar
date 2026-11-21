package app.chamcalendar;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

/**
 * 위젯 글씨 색.
 * - 자동: 배경이 진하면(50% 이상) 배경에 맞춘 기본 색, 투명하면 배경화면 밝기를 보고 어두운/밝은 글씨.
 * - 어두운 글씨 / 밝은 글씨: 고른 대로.
 */
final class WidgetStyle {
    final boolean custom;     // false면 레이아웃 기본 색 그대로
    final boolean clearRows;  // 배경이 투명하면 일정 줄 바탕도 빼요
    final int fg, muted, lav, peach, sky, on;

    private WidgetStyle(boolean custom, boolean clearRows, int fg, int muted, int lav, int peach, int sky, int on) {
        this.custom = custom; this.clearRows = clearRows;
        this.fg = fg; this.muted = muted; this.lav = lav; this.peach = peach; this.sky = sky; this.on = on;
    }

    static WidgetStyle of(Context ctx, int widgetId) {
        int pct = CalendarData.bgPercent(ctx, widgetId);
        String mode = CalendarData.textMode(ctx, widgetId);
        boolean clear = pct < 50;
        boolean darkText;
        if (mode.equals("auto")) {
            if (!clear) return new WidgetStyle(false, false, 0, 0, 0, 0, 0, 0);
            darkText = wallpaperIsLight(ctx);
        } else {
            darkText = mode.equals("dark");
        }
        return darkText
                ? new WidgetStyle(true, clear, 0xFF2E2A33, 0xFF5B5560, 0xFF6F5F9C, 0xFFA8634F, 0xFF4E6D96, 0xFFFFFFFF)
                : new WidgetStyle(true, clear, 0xFFFFFFFF, 0xFFEDE6F2, 0xFFE3D8FA, 0xFFF7D3C6, 0xFFD3E2F5, 0xFF2F2A3A);
    }

    /** 배경화면이 밝은지 (밝으면 어두운 글씨가 잘 보여요). 모르면 폰의 다크 모드를 따라요. */
    static boolean wallpaperIsLight(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                WallpaperColors c = WallpaperManager.getInstance(ctx).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (c != null) {
                    if (Build.VERSION.SDK_INT >= 31) return (c.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
                    return Color.luminance(c.getPrimaryColor().toArgb()) > 0.5f;
                }
            }
        } catch (Exception ignored) { }
        int night = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night != Configuration.UI_MODE_NIGHT_YES;
    }

    void text(RemoteViews v, int viewId, int color) { if (custom) v.setTextColor(viewId, color); }

    /** 요일 글자 7개 (일 피치, 토 하늘, 나머지 흐린 색) */
    void weekdays(RemoteViews v) {
        if (!custom) return;
        int[] ids = {R.id.wd0, R.id.wd1, R.id.wd2, R.id.wd3, R.id.wd4, R.id.wd5, R.id.wd6};
        for (int i = 0; i < 7; i++) v.setTextColor(ids[i], i == 0 ? peach : i == 6 ? sky : muted);
    }

    /** 달력 칸 숫자 색 (칸 종류별) */
    int cell(int layout) {
        if (layout == R.layout.w_mcell_peach || layout == R.layout.w_cell_peach) return peach;
        if (layout == R.layout.w_mcell_sky || layout == R.layout.w_cell_sky) return sky;
        if (layout == R.layout.w_mcell_muted || layout == R.layout.w_cell_muted) return muted;
        return fg;
    }
}
