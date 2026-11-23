package com.swadaya.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bersihkan TV dari HP: sambung lewat ADB, baca isi TV, tunjukkan rencananya, lalu nonaktifkan
 * aplikasi yang dipilih. Semua perubahan tercatat di HP dan bisa dipulihkan dari app ini.
 */
public class MainActivity extends Activity {

    private static final int REQ_APK = 41;
    private static final int STEP_CONNECT = 0;
    private static final int STEP_PLAN = 1;
    private static final int STEP_CLEAN = 2;
    private static final int STEP_RESTORE = 3;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Tv tv;
    private Catalog catalog;
    private Planner plan;
    private final Set<String> selectedGroups = new LinkedHashSet<>();
    private boolean selectionSeeded;

    private TextView tvStatus, btnReconnect;
    private TextView step0, step1, step2, step3;
    private View screenConnect, screenPlan, screenClean, screenRestore;
    private EditText host, port;
    private Button btnFind, btnConnect, btnDownloadLauncher, btnInstallApk, btnClean, btnRefresh, btnCleanNow, btnGoRestore;
    private Button btnEnableAll, btnExport;
    private ProgressBar connectProgress, cleanProgress;
    private TextView connectState, deviceInfo, planSummary, installState, keptTitle, cleanBody, cleanLog;
    private TextView restoreInfo;
    private LinearLayout foundList, groupList, keptList, restoreList;

    private int currentStep = STEP_CONNECT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = new Tv(this);

        tvStatus = findViewById(R.id.tvStatus);
        btnReconnect = findViewById(R.id.btnReconnect);
        step0 = findViewById(R.id.step0);
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        screenConnect = findViewById(R.id.screenConnect);
        screenPlan = findViewById(R.id.screenPlan);
        screenClean = findViewById(R.id.screenClean);
        screenRestore = findViewById(R.id.screenRestore);
        host = findViewById(R.id.host);
        port = findViewById(R.id.port);
        btnFind = findViewById(R.id.btnFind);
        btnConnect = findViewById(R.id.btnConnect);
        btnDownloadLauncher = findViewById(R.id.btnDownloadLauncher);
        btnInstallApk = findViewById(R.id.btnInstallApk);
        btnClean = findViewById(R.id.btnClean);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnCleanNow = findViewById(R.id.btnCleanNow);
        btnGoRestore = findViewById(R.id.btnGoRestore);
        btnEnableAll = findViewById(R.id.btnEnableAll);
        btnExport = findViewById(R.id.btnExport);
        connectProgress = findViewById(R.id.connectProgress);
        cleanProgress = findViewById(R.id.cleanProgress);
        connectState = findViewById(R.id.connectState);
        deviceInfo = findViewById(R.id.deviceInfo);
        planSummary = findViewById(R.id.planSummary);
        installState = findViewById(R.id.installState);
        keptTitle = findViewById(R.id.keptTitle);
        cleanBody = findViewById(R.id.cleanBody);
        cleanLog = findViewById(R.id.cleanLog);
        restoreInfo = findViewById(R.id.restoreInfo);
        foundList = findViewById(R.id.foundList);
        groupList = findViewById(R.id.groupList);
        keptList = findViewById(R.id.keptList);
        restoreList = findViewById(R.id.restoreList);

        String saved = getPreferences(MODE_PRIVATE).getString("host", "");
        if (!saved.isEmpty()) {
            host.setText(saved);
        }

        step0.setOnClickListener(v -> showStep(STEP_CONNECT));
        step1.setOnClickListener(v -> showStep(STEP_PLAN));
        step2.setOnClickListener(v -> showStep(STEP_CLEAN));
        step3.setOnClickListener(v -> showStep(STEP_RESTORE));

        btnConnect.setOnClickListener(v -> connect());
        btnReconnect.setOnClickListener(v -> showStep(STEP_CONNECT));
        btnFind.setOnClickListener(v -> findTvs());
        btnRefresh.setOnClickListener(v -> analyse());
        btnClean.setOnClickListener(v -> showStep(STEP_CLEAN));
        btnGoRestore.setOnClickListener(v -> showStep(STEP_RESTORE));
        btnCleanNow.setOnClickListener(v -> confirmClean());
        btnDownloadLauncher.setOnClickListener(v -> installLauncherFromReleases());
        btnInstallApk.setOnClickListener(v -> pickApk());
        btnEnableAll.setOnClickListener(v -> restoreAll());
        btnExport.setOnClickListener(v -> shareScript());

