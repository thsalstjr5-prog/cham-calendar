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
        String when; // 기간 일정의 중간·끝 날에 보여줄 시간 글씨
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

    // 날짜 ↔ 날짜 번호 (1970-01-01 = 0)
    static long dn(String k) {
        int y = Integer.parseInt(k.substring(0, 4)), m = Integer.parseInt(k.substring(5, 7)), d = Integer.parseInt(k.substring(8, 10));
        return civil(y, m, d);
    }
    static long civil(int y, int m, int d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
    static String kd(long z) {
        z += 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        if (m <= 2) y++;
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
    }
    static int monthDays(int y, int m) {
        if (m == 2) return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? 29 : 28;
        return (m == 4 || m == 6 || m == 9 || m == 11) ? 30 : 31;
    }

    /** k 날짜에 걸친 회차의 시작 날짜 번호. 없으면 Long.MIN_VALUE. (index.html의 occs()와 같은 규칙) */
    static long occStart(String base, long dur, String repeat, long n) {
        long s0 = dn(base);
        if (n < s0) return Long.MIN_VALUE;
        switch (repeat) {
            case "day": return n;
            case "week": { long st = n - ((n - s0) % 7); return n - st <= dur ? st : Long.MIN_VALUE; }
            case "month": case "year": {
                int y0 = Integer.parseInt(base.substring(0, 4)), m0 = Integer.parseInt(base.substring(5, 7)), dd = Integer.parseInt(base.substring(8, 10));
                String lo = kd(Math.max(s0, n - dur));
                int y = Integer.parseInt(lo.substring(0, 4)), m = Integer.parseInt(lo.substring(5, 7));
                if (repeat.equals("year")) { m = m0; if (y < y0) y = y0; y--; }
                for (int g = 0; g < 600; g++) {
                    if (dd <= monthDays(y, m)) {
                        long st = civil(y, m, dd);
                        if (st > n) break;
                        if (st >= s0 && n <= st + dur) return st;
                    }
                    if (repeat.equals("year")) y++; else { m++; if (m > 12) { m = 1; y++; } }
                }
                return Long.MIN_VALUE;
            }
            default: return n <= s0 + dur ? s0 : Long.MIN_VALUE;
        }
    }

    static Day day(JSONObject all, String k) {
        Day d = new Day();
        long n = dn(k);
        // 반복·기간 일정까지 펼쳐서 오늘(k)에 걸친 것을 모두 모아요
        java.util.Iterator<String> it = all.keys();
        while (it.hasNext()) {
            String sk = it.next();
            if (!sk.matches("\\d{4}-\\d{2}-\\d{2}") || sk.compareTo(k) > 0) continue;
            JSONObject se = all.optJSONObject(sk);
            JSONArray sev = se == null ? null : se.optJSONArray("events");
            if (sev == null) continue;
            for (int i = 0; i < sev.length(); i++) {
                JSONObject v = sev.optJSONObject(i);
                if (v == null || v.optString("title").trim().isEmpty()) continue;
                String end = v.optString("end", "");
                long dur = end.matches("\\d{4}-\\d{2}-\\d{2}") && end.compareTo(sk) > 0 ? dn(end) - dn(sk) : 0;
                long st = occStart(sk, dur, v.optString("repeat", ""), n);
                if (st == Long.MIN_VALUE) continue;
                Event ev = new Event();
                ev.title = v.optString("title").trim();
                ev.color = v.optString("color", "lav");
                String time = v.optString("time", ""), et = v.optString("endTime", "");
                if (st == n) { ev.time = time; }
                else {
                    ev.time = "";
                    ev.when = n == st + dur && et.matches("\\d{2}:\\d{2}") ? "~ " + fmtTime(et) : "하루 종일";
                }
                d.events.add(ev);
            }
        }
        JSONObject e = all.optJSONObject(k);
        // 예전 형식(메모)도 일정처럼 보여줘요
        String memo = e == null ? "" : e.optString("memo", "").trim();
        if (!memo.isEmpty()) { Event ev = new Event(); ev.title = memo; ev.time = ""; ev.color = "lav"; d.events.add(ev); }
        Collections.sort(d.events, (a, b) -> a.time.compareTo(b.time));
        JSONArray todos = e == null ? null : e.optJSONArray("todos");
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

    /** 위젯 배경 진하기 (앱에서 0~100%로 정해요). 0이면 완전 투명. */
    static int bgAlpha(Context ctx) {
        String v = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY + "-wbg", "100");
        try { return Math.max(0, Math.min(255, Math.round(Integer.parseInt(v.trim()) * 255 / 100f))); }
        catch (Exception e) { return 255; }
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
