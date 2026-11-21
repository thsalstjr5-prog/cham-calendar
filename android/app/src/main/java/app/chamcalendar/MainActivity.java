package app.chamcalendar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.OnBackPressedCallback;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        takeWidgetAction(getIntent());
        registerPlugin(ChamCalPlugin.class);   // 삼성 캘린더(폰 공용 달력) 연동
        super.onCreate(savedInstanceState);
        // 뒤로가기: 열린 창(팝업·추가 창)이 있으면 그것만 닫고, 첫 화면이면 앱을 끄지 않고 홈으로 내려가요
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getBridge() == null || getBridge().getWebView() == null) { moveTaskToBack(true); return; }
                getBridge().getWebView().evaluateJavascript("(window.chamBack && chamBack()) ? '1' : '0'", r -> {
                    if (!"\"1\"".equals(r)) moveTaskToBack(true);
                });
            }
        });
        // 자판이 올라오면 화면을 자판 위까지만 쓰도록 (자판이 목록을 덮지 않게)
        if (getBridge() == null || getBridge().getWebView() == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(getBridge().getWebView(), (v, wi) -> {
            Insets bars = wi.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = wi.getInsets(WindowInsetsCompat.Type.ime());
            MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = bars.left;
            mlp.topMargin = bars.top;
            mlp.rightMargin = bars.right;
            mlp.bottomMargin = Math.max(bars.bottom, ime.bottom);
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (takeWidgetAction(intent) && getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript("window.chamCheckOpen && chamCheckOpen()", null));
        }
    }

    /** 위젯의 + 버튼으로 열렸으면 앱(웹 화면)이 읽을 수 있게 표시해 둬요. */
    private boolean takeWidgetAction(Intent intent) {
        if (intent == null || !WidgetRefresh.ACTION_ADD.equals(intent.getAction())) return false;
        getSharedPreferences(CalendarData.PREFS, Context.MODE_PRIVATE).edit().putString(CalendarData.KEY + "-open", "add").commit();
        intent.setAction(Intent.ACTION_MAIN);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        // 앱에서 고친 내용이 홈 화면 위젯에 바로 보이게
        WidgetRefresh.all(this);
    }
}
