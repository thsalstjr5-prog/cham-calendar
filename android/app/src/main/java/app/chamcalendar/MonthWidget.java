package app.chamcalendar;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;

/** 이번 달 위젯: 월 달력과 일정 있는 날 점 표시. */
public class MonthWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        draw(ctx, m, ids);
        WidgetRefresh.scheduleMidnight(ctx);
    }

    static void draw(Context ctx, AppWidgetManager m, int[] ids) {
        String pkg = ctx.getPackageName();
        JSONObject all = CalendarData.load(ctx);
        Calendar now = Calendar.getInstance();
        String todayKey = CalendarData.key(now);
        int month = now.get(Calendar.MONTH);

        RemoteViews v = new RemoteViews(pkg, R.layout.w_month);
        v.setTextViewText(R.id.w_month, (month + 1) + "월");
        v.setTextViewText(R.id.w_year, String.valueOf(now.get(Calendar.YEAR)));
        v.removeAllViews(R.id.w_grid);

        Calendar c = (Calendar) now.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int lead = c.get(Calendar.DAY_OF_WEEK) - 1;
        int weeks = (int) Math.ceil((lead + c.getActualMaximum(Calendar.DAY_OF_MONTH)) / 7.0);
        c.add(Calendar.DAY_OF_MONTH, -lead);

        for (int w = 0; w < weeks; w++) {
            RemoteViews row = new RemoteViews(pkg, R.layout.w_week_row);
            for (int i = 0; i < 7; i++) {
                String k = CalendarData.key(c);
                int dow = c.get(Calendar.DAY_OF_WEEK);
                boolean other = c.get(Calendar.MONTH) != month;
                int layout = other ? R.layout.w_cell_muted
                        : (dow == Calendar.SUNDAY || CalendarData.HOLIDAYS.contains(k)) ? R.layout.w_cell_peach
                        : dow == Calendar.SATURDAY ? R.layout.w_cell_sky : R.layout.w_cell_fg;
                RemoteViews cell = new RemoteViews(pkg, layout);
                cell.setTextViewText(R.id.c_num, String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
                if (k.equals(todayKey)) cell.setInt(R.id.c_num, "setBackgroundResource", R.drawable.w_ring);
                if (!other) {
                    CalendarData.Day d = CalendarData.day(all, k);
                    if (!d.events.isEmpty()) {
                        cell.setImageViewResource(R.id.c_dot, CalendarData.dotRes(d.events.get(0).color));
                        cell.setViewVisibility(R.id.c_dot, View.VISIBLE);
                    } else if (!d.openTodos.isEmpty()) {
                        cell.setImageViewResource(R.id.c_dot, R.drawable.w_dot_peach);
                        cell.setViewVisibility(R.id.c_dot, View.VISIBLE);
                    }
                }
                row.addView(R.id.w_week, cell);
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            v.addView(R.id.w_grid, row);
        }
        v.setOnClickPendingIntent(R.id.w_root, WidgetRefresh.openApp(ctx));
        for (int id : ids) {
            v.setInt(R.id.w_bgimg, "setImageAlpha", CalendarData.bgAlpha(ctx, id));
            m.updateAppWidget(id, v);
        }
    }
}