        showStep(STEP_CONNECT);

        io.execute(() -> {
            try {
                Catalog c = Catalog.load(this);
                ui.post(() -> {
                    catalog = c;
                    if (!selectionSeeded) {
                        selectedGroups.addAll(c.groupIds());
                        selectionSeeded = true;
                    }
                    // A quick connection can land on Analisa before the lists are ready.
                    if (currentStep == STEP_PLAN && tv.connected() && plan == null) {
                        analyse();
                    }
                });
            } catch (Throwable e) {
                ui.post(() -> toast("Daftar debloat gagal dimuat: " + e));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tv.close();
        io.shutdownNow();
    }

    // ---------------------------------------------------------------- steps

    private void showStep(int step) {
        currentStep = step;
        screenConnect.setVisibility(step == STEP_CONNECT ? View.VISIBLE : View.GONE);
        screenPlan.setVisibility(step == STEP_PLAN ? View.VISIBLE : View.GONE);
        screenClean.setVisibility(step == STEP_CLEAN ? View.VISIBLE : View.GONE);
        screenRestore.setVisibility(step == STEP_RESTORE ? View.VISIBLE : View.GONE);
        TextView[] steps = {step0, step1, step2, step3};
        for (int i = 0; i < steps.length; i++) {
            steps[i].setTextColor(getColor(i == step ? R.color.accent : R.color.muted));
            steps[i].setTypeface(null, i == step ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        if (step == STEP_PLAN && tv.connected()) {
            analyse();
        } else if (step == STEP_RESTORE) {
            loadRestore();
        } else if (step == STEP_CLEAN) {
            renderCleanIntro();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep > STEP_CONNECT) {
            showStep(currentStep - 1);
            return;
        }
        super.onBackPressed();
    }

    // ------------------------------------------------------------- connect

    private void connect() {
        String h = host.getText().toString().trim();
        if (h.isEmpty()) {
            toast("Isi IP TV dulu.");
            return;
        }
        int p;
        try {
            p = Integer.parseInt(port.getText().toString().trim());
        } catch (Exception e) {
            p = 5555;
        }
        final int portNo = p;
        btnConnect.setEnabled(false);
        connectProgress.setVisibility(View.VISIBLE);
        connectState.setText("Menyambungkan ke " + h + ":" + portNo + "…");
        ui.postDelayed(() -> {
            if (connectProgress.getVisibility() == View.VISIBLE) {
                connectState.setText("Menunggu TV. Kalau TV menampilkan “Allow USB debugging?”, pilih Always allow lalu OK dengan remote.");
            }
        }, 9000);

        io.execute(() -> {
            try {
                tv.connect(h, portNo);
                getPreferences(MODE_PRIVATE).edit().putString("host", h).apply();
                final String model = tv.model;
                final String version = tv.androidVersion;
                ui.post(() -> {
                    tvStatus.setText(model + " · Android " + version);
                    btnReconnect.setVisibility(View.VISIBLE);
                    btnConnect.setEnabled(true);
                    connectProgress.setVisibility(View.GONE);
                    connectState.setText("Tersambung ke " + model + ".");
                    toast("Tersambung ke TV");
                    showStep(STEP_PLAN);
                });
            } catch (Throwable e) {
                final String why = explainConnect(e, portNo);
                tv.close();
                ui.post(() -> {
                    btnConnect.setEnabled(true);
                    connectProgress.setVisibility(View.GONE);
                    connectState.setText(why);
                });
            }
        });
    }

    private String explainConnect(Throwable e, int portNo) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        String low = m.toLowerCase();
        if (low.contains("refused") || low.contains("econnrefused")) {
            return "Port " + portNo + " tertutup di TV. Nyalakan Network debugging / ADB over network di Developer options, lalu coba lagi.";
        }
        if (low.contains("timeout") || low.contains("timed out")) {
            return "TV tidak menjawab. Pastikan HP dan TV di Wi-Fi yang sama, IP benar, dan debugging menyala.";
        }
        if (low.contains("auth") || low.contains("unable to authenticate")) {
            return "TV menolak kunci HP. Di TV buka Developer options → Revoke USB debugging authorizations, lalu sambungkan lagi.";
        }
        return "Gagal menyambung: " + m;
    }

    private void findTvs() {
        btnFind.setEnabled(false);
        foundList.removeAllViews();
        TextView searching = new TextView(this);
        searching.setText("Mencari TV di Wi-Fi ini…");
        searching.setTextColor(getColor(R.color.muted));
        foundList.addView(searching);
        TvFinder.discover(this, new TvFinder.Listener() {
            @Override
            public void onFound(String name, String hostAddr, int p) {
                ui.post(() -> {
                    if (foundList.getChildCount() == 1 && foundList.getChildAt(0) == searching) {
                        foundList.removeAllViews();
                    }
                    View row = getLayoutInflater().inflate(R.layout.row_device, foundList, false);
                    TextView n = row.findViewById(R.id.name);
                    TextView a = row.findViewById(R.id.addr);
                    n.setText(name);
                    a.setText(hostAddr + ":" + p);
                    row.setOnClickListener(v -> {
                        host.setText(hostAddr);
                        port.setText(String.valueOf(p));
                        toast("Alamat TV dimasukkan");
                    });
                    foundList.addView(row);
                });
            }

            @Override
            public void onFinished() {
                ui.post(() -> {
                    btnFind.setEnabled(true);
                    if (foundList.getChildCount() == 0) {
                        TextView none = new TextView(MainActivity.this);
                        none.setText("Tidak ada TV yang mengumumkan ADB. Isi IP TV dengan tangan (Settings → Network di TV).");
                        none.setTextColor(getColor(R.color.muted));
                        foundList.addView(none);
                    }
                });
            }
        });
    }

    // -------------------------------------------------------------- analyse

    private void analyse() {
        if (!tv.connected()) {
            showStep(STEP_CONNECT);
            return;
        }
        if (catalog == null) {
            planSummary.setText("Daftar debloat belum siap, coba lagi sebentar.");
            return;
        }
        planSummary.setText("Membaca isi TV…");
        groupList.removeAllViews();
        keptList.removeAllViews();
        io.execute(() -> {
            try {
                List<String> enabled = tv.enabledPackages();
                List<String> disabled = tv.disabledPackages();
                List<String> claims = tv.homeClaims();
                String home = tv.homeOwner();
                String keyboard = tv.keyboardPackage();
                Planner p = Planner.build(catalog, enabled, disabled, tv.brand, home, claims, keyboard);
                ui.post(() -> {
                    plan = p;
                    renderPlan();
                });
            } catch (Throwable e) {
                ui.post(() -> planSummary.setText("Gagal membaca TV: " + e));
            }
        });
    }

    private void renderPlan() {
        if (plan == null) {
            return;
        }
        deviceInfo.setText(tv.model + " · " + (tv.brand.isEmpty() ? "Android TV" : tv.brand)
                + " · Android " + tv.androidVersion);
        int count = plan.itemsIn(selectedGroups).size();
        planSummary.setText(count == 0
                ? "Tidak ada yang perlu dimatikan. TV ini sudah bersih, atau semua yang dikenal sudah nonaktif."
                : count + " aplikasi akan dinonaktifkan. Tidak ada yang di-uninstall, dan semuanya bisa dipulihkan.");

        groupList.removeAllViews();
        for (Catalog.Group g : catalog.groups) {
            final List<Planner.Item> items = plan.byGroup.get(g.id);
            if (items == null || items.isEmpty()) {
                continue;
            }
            View row = getLayoutInflater().inflate(R.layout.row_group, groupList, false);
            CheckBox check = row.findViewById(R.id.check);
            TextView name = row.findViewById(R.id.name);
            TextView desc = row.findViewById(R.id.desc);
            name.setText(g.name);
            desc.setText(items.size() + " aplikasi · " + g.desc);
            check.setChecked(selectedGroups.contains(g.id));
            check.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    selectedGroups.add(g.id);
                } else {
                    selectedGroups.remove(g.id);
                }
                int n = 0;
                for (Planner.Item i : plan.items) {
                    if (selectedGroups.contains(i.group)) {
                        n++;
                    }
                }
                planSummary.setText(n + " aplikasi akan dinonaktifkan. Tidak ada yang di-uninstall, dan semuanya bisa dipulihkan.");
            });
            row.setOnClickListener(v -> check.setChecked(!check.isChecked()));
            groupList.addView(row);
        }

        keptList.removeAllViews();
        for (Planner.Kept k : plan.kept) {
            View row = getLayoutInflater().inflate(R.layout.row_item, keptList, false);
            ((TextView) row.findViewById(R.id.name)).setText(k.name + " — " + k.why);
            ((TextView) row.findViewById(R.id.pkg)).setText(k.pkg);
            keptList.addView(row);
        }
        keptTitle.setText("Dibiarkan (tidak disentuh) · " + plan.kept.size());
    }

