package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class BrightnessActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        TextView title = new TextView(this);
        title.setText("Kontrol Kecerahan");
        title.setTextSize(20);
        root.addView(title);
        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        try {
            int cur = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            seek.setProgress(cur);
        } catch (Exception ignored) {}
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);
                } catch (Exception ignored) {}
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(seek);
        setContentView(root);
    }
}
