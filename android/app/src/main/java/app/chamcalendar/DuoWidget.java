package app.chamcalendar;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;

/** 4x2 위젯: 왼쪽은 이번 달 달력, 오른쪽은 오늘부터 일주일 동안의 일정과 할 일. */
public class DuoWidget extends AppWidgetProvider {
    private static final String[] WD = {"일", "월", "화", "수", "목", "금", "토"};
    private static final int LINE_DP = 18;   // 오른쪽 목록 한 줄 높이 (w_drow, w_dhead 와 같게)
    private static final int DAYS = 7;       // 오늘 포함 7일

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        draw(ctx, m, ids);
        WidgetRefresh.scheduleMidnight(ctx);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager m, int id, Bundle opts) {
        draw(ctx, m, new int[]{id});   // 크기를 바꾸면 줄 수를 다시 계산해요
    }

    static String md(Calendar c) {
        return (c.get(Calendar.MONTH) + 1) + "월 " + c.get(Calendar.DAY_OF_MONTH) + "일 (" + WD[c.get(Calendar.DAY_OF_WEEK) - 1] + ")";
    }

    static void draw(Context ctx, AppWidgetManager m, int[] ids) {
        JSONObject all = CalendarData.load(ctx);
        for (int id : ids) {
            Bundle o = m.getAppWidgetOptions(id);
            int h = o == null ? 0 : o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            if (h <= 0) h = 130;
            m.updateAppWidget(id, build(ctx, all, h, id));
        }
    }

    static RemoteViews build(Context ctx, JSONObject all, int heightDp, int widgetId) {
        String pkg = ctx.getPackageName();
        Calendar now = Calendar.getInstance();
        String todayKey = CalendarData.key(now);
        int month = now.get(Calendar.MONTH);
        RemoteViews v = new RemoteViews(pkg, R.layout.w_duo);
        v.setInt(R.id.w_bgimg, "setImageAlpha", CalendarData.bgAlpha(ctx, widgetId));

        // ── 왼쪽: 이번 달 달력 (숫자는 칸 높이에 맞춰 최대한 크게)
        v.setTextViewText(R.id.w_month, (month + 1) + "월");
        v.removeAllViews(R.id.w_grid);
        Calendar c = (Calendar) now.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int lead = c.get(Calendar.DAY_OF_WEEK) - 1;
        int weeks = (int) Math.ceil((lead + c.getActualMaximum(Calendar.DAY_OF_MONTH)) / 7.0);
        c.add(Calendar.DAY_OF_MONTH, -lead);
        float rowDp = (heightDp - 20 - 18 - 14) / (float) weeks;     // 위아래 여백, "11월", 요일 줄 빼고
        float numSp = Math.max(10f, Math.min(15f, rowDp * 0.62f));
        int textOn = ctx.getResources().getColor(R.color.w_on, ctx.getTheme());
        for (int w = 0; w < weeks; w++) {
            RemoteViews row = new RemoteViews(pkg, R.layout.w_week_row);
            for (int i = 0; i < 7; i++) {
                String k = CalendarData.key(c);
                int dow = c.get(Calendar.DAY_OF_WEEK);
                boolean other = c.get(Calendar.MONTH) != month;
                int layout = other ? R.layout.w_mcell_muted
                        : (dow == Calendar.SUNDAY || CalendarData.HOLIDAYS.contains(k)) ? R.layout.w_mcell_peach
                        : dow == Calendar.SATURDAY ? R.layout.w_mcell_sky : R.layout.w_mcell_fg;
                RemoteViews cell = new RemoteViews(pkg, layout);
                cell.setTextViewText(R.id.c_num, String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
                cell.setTextViewTextSize(R.id.c_num, TypedValue.COMPLEX_UNIT_SP, numSp);
                if (k.equals(todayKey)) {
                    cell.setInt(R.id.c_num, "setBackgroundResource", R.drawable.w_today_fill);
                    cell.setTextColor(R.id.c_num, textOn);
                }
                row.addView(R.id.w_week, cell);
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            v.addView(R.id.w_grid, row);
        }

        // ── 오른쪽: 오늘 제목 + 일주일 목록
        v.setTextViewText(R.id.w_today_head, "오늘 " + md(now));
        v.removeAllViews(R.id.w_list);
        int budget = Math.max(2, (heightDp - 20 - 24 - 4) / LINE_DP);   // 들어가는 줄 수
        Calendar d = (Calendar) now.clone();
        for (int day = 0; day < DAYS && budget > 0; day++) {
            CalendarData.Day dd = CalendarData.day(all, CalendarData.key(d));
            int items = dd.events.size() + dd.openTodos.size();
            if (day == 0) {
                if (items == 0) { note(v, pkg, "오늘은 일정이 없어요"); budget--; }
                else budget -= fill(v, pkg, dd, budget);
            } else if (items > 0) {
                if (budget < 2) break;            // 날짜 제목 + 한 줄은 들어가야 보여줘요
                RemoteViews hd = new RemoteViews(pkg, R.layout.w_dhead);
                hd.setTextViewText(R.id.r_text, (day == 1 ? "내일 " : "") + md(d));
                v.addView(R.id.w_list, hd);
                budget--;
                budget -= fill(v, pkg, dd, budget);
            }
            d.add(Calendar.DAY_OF_MONTH, 1);
        }

        v.setOnClickPendingIntent(R.id.w_root, WidgetRefresh.openApp(ctx));
        v.setOnClickPendingIntent(R.id.w_add, WidgetRefresh.openAdd(ctx));
        return v;
    }

    /** 하루치 일정 → 할 일 순서로 넣어요. 자리가 모자라면 마지막 줄을 "외 n개"로. 쓴 줄 수를 돌려줘요. */
    private static int fill(RemoteViews v, String pkg, CalendarData.Day dd, int budget) {
        int total = dd.events.size() + dd.openTodos.size();
        int show = total <= budget ? total : Math.max(0, budget - 1);
        int n = 0;
        for (CalendarData.Event e : dd.events) {
            if (n >= show) break;
            RemoteViews r = new RemoteViews(pkg, R.layout.w_drow);
            r.setImageViewResource(R.id.r_bar, barRes(e.color));
            r.setTextViewText(R.id.r_text, e.title);
            String t = e.when != null ? e.when : e.time.isEmpty() ? "" : CalendarData.fmtTime(e.time);
            r.setTextViewText(R.id.r_time, t.equals("하루 종일") ? "" : t);
            v.addView(R.id.w_list, r);
            n++;
        }
        for (String t : dd.openTodos) {
            if (n >= show) break;
            RemoteViews r = new RemoteViews(pkg, R.layout.w_drow_todo);
            r.setTextViewText(R.id.r_text, t);
            v.addView(R.id.w_list, r);
            n++;
        }
        if (show < total) { note(v, pkg, "외 " + (total - show) + "개"); n++; }
        return n;
    }

    private static void note(RemoteViews v, String pkg, String text) {
        RemoteViews r = new RemoteViews(pkg, R.layout.w_dnote);
        r.setTextViewText(R.id.r_text, text);
        v.addView(R.id.w_list, r);
    }

    static int barRes(String color) {
        switch (color) {
            case "peach": return R.drawable.w_bar_peach;
            case "mint": return R.drawable.w_bar_mint;
            case "sky": return R.drawable.w_bar_sky;
            default: return R.drawable.w_bar_lav;
        }
    }
}