    // ---------------------------------------------------------------- clean

    private void renderCleanIntro() {
        if (plan == null || !tv.connected()) {
            cleanBody.setText("Sambungkan ke TV dan baca isinya dulu di langkah Analisa.");
            btnCleanNow.setEnabled(false);
            return;
        }
        List<Planner.Item> items = plan.itemsIn(selectedGroups);
        btnCleanNow.setEnabled(!items.isEmpty());
        StringBuilder sb = new StringBuilder();
        sb.append(items.size()).append(" aplikasi akan dinonaktifkan untuk user 0 di ")
                .append(tv.model).append(".\n\n");
        if (items.isEmpty()) {
            sb.append("Tidak ada yang dipilih. Kembali ke Analisa dan pilih minimal satu kategori.");
        } else {
            sb.append("Yang tetap hidup: Wi-Fi, Play Store, Settings, keyboard, dan input TV. ")
                    .append("Home bawaan hanya ikut dimatikan kalau SwadayaTV (atau launcher lain) ")
                    .append("sudah terpasang dan siap menggantikannya.\n\n")
                    .append("Setiap perubahan dicatat di HP ini, jadi bisa dipulihkan dari langkah 4.");
        }
        cleanBody.setText(sb);
    }

    private void confirmClean() {
        if (plan == null) {
            return;
        }
        List<Planner.Item> items = plan.itemsIn(selectedGroups);
        if (items.isEmpty()) {
            toast("Tidak ada yang dipilih.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Bersihkan " + tv.model + "?")
                .setMessage(items.size() + " aplikasi akan dinonaktifkan. Tidak ada yang dihapus. Lanjutkan?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Bersihkan", (d, w) -> runClean(items))
                .show();
    }

    private void runClean(List<Planner.Item> items) {
        btnCleanNow.setEnabled(false);
        cleanLog.setText("");
        cleanProgress.setIndeterminate(false);
        cleanProgress.setProgress(0);
        final int total = items.size();
        io.execute(() -> {
            int done = 0;
            int failed = 0;
            for (Planner.Item item : items) {
                try {
                    boolean ok = tv.disable(item.pkg);
                    if (ok) {
                        RestoreStore.add(this, item.pkg, item.name, tv.model);
                        log("✓ " + item.pkg);
                    } else {
                        failed++;
                        log("✗ " + item.pkg + " (ditolak TV)");
                    }
                } catch (Throwable e) {
                    failed++;
                    log("✗ " + item.pkg + " (" + e.getMessage() + ")");
                }
                done++;
                final int pct = total == 0 ? 100 : done * 100 / total;
                ui.post(() -> cleanProgress.setProgress(pct));
            }
            final int okCount = total - failed;
            final int failedCount = failed;
            ui.post(() -> {
                btnCleanNow.setEnabled(true);
                log("");
                log("Selesai: " + okCount + " dinonaktifkan, " + failedCount + " gagal.");
                log("Buka langkah 4 (Pulihkan) kalau ada yang perlu dinyalakan lagi.");
                analyse(); // the plan on the previous screen is stale now
            });
        });
    }

    private void log(String line) {
        ui.post(() -> {
            cleanLog.append(line + "\n");
        });
    }

    // -------------------------------------------------------------- restore

    private void loadRestore() {
        if (!tv.connected()) {
            restoreInfo.setText("Sambungkan ke TV dulu di langkah 1.");
            restoreList.removeAllViews();
            return;
        }
        restoreInfo.setText("Membaca daftar nonaktif di " + tv.model + "…");
        io.execute(() -> {
            try {
                List<String> disabled = tv.disabledPackages();
                List<RestoreStore.Entry> mine = RestoreStore.forTv(this, tv.model);
                ui.post(() -> {
                    restoreList.removeAllViews();
                    int shown = 0;
                    for (RestoreStore.Entry e : mine) {
                        String state = disabled.contains(e.pkg) ? "nonaktif" : "sudah aktif";
                        View row = getLayoutInflater().inflate(R.layout.row_restore, restoreList, false);
                        ((TextView) row.findViewById(R.id.name)).setText(e.name + " · " + state);
                        ((TextView) row.findViewById(R.id.pkg)).setText(e.pkg);
                        Button action = row.findViewById(R.id.action);
                        action.setOnClickListener(v -> restoreOne(e, action));
                        restoreList.addView(row);
                        shown++;
                    }
                    restoreInfo.setText(shown == 0
                            ? "Belum ada aplikasi yang dinonaktifkan lewat app ini di TV tersebut."
                            : shown + " perubahan tercatat. Tekan Pulihkan pada satu baris, atau semua di bawah.");
                    btnEnableAll.setEnabled(shown > 0);
                });
            } catch (Throwable e) {
                ui.post(() -> restoreInfo.setText("Gagal membaca TV: " + e));
            }
        });
    }

    private void restoreOne(RestoreStore.Entry e, Button action) {
        action.setEnabled(false);
        io.execute(() -> {
            try {
                if (!tv.enable(e.pkg)) {
                    throw new Exception("TV menolak perintah enable");
                }
                RestoreStore.remove(this, e.pkg, tv.model);
                ui.post(() -> {
                    toast(e.name + " dinyalakan lagi");
                    loadRestore();
                });
            } catch (Throwable ex) {
                ui.post(() -> {
                    action.setEnabled(true);
                    toast("Gagal memulihkan: " + ex.getMessage());
                });
            }
        });
    }

    private void restoreAll() {
        List<RestoreStore.Entry> mine = RestoreStore.forTv(this, tv.model);
        if (mine.isEmpty()) {
            return;
        }
        btnEnableAll.setEnabled(false);
        io.execute(() -> {
            int ok = 0;
            int failed = 0;
            for (RestoreStore.Entry e : mine) {
                try {
                    if (tv.enable(e.pkg)) {
                        RestoreStore.remove(this, e.pkg, tv.model);
                        ok++;
                    } else {
                        failed++;
                    }
                } catch (Throwable ignored) {
                    failed++;
                }
            }
            final int done = ok;
            final int left = failed;
            ui.post(() -> {
                toast(left == 0 ? done + " aplikasi dipulihkan"
                        : done + " dipulihkan, " + left + " gagal");
                loadRestore();
            });
        });
    }

    private void shareScript() {
        String script = RestoreStore.script(this, tv.host, tv.model);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Skrip pemulihan Swadaya App");
        i.putExtra(Intent.EXTRA_TEXT, script);
        startActivity(Intent.createChooser(i, "Bagikan skrip pemulihan"));
    }

    // ---------------------------------------------------------- launcher APK

    /** Fetches the newest tvhome APK from this repository's Releases and installs it on the TV. */
    private void installLauncherFromReleases() {
        if (!tv.connected()) {
            toast("Sambungkan ke TV dulu.");
            return;
        }
        btnDownloadLauncher.setEnabled(false);
        btnInstallApk.setEnabled(false);
        installState.setText("Mencari rilis SwadayaTV terbaru di GitHub…");
        io.execute(() -> {
            File apk = new File(getCacheDir(), "swadayatv-download.apk");
            try {
                Tv.AppVersion onTv = tvVersion();
                ui.post(() -> installState.setText((onTv == null
                        ? "SwadayaTV belum terpasang di TV."
                        : "SwadayaTV di TV: " + onTv.text() + ".")
                        + " Mencari rilis terbaru di GitHub…"));
                LauncherApk.Release release = LauncherApk.latest();
                ui.post(() -> installState.setText("Mengunduh " + release.asset
                        + " dari Releases (" + LauncherApk.sizeText(release.size) + ")…"));
                LauncherApk.download(release, apk, percent -> ui.post(() ->
                        installState.setText("Mengunduh " + release.asset + " · " + percent + "%")));
                ui.post(() -> installState.setText("Memasang SwadayaTV "
                        + (release.version.isEmpty() ? "" : release.version + " ") + "di TV…"));
                String done = installToTv(apk, true);
                ui.post(() -> installState.setText(done));
            } catch (Throwable e) {
                String why = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> installState.setText("Gagal mengunduh/memasang: " + why
                        + " Kalau GitHub tidak bisa dihubungi, pakai tombol APK di HP."));
            } finally {
                //noinspection ResultOfMethodCallIgnored
                apk.delete();
                ui.post(() -> {
                    btnDownloadLauncher.setEnabled(true);
                    btnInstallApk.setEnabled(true);
                });
            }
        });
    }

