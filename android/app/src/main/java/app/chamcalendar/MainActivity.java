package app.chamcalendar;

import android.os.Bundle;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onPause() {
        super.onPause();
        // 앱에서 고친 내용이 홈 화면 위젯에 바로 보이게
        WidgetRefresh.all(this);
    }
}
