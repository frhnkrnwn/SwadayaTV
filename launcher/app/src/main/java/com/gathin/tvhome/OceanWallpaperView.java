package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Calendar;
import java.util.Random;

/**
 * Wallpaper hidup "Samudra & Pantai": langit gradien waktu nyata (siang/malam),
 * matahari/bulan terbit-terbenam, 3 lapis ombak, bukit pasir, pohon kelapa,
 * dan kawanan ikan yang berenang. 60 FPS via ValueAnimator.
 */
public final class OceanWallpaperView extends View {

    private static final int[] HOURS = {0, 4, 5, 6, 10, 12, 16, 18, 19, 21, 24};

    // [top, horizon] RGB stops untuk langit per jam
    private static final int[][] SKY_TOP = {
            {0x06,0x0B,0x14}, {0x08,0x0F,0x1E}, {0x2E,0x19,0x3C}, {0x4A,0x24,0x48},
            {0x12,0x68,0xA8}, {0x10,0x70,0xB2}, {0x1A,0x38,0x5C}, {0x1E,0x14,0x30},
            {0x14,0x12,0x26}, {0x09,0x0E,0x1A}, {0x06,0x0B,0x14}
    };
    private static final int[][] SKY_HOR = {
            {0x0B,0x16,0x28}, {0x10,0x1F,0x38}, {0x87,0x38,0x5A}, {0xD8,0x67,0x62},
            {0x2B,0xB0,0xD4}, {0x32,0xB4,0xD8}, {0x7B,0x38,0x58}, {0x7D,0x1E,0x4E},
            {0x35,0x20,0x42}, {0x0F,0x1A,0x2E}, {0x0B,0x16,0x28}
    };
    private static final int[][] SEA_TOP = {
            {0x0A,0x16,0x26}, {0x0E,0x1E,0x34}, {0x66,0x30,0x48}, {0x8A,0x46,0x44},
            {0x16,0x8C,0xA6}, {0x1A,0xA0,0xBC}, {0x78,0x3E,0x38}, {0x9C,0x40,0x28},
            {0x38,0x1C,0x2C}, {0x0E,0x1A,0x2C}, {0x0A,0x16,0x26}
    };
    private static final int[][] SEA_BOT = {
            {0x04,0x0A,0x14}, {0x05,0x0C,0x18}, {0x12,0x10,0x20}, {0x16,0x1E,0x2E},
            {0x08,0x32,0x4E}, {0x0A,0x36,0x54}, {0x14,0x1A,0x28}, {0x18,0x14,0x22},
            {0x0E,0x0E,0x1A}, {0x06,0x0B,0x14}, {0x04,0x0A,0x14}
    };
    private static final int[][] SHORE = {
            {0x16,0x1E,0x2A}, {0x1A,0x22,0x30}, {0x7C,0x5A,0x56}, {0xC2,0x8E,0x70},
            {0xDF,0xC0,0x88}, {0xE2,0xC4,0x8E}, {0xC4,0x8E,0x66}, {0xB8,0x66,0x44},
            {0x42,0x28,0x34}, {0x1C,0x22,0x2E}, {0x16,0x1E,0x2A}
    };

    private final Paint skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint seaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fishPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint palmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float phase = 0f;
    private final float[] fishPhase = {0f, 0.9f, 1.7f, 2.6f, 3.4f, 4.2f, 5.0f, 5.9f};
    private final float[] fishY = {0.64f, 0.68f, 0.72f, 0.67f, 0.74f, 0.70f, 0.76f, 0.73f};
    private final float[] fishX = {0.28f, 0.36f, 0.45f, 0.52f, 0.34f, 0.44f, 0.55f, 0.30f};

