package io.freemytv;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Procedural dynamic Ocean & Coast vector wallpaper.
 * Renders an open ocean horizon, 3 layered animated waves, time-of-day lighting
 * with Sun/Moon rising and setting directly into the sea horizon, celestial reflection
 * corridor, coastal beach shoreline, wind-swayed coconut palms, and an interactive
 * school of fish swimming in the sea responsive to D-Pad navigation.
 */
final class InkSky {
    static final float SUNRISE = 5.75f, SUNSET = 17.7f;

    private static final int CLEAR = 0, PARTLY = 1, CLOUDY = 2, RAIN = 3, STORM = 4, FOG = 5;

    // Fixed keyframe hours across 24h cycle
    private static final float[] HOURS = {0f, 4.5f, 5.5f, 6.5f, 10.0f, 13.0f, 16.5f, 18.0f, 19.2f, 21.0f, 24.0f};

    // Sky colors: top, mid, horizon
    private static final int[][] SKY_COLORS = {
        {0xFF060B14, 0xFF0B1628, 0xFF12243C}, // 00:00 Deep midnight
        {0xFF080F1E, 0xFF101F38, 0xFF18304E}, // 04:30 Pre-dawn navy
        {0xFF2E193C, 0xFF87385A, 0xFFE87766}, // 05:30 Dawn coral pink
        {0xFF4A2448, 0xFFD86762, 0xFFFFBE85}, // 06:30 Sunrise peach gold
        {0xFF1268A8, 0xFF2BB0D4, 0xFF6FE2E4}, // 10:00 Morning tropical turquoise
        {0xFF1070B2, 0xFF32B4D8, 0xFF7AE6E8}, // 13:00 Midday bright blue
        {0xFF1A385C, 0xFF7B3858, 0xFFE06C42}, // 16:30 Golden hour
        {0xFF1E1430, 0xFF7D1E4E, 0xFFFF6C2C}, // 18:00 Sunset amber magenta
        {0xFF141226, 0xFF352042, 0xFF7A3644}, // 19:12 Dusk twilight
        {0xFF090E1A, 0xFF0F1A2E, 0xFF14243A}, // 21:00 Nightfall indigo
        {0xFF060B14, 0xFF0B1628, 0xFF12243C}  // 24:00 Deep midnight
    };

    // Ocean colors: horizon, mid, deep
    private static final int[][] SEA_COLORS = {
        {0xFF0A1626, 0xFF07101E, 0xFF040A14}, // Midnight deep sea
        {0xFF0E1E34, 0xFF091424, 0xFF050C18}, // Pre-dawn sea
        {0xFF663048, 0xFF2A1C34, 0xFF121020}, // Dawn rosy reflection
        {0xFF8A4644, 0xFF32364C, 0xFF161E2E}, // Sunrise gold gleam
        {0xFF168CA6, 0xFF0E5478, 0xFF08324E}, // Morning tropical cyan
        {0xFF1AA0BC, 0xFF105C84, 0xFF0A3654}, // Midday deep blue
        {0xFF783E38, 0xFF2E3048, 0xFF141A28}, // Golden hour sea
        {0xFF9C4028, 0xFF3E243A, 0xFF181422}, // Sunset fiery reflection
        {0xFF381C2C, 0xFF1A1828, 0xFF0E0E1A}, // Dusk deep sea
        {0xFF0E1A2C, 0xFF0A1220, 0xFF060B14}, // Nightfall sea
        {0xFF0A1626, 0xFF07101E, 0xFF040A14}  // Midnight deep sea
    };

    // Shore and palm silhouette colors: sand base, wet sand, palm silhouette
    private static final int[][] SHORE_COLORS = {
        {0xFF161E2A, 0xFF0E141E, 0xFF080D14}, // Midnight
        {0xFF1A2230, 0xFF121824, 0xFF0A0F18}, // Pre-dawn
        {0xFF7C5A56, 0xFF583A3C, 0xFF281822}, // Dawn
        {0xFFC28E70, 0xFF8A5C50, 0xFF341C24}, // Sunrise
        {0xFFDFC088, 0xFFB4925C, 0xFF14281E}, // Morning
        {0xFFE2C48E, 0xFFBA9662, 0xFF162C20}, // Midday
        {0xFFC48E66, 0xFF8E5C46, 0xFF2E1C22}, // Golden hour
        {0xFFB86644, 0xFF7A3E34, 0xFF24121A}, // Sunset
        {0xFF422834, 0xFF2C1A24, 0xFF140C12}, // Dusk
        {0xFF1C222E, 0xFF121620, 0xFF0A0E16}, // Nightfall
        {0xFF161E2A, 0xFF0E141E, 0xFF080D14}  // Midnight
    };

    // Rainy/storm moody palette blend targets
    private static final int[] RAIN_SKY = {0xFF1E2834, 0xFF283442, 0xFF3A4856};
    private static final int[] RAIN_SEA = {0xFF26323E, 0xFF1A242E, 0xFF10161E};
    private static final int[] RAIN_SHORE = {0xFF38404A, 0xFF283038, 0xFF14181E};

    private final WeatherSky helper;
    private int w, h;
    private float k = 1f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fishPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path path2 = new Path();
    private final Path fishPath = new Path();
    private final RectF rectF = new RectF();