    /**
     * Installs an APK on the TV, makes it Home, then compares the version now on the TV with the
     * version inside the APK. Blocks; throws when the TV refuses the install.
     */
    private String installToTv(File apk, boolean fromReleases) throws Exception {
        Tv.AppVersion apkVersion = apkVersion(apk);
        Tv.AppVersion before = tvVersion();
        tv.installApk(apk);
        String home;
        try {
            tv.setSwadayaHome();
            home = "SwadayaTV terpasang dan sudah jadi Home. Tekan Home di remote untuk mencobanya.";
        } catch (Throwable e) {
            home = "SwadayaTV terpasang. Tekan Home di remote, pilih SwadayaTV, lalu Always.";
        }
        return home + "\n\n"
                + versionReport(apkVersion, fromReleases ? "Rilis terbaru" : "APK ini", before, tvVersion());
    }

    /** "TV vs. the APK just installed", in three short lines. */
    private String versionReport(Tv.AppVersion apk, String apkLabel, Tv.AppVersion before, Tv.AppVersion after) {
        if (after == null) {
            return "Versi SwadayaTV di TV tidak terbaca (paket " + Tv.SWADAYA_TV
                    + " tidak ditemukan), jadi tidak bisa dibandingkan dengan rilis terbaru.";
        }
        StringBuilder sb = new StringBuilder("SwadayaTV di TV: ").append(after.text());
        if (apk == null) {
            return sb.append("\nVersi berkas APK tidak terbaca, jadi tidak bisa dibandingkan.").toString();
        }
        sb.append('\n').append(apkLabel).append(": ").append(apk.text());
        if (!sameVersion(after, apk)) {
            return sb.append("\nTV belum memakai versi terbaru. Coba tombol sekali lagi, atau pasang manual.").toString();
        }
        sb.append("\nTV sudah memakai versi terbaru.");
        if (before != null && !sameVersion(before, after)) {
            sb.append(" Diperbarui dari ").append(before.text()).append('.');
        }
        return sb.toString();
    }

