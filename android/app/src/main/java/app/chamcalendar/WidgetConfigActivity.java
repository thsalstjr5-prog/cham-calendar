package app.chamcalendar;

import androidx.appcompat.app.AppCompatActivity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/** 위젯을 꾹 눌러 "설정"을 고르면 열리는 창: 위젯 배경 진하기(투명도). */
public class WidgetConfigActivity extends AppCompatActivity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setResult(RESULT_CANCELED);
        Bundle ex = getIntent().getExtras();
        if (ex != null) widgetId = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }

        setContentView(R.layout.w_config);
        SeekBar bar = findViewById(R.id.cfg_bar);
        TextView val = findViewById(R.id.cfg_val);
        ImageView preview = findViewById(R.id.cfg_preview);
        int pct = CalendarData.bgPercent(this, widgetId);
        bar.setProgress(pct / 5);
        val.setText(pct + "%");
        preview.setImageAlpha(Math.round(pct * 255 / 100f));
        RadioGroup modes = findViewById(R.id.cfg_text_mode);
        TextView sample = findViewById(R.id.cfg_text);
        String mode = CalendarData.textMode(this, widgetId);
        modes.check(mode.equals("dark") ? R.id.cfg_dark : mode.equals("light") ? R.id.cfg_light : R.id.cfg_auto);
        Runnable paint = () -> {
            int sel = modes.getCheckedRadioButtonId();
            boolean dark = sel == R.id.cfg_dark || (sel == R.id.cfg_auto && (bar.getProgress() * 5 >= 50 || WidgetStyle.wallpaperIsLight(this)));
            if (sel == R.id.cfg_auto && bar.getProgress() * 5 >= 50) sample.setTextColor(getResources().getColor(R.color.w_fg, getTheme()));
            else sample.setTextColor(dark ? 0xFF2E2A33 : 0xFFFFFFFF);
        };
        paint.run();
        modes.setOnCheckedChangeListener((g, id) -> paint.run());
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                val.setText((p * 5) + "%");
                preview.setImageAlpha(Math.round(p * 5 * 255 / 100f));
                paint.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        ((Button) findViewById(R.id.cfg_cancel)).setOnClickListener(v -> finish());
        ((Button) findViewById(R.id.cfg_save)).setOnClickListener(v -> {
            CalendarData.setBgPercent(this, widgetId, bar.getProgress() * 5);
            int sel = modes.getCheckedRadioButtonId();
            CalendarData.setTextMode(this, widgetId, sel == R.id.cfg_dark ? "dark" : sel == R.id.cfg_light ? "light" : "auto");
            WidgetRefresh.all(this);
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            finish();
        });
    }
}