    // Interactive School of Fish
    private static final int FISH_COUNT = 8;
    private float schoolX = 600f, schoolY = 700f;
    private float targetFacingX = 1.0f; // +1.0 = swimming right, -1.0 = swimming left
    private float targetPitch = 0.0f; // radians, negative = up, positive = down
    private float schoolVx = 75f;
    private float schoolVy = 0f;
    private float schoolSpeed = 75f; // px/s
    private float cruiseSpeed = 75f;
    private long lastDrawMs = 0L;

    private final float[] fishOffX = new float[FISH_COUNT];
    private final float[] fishOffY = new float[FISH_COUNT];
    private final float[] fishScale = new float[FISH_COUNT];
    private final float[] fishWigglePhase = new float[FISH_COUNT];

    private final float[] fishPosX = new float[FISH_COUNT];
    private final float[] fishPosY = new float[FISH_COUNT];
    private final float[] fishFacing = new float[FISH_COUNT];
    private final float[] fishPitch = new float[FISH_COUNT];

    // Stars
    private static final int STAR_COUNT = 72;
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starRadius = new float[STAR_COUNT];
    private final float[] starPhase = new float[STAR_COUNT];

    // Rain particles: two depth layers for crisp visual fidelity
    private static final int RAIN_COUNT = 150;
    private final float[] rainX = new float[RAIN_COUNT];
    private final float[] rainPhase = new float[RAIN_COUNT];
    private final float[] rainSpeed = new float[RAIN_COUNT];
    private final float[] rainLen = new float[RAIN_COUNT];
    private final boolean[] rainIsFg = new boolean[RAIN_COUNT];
    private final float[] rainFgLines = new float[RAIN_COUNT * 4];
    private final float[] rainBgLines = new float[RAIN_COUNT * 4];

    // Wind wisps
    private static final int WISP_COUNT = 5;
    private final float[] wispY = new float[WISP_COUNT];
    private final float[] wispSpeed = new float[WISP_COUNT];
    private final float[] wispPhase = new float[WISP_COUNT];
    private final float[] wispLen = new float[WISP_COUNT];

    // Current interpolated state
    private int curSky0, curSky1, curSky2;
    private int curSea0, curSea1, curSea2;
    private int curSand0, curSand1, curPalm;
    private float curSunX, curSunY, curSunR, curSunAlpha;
    private boolean isMoon;
    private float curNightFactor;
    private float curSunStrength;
    private float effectiveHour = 12f;

    InkSky(WeatherSky helper) {
        this.helper = helper;
        strokePaint.setStyle(Paint.Style.STROKE);
        fishPaint.setStyle(Paint.Style.FILL);
        rainFgPaint.setStyle(Paint.Style.STROKE);
        rainFgPaint.setStrokeCap(Paint.Cap.ROUND);
        rainBgPaint.setStyle(Paint.Style.STROKE);
        rainBgPaint.setStrokeCap(Paint.Cap.ROUND);

        Random r = new Random(1024);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = r.nextFloat();
            starY[i] = r.nextFloat() * 0.50f;
            starRadius[i] = 0.8f + r.nextFloat() * 1.6f;
            starPhase[i] = r.nextFloat() * 6.28f;
        }

        Random rnd = new Random(42);
        for (int i = 0; i < RAIN_COUNT; i++) {
            rainX[i] = rnd.nextFloat();
            rainPhase[i] = rnd.nextFloat();
            rainSpeed[i] = 1900f + rnd.nextFloat() * 1300f;
            rainLen[i] = 38f + rnd.nextFloat() * 38f;
            rainIsFg[i] = rnd.nextFloat() < 0.35f;
        }

        for (int i = 0; i < WISP_COUNT; i++) {
            wispY[i] = 0.15f + i * 0.12f + (rnd.nextFloat() - 0.5f) * 0.05f;
            wispSpeed[i] = 55f + rnd.nextFloat() * 45f;
            wispPhase[i] = rnd.nextFloat() * 6.28f;
            wispLen[i] = 120f + rnd.nextFloat() * 100f;
        }