    /** Version codes are the real order; version names are the fallback when one is missing. */
    private static boolean sameVersion(Tv.AppVersion a, Tv.AppVersion b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.code > 0 && b.code > 0) {
            return a.code == b.code;
        }
        return !a.name.isEmpty() && a.name.equals(b.name);
    }

    /** The version inside the APK file itself, or null when it can't be read. */
    private Tv.AppVersion apkVersion(File apk) {
        try {
            PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            return info == null ? null : new Tv.AppVersion(info.versionName, info.versionCode);
        } catch (Throwable e) {
            return null;
        }
    }

    /** The version on the TV, or null when it isn't installed or can't be read. */
    private Tv.AppVersion tvVersion() {
        try {
            return tv.installedVersion(Tv.SWADAYA_TV);
        } catch (Throwable e) {
            return null;
        }
    }

    private void pickApk() {
        if (!tv.connected()) {
            toast("Sambungkan ke TV dulu.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.android.package-archive");
        try {
            startActivityForResult(i, REQ_APK);
        } catch (Throwable e) {
            toast("Tidak ada aplikasi pemilih berkas.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_APK || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        installState.setText("Menyalin APK…");
        btnInstallApk.setEnabled(false);
        io.execute(() -> {
            File apk = new File(getCacheDir(), "swadayatv.apk");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(apk)) {
                if (in == null) {
                    throw new Exception("Berkas tidak bisa dibaca.");
                }
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } catch (Throwable e) {
                ui.post(() -> {
                    btnInstallApk.setEnabled(true);
                    installState.setText("Gagal menyalin APK: " + e.getMessage());
                });
                return;
            }

            ui.post(() -> installState.setText("Memasang SwadayaTV di TV…"));
            try {
                String msg = installToTv(apk, false);
                ui.post(() -> installState.setText(msg));
            } catch (Throwable e) {
                ui.post(() -> installState.setText("Gagal memasang: " + e.getMessage()));
            } finally {
                ui.post(() -> btnInstallApk.setEnabled(true));
            }
        });
    }

    // ------------------------------------------------------------------ misc

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
