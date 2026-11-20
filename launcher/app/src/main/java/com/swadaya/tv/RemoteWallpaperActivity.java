package com.swadaya.tv;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Settings → Home screen → Remote Wallpaper: a QR code and a code on the TV, and a small page for
 * a phone on the same network that takes a photo, checks the code and saves it to the Wallpapers
 * folder and sets it. The code is what stops anyone else on the Wi-Fi (or a camera that read the
 * QR across the room) from changing the wallpaper: five wrong tries put a new code on the TV.
 */
public class RemoteWallpaperActivity extends Activity {
    static final int PORT = 8080;
    private static final long MAX_UPLOAD = 24L * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private volatile String code = "";
    private int wrong;
    private WebServer server;
    private TextView codeView;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newCode();
        setContentView(build());
        startServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // OK on the TV remote: a fresh code, for when the phone's page was left open too long.
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            newCode();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---------------------------------------------------------------- screen

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(0xFF0B0F0C);
        root.setPadding(dp(56), dp(32), dp(48), dp(32));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        root.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        text.addView(label("WALLPAPER · FROM YOUR PHONE", 12, 0xFF8FA096, 0.14f, false));
        TextView title = label("Put your own photo behind the home screen", 30, 0xFFF2F5F0, 0f, true);
        title.setPadding(0, dp(10), 0, 0);
        text.addView(title);

        TextView steps = label("Scan the QR code with your phone's camera, or open the address, "
                + "on the same Wi-Fi or network as the TV. Then enter the code below and pick a picture.",
                16, 0xFFB9C4BC, 0f, false);
        steps.setLineSpacing(dp(6), 1f);
        steps.setPadding(0, dp(14), 0, dp(4));
        text.addView(steps);

        String ip = RemoteServer.lanIp(this);
        TextView url = label(ip == null ? "No network address: connect the TV to Wi-Fi or Ethernet."
                : "http://" + ip + ":" + PORT, 22, 0xFF7FD4A0, 0f, true);
        url.setPadding(0, dp(14), 0, dp(22));
        text.addView(url);

        text.addView(label("CODE ON THIS TV", 12, 0xFF8FA096, 0.14f, false));
        codeView = label(code, 64, 0xFFFFFFFF, 0.18f, true);
        codeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        codeView.setPadding(0, dp(2), 0, dp(10));
        text.addView(codeView);

        TextView hint = label("Press OK on the TV remote for a new code.", 14, 0xFF8FA096, 0f, false);
        text.addView(hint);

        status = label("", 15, 0xFFE0A96D, 0f, false);
        status.setLineSpacing(dp(4), 1f);
        status.setPadding(0, dp(16), 0, 0);
        if (!FileApi.hasAllFilesAccess()) {
            status.setText("All files access is off. Turn it on in Settings → About → All files access, "
                    + "or the TV can't save pictures to its Wallpapers folder.");
        }
        text.addView(status);

