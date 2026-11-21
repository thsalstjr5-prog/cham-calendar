package app.chamcalendar;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;

/** 4x2 위젯: 왼쪽은 이번 달 달력, 오른쪽은 오늘과 내일 일정. */
public class DuoWidget extends AppWidgetProvider {
    private static final String[] WD = {"일", "월", "화", "수", "목", "금", "토"};
    private static final int MAX_ROWS = 4;   // 오늘 + 내일 합쳐서 보이는 일정 줄 수 (4x2 높이에 맞춤)

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        draw(ctx, m, ids);
        WidgetRefresh.scheduleMidnight(ctx);
    }

    static String head(String word, Calendar c) {
        return word + " " + (c.get(Calendar.MONTH) + 1) + "월 " + c.get(Calendar.DAY_OF_MONTH) + "일 (" + WD[c.get(Calendar.DAY_OF_WEEK) - 1] + ")";
    }

    static void draw(Context ctx, AppWidgetManager m, int[] ids) {
        String pkg = ctx.getPackageName();
        JSONObject all = CalendarData.load(ctx);
        Calendar now = Calendar.getInstance();
        Calendar tom = (Calendar) now.clone();
        tom.add(Calendar.DAY_OF_MONTH, 1);
        String todayKey = CalendarData.key(now);
        int month = now.get(Calendar.MONTH);

        RemoteViews v = new RemoteViews(pkg, R.layout.w_duo);

        // 왼쪽: 이번 달 달력
        v.setTextViewText(R.id.w_month, (month + 1) + "월");
        v.removeAllViews(R.id.w_grid);
        Calendar c = (Calendar) now.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int lead = c.get(Calendar.DAY_OF_WEEK) - 1;
        c.add(Calendar.DAY_OF_MONTH, -lead);
        for (int w = 0; w < 6; w++) {   // 늘 6줄이라 위젯 높이가 달마다 같아요
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
                if (k.equals(todayKey)) {
                    cell.setInt(R.id.c_num, "setBackgroundResource", R.drawable.w_today_fill);
                    cell.setTextColor(R.id.c_num, ctx.getResources().getColor(R.color.w_on, ctx.getTheme()));
                }
                row.addView(R.id.w_week, cell);
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            v.addView(R.id.w_grid, row);
        }

        // 오른쪽: 오늘 / 내일
        CalendarData.Day d1 = CalendarData.day(all, todayKey);
        CalendarData.Day d2 = CalendarData.day(all, CalendarData.key(tom));
        v.setTextViewText(R.id.w_today_head, head("오늘", now));
        v.setTextViewText(R.id.w_tom_head, head("내일", tom));
        // 오늘을 먼저 최대 3줄(넘치면 2줄 + "외 n개"), 남는 줄은 내일에 (내일은 최소 1줄)
        int s1 = d1.events.size();
        int n1 = s1 > 3 ? 2 : s1;
        int used = Math.max(1, n1) + (s1 > n1 ? 1 : 0);
        int n2 = Math.min(d2.events.size(), Math.max(1, MAX_ROWS - used));
        rows(ctx, v, R.id.w_today_rows, d1, n1, "오늘은 일정이 없어요");
        rows(ctx, v, R.id.w_tom_rows, d2, n2, "내일은 일정이 없어요");

        v.setOnClickPendingIntent(R.id.w_root, WidgetRefresh.openApp(ctx));
        v.setOnClickPendingIntent(R.id.w_add, WidgetRefresh.openAdd(ctx));
        for (int id : ids) m.updateAppWidget(id, v);
    }

    private static void rows(Context ctx, RemoteViews v, int target, CalendarData.Day d, int n, String empty) {
        String pkg = ctx.getPackageName();
        v.removeAllViews(target);
        for (int i = 0; i < n; i++) {
            CalendarData.Event e = d.events.get(i);
            RemoteViews r = new RemoteViews(pkg, R.layout.w_drow);
            r.setImageViewResource(R.id.r_bar, barRes(e.color));
            r.setTextViewText(R.id.r_text, e.title);
            String t = e.when != null ? e.when : e.time.isEmpty() ? "" : CalendarData.fmtTime(e.time);
            r.setTextViewText(R.id.r_time, t.equals("하루 종일") ? "" : t);
            v.addView(target, r);
        }
        int hidden = d.events.size() - n;
        if (n == 0 || hidden > 0) {
            RemoteViews r = new RemoteViews(pkg, R.layout.w_dnote);
            String txt = n > 0 ? "외 " + hidden + "개"
                    : !d.events.isEmpty() ? "일정 " + d.events.size() + "개"
                    : !d.openTodos.isEmpty() ? "할 일 " + d.openTodos.size() + "개" : empty;
            r.setTextViewText(R.id.r_text, txt);
            v.addView(target, r);
        }
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
