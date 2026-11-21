package app.chamcalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** 위젯 새로 그리기: 앱을 나갈 때, 자정이 지날 때, 30분마다. */
final class WidgetRefresh {
    static final String ACTION_MIDNIGHT = "app.chamcalendar.MIDNIGHT";
    static final String ACTION_ADD = "app.chamcalendar.ADD";

    static void all(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        int[] today = m.getAppWidgetIds(new ComponentName(ctx, TodayWidget.class));
        if (today.length > 0) TodayWidget.draw(ctx, m, today);
        int[] month = m.getAppWidgetIds(new ComponentName(ctx, MonthWidget.class));
        if (month.length > 0) MonthWidget.draw(ctx, m, month);
        int[] duo = m.getAppWidgetIds(new ComponentName(ctx, DuoWidget.class));
        if (duo.length > 0) DuoWidget.draw(ctx, m, duo);
        scheduleMidnight(ctx);
    }

    static PendingIntent openApp(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 위젯의 + 버튼: 앱을 열고 바로 일정 추가 창을 띄워요. */
    static PendingIntent openAdd(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setAction(ACTION_ADD);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, 2, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void scheduleMidnight(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, TodayWidget.class).setAction(ACTION_MIDNIGHT);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 30); c.set(Calendar.MILLISECOND, 0);
        am.set(AlarmManager.RTC, c.getTimeInMillis(), pi);
    }

    private WidgetRefresh() {}
}
