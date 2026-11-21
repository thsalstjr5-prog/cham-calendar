package app.chamcalendar;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;

/** 오늘 위젯: 오늘 일정과 남은 할 일. */
public class TodayWidget extends AppWidgetProvider {
    private static final String[] WD = {"일", "월", "화", "수", "목", "금", "토"};
    private static final int MAX_ROWS = 6;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        draw(ctx, m, ids);
        WidgetRefresh.scheduleMidnight(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (WidgetRefresh.ACTION_MIDNIGHT.equals(intent.getAction())) WidgetRefresh.all(ctx);
    }

    static void draw(Context ctx, AppWidgetManager m, int[] ids) {
        Calendar now = Calendar.getInstance();
        CalendarData.Day d = CalendarData.day(CalendarData.load(ctx), CalendarData.key(now));
        for (int id : ids) m.updateAppWidget(id, build(ctx, now, d, id));
    }

    static RemoteViews build(Context ctx, Calendar now, CalendarData.Day d, int widgetId) {
        String pkg = ctx.getPackageName();
        WidgetStyle st = WidgetStyle.of(ctx, widgetId);
        RemoteViews v = new RemoteViews(pkg, R.layout.w_today);
        v.setInt(R.id.w_bgimg, "setImageAlpha", CalendarData.bgAlpha(ctx, widgetId));
        v.setTextViewText(R.id.w_date, (now.get(Calendar.MONTH) + 1) + "월 " + now.get(Calendar.DAY_OF_MONTH) + "일 "
                + WD[now.get(Calendar.DAY_OF_WEEK) - 1] + "요일");
        st.text(v, R.id.w_date, st.lav);
        v.setTextViewText(R.id.w_count, d.todoCount == 0 ? "" : d.openTodos.isEmpty() ? "할 일 모두 완료" : "할 일 " + d.openTodos.size() + "개 남음");
        v.setViewVisibility(R.id.w_count, d.todoCount == 0 ? View.GONE : View.VISIBLE);
        st.text(v, R.id.w_count, st.muted);

        v.removeAllViews(R.id.w_rows);
        int rows = 0;
        for (CalendarData.Event e : d.events) {
            if (rows >= MAX_ROWS) break;
            RemoteViews r = new RemoteViews(pkg, R.layout.w_row_event);
            r.setImageViewResource(R.id.r_dot, CalendarData.dotRes(e.color));
            r.setTextViewText(R.id.r_time, e.when != null ? e.when : CalendarData.fmtTime(e.time));
            r.setTextViewText(R.id.r_text, e.title);
            st.text(r, R.id.r_time, st.muted);
            st.text(r, R.id.r_text, st.fg);
            if (st.clearRows) r.setInt(R.id.r_root, "setBackgroundResource", 0);
            v.addView(R.id.w_rows, r);
            rows++;
        }
        for (String t : d.openTodos) {
            if (rows >= MAX_ROWS) break;
            RemoteViews r = new RemoteViews(pkg, R.layout.w_row_todo);
            r.setTextViewText(R.id.r_text, t);
            st.text(r, R.id.r_mark, st.peach);
            st.text(r, R.id.r_text, st.fg);
            v.addView(R.id.w_rows, r);
            rows++;
        }
        int hidden = d.events.size() + d.openTodos.size() - rows;
        if (rows == 0 || hidden > 0) {
            RemoteViews r = new RemoteViews(pkg, R.layout.w_row_note);
            r.setTextViewText(R.id.r_text, rows == 0 ? "오늘은 일정이 없어요" : "외 " + hidden + "개 더 있어요");
            st.text(r, R.id.r_text, st.muted);
            v.addView(R.id.w_rows, r);
        }
        v.setOnClickPendingIntent(R.id.w_root, WidgetRefresh.openApp(ctx));
        return v;
    }
}
