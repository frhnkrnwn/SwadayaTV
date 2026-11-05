package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Wallpaper hidup "Stadion Malam": lapangan rumput dengan lampu stadion.
 * Cocok untuk penggemar bola; beranimasi halus (lampu berdenyut).
 */
public final class StadiumWallpaperView extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float beat = 0f;

    public StadiumWallpaperView(Context ctx) {
        super(ctx);
        setFocusable(false);
        setClickable(false);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(4000L);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            beat = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // langit stadion gelap
        bgPaint.setShader(new LinearGradient(0, 0, 0, h * 0.62f,
                0xFF05120C, 0xFF0E241A, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h * 0.62f, bgPaint);

        // rumput
        grassPaint.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                0xFF1B4A2E, 0xFF0F2E1F, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.55f, w, h, grassPaint);

        // lampu stadion (4 menara cahaya)
        float lightY = h * 0.10f;
        float[][] towers = {{w * 0.08f, lightY}, {w * 0.36f, lightY * 0.7f}, {w * 0.64f, lightY * 0.7f}, {w * 0.92f, lightY}};
        for (float[] t : towers) {
            float pulse = 0.55f + 0.45f * beat;
            glowPaint.setShader(new RadialGradient(t[0], t[1], h * 0.55f,
                    0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            glowPaint.setAlpha(Math.round(255 * pulse));
            canvas.drawCircle(t[0], t[1], h * 0.55f, glowPaint);
            // menara lampu
            lightPaint.setColor(0xFF2E3E34);
            canvas.drawRect(t[0] - w * 0.012f, t[1], t[0] + w * 0.012f, h * 0.60f, lightPaint);
        }

        // garis lapangan
        int line = 0x66FFFFFF;
        linePaint.setColor(line);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(Math.max(2f, w * 0.004f));
        // garis tengah
        canvas.drawLine(0, h * 0.77f, w, h * 0.77f, linePaint);
        // lingkaran tengah
        canvas.drawOval(new RectF(w * 0.36f, h * 0.60f, w * 0.64f, h * 0.94f), linePaint);
        canvas.drawCircle(w * 0.5f, h * 0.77f, h * 0.045f, linePaint);
        // area penalti
        canvas.drawRect(w * 0.05f, h * 0.60f, w * 0.28f, h * 0.92f, linePaint);
        canvas.drawRect(w * 0.72f, h * 0.60f, w * 0.95f, h * 0.92f, linePaint);

        // sorot lampu menyapu
        lightPaint.setShader(new LinearGradient(0, h * 0.1f, w, h * 0.6f,
                0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        lightPaint.setStyle(Paint.Style.FILL);
        Path beam = new Path();
        float sway = (float) Math.sin(beat * Math.PI) * w * 0.04f;
        beam.moveTo(w * 0.36f + sway, h * 0.07f);
        beam.lineTo(w * 0.05f + sway, h * 0.62f);
        beam.lineTo(w * 0.40f + sway, h * 0.62f);
        beam.close();
        canvas.drawPath(beam, lightPaint);
        Path beam2 = new Path();
        beam2.moveTo(w * 0.64f - sway, h * 0.07f);
        beam2.lineTo(w * 0.60f - sway, h * 0.62f);
        beam2.lineTo(w * 0.95f - sway, h * 0.62f);
        beam2.close();
        canvas.drawPath(beam2, lightPaint);
    }
}
