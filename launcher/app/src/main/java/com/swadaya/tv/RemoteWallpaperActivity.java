package com.swadaya.tv;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class RemoteWallpaperActivity extends Activity {

    private static final int PORT = 8080;
    private WebServer server;
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        info = new TextView(this);
        info.setTextSize(16);
        info.setText("Memulai server...");
        root.addView(info);
        setContentView(root);
        startServer();
    }

    private void startServer() {
        try {
            server = new WebServer(this, PORT);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ip = wm != null ? Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()) : "127.0.0.1";
            info.setText("Remote Wallpaper aktif.\nBuka di HP: http://" + ip + ":" + PORT + "\n\nServer akan mati saat keluar.");
        } catch (IOException e) {
            info.setText("Gagal memulai server: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    static class WebServer extends NanoHTTPD {
        private final Context ctx;

        WebServer(Context ctx, int port) {
            super(port);
            this.ctx = ctx;
        }

        @Override
        public Response serve(IHTTPSession session) {
            try {
                if (session.getMethod() == Method.GET) {
                    String html = "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>" +
                            "<h2>SwadayaTV Remote Wallpaper</h2>" +
                            "<form method='POST' enctype='multipart/form-data'>" +
                            "<input type='file' name='file' accept='image/*'><br><br>" +
                            "<button type='submit'>Upload</button></form></body></html>";
                    return newFixedLengthResponse(Response.Status.OK, "text/html", html);
                }
                if (session.getMethod() == Method.POST) {
                    Map<String, String> files = new java.util.HashMap<>();
                    session.parseBody(files);
                    String saved = files.get("file");
                    if (saved != null) {
                        File tmp = new File(saved);
                        File dir = new File(ctx.getFilesDir(), "wallpapers");
                        if (!dir.exists()) dir.mkdirs();
                        File out = new File(dir, "remote_wallpaper.jpg");
                        FileInputStream fis = new FileInputStream(tmp);
                        FileOutputStream fos = new FileOutputStream(out);
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                        fis.close();
                        fos.close();
                        ctx.getSharedPreferences("remote_wallpaper", Context.MODE_PRIVATE)
                                .edit().putString("path", out.getAbsolutePath()).apply();
                        return newFixedLengthResponse(Response.Status.OK, "text/html",
                                "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>" +
                                        "<h2>Upload berhasil!</h2><p>Wallpaper TV diperbarui.</p></body></html>");
                    }
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No file");
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
        }
    }
}
