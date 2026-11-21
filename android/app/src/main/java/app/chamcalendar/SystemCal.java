package app.chamcalendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * 폰의 공용 달력 저장소(삼성 캘린더·구글 캘린더가 쓰는 곳)를 읽고 써요.
 * 앱(ChamCalPlugin)과 홈 화면 위젯이 함께 써요.
 */
final class SystemCal {
    static final long DAY = 86400000L;
    private static final int JULIAN_EPOCH = 2440588;   // 1970-01-01의 율리우스일

    static final class Ev {
        long eventId, calId, begin, end, dtstart;
        boolean allDay;
        String title = "", place = "", memo = "", rrule = "", s, e, time = "", endTime = "";
        int color;
    }

    static boolean granted(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    /** 연동 설정 (앱의 설정 창에서 저장): {on, show:[캘린더 id], target} */
    static JSONObject settings(Context ctx) {
        try {
            String v = ctx.getSharedPreferences(CalendarData.PREFS, Context.MODE_PRIVATE).getString(CalendarData.KEY + "-sys", "{}");
            return new JSONObject(v);
        } catch (Exception e) { return new JSONObject(); }
    }

    /** 위젯용: 연동이 켜져 있고 권한이 있으면 보여줄 캘린더 id, 아니면 null */
    static Set<Long> shownCalendars(Context ctx) {
        JSONObject s = settings(ctx);
        if (!s.optBoolean("on") || !granted(ctx)) return null;
        Set<Long> ids = new HashSet<>();
        JSONArray a = s.optJSONArray("show");
        if (a != null) for (int i = 0; i < a.length(); i++) ids.add(a.optLong(i));
        return ids;
    }

    static long floorDiv(long a, long b) { long q = a / b; if ((a % b != 0) && ((a < 0) != (b < 0))) q--; return q; }
    static String utcKey(long ms) { return CalendarData.kd(floorDiv(ms, DAY)); }
    static String localKey(Calendar c) { return CalendarData.key(c); }
    static String hm(Calendar c) { return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)); }

    /** fromKey ~ toKey 사이에 걸친 일정 (반복은 날짜마다 펼쳐서). calIds가 비어 있으면 아무것도 안 보여줘요. */
    static List<Ev> query(Context ctx, String fromKey, String toKey, Set<Long> calIds) {
        List<Ev> out = new ArrayList<>();
        if (!granted(ctx) || calIds == null || calIds.isEmpty()) return out;
        Uri.Builder b = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        ContentUris.appendId(b, CalendarData.dn(fromKey) + JULIAN_EPOCH - 1);
        ContentUris.appendId(b, CalendarData.dn(toKey) + JULIAN_EPOCH + 1);
        String[] proj = {
            CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION, CalendarContract.Instances.CALENDAR_ID, CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.RRULE, CalendarContract.Instances.DTSTART
        };
        try (Cursor c = ctx.getContentResolver().query(b.build(), proj, null, null, CalendarContract.Instances.BEGIN + " ASC")) {
            if (c == null) return out;
            Calendar lc = Calendar.getInstance();
            while (c.moveToNext()) {
                long cal = c.getLong(7);
                if (!calIds.contains(cal)) continue;
                Ev v = new Ev();
                v.eventId = c.getLong(0);
                v.title = c.isNull(1) ? "" : c.getString(1);
                v.begin = c.getLong(2);
                v.end = c.getLong(3);
                v.allDay = c.getInt(4) != 0;
                v.place = c.isNull(5) ? "" : c.getString(5);
                v.memo = c.isNull(6) ? "" : c.getString(6);
                v.calId = cal;
                v.color = c.isNull(8) ? 0 : (c.getInt(8) | 0xFF000000);
                v.rrule = c.isNull(9) ? "" : c.getString(9);
                v.dtstart = c.isNull(10) ? v.begin : c.getLong(10);
                if (v.allDay) {
                    v.s = utcKey(v.begin);
                    v.e = utcKey(Math.max(v.begin, v.end - 1));
                } else {
                    lc.setTimeInMillis(v.begin); v.s = localKey(lc); v.time = hm(lc);
                    lc.setTimeInMillis(Math.max(v.begin, v.end - 1)); v.e = localKey(lc);
                    lc.setTimeInMillis(v.end); v.endTime = v.end > v.begin ? hm(lc) : "";
                }
                if (v.e.compareTo(fromKey) < 0 || v.s.compareTo(toKey) > 0) continue;
                if (v.title.trim().isEmpty()) v.title = "(제목 없음)";
                out.add(v);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static JSONArray calendars(Context ctx) {
        JSONArray out = new JSONArray();
        if (!granted(ctx)) return out;
        String[] proj = {
            CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.Calendars.CALENDAR_COLOR, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        };
        try (Cursor c = ctx.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, proj, null, null, null)) {
            if (c == null) return out;
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", c.getLong(0));
                o.put("name", c.isNull(1) ? "" : c.getString(1));
                o.put("account", c.isNull(2) ? "" : c.getString(2));
                o.put("type", c.isNull(3) ? "" : c.getString(3));
                o.put("color", String.format("#%06X", (c.getInt(4) & 0xFFFFFF)));
                o.put("writable", c.getInt(5) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR);
                o.put("visible", c.getInt(6) != 0);
                out.put(o);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static String accountType(ContentResolver cr, long calId) {
        try (Cursor c = cr.query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId),
                new String[]{CalendarContract.Calendars.ACCOUNT_TYPE}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.isNull(0) ? "" : c.getString(0);
        } catch (Exception ignored) { }
        return "";
    }

    private static long localMs(String k, String t) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(k.substring(0, 4)), Integer.parseInt(k.substring(5, 7)) - 1, Integer.parseInt(k.substring(8, 10)),
                t.length() == 5 ? Integer.parseInt(t.substring(0, 2)) : 0, t.length() == 5 ? Integer.parseInt(t.substring(3, 5)) : 0, 0);
        return c.getTimeInMillis();
    }

    /** 일정 저장 (eventId가 있으면 고치기, 없으면 새로). 저장된 eventId를 돌려줘요. */
    static long save(Context ctx, JSONObject o) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        long eventId = o.optLong("eventId", -1);
        long calId = o.optLong("calendarId", -1);
        boolean allDay = o.optBoolean("allDay");
        String sK = o.getString("sK"), eK = o.optString("eK", sK), sT = o.optString("sT", ""), eT = o.optString("eT", "");
        if (eK.compareTo(sK) < 0) eK = sK;
        String rule = o.optString("rrule", "").trim();
        long start, end;
        String tz;
        if (allDay) {
            start = CalendarData.dn(sK) * DAY;
            end = (CalendarData.dn(eK) + 1) * DAY;
            tz = "UTC";
        } else {
            start = localMs(sK, sT);
            end = eT.length() == 5 ? localMs(eK, eT) : start + 3600000L;
            if (end <= start) end = start + 3600000L;
            tz = TimeZone.getDefault().getID();
        }
        ContentValues v = new ContentValues();
        v.put(CalendarContract.Events.TITLE, o.optString("title", ""));
        v.put(CalendarContract.Events.EVENT_LOCATION, o.optString("place", ""));
        v.put(CalendarContract.Events.DESCRIPTION, o.optString("memo", ""));
        v.put(CalendarContract.Events.ALL_DAY, allDay ? 1 : 0);
        v.put(CalendarContract.Events.EVENT_TIMEZONE, tz);
        if (rule.isEmpty()) {
            v.put(CalendarContract.Events.DTSTART, start);
            v.put(CalendarContract.Events.DTEND, end);
            v.putNull(CalendarContract.Events.RRULE);
            v.putNull(CalendarContract.Events.DURATION);
        } else {
            long dtstart = start;
            // 반복 일정을 고칠 때: 고른 날(회차)이 옮겨진 만큼 반복 전체의 시작을 옮겨요
            if (eventId > 0 && o.optBoolean("wasRecurring") && o.has("origBegin") && o.has("origDtstart")) {
                dtstart = o.getLong("origDtstart") + (start - o.getLong("origBegin"));
            }
            v.put(CalendarContract.Events.DTSTART, dtstart);
            v.putNull(CalendarContract.Events.DTEND);
            v.put(CalendarContract.Events.RRULE, rule);
            v.put(CalendarContract.Events.DURATION, allDay ? "P" + Math.max(1, (end - start) / DAY) + "D" : "P" + ((end - start) / 1000) + "S");
        }
        long target = eventId > 0 ? o.optLong("calId", calId) : calId;
        String color = o.optString("color", "");
        if (color.matches("#[0-9a-fA-F]{6}") && !accountType(cr, target).contains("google")) {
            v.put(CalendarContract.Events.EVENT_COLOR, 0xFF000000 | Integer.parseInt(color.substring(1), 16));
        }
        if (eventId > 0) {
            cr.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), v, null, null);
            return eventId;
        }
        if (calId < 0) throw new Exception("저장할 캘린더가 없어요");
        v.put(CalendarContract.Events.CALENDAR_ID, calId);
        Uri u = cr.insert(CalendarContract.Events.CONTENT_URI, v);
        if (u == null) throw new Exception("저장하지 못했어요");
        return ContentUris.parseId(u);
    }

    static void delete(Context ctx, long eventId) {
        ctx.getContentResolver().delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), null, null);
    }

    private SystemCal() {}
}