    public OceanWallpaperView(Context ctx) {
        super(ctx);
        setFocusable(false);
        setClickable(false);
        // animasi halus 60fps
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(12000L);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue() * (float) Math.PI * 2f;
            invalidate();
        });
        anim.start();
    }

    private static int lerp(int[] a, int[] b, float t) {
        int r = Math.round(a[0] + (b[0] - a[0]) * t);
        int g = Math.round(a[1] + (b[1] - a[1]) * t);
        int bl = Math.round(a[2] + (b[2] - a[2]) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int argb(int c, float alpha) {
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return (a << 24) | (c & 0x00FFFFFF);
    }

    private void stops(int[][] arr, double hour, int[] topOut, int[] horOut) {
        // cari titik jam terdekat, interpolasi
        for (int i = 0; i < HOURS.length - 1; i++) {
            if (hour >= HOURS[i] && hour <= HOURS[i + 1]) {
                float t = (float) ((hour - HOURS[i]) / Math.max(0.001, HOURS[i + 1] - HOURS[i]));
                int[] s = arr[i], e = arr[i + 1];
                topOut[0] = Math.round(s[0] + (e[0] - s[0]) * t);
                topOut[1] = Math.round(s[1] + (e[1] - s[1]) * t);
                topOut[2] = Math.round(s[2] + (e[2] - s[2]) * t);
                return;
            }
        }
        System.arraycopy(arr[0], 0, topOut, 0, 3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        Calendar now = Calendar.getInstance();
        double hour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;
        float horizon = h * 0.58f;

        int[] top = new int[3], hor = new int[3];
        stops(SKY_TOP, hour, top, new int[3]);
        stops(SKY_HOR, hour, hor, new int[3]);
        int skyTop = lerp(top, top, 0f);
        int skyHor = lerp(hor, hor, 0f);

        int[] seaTop = new int[3], seaBot = new int[3];
        stops(SEA_TOP, hour, seaTop, new int[3]);
        stops(SEA_BOT, hour, seaBot, new int[3]);
        int seaT = lerp(seaTop, seaTop, 0f);
        int seaB = lerp(seaBot, seaBot, 0f);

        // langit
        skyPaint.setShader(new LinearGradient(0, 0, 0, horizon, skyTop, skyHor, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, horizon, skyPaint);

        // laut
        seaPaint.setShader(new LinearGradient(0, horizon, 0, h, seaT, seaB, Shader.TileMode.CLAMP));
        canvas.drawRect(0, horizon, w, h, seaPaint);

        // matahari & bulan
        float sunAngle = (float) ((hour - 6.0) / 12.0 * Math.PI); // naik 6:00, turun 18:00
        float sunElev = (float) Math.sin(sunAngle); // <0 = di bawah horizon
        float sunX = w * (0.15f + 0.7f * (float) ((hour - 6.0) / 12.0));
        float sunY = horizon - sunElev * (horizon * 0.75f);
        if (sunElev > -0.08f) {
            float sunR = h * 0.055f;
            glowPaint.setShader(new RadialGradient(sunX, sunY, sunR * 3.2f,
                    0x55FFDD88, 0x00FFDD88, Shader.TileMode.CLAMP));
            canvas.drawCircle(sunX, sunY, sunR * 3.2f, glowPaint);
            sunPaint.setColor(0xFFFFE9A8);
            canvas.drawCircle(sunX, sunY, sunR, sunPaint);
            // refleksi cahaya di air
            wavePaint.setColor(0x66FFDD88);
            for (int i = 0; i < 5; i++) {
                float ry = horizon + (h - horizon) * (0.08f + i * 0.13f) + (float) Math.sin(phase * 0.6f + i) * 3f;
                float rw = w * (0.10f + 0.05f * i) * (1f + 0.08f * (float) Math.sin(phase + i * 1.7));
                canvas.drawOval(new RectF(sunX - rw, ry, sunX + rw, ry + h * 0.012f), wavePaint);
            }
        } else {
            // bulan di malam hari
            float moonElev = (float) Math.sin((hour - 18.0) / 12.0 * Math.PI);
            if (moonElev > 0) {
                float moonX = w * (0.2f + 0.6f * (float) ((hour - 18.0) / 12.0));
                float moonY = horizon - moonElev * (horizon * 0.7f);
                float moonR = h * 0.038f;
                glowPaint.setShader(new RadialGradient(moonX, moonY, moonR * 2.8f,
                        0x44EEEEFF, 0x00EEEEFF, Shader.TileMode.CLAMP));
                canvas.drawCircle(moonX, moonY, moonR * 2.8f, glowPaint);
                moonPaint.setColor(0xFFF4F6FF);
                canvas.drawCircle(moonX, moonY, moonR, moonPaint);
                // kawah kecil
                moonPaint.setColor(0xFFDDE2F0);
                canvas.drawCircle(moonX - moonR * 0.35f, moonY - moonR * 0.2f, moonR * 0.22f, moonPaint);
                canvas.drawCircle(moonX + moonR * 0.25f, moonY + moonR * 0.25f, moonR * 0.16f, moonPaint);
            }
        }

        // 3 lapis ombak
        float[] waveY = {horizon + (h - horizon) * 0.18f, horizon + (h - horizon) * 0.38f, horizon + (h - horizon) * 0.62f};
        float[] amp = {h * 0.012f, h * 0.018f, h * 0.026f};
        float[] speed = {1.0f, 0.7f, 0.45f};
        int[] waveColors = {0x22FFFFFF, 0x33FFFFFF, 0x44FFFFFF};
        for (int i = 0; i < 3; i++) {
            wavePaint.setColor(waveColors[i]);
            wavePaint.setStyle(Paint.Style.FILL);
            Path path = new Path();
            path.moveTo(0, waveY[i]);
            for (int x = 0; x <= w; x += Math.max(8, w / 60)) {
                float y = waveY[i] + (float) Math.sin(x * 0.012f + phase * speed[i] + i * 1.3f) * amp[i];
                path.lineTo(x, y);
            }
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            canvas.drawPath(path, wavePaint);
        }

        // bukit pasir (pantai) di kanan bawah
        int[] shore = new int[3];
        stops(SHORE, hour, shore, new int[3]);
        int shoreC = lerp(shore, shore, 0f);
        shorePaint.setShader(new LinearGradient(0, h * 0.78f, 0, h, shoreC, 0xFF000000 |
                ((shore[0] * 3 / 4) << 16) | ((shore[1] * 3 / 4) << 8) | (shore[2] * 3 / 4), Shader.TileMode.CLAMP));
        Path dune = new Path();
        dune.moveTo(w * 0.55f, h);
        dune.cubicTo(w * 0.62f, h * 0.86f, w * 0.78f, h * 0.80f, w, h * 0.78f);
        dune.lineTo(w, h);
        dune.close();
        canvas.drawPath(dune, shorePaint);

        // pohon kelapa di bukit
        drawPalm(canvas, w * 0.82f, h * 0.80f, h * 0.16f, phase);

        // kawanan ikan
        fishPaint.setColor(0xCCFFFFFF);
        for (int i = 0; i < fishPhase.length; i++) {
            float fx = (fishX[i] + 0.05f * (float) Math.sin(phase * 0.35f + fishPhase[i]) * 2f) * w;
            float fy = fishY[i] * h + (float) Math.sin(phase + fishPhase[i]) * h * 0.008f;
            float s = h * 0.012f * (1f - i * 0.05f);
            drawFish(canvas, fx, fy, s, phase * 2f + fishPhase[i]);
        }

        // bintang di langit malam
        if (sunElev <= -0.05f) {
            Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
            Random rnd = new Random(7);
            for (int i = 0; i < 40; i++) {
                float sx = rnd.nextFloat() * w;
                float sy = rnd.nextFloat() * horizon * 0.75f;
                float r = 1f + rnd.nextFloat() * 1.6f;
                float a = 0.4f + 0.6f * (float) Math.abs(Math.sin(phase * 0.5f + i));
                star.setColor(argb(0xFFFFFFFF, a));
                canvas.drawCircle(sx, sy, r, star);
            }
        }
    }

    private void drawPalm(Canvas canvas, float baseX, float baseY, float size, float phase) {
        palmPaint.setColor(0xFF2A1F16);
        palmPaint.setStyle(Paint.Style.STROKE);
        palmPaint.setStrokeWidth(size * 0.14f);
        palmPaint.setStrokeCap(Paint.Cap.ROUND);
        Path trunk = new Path();
        trunk.moveTo(baseX, baseY);
        trunk.cubicTo(baseX - size * 0.1f, baseY - size * 0.35f,
                baseX + size * 0.18f, baseY - size * 0.6f,
                baseX + size * 0.28f, baseY - size * 0.95f);
        canvas.drawPath(trunk, palmPaint);
        // daun (fronds)
        palmPaint.setStyle(Paint.Style.FILL);
        palmPaint.setColor(0xFF1E3322);
        float topX = baseX + size * 0.28f, topY = baseY - size * 0.95f;
        for (int i = 0; i < 6; i++) {
            float ang = (-90 + i * 36) * (float) Math.PI / 180f;
            float sway = (float) Math.sin(phase * 1.3f + i) * 0.06f;
            float len = size * (0.75f + 0.15f * (float) Math.sin(i * 2.4f));
            float tipX = topX + (float) Math.cos(ang + sway) * len;
            float tipY = topY + (float) Math.sin(ang + sway) * len;
            Path frond = new Path();
            frond.moveTo(topX, topY);
            frond.quadTo((topX + tipX) / 2f, (topY + tipY) / 2f - size * 0.12f, tipX, tipY);
            frond.quadTo((topX + tipX) / 2f + size * 0.08f, (topY + tipY) / 2f + size * 0.05f, topX, topY);
            frond.close();
            canvas.drawPath(frond, palmPaint);
        }
    }

    private void drawFish(Canvas canvas, float cx, float cy, float size, float wiggle) {
        Path body = new Path();
        body.moveTo(cx - size, cy);
        body.quadTo(cx, cy - size * 0.6f, cx + size, cy);
        body.quadTo(cx, cy + size * 0.6f, cx - size, cy);
        body.close();
        canvas.drawPath(body, fishPaint);
        Path tail = new Path();
        tail.moveTo(cx - size, cy);
        float wag = (float) Math.sin(wiggle * 2f) * size * 0.3f;
        tail.lineTo(cx - size - size * 0.55f, cy - size * 0.45f + wag);
        tail.lineTo(cx - size - size * 0.55f, cy + size * 0.45f + wag);
        tail.close();
        canvas.drawPath(tail, fishPaint);
    }
}
