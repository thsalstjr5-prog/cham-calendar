package app.chamcalendar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 앱이 저장한 기록(Capacitor Preferences)을 위젯에서 읽어요. */
final class CalendarData {
    static final String PREFS = "CapacitorStorage";
    static final String KEY = "cham-calendar-v1";

    static final class Event {
        String title; String time; String color;
    }

    static final class Day {
        final List<Event> events = new ArrayList<>();
        final List<String> openTodos = new ArrayList<>();
        int todoCount;
    }

    // 대한민국 공휴일 (2026–2027, 대체공휴일 포함). index.html 목록과 같아요.
    static final Set<String> HOLIDAYS = new HashSet<>(java.util.Arrays.asList(
        "2026-01-01", "2026-02-16", "2026-02-17", "2026-02-18", "2026-03-01", "2026-03-02", "2026-05-05",
        "2026-05-24", "2026-05-25", "2026-06-03", "2026-06-06", "2026-08-15", "2026-08-17", "2026-09-24",
        "2026-09-25", "2026-09-26", "2026-10-03", "2026-10-05", "2026-10-09", "2026-12-25",
        "2027-01-01", "2027-02-06", "2027-02-07", "2027-02-08", "2027-02-09", "2027-03-01", "2027-05-05",
        "2027-05-13", "2027-06-06", "2027-08-15", "2027-08-16", "2027-09-14", "2027-09-15", "2027-09-16",
        "2027-10-03", "2027-10-04", "2027-10-09", "2027-10-11", "2027-12-25", "2027-12-27"));

    static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static JSONObject load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            return new JSONObject(sp.getString(KEY, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static Day day(JSONObject all, String k) {
        Day d = new Day();
        JSONObject e = all.optJSONObject(k);
        if (e == null) return d;
        JSONArray evs = e.optJSONArray("events");
        if (evs != null) {
            for (int i = 0; i < evs.length(); i++) {
                JSONObject v = evs.optJSONObject(i);
                if (v == null || v.optString("title").trim().isEmpty()) continue;
                Event ev = new Event();
                ev.title = v.optString("title").trim();
                ev.time = v.optString("time", "");
                ev.color = v.optString("color", "lav");
                d.events.add(ev);
            }
        }
        // 예전 형식(메모)도 일정처럼 보여줘요
        String memo = e.optString("memo", "").trim();
        if (!memo.isEmpty()) { Event ev = new Event(); ev.title = memo; ev.time = ""; ev.color = "lav"; d.events.add(ev); }
        Collections.sort(d.events, (a, b) -> {
            String ta = a.time.isEmpty() ? "" : a.time, tb = b.time.isEmpty() ? "" : b.time;
            return ta.compareTo(tb);
        });
        JSONArray todos = e.optJSONArray("todos");
        if (todos != null) {
            for (int i = 0; i < todos.length(); i++) {
                JSONObject t = todos.optJSONObject(i);
                if (t == null || t.optString("text").trim().isEmpty()) continue;
                d.todoCount++;
                if (!t.optBoolean("done")) d.openTodos.add(t.optString("text").trim());
            }
        }
        return d;
    }

    static String fmtTime(String t) {
        if (t == null || !t.matches("\\d{2}:\\d{2}")) return "하루 종일";
        int h = Integer.parseInt(t.substring(0, 2));
        return (h < 12 ? "오전 " : "오후 ") + ((h % 12) == 0 ? 12 : h % 12) + ":" + t.substring(3);
    }

    static int dotRes(String color) {
        switch (color) {
            case "peach": return R.drawable.w_dot_peach;
            case "mint": return R.drawable.w_dot_mint;
            case "sky": return R.drawable.w_dot_sky;
            default: return R.drawable.w_dot_lav;
        }
    }

    private CalendarData() {}
}
