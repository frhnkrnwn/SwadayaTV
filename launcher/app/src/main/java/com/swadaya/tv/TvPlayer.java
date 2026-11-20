package com.swadaya.tv;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;

/**
 * Plays a Live TV channel: the Indonesian streams are plain HLS playlists, so Android's own media
 * framework plays them in a {@link VideoView}. No web page, no sign-in and no DRM; the remote stays
 * with the launcher because the view never takes focus.
 */
final class TvPlayer {
    static final int CONNECTING = 0, PLAYING = 1, FAILED = 2;

    interface Listener {
        /** {@code message} is the startup stage for CONNECTING, or an explanation for FAILED. */
        void onState(int state, String message);
    }

    private static final long GIVE_UP_MS = 45_000;
    private static final String TAG = "TvPlayer";

    private final Context ctx;
    private final FrameLayout host;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());

    private VideoView view;
    private String url;
    private String channelSlug;
    private long startedAt;
    private int state = -1;
    private String stateMessage;
    private int generation;

    TvPlayer(Context ctx, FrameLayout host, Listener listener) {
        this.ctx = ctx;
        this.host = host;
        this.listener = listener;
    }

    /** Nothing to warm up: the media framework is always ready. */
    void prepare() {
    }

    /** Starts {@code channel}, replacing whatever was playing. */
    void play(LiveChannels.Channel channel) {
        String page = channel.page();
        if (!LiveChannels.isChannelPage(page)) return;
        if (page.equals(url) && view != null && (state == PLAYING || state == CONNECTING)) return;
        generation++;
        main.removeCallbacks(watchdog);
        releaseView();
        url = page;
        channelSlug = channel.slug;
        startedAt = SystemClock.elapsedRealtime();
        trace("tune requested");
        setState(CONNECTING, "Menghubungkan ke " + channel.name + "…");

        final VideoView v = new VideoView(ctx);
        view = v;
        v.setBackgroundColor(Color.BLACK);
        v.setFocusable(false);
        v.setFocusableInTouchMode(false);
        v.setVideoURI(Uri.parse(page));
        v.setOnPreparedListener(mp -> {
            if (v != view) return;
            mp.setLooping(false);
            v.start();
            trace("playing");
            setState(PLAYING, null);
        });
        v.setOnInfoListener((mp, what, extra) -> {
            if (v != view) return false;
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                setState(CONNECTING, "Memuat siaran…");
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                    || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                setState(PLAYING, null);
            }
            return false;
        });
        v.setOnErrorListener((mp, what, extra) -> {
            if (v == view) fail("Siaran ini tidak dapat diputar. Coba lagi nanti");
            return true;
        });
        host.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        main.postDelayed(watchdog, GIVE_UP_MS);
    }

    /** The view height no longer selects a rendition: HLS picks its own. */
    void setViewHeight(int px) {
    }

    /** Stops playback and frees the player. */
    void release() {
        generation++;
        main.removeCallbacks(watchdog);
        releaseView();
        url = null;
        state = -1;
    }

    private void releaseView() {
        VideoView v = view;
        view = null;
        if (v != null) {
            try {
                v.stopPlayback();
            } catch (RuntimeException ignored) {
            }
            host.removeView(v);
        }
    }

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (view == null || url == null || state == FAILED) return;
            fail("Siaran terlalu lama dimuat. Periksa koneksi lalu coba lagi");
        }
    };

    /** Log only channel slugs and timings, never stream URLs. */
    private void trace(String event) {
        if (channelSlug != null) Log.i(TAG, channelSlug + " +"
                + (SystemClock.elapsedRealtime() - startedAt) + " ms: " + event);
    }

    private void fail(String why) {
        if (generation == 0 && view == null) return;
        generation++;
        main.removeCallbacks(watchdog);
        releaseView();
        trace("failed");
        setState(FAILED, why);
    }

    private void setState(int s, String message) {
        if (s == state && java.util.Objects.equals(message, stateMessage)) return;
        state = s;
        stateMessage = message;
        listener.onState(s, message);
    }
}
