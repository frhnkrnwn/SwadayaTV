package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AdBlockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        TextView title = new TextView(this);
        title.setText("AdBlock / Private DNS");
        title.setTextSize(20);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Mengaktifkan AdGuard DNS...");
        root.addView(info);
        setContentView(root);
        try {
            Runtime.getRuntime().exec("settings put global private_dns_mode hostname");
            Runtime.getRuntime().exec("settings put global private_dns_specifier dns.adguard-dns.com");
            Toast.makeText(this, "Private DNS diaktifkan (AdGuard)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
