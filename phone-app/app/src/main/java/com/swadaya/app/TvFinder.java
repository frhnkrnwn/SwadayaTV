package com.swadaya.app;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

/** Finds TVs that advertise ADB (wireless debugging) on the same Wi-Fi. */
final class TvFinder {

    interface Listener {
        void onFound(String name, String host, int port);
        void onFinished();
    }

    private static final String[] TYPES = {
            "_adb-tls-connect._tcp",
            "_adb._tcp",
            "_adb-tls-pairing._tcp",
    };

    private TvFinder() {
    }

    static void discover(Context ctx, Listener listener) {
        NsdManager nsd = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
        if (nsd == null) {
            listener.onFinished();
            return;
        }

        WifiManager.MulticastLock[] lock = new WifiManager.MulticastLock[1];
        try {
            WifiManager wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                lock[0] = wifi.createMulticastLock("swadaya-find");
                lock[0].setReferenceCounted(false);
                lock[0].acquire();
            }
        } catch (Throwable ignored) {
        }

        Handler main = new Handler(Looper.getMainLooper());
        NsdManager.DiscoveryListener[] started = new NsdManager.DiscoveryListener[TYPES.length];
        final boolean[] finished = {false};

        Runnable finish = () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            for (NsdManager.DiscoveryListener l : started) {
                if (l != null) {
                    try {
                        nsd.stopServiceDiscovery(l);
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (lock[0] != null && lock[0].isHeld()) {
                lock[0].release();
            }
            listener.onFinished();
        };

        for (int i = 0; i < TYPES.length; i++) {
            final String type = TYPES[i];
            NsdManager.DiscoveryListener l = new NsdManager.DiscoveryListener() {
                @Override
                public void onDiscoveryStarted(String serviceType) {
                }

                @Override
                public void onServiceFound(NsdServiceInfo info) {
                    try {
                        nsd.resolveService(info, new NsdManager.ResolveListener() {
                            @Override
                            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            }

                            @Override
                            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                                if (serviceInfo.getHost() == null) {
                                    return;
                                }
                                String host = serviceInfo.getHost().getHostAddress();
                                int port = serviceInfo.getPort();
                                if (host == null || host.isEmpty()) {
                                    return;
                                }
                                String name = serviceInfo.getServiceName();
                                listener.onFound(name + (type.contains("pairing") ? " (pairing)" : ""),
                                        host, port);
                            }
                        });
                    } catch (Throwable ignored) {
                        // One resolve at a time: a second service resolves on a later discovery.
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo info) {
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                }

                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                }
            };
            started[i] = l;
            try {
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l);
            } catch (Throwable ignored) {
                started[i] = null;
            }
        }
        main.postDelayed(finish, 6000);
    }
}
