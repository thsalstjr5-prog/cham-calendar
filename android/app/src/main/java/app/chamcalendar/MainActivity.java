package app.chamcalendar;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onPause() {
        super.onPause();
        // 앱에서 고친 내용이 홈 화면 위젯에 바로 보이게
        WidgetRefresh.all(this);
    }
}
