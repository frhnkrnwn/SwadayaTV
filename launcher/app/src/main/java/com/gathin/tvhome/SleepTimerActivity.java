package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SleepTimerActivity extends Activity {
    private TextView status;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        TextView title = new TextView(this);
        title.setText("Sleep Timer");
        title.setTextSize(20);
        root.addView(title);
        status = new TextView(this);
        status.setText("Pilih durasi: 15/30/60 menit");
        root.addView(status);
        // auto 15 menit demo
        startTimer(15 * 60 * 1000L);
        setContentView(root);
    }

    private void startTimer(long ms) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 1000) {
            public void onTick(long millisUntilFinished) {
                status.setText("Sisa: " + (millisUntilFinished / 1000) + " detik");
            }
            public void onFinish() {
                Toast.makeText(SleepTimerActivity.this, "Sleep timer selesai", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
