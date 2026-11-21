package app.chamcalendar;

import android.Manifest;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 웹 화면(index.html)에서 삼성 캘린더(폰 공용 달력)를 읽고 쓰는 다리. */
@CapacitorPlugin(
    name = "ChamCal",
    permissions = { @Permission(alias = "calendar", strings = { Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR }) }
)
public class ChamCalPlugin extends Plugin {

    private boolean ok() { return getPermissionState("calendar") == PermissionState.GRANTED; }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", ok());
        call.resolve(r);
    }

    @PluginMethod
    public void request(PluginCall call) {
        if (ok()) { status(call); return; }
        requestPermissionForAlias("calendar", call, "permDone");
    }

    @PermissionCallback
    private void permDone(PluginCall call) { status(call); }

    @PluginMethod
    public void calendars(PluginCall call) {
        if (!ok()) { call.reject("캘린더 권한이 없어요", "NO_PERMISSION"); return; }
        try {
            JSObject r = new JSObject();
            // JSONArray → JSArray (JSArray.from은 자바 배열만 받아서 목록이 비어버렸어요)
            r.put("calendars", new JSArray(SystemCal.calendars(getContext()).toString()));
            call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod
    public void instances(PluginCall call) {
        if (!ok()) { call.reject("캘린더 권한이 없어요", "NO_PERMISSION"); return; }
        String from = call.getString("from"), to = call.getString("to");
        JSArray ids = call.getArray("calendars", new JSArray());
        Set<Long> set = new HashSet<>();
        for (int i = 0; i < ids.length(); i++) set.add(ids.optLong(i));
        List<SystemCal.Ev> list = SystemCal.query(getContext(), from, to, set);
        JSArray out = new JSArray();
        for (SystemCal.Ev v : list) {
            JSObject o = new JSObject();
            o.put("eventId", v.eventId); o.put("calId", v.calId);
            o.put("title", v.title); o.put("place", v.place); o.put("memo", v.memo);
            o.put("allDay", v.allDay); o.put("s", v.s); o.put("e", v.e);
            o.put("time", v.time); o.put("endTime", v.endTime);
            o.put("color", v.color == 0 ? "" : String.format("#%06X", v.color & 0xFFFFFF));
            o.put("rrule", v.rrule); o.put("begin", v.begin); o.put("dtstart", v.dtstart);
            out.put(o);
        }
        JSObject r = new JSObject();
        r.put("events", out);
        call.resolve(r);
    }

    @PluginMethod
    public void save(PluginCall call) {
        if (!ok()) { call.reject("캘린더 권한이 없어요", "NO_PERMISSION"); return; }
        try {
            long id = SystemCal.save(getContext(), call.getData());
            WidgetRefresh.all(getContext());
            JSObject r = new JSObject();
            r.put("eventId", id);
            call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage() == null ? "저장하지 못했어요" : e.getMessage()); }
    }

    @PluginMethod
    public void remove(PluginCall call) {
        if (!ok()) { call.reject("캘린더 권한이 없어요", "NO_PERMISSION"); return; }
        try {
            SystemCal.delete(getContext(), call.getLong("eventId", -1L));
            WidgetRefresh.all(getContext());
            call.resolve();
        } catch (Exception e) { call.reject(e.getMessage() == null ? "지우지 못했어요" : e.getMessage()); }
    }
}
