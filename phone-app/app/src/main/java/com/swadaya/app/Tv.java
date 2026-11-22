package com.swadaya.app;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dadb.AdbKeyPair;
import dadb.AdbShellResponse;
import dadb.Dadb;

/**
 * One ADB connection to the TV. All methods block, so callers run them on a background thread.
 *
 * The phone is the ADB host: it speaks the adb protocol to the TV's adbd itself, with no computer
 * and no adb binary in between. The key pair lives in the app's private storage; the TV asks once
 * whether to trust it ("Allow USB debugging?").
 */
final class Tv {

    private final Context ctx;
    private Dadb dadb;

    String host = "";
    int port = 5555;
    String model = "";
    String brand = "";
    String androidVersion = "";

    Tv(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    boolean connected() {
        return dadb != null;
    }

    void connect(String host, int port) throws Exception {
        close();
        File priv = new File(ctx.getFilesDir(), "adbkey");
        File pub = new File(ctx.getFilesDir(), "adbkey.pub");
        if (!priv.isFile() || !pub.isFile()) {
            AdbKeyPair.Companion.generate(priv, pub);
        }
        AdbKeyPair keys = AdbKeyPair.Companion.read(priv, pub);
        Dadb d = Dadb.Companion.create(host, port, keys, 8000, 0, false);

        // Read the TV's identity on the new connection before making it "the" connection: a TV
        // that accepts the socket but never answers must not leave a half-open session behind.
        String newModel;
        String newBrand;
        String newVersion;
        try {
            newModel = shell("getprop ro.product.model", d).trim();
            String b = shell("getprop ro.product.brand", d).trim();
            String manufacturer = shell("getprop ro.product.manufacturer", d).trim();
            if (b.isEmpty()) {
                b = manufacturer;
            }
            newBrand = (b + " " + manufacturer).toLowerCase(java.util.Locale.ROOT).trim();
            newVersion = shell("getprop ro.build.version.release", d).trim();
        } catch (Throwable e) {
            quietClose(d);
            throw e;
        }
        if (newModel.isEmpty() && newVersion.isEmpty()) {
            quietClose(d);
            throw new IOException("TV tersambung tapi tidak menjawab.");
        }

        this.dadb = d;
        this.host = host;
        this.port = port;
        this.model = newModel;
        this.brand = newBrand;
        this.androidVersion = newVersion;
    }

    private static void quietClose(Dadb d) {
        try {
            d.close();
        } catch (Throwable ignored) {
        }
    }

    String shell(String command) throws IOException {
        return shell(command, dadb);
    }

    private String shell(String command, Dadb d) throws IOException {
        if (d == null) {
            throw new IOException("Belum tersambung ke TV.");
        }
        AdbShellResponse r = d.shell(command);
        String out = r.getOutput();
        return out == null ? "" : out;
    }

    /** Enabled packages for user 0. */
    List<String> enabledPackages() throws IOException {
        return parsePackages(shell("pm list packages -e --user 0"));
    }

    /** Disabled packages for user 0. */
    List<String> disabledPackages() throws IOException {
        return parsePackages(shell("pm list packages -d --user 0"));
    }

    static List<String> parsePackages(String out) {
        List<String> list = new ArrayList<>();
        for (String line : out.split("\\r?\\n")) {
            if (line.startsWith("package:")) {
                list.add(line.substring("package:".length()).trim());
            }
        }
        return list;
    }

    /** Every package that can answer Home on this TV. */
    List<String> homeClaims() throws IOException {
        List<String> list = new ArrayList<>();
        String out = shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME");
        for (String line : out.split("\\r?\\n")) {
            String s = line.trim();
            int slash = s.indexOf('/');
            if (slash > 0) {
                String pkg = s.substring(0, slash);
                if (pkg.matches("[A-Za-z0-9_.]+") && !list.contains(pkg)) {
                    list.add(pkg);
                }
            }
        }
        return list;
    }

    /** The package that currently owns Home, or "". */
    String homeOwner() {
        try {
            String out = shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME");
            String last = "";
            for (String line : out.split("\\r?\\n")) {
                if (!line.trim().isEmpty()) {
                    last = line.trim();
                }
            }
            int slash = last.indexOf('/');
            if (slash > 0) {
                String pkg = last.substring(0, slash);
                if (pkg.matches("[A-Za-z0-9_.]+")) {
                    return pkg;
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** The package of the active keyboard, or "". */
    String keyboardPackage() {
        try {
            String s = shell("settings get secure default_input_method").trim();
            int slash = s.indexOf('/');
            String pkg = slash > 0 ? s.substring(0, slash) : s;
            return pkg.matches("[A-Za-z0-9_.]+") ? pkg : "";
        } catch (Throwable e) {
            return "";
        }
    }

    /** Returns true when the TV reports the package as disabled afterwards. */
    boolean disable(String pkg) throws IOException {
        String out = shell("pm disable-user --user 0 " + pkg);
        return out.contains("disabled-user") || out.contains("new state: disabled");
    }

    boolean enable(String pkg) throws IOException {
        String out = shell("pm enable --user 0 " + pkg);
        return out.contains("new state: enabled") || out.contains("enabled");
    }

    void installApk(File apk) throws IOException {
        Dadb d = dadb;
        if (d == null) {
            throw new IOException("Belum tersambung ke TV.");
        }
        d.install(apk, "-r");
    }

    /** Makes SwadayaTV the Home app (Android 10+). */
    void setSwadayaHome() throws IOException {
        shell("cmd package set-home-activity com.swadaya.tv/com.swadaya.tv.MainActivity");
    }

    void close() {
        Dadb d = dadb;
        dadb = null;
        if (d != null) {
            try {
                d.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