        QrView qr = new QrView(this);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(dp(400), dp(400));
        qlp.leftMargin = dp(40);
        root.addView(qr, qlp);
        if (ip == null || !qr.setText("http://" + ip + ":" + PORT)) qr.setVisibility(View.INVISIBLE);
        return root;
    }

    private TextView label(String s, int sp, int color, float letterSpacing, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setLetterSpacing(letterSpacing);
        if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------- code

    private void newCode() {
        code = String.format(Locale.US, "%06d", RANDOM.nextInt(1_000_000));
        synchronized (this) {
            wrong = 0;
        }
        if (codeView != null) codeView.setText(code);
    }

    /** 0: right, 1: wrong, 2: five wrong tries (a new code is on the TV now). */
    synchronized int check(String given) {
        if (given != null && given.trim().equals(code)) {
            wrong = 0;
            return 0;
        }
        if (++wrong >= 5) {
            runOnUiThread(this::newCode);
            return 2;
        }
        return 1;
    }

    // ---------------------------------------------------------------- server

    private void startServer() {
        try {
            server = new WebServer(this);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            status.setText("Couldn't start the upload page (port " + PORT + " is in use). "
                    + "Close the phone remote's wallpaper page and open this screen again.");
            server = null;
        }
    }

    static class WebServer extends NanoHTTPD {
        private final RemoteWallpaperActivity owner;

        WebServer(RemoteWallpaperActivity owner) {
            super(PORT);
            this.owner = owner;
        }

        @Override
        public Response serve(IHTTPSession session) {
            try {
                if (session.getMethod() == Method.GET) return page(Response.Status.OK, form(null));
                if (session.getMethod() != Method.POST) {
                    return page(Response.Status.METHOD_NOT_ALLOWED, form("Use the form on this page."));
                }
                return upload(session);
            } catch (Exception e) {
                return page(Response.Status.INTERNAL_ERROR,
                        form("The TV couldn't finish that: " + e.getMessage()));
            }
        }

        private Response upload(IHTTPSession session) throws IOException, ResponseException {
            long size = -1;
            try {
                size = Long.parseLong(session.getHeaders().get("content-length"));
            } catch (Exception ignored) {
            }
            if (size > MAX_UPLOAD) {
                return page(Response.Status.BAD_REQUEST,
                        form("That picture is too big. Use one under 24 MB."));
            }
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();
            int check = owner.check(params.get("code"));
            if (check != 0) {
                return page(Response.Status.FORBIDDEN, form(check == 2
                        ? "Too many wrong codes. A new code is on the TV."
                        : "That code doesn't match the one on the TV."));
            }
            String temp = files.get("picture");
            if (temp == null) return page(Response.Status.BAD_REQUEST, form("Pick a picture first."));
            File tmp = new File(temp);
            if (!tmp.isFile() || tmp.length() == 0) {
                return page(Response.Status.BAD_REQUEST, form("Pick a picture first."));
            }
            if (extOf(tmp) == null) {
                return page(Response.Status.BAD_REQUEST,
                        form("That isn't a JPEG, PNG or WebP picture. Try another one."));
            }
            if (!FileApi.hasAllFilesAccess()) {
                return page(Response.Status.FORBIDDEN, form("The TV can't write its Wallpapers folder yet. "
                        + "On the TV open Settings → About → All files access, turn it on, then try again."));
            }
            File dir = Wallpapers.folder();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return page(Response.Status.INTERNAL_ERROR, form("The TV couldn't open its Wallpapers folder."));
            }
            String name = save(tmp, dir, params.get("name"));
            if (name == null) {
                return page(Response.Status.INTERNAL_ERROR, form("The TV couldn't save that picture."));
            }
            owner.runOnUiThread(() -> LauncherSettings.setWallpaper(owner, Wallpapers.FILE + name));
            return page(Response.Status.OK, done(name));
        }

        /**
         * Copies the upload into the Wallpapers folder under a safe, unique name ("holiday.jpg",
         * then "holiday-2.jpg", …); null when the copy fails.
         */
        private static String save(File tmp, File dir, String rawName) {
            String ext = extOf(tmp);
            String base = rawName == null ? "" : rawName.trim();
            int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
            if (slash >= 0) base = base.substring(slash + 1);
            base = base.toLowerCase(Locale.US).replaceAll("\\.[a-z0-9]+$", "")
                    .replaceAll("[^a-z0-9._-]+", "-").replaceAll("^[.-]+|-+$", "");
            if (base.length() > 48) base = base.substring(0, 48);
            if (base.isEmpty()) base = "wallpaper";
            File out = new File(dir, base + ext);
            for (int i = 2; out.exists() && i < 1000; i++) out = new File(dir, base + "-" + i + ext);
            try (InputStream in = new FileInputStream(tmp); OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[64 * 1024];
                for (int n; (n = in.read(buf)) > 0; ) os.write(buf, 0, n);
                return out.getName();
            } catch (IOException e) {
                if (out.isFile()) out.delete();
                return null;
            }
        }

        /** The extension that matches the file's first bytes, or null when it isn't a picture we take. */
        private static String extOf(File f) {
            byte[] b = new byte[12];
            try (InputStream in = new FileInputStream(f)) {
                if (in.read(b) < 12) return null;
            } catch (IOException e) {
                return null;
            }
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return ".jpg";
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return ".png";
            if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ".webp";
            return null;
        }

        private Response page(Response.Status status, String html) {
            Response r = newFixedLengthResponse(status, "text/html; charset=utf-8", html);
            r.addHeader("Cache-Control", "no-store");
            return r;
        }

        /** The upload page; {@code error} is shown above the form when it isn't null. */
        private String form(String error) {
            boolean canSave = FileApi.hasAllFilesAccess();
            return "<!doctype html><html lang=en><meta name=viewport content=\"width=device-width,initial-scale=1\">"
                    + "<meta name=color-scheme content=dark><title>SwadayaTV wallpaper</title>"
                    + "<style>"
                    + "body{margin:0;padding:24px;font:16px/1.5 system-ui,sans-serif;background:#0b0f0c;color:#eef2ee}"
                    + "h1{font-size:22px;margin:0 0 6px}p{margin:0 0 16px;color:#b9c4bc}"
                    + "form{display:grid;gap:14px;max-width:480px}"
                    + "label{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#8fa096}"
                    + "input{width:100%;box-sizing:border-box;padding:14px;border-radius:12px;border:1px solid #2a3530;"
                    + "background:#131a16;color:#fff;font-size:16px}"
                    + "input[type=file]{padding:10px}"
                    + "button{padding:16px;border:0;border-radius:12px;background:#2f7d5b;color:#fff;font-size:17px;font-weight:600}"
                    + ".err{padding:12px 14px;border-radius:12px;background:#3a2418;color:#ffcf9e;margin-bottom:16px}"
                    + ".ok{color:#9fe3bd}"
                    + "</style><body><h1>Wallpaper from your phone</h1>"
                    + "<p>SwadayaTV · same Wi-Fi as the TV.</p>"
                    + (error != null ? "<div class=err>" + escape(error) + "</div>" : "")
                    + (canSave ? "" : "<div class=err>The TV's All files access is off: turn it on in the TV's "
                    + "Settings → About → All files access before uploading.</div>")
                    + "<form method=post enctype=multipart/form-data>"
                    + "<div><label for=code>Code on the TV</label>"
                    + "<input id=code name=code inputmode=numeric pattern=\"[0-9]{6}\" maxlength=6 "
                    + "autocomplete=off required placeholder=\"123456\"></div>"
                    + "<div><label for=picture>Picture</label>"
                    + "<input id=picture type=file name=picture accept=\"image/jpeg,image/png,image/webp\" required></div>"
                    + "<button>Upload and set</button></form>"
                    + "<p style=\"margin-top:18px\">JPEG, PNG or WebP, up to 24 MB. The picture goes to the TV's "
                    + "Wallpapers folder and becomes the wallpaper straight away.</p></body></html>";
        }

        private String done(String name) {
            return "<!doctype html><html lang=en><meta name=viewport content=\"width=device-width,initial-scale=1\">"
                    + "<meta name=color-scheme content=dark><title>Wallpaper sent</title>"
                    + "<style>body{margin:0;padding:24px;font:16px/1.5 system-ui,sans-serif;background:#0b0f0c;"
                    + "color:#eef2ee}h1{font-size:22px;margin:0 0 8px}p{color:#b9c4bc}"
                    + "a{color:#9fe3bd}</style><body><h1 class=ok>Uploaded</h1>"
                    + "<p><strong>" + escape(name) + "</strong> is now the TV's wallpaper. "
                    + "It also appears in the TV's Settings → Home screen → Wallpaper list.</p>"
                    + "<p><a href=\"/\">Send another picture</a></p></body></html>";
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