        // Initialize schooling formation offsets
        float[] baseOffsetsX = {0f, -42f, -50f, -86f, -95f, -135f, -145f, -170f};
        float[] baseOffsetsY = {0f, -22f, 26f, -38f, 32f, -16f, 44f, 8f};
        for (int i = 0; i < FISH_COUNT; i++) {
            fishOffX[i] = baseOffsetsX[i];
            fishOffY[i] = baseOffsetsY[i];
            fishScale[i] = 0.85f + rnd.nextFloat() * 0.30f;
            fishWigglePhase[i] = rnd.nextFloat() * 6.28f;
            fishFacing[i] = 1.0f;
            fishPitch[i] = 0.0f;
        }
    }

    void setSize(int width, int height) {
        if (width == w && height == h) return;
        this.w = width;
        this.h = height;
        this.k = width / 1920f;
        this.schoolX = width * 0.40f;
        this.schoolY = height * 0.68f;
        this.cruiseSpeed = 75f * k;
        this.schoolSpeed = cruiseSpeed;
        this.schoolVx = cruiseSpeed;
        this.schoolVy = 0f;
        this.targetFacingX = 1.0f;
        this.targetPitch = 0.0f;
        for (int i = 0; i < FISH_COUNT; i++) {
            fishPosX[i] = schoolX - Math.abs(fishOffX[i]) * k;
            fishPosY[i] = schoolY + fishOffY[i] * k;
            fishFacing[i] = 1.0f;
            fishPitch[i] = 0.0f;
        }
    }

    void release() {
        // Pure procedural canvas drawing without persistent heavy bitmaps
    }

    static float canonical(float hour, float rise, float set) {
        if (!(set > rise + 6f)) return hour;
        float c;
        if (hour >= rise && hour <= set) c = SUNRISE + (hour - rise) / (set - rise) * (SUNSET - SUNRISE);
        else if (hour > set) c = SUNSET + (hour - set);
        else c = SUNRISE - (rise - hour);
        return ((c % 24f) + 24f) % 24f;
    }

    static float night(float hour) {
        float h = ((hour % 24f) + 24f) % 24f;
        if (h >= 19.5f || h <= 4.8f) return 1f;
        if (h >= 6.8f && h <= 17.0f) return 0f;
        if (h < 6.8f) return 1f - (h - 4.8f) / 2.0f;
        return (h - 17.0f) / 2.5f;
    }

    int glassColor(int code, float hour) {
        computeLighting(WeatherSky.kind(code), hour);
        int base = blend(curSea1, curSky1, 0.35f);
        return (base & 0x00FFFFFF) | 0xDD000000;
    }

    boolean light(float[] out) {
        if (curSunAlpha <= 0.03f) return false;
        out[0] = curSunX;
        out[1] = curSunY;
        return true;
    }

    float lightStrength() {
        return curSunStrength;
    }

    /** Directs the school of fish to swim in the direction of the remote D-Pad navigation. */
    void onDpad(float dx, float dy) {
        if (dx < 0) {
            targetFacingX = -1.0f; // Swim left
            targetPitch = 0.0f;
            schoolSpeed = 240f * k;
        } else if (dx > 0) {
            targetFacingX = 1.0f; // Swim right
            targetPitch = 0.0f;
            schoolSpeed = 240f * k;
        }

        if (dy < 0) {
            // Swim upward towards upper sea layers with natural gentle tilt (~18 degrees)
            targetPitch = -0.32f;
            schoolSpeed = 220f * k;
        } else if (dy > 0) {
            // Swim downward towards ocean depths with natural gentle tilt (~18 degrees)
            targetPitch = 0.32f;
            schoolSpeed = 220f * k;
        }
    }

    void draw(Canvas c, int code, float hour, long ms, float parallax, boolean motion) {
        draw(c, code, hour, ms, parallax, 0f, motion);
    }

    void draw(Canvas c, int code, float hour, long ms, float parallaxX, float parallaxY, boolean motion) {
        if (w == 0 || h == 0) return;
        int kind = WeatherSky.kind(code);
        boolean isRaining = kind == RAIN || kind == STORM || (code >= 51 && code <= 57);
        float time = motion ? (ms % 3_600_000L) / 1000f : 0f;

        // Smooth time delta for fish swimming physics
        float dt = lastDrawMs > 0 ? (ms - lastDrawMs) / 1000f : 0.016f;
        if (dt > 0.1f) dt = 0.1f;
        lastDrawMs = ms;

        // Update fish swimming physics
        if (motion) {
            updateFishSchool(dt, time);
        }

        computeLighting(kind, hour);

        float horizonY = h * 0.52f;
        float skyParallaxX = -parallaxX * 18f * k;
        float skyParallaxY = -parallaxY * 10f * k;

        // 1. Sky gradient
        drawSky(c, horizonY + skyParallaxY * 0.4f);

        // 2. Stars at night
        if (curNightFactor > 0.05f) {
            drawStars(c, time, skyParallaxX, skyParallaxY, horizonY);
        }

        // 3. Sun or Moon rising/setting from ocean horizon
        drawCelestialBody(c, horizonY, skyParallaxX * 0.5f, skyParallaxY * 0.5f, time);

        // 4. Subtle wind wisps in upper sky
        if (motion) {
            drawWindWisps(c, time, skyParallaxX * 0.7f, skyParallaxY * 0.5f);
        }

        // 5. Lightning flash in storm
        if (kind == STORM && helper != null) {
            helper.drawLightning(c, time, skyParallaxX);
        }

        // 6. Layered animated ocean waves & school of fish swimming through the water
        drawOcean(c, horizonY, parallaxX, parallaxY, time, isRaining);

        // 7. Coastal beach shoreline & coconut palms (completely static and peaceful)
        drawShoreAndPalms(c, time, isRaining);

        // 8. Rain streaks overlay: luminous silver-white contrast across all daylight and night states
        if (isRaining) {
            drawRain(c, time, kind == STORM, -parallaxX * 60f * k);
        }
    }

    private void updateFishSchool(float dt, float time) {
        // Ease speed back down to cruising speed
        if (schoolSpeed > cruiseSpeed) {
            schoolSpeed -= (schoolSpeed - cruiseSpeed) * Math.min(1f, dt * 2.2f);
        }

        if (Math.abs(targetPitch) > 0.01f) {
            targetPitch -= targetPitch * Math.min(1f, dt * 0.9f);
        }
        float ambientPitch = targetPitch + (float) Math.sin(time * 1.4f) * 0.04f;

        // Hydrodynamic velocity interpolation (fluid deceleration and acceleration)
        float targetVx = targetFacingX * schoolSpeed;
        float targetVy = (float) Math.sin(ambientPitch) * schoolSpeed;
        schoolVx += (targetVx - schoolVx) * Math.min(1f, dt * 3.4f);
        schoolVy += (targetVy - schoolVy) * Math.min(1f, dt * 3.8f);

        schoolX += schoolVx * dt;
        schoolY += schoolVy * dt;

        // Boundary containment
        float horizonY = h * 0.52f;
        float topBound = horizonY + 45f * k;
        float bottomBound = h * 0.88f;

        if (schoolY < topBound) {
            schoolY = topBound;
            targetPitch = 0.25f;
        } else if (schoolY > bottomBound) {
            schoolY = bottomBound;
            targetPitch = -0.25f;
        }

        // Horizontal wrap-around
        float pad = 200f * k;
        if (schoolX > w + pad) {
            schoolX = -pad * 0.5f;
            float shiftR = (w + pad * 1.5f);
            for (int j = 0; j < FISH_COUNT; j++) fishPosX[j] -= shiftR;
        } else if (schoolX < -pad) {
            schoolX = w + pad * 0.5f;
            float shiftL = (w + pad * 1.5f);
            for (int j = 0; j < FISH_COUNT; j++) fishPosX[j] += shiftL;
        }

        // Cascading follower physics and 3D yaw turning across the school
        for (int i = 0; i < FISH_COUNT; i++) {
            float targetFx = schoolX - (targetFacingX * Math.abs(fishOffX[i]) * k);
            float targetFy = schoolY + fishOffY[i] * k;

            float followSpeed = 5.2f - i * 0.32f;
            fishPosX[i] += (targetFx - fishPosX[i]) * Math.min(1f, dt * followSpeed);
            fishPosY[i] += (targetFy - fishPosY[i]) * Math.min(1f, dt * 5.5f);

            // Staggered 3D yaw turning rate: leader turns first, followers follow
            float turnRate = 6.2f - i * 0.45f;
            fishFacing[i] += (targetFacingX - fishFacing[i]) * Math.min(1f, dt * turnRate);

            // Staggered pitch easing
            fishPitch[i] += (ambientPitch - fishPitch[i]) * Math.min(1f, dt * (5.5f - i * 0.25f));
        }
    }

    private void computeLighting(int kind, float hour) {
        effectiveHour = hour;
        float hNorm = ((hour % 24f) + 24f) % 24f;
        int idx = 0;
        while (idx < HOURS.length - 2 && HOURS[idx + 1] <= hNorm) {
            idx++;
        }
        float span = HOURS[idx + 1] - HOURS[idx];
        float t = span > 0.001f ? (hNorm - HOURS[idx]) / span : 0f;
        float ease = t * t * (3f - 2f * t);

        int[] skyA = SKY_COLORS[idx], skyB = SKY_COLORS[idx + 1];
        curSky0 = blend(skyA[0], skyB[0], ease);
        curSky1 = blend(skyA[1], skyB[1], ease);
        curSky2 = blend(skyA[2], skyB[2], ease);

        int[] seaA = SEA_COLORS[idx], seaB = SEA_COLORS[idx + 1];
        curSea0 = blend(seaA[0], seaB[0], ease);
        curSea1 = blend(seaA[1], seaB[1], ease);
        curSea2 = blend(seaA[2], seaB[2], ease);

        int[] shoreA = SHORE_COLORS[idx], shoreB = SHORE_COLORS[idx + 1];
        curSand0 = blend(shoreA[0], shoreB[0], ease);
        curSand1 = blend(shoreA[1], shoreB[1], ease);
        curPalm = blend(shoreA[2], shoreB[2], ease);

        curNightFactor = night(hNorm);

        // Weather tinting (rain / storm / cloud desaturation)
        float weatherTint = kind == STORM ? 0.75f : kind == RAIN ? 0.65f : kind == CLOUDY ? 0.40f : 0f;
        if (weatherTint > 0f) {
            curSky0 = blend(curSky0, RAIN_SKY[0], weatherTint);
            curSky1 = blend(curSky1, RAIN_SKY[1], weatherTint);
            curSky2 = blend(curSky2, RAIN_SKY[2], weatherTint);
            curSea0 = blend(curSea0, RAIN_SEA[0], weatherTint);
            curSea1 = blend(curSea1, RAIN_SEA[1], weatherTint);
            curSea2 = blend(curSea2, RAIN_SEA[2], weatherTint);
            curSand0 = blend(curSand0, RAIN_SHORE[0], weatherTint);
            curSand1 = blend(curSand1, RAIN_SHORE[1], weatherTint);
            curPalm = blend(curPalm, RAIN_SHORE[2], weatherTint);
        }

        // Sun / Moon trajectory: placed safely in center and center-right open sky to prevent overlapping text
        float horizonY = h * 0.52f;
        if (hNorm >= 5.0f && hNorm <= 19.2f) {
            isMoon = false;
            float sunProg = (hNorm - 5.0f) / 14.2f;
            curSunX = w * (0.50f + 0.22f * sunProg);
            float arc = (float) Math.sin(sunProg * Math.PI);
            curSunY = horizonY - arc * (h * 0.38f) + (1.0f - arc) * (18f * k);
            curSunR = (46f + (1.0f - arc) * 12f) * k;
            curSunAlpha = Math.min(1f, arc * 1.5f + 0.15f);
            curSunStrength = arc;
        } else {
            isMoon = true;
            float moonHour = hNorm >= 19.2f ? hNorm - 19.2f : hNorm + 4.8f;
            float moonProg = moonHour / 9.8f;
            curSunX = w * (0.50f + 0.20f * moonProg);
            float arc = (float) Math.sin(moonProg * Math.PI);
            curSunY = horizonY - arc * (h * 0.38f) + (1.0f - arc) * (14f * k);
            curSunR = 36f * k;
            curSunAlpha = Math.min(1f, arc * 1.4f + 0.2f);
            curSunStrength = arc * 0.65f;
        }
    }

    private void drawSky(Canvas c, float horizonY) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
            0, 0, 0, horizonY + 2f,
            new int[]{curSky0, curSky1, curSky2},
            new float[]{0f, 0.55f, 1f},
            Shader.TileMode.CLAMP
        ));
        c.drawRect(0, 0, w, horizonY + 2f, paint);
        paint.setShader(null);
    }

    private void drawStars(Canvas c, float time, float pX, float pY, float horizonY) {
        paint.setStyle(Paint.Style.FILL);
        float baseAlpha = curNightFactor;
        for (int i = 0; i < STAR_COUNT; i++) {
            float sx = (starX[i] * w + pX) % w;
            if (sx < 0) sx += w;
            float sy = starY[i] * h + pY;
            if (sy >= horizonY - 10f * k) continue;
            float twinkle = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(time * 2.2f + starPhase[i]));
            int alpha = Math.round(255f * baseAlpha * twinkle);
            if (alpha <= 0) continue;
            paint.setColor((0xFFF6F8FF & 0x00FFFFFF) | (alpha << 24));
            c.drawCircle(sx, sy, starRadius[i] * k, paint);
        }
    }

    private void drawCelestialBody(Canvas c, float horizonY, float pX, float pY, float time) {
        float x = curSunX + pX;
        float y = curSunY + pY;
        float r = curSunR;
        float alpha = curSunAlpha;
        if (alpha <= 0.01f) return;

        // Radial atmospheric glow
        int glowColor = isMoon ? 0xFFE0ECFF : (curNightFactor > 0.3f ? 0xFFFF7A38 : 0xFFFFEE99);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
            x, y, r * 3.4f,
            new int[]{Ui.alpha(glowColor, alpha * 0.45f), Ui.alpha(glowColor, alpha * 0.12f), glowColor & 0x00FFFFFF},
            new float[]{0f, 0.40f, 1f},
            Shader.TileMode.CLAMP
        ));
        c.drawCircle(x, y, r * 3.4f, paint);
        paint.setShader(null);

        // Core celestial disc
        if (!isMoon) {
            int sunTop = blend(0xFFFFFDF0, 0xFFFFE4A0, curNightFactor);
            int sunBottom = blend(0xFFFFB04A, 0xFFE64228, curNightFactor);
            paint.setShader(new LinearGradient(x, y - r, x, y + r, sunTop, sunBottom, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r, paint);
            paint.setShader(null);
        } else {
            // Moon disc with crater shading
            paint.setColor(Ui.alpha(0xFFF2F6FF, alpha));
            c.drawCircle(x, y, r, paint);
            paint.setColor(Ui.alpha(0xFFCBD6E8, alpha * 0.55f));
            c.drawCircle(x - r * 0.28f, y - r * 0.15f, r * 0.26f, paint);
            c.drawCircle(x + r * 0.22f, y + r * 0.24f, r * 0.22f, paint);
            c.drawCircle(x - r * 0.05f, y + r * 0.32f, r * 0.16f, paint);
        }
    }

    private void drawWindWisps(Canvas c, float time, float pX, float pY) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(1.4f * k);
        int wispColor = isMoon ? 0x1AE0ECFF : 0x22FFFFFF;
        strokePaint.setColor(wispColor);

        for (int i = 0; i < WISP_COUNT; i++) {
            float speed = wispSpeed[i] * k;
            float wx = ((time * speed + wispPhase[i] * 300f + pX) % (w + 400f * k)) - 200f * k;
            float wy = wispY[i] * h + pY + (float) Math.sin(wx * 0.003f + time * 0.8f + i) * 12f * k;
            float len = wispLen[i] * k;

            path.reset();
            path.moveTo(wx, wy);
            path.quadTo(wx + len * 0.5f, wy - 8f * k, wx + len, wy + (float) Math.sin(time + i) * 6f * k);
            c.drawPath(path, strokePaint);
        }
    }

    private void drawOcean(Canvas c, float horizonY, float pX, float pY, float time, boolean rain) {
        float chop = rain ? 1.45f : 1.0f;
        float speed = rain ? 1.4f : 1.0f;

        float parHorizonY = horizonY - pY * 6f * k;

        // 1. Distant base sea
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
            0, parHorizonY, 0, h,
            new int[]{curSea0, curSea1, curSea2},
            new float[]{0f, 0.40f, 1f},
            Shader.TileMode.CLAMP
        ));
        c.drawRect(0, parHorizonY, w, h, paint);
        paint.setShader(null);

        // 2. Celestial reflection corridor
        drawReflectionPath(c, parHorizonY, pX, time);

        // 3. Layer 1: Distant subtle wave ripples (peaceful, rhythmic 60 FPS motion)
        drawWaveLayer(c, parHorizonY + 16f * k, 0.006f, 3.5f * k * chop,
            time * 1.1f * speed, 0f, curSea0, 0.40f, -pX * 14f * k, false);

        // 4. Interactive School of Fish swimming in the mid-depths
        drawFishSchool(c, time);

        // 5. Layer 2: Midground ocean swell
        drawWaveLayer(c, parHorizonY + h * 0.12f, 0.0042f, 8.5f * k * chop,
            time * 1.5f * speed, 1.8f, curSea1, 0.70f, -pX * 28f * k, true);

        // 6. Layer 3: Foreground surf roll
        drawWaveLayer(c, parHorizonY + h * 0.26f, 0.0028f, 14.0f * k * chop,
            time * 1.9f * speed, 3.6f, curSea2, 0.95f, -pX * 45f * k, true);
    }

    /**
     * Renders an interactive school of vector fish gliding through the ocean water,
     * undulating their tails and swimming in the direction of the user's remote D-Pad navigation.
     */
    private void drawFishSchool(Canvas c, float time) {
        // Compute fish colors matching current time-of-day lighting
        int fishBodyColor, fishSheenColor;
        if (isMoon) {
            fishBodyColor = 0x88B0D8F8; // Bioluminescent silver-cyan
            fishSheenColor = 0xC0E4F4FF;
        } else if (effectiveHour >= 16.0f && effectiveHour <= 19.2f) {
            fishBodyColor = 0x99F6B894; // Sunset coral glow
            fishSheenColor = 0xC8FFE2CE;
        } else if (effectiveHour >= 5.0f && effectiveHour < 7.0f) {
            fishBodyColor = 0x90F2C2B8; // Sunrise peach-rose sheen
            fishSheenColor = 0xC0FFE6DF;
        } else {
            fishBodyColor = 0x8CE0F2FE; // Shimmering tropical aqua-silver
            fishSheenColor = 0xC0F0F9FF;
        }

        for (int i = 0; i < FISH_COUNT; i++) {
            float fx = fishPosX[i];
            float fy = fishPosY[i];

            float facing = fishFacing[i];
            float absFacing = Math.abs(facing);
            float sign = facing >= 0f ? 1f : -1f;

            // 3D perspective yaw scale: smoothly narrows to a thin sleek profile, then expands facing other way
            float drawScaleX = sign * Math.max(0.18f, absFacing);
            // Vertical banking arch during the apex of turn
            float turnBank = (1f - absFacing * absFacing) * (sign * -6f * k);
            float drawScaleY = 1.0f + (1f - absFacing) * 0.16f;

            float sc = fishScale[i] * k;
            float flen = 32f * sc;
            float fht = flen * 0.36f;

            // Tail wagging speed: ambient wag + speed boost + turn flick
            float wagRate = (8.5f + (schoolSpeed / (cruiseSpeed + 1f)) * 4.5f) + (1f - absFacing) * 6.0f;
            float tailWag = (float) Math.sin(time * wagRate + fishWigglePhase[i]) * (flen * 0.24f);

            c.save();
            c.translate(fx, fy + turnBank);
            c.rotate((float) Math.toDegrees(fishPitch[i]));
            c.scale(drawScaleX, drawScaleY);

            // Streamlined fish body
            fishPath.reset();
            fishPath.moveTo(flen * 0.5f, 0f); // Head snout
            fishPath.cubicTo(
                flen * 0.20f, -fht * 0.95f,
                -flen * 0.25f, -fht * 0.70f,
                -flen * 0.45f, 0f // Tail peduncle
            );
            fishPath.cubicTo(
                -flen * 0.25f, fht * 0.70f,
                flen * 0.20f, fht * 0.95f,
                flen * 0.5f, 0f
            );
            fishPath.close();

            fishPaint.setColor(fishBodyColor);
            c.drawPath(fishPath, fishPaint);

            // Caudal fin (tail fin) swinging with harmonic undulation
            fishPath.reset();
            fishPath.moveTo(-flen * 0.42f, 0f);
            fishPath.lineTo(-flen * 0.82f, -fht * 0.85f + tailWag);
            fishPath.quadTo(-flen * 0.65f, tailWag * 0.5f, -flen * 0.82f, fht * 0.85f + tailWag);
            fishPath.close();

            fishPaint.setColor(fishSheenColor);
            c.drawPath(fishPath, fishPaint);

            // Dorsal fin
            fishPath.reset();
            fishPath.moveTo(-flen * 0.05f, -fht * 0.55f);
            fishPath.lineTo(-flen * 0.25f, -fht * 1.15f);
            fishPath.lineTo(-flen * 0.30f, -fht * 0.40f);
            fishPath.close();
            c.drawPath(fishPath, fishPaint);

            // Glistening eye
            if (absFacing > 0.25f) {
                fishPaint.setColor(0xE0FFFFFF);
                c.drawCircle(flen * 0.32f, -fht * 0.18f, 1.8f * sc, fishPaint);
            }

            c.restore();
        }
    }

    private void drawReflectionPath(Canvas c, float horizonY, float pX, float time) {
        if (curSunAlpha <= 0.05f) return;
        float cx = curSunX - pX * 18f * k;
        if (cx < -100f * k || cx > w + 100f * k) return;

        int gleam = isMoon ? 0xFFD8EAFF : (curNightFactor > 0.35f ? 0xFFFF8E32 : 0xFFFFD66E);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        for (float y = horizonY + 2f * k; y < h; y += 14f * k) {
            float progress = (y - horizonY) / (h - horizonY);
            float spread = (20f + progress * 240f) * k;
            float waveWobble = (float) Math.sin(y * 0.04f + time * 3.5f) * 14f * k * progress;
            float rx = cx + waveWobble;
            float shimmer = 0.35f + 0.65f * (float) Math.sin(y * 0.15f - time * 5f);
            int alpha = Math.round(255f * curSunAlpha * (1f - progress * 0.65f) * shimmer * 0.45f);
            if (alpha <= 0) continue;

            strokePaint.setColor((gleam & 0x00FFFFFF) | (alpha << 24));
            strokePaint.setStrokeWidth((2.0f + progress * 3.5f) * k);
            c.drawLine(rx - spread * 0.5f, y, rx + spread * 0.5f, y, strokePaint);
        }
    }

    private void drawWaveLayer(Canvas c, float baseY, float freq, float amp, float phase, float offset,
                               int color, float alpha, float pX, boolean hasFoam) {
        path.reset();
        path.moveTo(0, h);
        float step = 20f * k;
        for (float x = 0; x <= w + step; x += step) {
            float y = baseY + (float) Math.sin((x + pX) * freq + phase + offset) * amp
                + (float) Math.cos((x + pX) * (freq * 1.8f) - phase * 0.7f) * (amp * 0.45f);
            if (x == 0) path.lineTo(0, y);
            else path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.alpha(color, alpha));
        c.drawPath(path, paint);

        if (hasFoam) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.2f * k);
            float foamAlpha = isMoon ? 0.35f : 0.50f;
            int foamRgb = isMoon ? 0xE8F4FF : (curNightFactor > 0.4f ? 0xFFDEC0 : 0xFFFFFF);
            strokePaint.setColor((foamRgb & 0x00FFFFFF) | (Math.round(255f * foamAlpha) << 24));

            path2.reset();
            for (float x = 0; x <= w + step; x += step) {
                float y = baseY + (float) Math.sin((x + pX) * freq + phase + offset) * amp
                    + (float) Math.cos((x + pX) * (freq * 1.8f) - phase * 0.7f) * (amp * 0.45f);
                if (x == 0) path2.moveTo(0, y);
                else path2.lineTo(x, y);
            }
            c.drawPath(path2, strokePaint);
        }
    }

    /** Shoreline and palm trees stay serenely static, rooted without camera movement. */
    private void drawShoreAndPalms(Canvas c, float time, boolean rain) {
        // 1. Wet sand beach tidal zone
        path.reset();
        path.moveTo(w * 0.48f, h);
        path.cubicTo(
            w * 0.62f, h * 0.88f,
            w * 0.75f, h * 0.80f,
            w, h * 0.76f
        );
        path.lineTo(w, h);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(curSand1);
        c.drawPath(path, paint);

        // 2. Main dry sand dune
        path.reset();
        path.moveTo(w * 0.56f, h);
        path.cubicTo(
            w * 0.68f, h * 0.84f,
            w * 0.80f, h * 0.74f,
            w, h * 0.70f
        );
        path.lineTo(w, h);
        path.close();
        paint.setColor(curSand0);
        c.drawPath(path, paint);

        // 3. Vector coconut palm trees (peaceful ambient wind sway)
        drawPalmTree(c, w * 0.83f, h * 0.88f, 460f * k, -0.18f, time, rain, 0);
        drawPalmTree(c, w * 0.93f, h * 0.89f, 380f * k, 0.12f, time, rain, 1);
    }

    private void drawPalmTree(Canvas c, float baseX, float baseY, float treeHeight, float lean, float time, boolean rain, int seed) {
        float windSpeed = rain ? 2.5f : 1.5f;
        float swayAngle = (float) Math.sin(time * windSpeed + seed * 1.5f) * (rain ? 0.08f : 0.04f);
        float totalLean = lean + swayAngle;

        float crownX = baseX + (float) Math.sin(totalLean) * treeHeight;
        float crownY = baseY - (float) Math.cos(totalLean) * treeHeight;
        float midX = (baseX + crownX) * 0.5f + (float) Math.sin(totalLean * 1.4f) * (treeHeight * 0.12f);
        float midY = (baseY + crownY) * 0.5f;

        // Trunk
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(curPalm);

        float baseW = 20f * k;
        float crownW = 9f * k;
        path.reset();
        path.moveTo(baseX - baseW * 0.5f, baseY);
        path.quadTo(midX - (baseW + crownW) * 0.28f, midY, crownX - crownW * 0.5f, crownY);
        path.lineTo(crownX + crownW * 0.5f, crownY);
        path.quadTo(midX + (baseW + crownW) * 0.28f, midY, baseX + baseW * 0.5f, baseY);
        path.close();
        c.drawPath(path, paint);

        // Coconuts cluster
        paint.setColor(blend(curPalm, 0xFF000000, 0.25f));
        c.drawCircle(crownX - 5f * k, crownY + 8f * k, 6.5f * k, paint);
        c.drawCircle(crownX + 5f * k, crownY + 9f * k, 6.0f * k, paint);
        c.drawCircle(crownX, crownY + 14f * k, 5.5f * k, paint);

        // 7 Arching palm fronds
        float[] frondAngles = {-2.4f, -1.8f, -1.1f, -0.5f, 0.2f, 0.8f, 1.4f};
        float[] frondLens = {170f, 210f, 240f, 220f, 190f, 160f, 140f};

        for (int i = 0; i < frondAngles.length; i++) {
            float frondSway = (float) Math.sin(time * (windSpeed * 1.2f) + i * 0.7f + seed) * (rain ? 0.12f : 0.06f);
            float angle = totalLean + frondAngles[i] + frondSway;
            float fLen = frondLens[i] * k;

            float tipX = crownX + (float) Math.cos(angle) * fLen;
            float tipY = crownY + (float) Math.sin(angle) * fLen + (fLen * 0.22f);
            float ctrlX = crownX + (float) Math.cos(angle) * (fLen * 0.55f);
            float ctrlY = crownY + (float) Math.sin(angle) * (fLen * 0.45f) - (fLen * 0.10f);

            // Leaf silhouette
            path.reset();
            path.moveTo(crownX, crownY);
            float leafW = 22f * k;
            float normX = -(float) Math.sin(angle) * leafW;
            float normY = (float) Math.cos(angle) * leafW;
            path.quadTo(ctrlX + normX, ctrlY + normY, tipX, tipY);
            path.quadTo(ctrlX - normX * 0.6f, ctrlY - normY * 0.6f, crownX, crownY);
            path.close();

            paint.setColor(curPalm);
            c.drawPath(path, paint);

            // Frond central spine line
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(1.6f * k);
            strokePaint.setColor(blend(curPalm, 0xFFFFFFFF, 0.15f));
            path2.reset();
            path2.moveTo(crownX, crownY);
            path2.quadTo(ctrlX, ctrlY, tipX, tipY);
            c.drawPath(path2, strokePaint);
        }
    }

    /**
     * Renders crisp, luminous silver-white raindrops with clear contrast across all daylight,
     * golden-hour, sunset, and nighttime conditions.
     */
    private void drawRain(Canvas c, float time, boolean heavy, float shift) {
        int count = heavy ? RAIN_COUNT : 100;
        int fgCount = 0, bgCount = 0;
        float span = h + 240f * k;
        float slant = 0.16f;
        float wrap = w + 300f * k;

        for (int i = 0; i < count; i++) {
            float y = ((rainPhase[i] + time * rainSpeed[i] / span) % 1.0f) * span - 120f * k;
            float x = rainX[i] * wrap - y * slant + shift;
            x = ((x % wrap) + wrap) % wrap - 100f * k;

            if (rainIsFg[i]) {
                rainFgLines[fgCount] = x;
                rainFgLines[fgCount + 1] = y;
                rainFgLines[fgCount + 2] = x - rainLen[i] * k * slant;
                rainFgLines[fgCount + 3] = y + rainLen[i] * k;
                fgCount += 4;
            } else {
                rainBgLines[bgCount] = x;
                rainBgLines[bgCount + 1] = y;
                rainBgLines[bgCount + 2] = x - rainLen[i] * k * slant * 0.75f;
                rainBgLines[bgCount + 3] = y + rainLen[i] * k * 0.75f;
                bgCount += 4;
            }
        }

        // Luminous water drops reflecting daylight, sunset glow, or moon
        int fgColor, bgColor;
        if (isMoon) {
            fgColor = 0x8CD2E6FF; // Luminous moonlit silver
            bgColor = 0x48A0C2E8;
        } else if (effectiveHour >= 16.0f && effectiveHour <= 19.2f) {
            fgColor = 0xAAFFE6D4; // Sunset amber-gold rim reflection
            bgColor = 0x55E8B49E;
        } else if (effectiveHour >= 5.0f && effectiveHour < 7.0f) {
            fgColor = 0xAAF8E4DF; // Sunrise coral gleam
            bgColor = 0x55E4B2AA;
        } else {
            fgColor = 0xA0F2F8FF; // Bright crystal daylight drops
            bgColor = 0x58D0E4FA;
        }

        rainBgPaint.setColor(bgColor);
        rainBgPaint.setStrokeWidth(Math.max(1.0f, 1.4f * k));
        c.drawLines(rainBgLines, 0, bgCount, rainBgPaint);

        rainFgPaint.setColor(fgColor);
        rainFgPaint.setStrokeWidth(Math.max(1.6f, 2.3f * k));
        c.drawLines(rainFgLines, 0, fgCount, rainFgPaint);
    }

    private static int blend(int c0, int c1, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a0 = (c0 >>> 24), r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
        int a1 = (c1 >>> 24), r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a = Math.round(a0 + (a1 - a0) * t);
        int r = Math.round(r0 + (r1 - r0) * t);
        int g = Math.round(g0 + (g1 - g0) * t);
        int b = Math.round(b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
