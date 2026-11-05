/**
 * Dynamic Ocean & Coast vector wallpaper engine for HTML5 Canvas.
 * Mathematically mirrors InkSky.java:
 * - Open ocean horizon and 3 layered sinusoidal wave paths (serene 60 FPS motion)
 * - Real-time sun/moon rise and set directly from the sea horizon
 * - Molten reflection corridor across the water ripples
 * - Coastal sand dune shoreline and wind-swayed coconut palms (static camera anchor)
 * - Interactive School of Fish gliding through the water, responsive to D-Pad (Left/Right/Up/Down)
 * - Luminous silver-white raindrops with clear contrast across all daylight and night states
 */
(function (global) {
  'use strict';

  var HOURS = [0, 4.5, 5.5, 6.5, 10.0, 13.0, 16.5, 18.0, 19.2, 21.0, 24.0];

  var SKY_COLORS = [
    [0x06, 0x0B, 0x14, 0x0B, 0x16, 0x28, 0x12, 0x24, 0x3C], // Midnight
    [0x08, 0x0F, 0x1E, 0x10, 0x1F, 0x38, 0x18, 0x30, 0x4E], // Pre-dawn
    [0x2E, 0x19, 0x3C, 0x87, 0x38, 0x5A, 0xE8, 0x77, 0x66], // Dawn coral pink
    [0x4A, 0x24, 0x48, 0xD8, 0x67, 0x62, 0xFF, 0xBE, 0x85], // Sunrise peach gold
    [0x12, 0x68, 0xA8, 0x2B, 0xB0, 0xD4, 0x6F, 0xE2, 0xE4], // Morning turquoise
    [0x10, 0x70, 0xB2, 0x32, 0xB4, 0xD8, 0x7A, 0xE6, 0xE8], // Midday bright blue
    [0x1A, 0x38, 0x5C, 0x7B, 0x38, 0x58, 0xE0, 0x6C, 0x42], // Golden hour
    [0x1E, 0x14, 0x30, 0x7D, 0x1E, 0x4E, 0xFF, 0x6C, 0x2C], // Sunset amber magenta
    [0x14, 0x12, 0x26, 0x35, 0x20, 0x42, 0x7A, 0x36, 0x44], // Dusk twilight
    [0x09, 0x0E, 0x1A, 0x0F, 0x1A, 0x2E, 0x14, 0x24, 0x3A], // Nightfall indigo
    [0x06, 0x0B, 0x14, 0x0B, 0x16, 0x28, 0x12, 0x24, 0x3C]  // Midnight
  ];

  var SEA_COLORS = [
    [0x0A, 0x16, 0x26, 0x07, 0x10, 0x1E, 0x04, 0x0A, 0x14], // Midnight
    [0x0E, 0x1E, 0x34, 0x09, 0x14, 0x24, 0x05, 0x0C, 0x18], // Pre-dawn
    [0x66, 0x30, 0x48, 0x2A, 0x1C, 0x34, 0x12, 0x10, 0x20], // Dawn rosy reflection
    [0x8A, 0x46, 0x44, 0x32, 0x36, 0x4C, 0x16, 0x1E, 0x2E], // Sunrise gold gleam
    [0x16, 0x8C, 0xA6, 0x0E, 0x54, 0x78, 0x08, 0x32, 0x4E], // Morning cyan
    [0x1A, 0xA0, 0xBC, 0x10, 0x5C, 0x84, 0x0A, 0x36, 0x54], // Midday deep blue
    [0x78, 0x3E, 0x38, 0x2E, 0x30, 0x48, 0x14, 0x1A, 0x28], // Golden hour sea
    [0x9C, 0x40, 0x28, 0x3E, 0x24, 0x3A, 0x18, 0x14, 0x22], // Sunset fiery reflection
    [0x38, 0x1C, 0x2C, 0x1A, 0x18, 0x28, 0x0E, 0x0E, 0x1A], // Dusk deep sea
    [0x0E, 0x1A, 0x2C, 0x0A, 0x12, 0x20, 0x06, 0x0B, 0x14], // Nightfall
    [0x0A, 0x16, 0x26, 0x07, 0x10, 0x1E, 0x04, 0x0A, 0x14]  // Midnight
  ];

  var SHORE_COLORS = [
    [0x16, 0x1E, 0x2A, 0x0E, 0x14, 0x1E, 0x08, 0x0D, 0x14], // Midnight
    [0x1A, 0x22, 0x30, 0x12, 0x18, 0x24, 0x0A, 0x0F, 0x18], // Pre-dawn
    [0x7C, 0x5A, 0x56, 0x58, 0x3A, 0x3C, 0x28, 0x18, 0x22], // Dawn
    [0xC2, 0x8E, 0x70, 0x8A, 0x5C, 0x50, 0x34, 0x1C, 0x24], // Sunrise
    [0xDF, 0xC0, 0x88, 0xB4, 0x92, 0x5C, 0x14, 0x28, 0x1E], // Morning
    [0xE2, 0xC4, 0x8E, 0xBA, 0x96, 0x62, 0x16, 0x2C, 0x20], // Midday
    [0xC4, 0x8E, 0x66, 0x8E, 0x5C, 0x46, 0x2E, 0x1C, 0x22], // Golden hour
    [0xB8, 0x66, 0x44, 0x7A, 0x3E, 0x34, 0x24, 0x12, 0x1A], // Sunset
    [0x42, 0x28, 0x34, 0x2C, 0x1A, 0x24, 0x14, 0x0C, 0x12], // Dusk
    [0x1C, 0x22, 0x2E, 0x12, 0x16, 0x20, 0x0A, 0x0E, 0x16], // Nightfall
    [0x16, 0x1E, 0x2A, 0x0E, 0x14, 0x1E, 0x08, 0x0D, 0x14]  // Midnight
  ];

  var RAIN_SKY = [0x1E, 0x28, 0x34, 0x28, 0x34, 0x42, 0x3A, 0x48, 0x56];
  var RAIN_SEA = [0x26, 0x32, 0x3E, 0x1A, 0x24, 0x2E, 0x10, 0x16, 0x1E];
  var RAIN_SHORE = [0x38, 0x40, 0x4A, 0x28, 0x30, 0x38, 0x14, 0x18, 0x1E];

  function blendRgb(a, b, t) {
    t = Math.max(0, Math.min(1, t));
    return [
      Math.round(a[0] + (b[0] - a[0]) * t),
      Math.round(a[1] + (b[1] - a[1]) * t),
      Math.round(a[2] + (b[2] - a[2]) * t)
    ];
  }

  function rgbStr(c, alpha) {
    if (alpha !== undefined && alpha < 1) {
      return 'rgba(' + c[0] + ',' + c[1] + ',' + c[2] + ',' + Math.max(0, Math.min(1, alpha)).toFixed(3) + ')';
    }
    return 'rgb(' + c[0] + ',' + c[1] + ',' + c[2] + ')';
  }

  function OceanEngine(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.w = canvas.width || 1920;
    this.h = canvas.height || 1080;
    this.k = this.w / 1920;

    // Simulation states
    this.hour = -1; // -1 for auto real-time
    this.isRaining = false;
    this.motion = true;

    // Subtle background parallax
    this.parallaxX = 0;
    this.parallaxTargetX = 0;
    this.parallaxY = 0;
    this.parallaxTargetY = 0;

    // Interactive School of Fish
    this.fishCount = 8;
    this.schoolX = this.w * 0.40;
    this.schoolY = this.h * 0.68;
    this.targetFacingX = 1.0; // +1.0 = swimming right, -1.0 = swimming left
    this.targetPitch = 0.0; // radians, negative = up, positive = down
    this.schoolVx = 75 * this.k;
    this.schoolVy = 0.0;
    this.cruiseSpeed = 75 * this.k;
    this.schoolSpeed = this.cruiseSpeed;

    this.fishOffsetsX = [0, -42, -50, -86, -95, -135, -145, -170];
    this.fishOffsetsY = [0, -22, 26, -38, 32, -16, 44, 8];
    this.fishScales = [1.1, 1.0, 0.95, 0.9, 0.85, 0.9, 0.85, 0.8];
    this.fishWiggles = [0, 0.8, 1.5, 2.3, 3.1, 4.0, 4.8, 5.5];

    this.fishPosX = new Float32Array(8);
    this.fishPosY = new Float32Array(8);
    this.fishFacing = new Float32Array(8);
    this.fishPitch = new Float32Array(8);

    for (var fi = 0; fi < 8; fi++) {
      this.fishPosX[fi] = this.schoolX - Math.abs(this.fishOffsetsX[fi]) * this.k;
      this.fishPosY[fi] = this.schoolY + this.fishOffsetsY[fi] * this.k;
      this.fishFacing[fi] = 1.0;
      this.fishPitch[fi] = 0.0;
    }

    // Particles
    this.initParticles();

    // Bind keyboard arrow keys
    this.bindKeys();

    // Animation frame timing
    this.lastTime = 0;
    this.runningTime = 0;
  }

  OceanEngine.prototype.initParticles = function () {
    var i;
    this.stars = [];
    for (i = 0; i < 72; i++) {
      this.stars.push({
        x: Math.random(),
        y: Math.random() * 0.50,
        r: 0.8 + Math.random() * 1.6,
        phase: Math.random() * 6.28
      });
    }

    this.rain = [];
    for (i = 0; i < 150; i++) {
      this.rain.push({
        x: Math.random(),
        phase: Math.random(),
        speed: 1900 + Math.random() * 1300,
        len: 38 + Math.random() * 38,
        isFg: Math.random() < 0.35
      });
    }

    this.wisps = [];
    for (i = 0; i < 5; i++) {
      this.wisps.push({
        y: 0.15 + i * 0.12 + (Math.random() - 0.5) * 0.05,
        speed: 55 + Math.random() * 45,
        phase: Math.random() * 6.28,
        len: 120 + Math.random() * 100
      });
    }
  };

  OceanEngine.prototype.bindKeys = function () {
    var self = this;
    window.addEventListener('keydown', function (e) {
      if (e.keyCode === 37) { // Left
        self.onDpad(-1, 0);
      } else if (e.keyCode === 39) { // Right
        self.onDpad(1, 0);
      } else if (e.keyCode === 38) { // Up
        self.onDpad(0, -1);
      } else if (e.keyCode === 40) { // Down
        self.onDpad(0, 1);
      } else if (e.key === 'r' || e.key === 'R') {
        self.isRaining = !self.isRaining;
        var btn = document.getElementById('btnRainToggle');
        if (btn) btn.textContent = self.isRaining ? 'Rain: ON' : 'Rain: OFF';
      } else if (e.key === '1') {
        self.setHour(6.0); // Sunrise
      } else if (e.key === '2') {
        self.setHour(12.0); // Midday
      } else if (e.key === '3') {
        self.setHour(18.0); // Sunset
      } else if (e.key === '4') {
        self.setHour(23.5); // Night
      } else if (e.key === '0') {
        self.setHour(-1); // Live real-time
      }
    });
  };

  /** Directs the school of fish to swim in the direction of D-Pad navigation. */
  OceanEngine.prototype.triggerWaveSurge = function (dx, dy) { this.onDpad(dx, dy); };
  OceanEngine.prototype.onDpad = function (dx, dy) {
    if (dx < 0) {
      this.targetFacingX = -1.0; // Swim left
      this.targetPitch = 0.0;
      this.schoolSpeed = 240 * this.k;
      this.parallaxTargetX = Math.max(-0.8, Math.min(0.8, this.parallaxTargetX - 0.12));
    } else if (dx > 0) {
      this.targetFacingX = 1.0; // Swim right
      this.targetPitch = 0.0;
      this.schoolSpeed = 240 * this.k;
      this.parallaxTargetX = Math.max(-0.8, Math.min(0.8, this.parallaxTargetX + 0.12));
    }

    if (dy < 0) {
      // Swim upward towards upper sea layers with natural gentle tilt (~18 degrees)
      this.targetPitch = -0.32;
      this.schoolSpeed = 220 * this.k;
      this.parallaxTargetY = Math.max(-0.5, Math.min(0.5, this.parallaxTargetY - 0.10));
    } else if (dy > 0) {
      // Swim downward towards ocean depths with natural gentle tilt (~18 degrees)
      this.targetPitch = 0.32;
      this.schoolSpeed = 220 * this.k;
      this.parallaxTargetY = Math.max(-0.5, Math.min(0.5, this.parallaxTargetY + 0.10));
    }
  };

  OceanEngine.prototype.setShelfIndex = function (index, total) {
    if (total > 1) {
      this.parallaxTargetX = (index / (total - 1)) * 1.2 - 0.6;
    }
  };

  OceanEngine.prototype.setZone = function (zoneName) {
    if (zoneName === 'top') {
      this.parallaxTargetY = -0.25;
    } else if (zoneName === 'drawer') {
      this.parallaxTargetY = 0.25;
    } else {
      this.parallaxTargetY = 0;
    }
  };

  OceanEngine.prototype.setHour = function (h) {
    this.hour = h;
  };

  OceanEngine.prototype.getEffectiveHour = function () {
    if (this.hour >= 0) return this.hour;
    var d = new Date();
    return d.getHours() + d.getMinutes() / 60 + d.getSeconds() / 3600;
  };

  OceanEngine.prototype.computeLighting = function (hNorm) {
    var idx = 0;
    while (idx < HOURS.length - 2 && HOURS[idx + 1] <= hNorm) {
      idx++;
    }
    var span = HOURS[idx + 1] - HOURS[idx];
    var t = span > 0.001 ? (hNorm - HOURS[idx]) / span : 0;
    var ease = t * t * (3 - 2 * t);

    var skA = SKY_COLORS[idx], skB = SKY_COLORS[idx + 1];
    var curSky0 = blendRgb([skA[0], skA[1], skA[2]], [skB[0], skB[1], skB[2]], ease);
    var curSky1 = blendRgb([skA[3], skA[4], skA[5]], [skB[3], skB[4], skB[5]], ease);
    var curSky2 = blendRgb([skA[6], skA[7], skA[8]], [skB[6], skB[7], skB[8]], ease);

    var seA = SEA_COLORS[idx], seB = SEA_COLORS[idx + 1];
    var curSea0 = blendRgb([seA[0], seA[1], seA[2]], [seB[0], seB[1], seB[2]], ease);
    var curSea1 = blendRgb([seA[3], seA[4], seA[5]], [seB[3], seB[4], seB[5]], ease);
    var curSea2 = blendRgb([seA[6], seA[7], seA[8]], [seB[6], seB[7], seB[8]], ease);

    var shA = SHORE_COLORS[idx], shB = SHORE_COLORS[idx + 1];
    var curSand0 = blendRgb([shA[0], shA[1], shA[2]], [shB[0], shB[1], shB[2]], ease);
    var curSand1 = blendRgb([shA[3], shA[4], shA[5]], [shB[3], shB[4], shB[5]], ease);
    var curPalm = blendRgb([shA[6], shA[7], shA[8]], [shB[6], shB[7], shB[8]], ease);

    var nightFactor = 0;
    if (hNorm >= 19.5 || hNorm <= 4.8) nightFactor = 1;
    else if (hNorm >= 6.8 && hNorm <= 17.0) nightFactor = 0;
    else if (hNorm < 6.8) nightFactor = 1 - (hNorm - 4.8) / 2.0;
    else nightFactor = (hNorm - 17.0) / 2.5;

    // Desaturate if raining
    if (this.isRaining) {
      curSky0 = blendRgb(curSky0, [RAIN_SKY[0], RAIN_SKY[1], RAIN_SKY[2]], 0.65);
      curSky1 = blendRgb(curSky1, [RAIN_SKY[3], RAIN_SKY[4], RAIN_SKY[5]], 0.65);
      curSky2 = blendRgb(curSky2, [RAIN_SKY[6], RAIN_SKY[7], RAIN_SKY[8]], 0.65);

      curSea0 = blendRgb(curSea0, [RAIN_SEA[0], RAIN_SEA[1], RAIN_SEA[2]], 0.65);
      curSea1 = blendRgb(curSea1, [RAIN_SEA[3], RAIN_SEA[4], RAIN_SEA[5]], 0.65);
      curSea2 = blendRgb(curSea2, [RAIN_SEA[6], RAIN_SEA[7], RAIN_SEA[8]], 0.65);

      curSand0 = blendRgb(curSand0, [RAIN_SHORE[0], RAIN_SHORE[1], RAIN_SHORE[2]], 0.65);
      curSand1 = blendRgb(curSand1, [RAIN_SHORE[3], RAIN_SHORE[4], RAIN_SHORE[5]], 0.65);
      curPalm = blendRgb(curPalm, [RAIN_SHORE[6], RAIN_SHORE[7], RAIN_SHORE[8]], 0.65);
    }

    var horizonY = this.h * 0.52;
    var isMoon = false;
    var sunX, sunY, sunR, sunAlpha;

    if (hNorm >= 5.0 && hNorm <= 19.2) {
      isMoon = false;
      var sunProg = (hNorm - 5.0) / 14.2;
      sunX = this.w * (0.50 + 0.22 * sunProg);
      var arc = Math.sin(sunProg * Math.PI);
      sunY = horizonY - arc * (this.h * 0.38) + (1.0 - arc) * (18 * this.k);
      sunR = (46 + (1.0 - arc) * 12) * this.k;
      sunAlpha = Math.min(1, arc * 1.5 + 0.15);
    } else {
      isMoon = true;
      var moonHour = hNorm >= 19.2 ? hNorm - 19.2 : hNorm + 4.8;
      var moonProg = moonHour / 9.8;
      sunX = this.w * (0.50 + 0.20 * moonProg);
      var arcM = Math.sin(moonProg * Math.PI);
      sunY = horizonY - arcM * (this.h * 0.38) + (1.0 - arcM) * (14 * this.k);
      sunR = 36 * this.k;
      sunAlpha = Math.min(1, arcM * 1.4 + 0.2);
    }

    return {
      sky0: curSky0, sky1: curSky1, sky2: curSky2,
      sea0: curSea0, sea1: curSea1, sea2: curSea2,
      sand0: curSand0, sand1: curSand1, palm: curPalm,
      nightFactor: nightFactor,
      isMoon: isMoon,
      sunX: sunX, sunY: sunY, sunR: sunR, sunAlpha: sunAlpha
    };
  };

  OceanEngine.prototype.updateFishSchool = function (dt, time) {
    // Ease speed back down to cruising speed
    if (this.schoolSpeed > this.cruiseSpeed) {
      this.schoolSpeed -= (this.schoolSpeed - this.cruiseSpeed) * Math.min(1, dt * 2.2);
    }

    if (Math.abs(this.targetPitch) > 0.01) {
      this.targetPitch -= this.targetPitch * Math.min(1, dt * 0.9);
    }
    var ambientPitch = this.targetPitch + Math.sin(time * 1.4) * 0.04;

    // Hydrodynamic velocity interpolation (fluid deceleration and acceleration)
    var targetVx = this.targetFacingX * this.schoolSpeed;
    var targetVy = Math.sin(ambientPitch) * this.schoolSpeed;
    this.schoolVx += (targetVx - this.schoolVx) * Math.min(1, dt * 3.4);
    this.schoolVy += (targetVy - this.schoolVy) * Math.min(1, dt * 3.8);

    this.schoolX += this.schoolVx * dt;
    this.schoolY += this.schoolVy * dt;

    // Boundary containment
    var horizonY = this.h * 0.52;
    var topBound = horizonY + 45 * this.k;
    var bottomBound = this.h * 0.88;

    if (this.schoolY < topBound) {
      this.schoolY = topBound;
      this.targetPitch = 0.25;
    } else if (this.schoolY > bottomBound) {
      this.schoolY = bottomBound;
      this.targetPitch = -0.25;
    }

    // Horizontal wrap-around
    var pad = 200 * this.k;
    if (this.schoolX > this.w + pad) {
      this.schoolX = -pad * 0.5;
      var shiftR = (this.w + pad * 1.5);
      for (var j = 0; j < 8; j++) this.fishPosX[j] -= shiftR;
    } else if (this.schoolX < -pad) {
      this.schoolX = this.w + pad * 0.5;
      var shiftL = (this.w + pad * 1.5);
      for (var j = 0; j < 8; j++) this.fishPosX[j] += shiftL;
    }

    // Cascading follower physics and 3D yaw turning across the school
    for (var i = 0; i < 8; i++) {
      var targetFx = this.schoolX - (this.targetFacingX * Math.abs(this.fishOffsetsX[i]) * this.k);
      var targetFy = this.schoolY + this.fishOffsetsY[i] * this.k;

      var followSpeed = 5.2 - i * 0.32;
      this.fishPosX[i] += (targetFx - this.fishPosX[i]) * Math.min(1, dt * followSpeed);
      this.fishPosY[i] += (targetFy - this.fishPosY[i]) * Math.min(1, dt * 5.5);

      // Staggered 3D yaw turning rate: leader turns first, followers follow
      var turnRate = 6.2 - i * 0.45;
      this.fishFacing[i] += (this.targetFacingX - this.fishFacing[i]) * Math.min(1, dt * turnRate);

      // Staggered pitch easing
      this.fishPitch[i] += (ambientPitch - this.fishPitch[i]) * Math.min(1, dt * (5.5 - i * 0.25));
    }
  };

  OceanEngine.prototype.render = function (timestamp) {
    if (!this.lastTime) this.lastTime = timestamp;
    var dt = (timestamp - this.lastTime) / 1000;
    this.lastTime = timestamp;
    if (dt > 0.1) dt = 0.1;

    if (this.motion) {
      this.runningTime += dt;
      this.updateFishSchool(dt, this.runningTime);
    }

    // Subtle sky parallax smoothing
    this.parallaxX += (this.parallaxTargetX - this.parallaxX) * Math.min(1, dt * 2.5);
    this.parallaxY += (this.parallaxTargetY - this.parallaxY) * Math.min(1, dt * 2.5);

    var totalPx = Math.max(-1.2, Math.min(1.2, this.parallaxX));
    var totalPy = Math.max(-0.8, Math.min(0.8, this.parallaxY));

    var ctx = this.ctx;
    var w = this.w, h = this.h, k = this.k;
    var time = this.runningTime;
    var hour = this.getEffectiveHour();
    var L = this.computeLighting(hour);

    var horizonY = h * 0.52;
    var skyShiftX = -totalPx * 18 * k;
    var skyShiftY = -totalPy * 10 * k;

    // 1. Sky Gradient
    var skyGrad = ctx.createLinearGradient(0, 0, 0, horizonY + 2);
    skyGrad.addColorStop(0, rgbStr(L.sky0));
    skyGrad.addColorStop(0.55, rgbStr(L.sky1));
    skyGrad.addColorStop(1, rgbStr(L.sky2));
    ctx.fillStyle = skyGrad;
    ctx.fillRect(0, 0, w, horizonY + 2);

    // 2. Stars
    if (L.nightFactor > 0.05) {
      ctx.fillStyle = '#F6F8FF';
      for (var s = 0; s < this.stars.length; s++) {
        var star = this.stars[s];
        var sx = (star.x * w + skyShiftX) % w;
        if (sx < 0) sx += w;
        var sy = star.y * h + skyShiftY;
        if (sy >= horizonY - 10 * k) continue;
        var twinkle = 0.4 + 0.6 * (0.5 + 0.5 * Math.sin(time * 2.2 + star.phase));
        var a = L.nightFactor * twinkle;
        if (a > 0.02) {
          ctx.globalAlpha = a;
          ctx.beginPath();
          ctx.arc(sx, sy, star.r * k, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      ctx.globalAlpha = 1;
    }

    // 3. Sun or Moon
    if (L.sunAlpha > 0.01) {
      var cx = L.sunX + skyShiftX * 0.5;
      var cy = L.sunY + skyShiftY * 0.5;
      var cr = L.sunR;

      // Glow
      var glowCol = L.isMoon ? 'rgba(224,236,255,' : (L.nightFactor > 0.3 ? 'rgba(255,122,56,' : 'rgba(255,238,153,');
      var radGlow = ctx.createRadialGradient(cx, cy, 0, cx, cy, cr * 3.4);
      radGlow.addColorStop(0, glowCol + (L.sunAlpha * 0.45).toFixed(3) + ')');
      radGlow.addColorStop(0.4, glowCol + (L.sunAlpha * 0.12).toFixed(3) + ')');
      radGlow.addColorStop(1, glowCol + '0)');
      ctx.fillStyle = radGlow;
      ctx.beginPath();
      ctx.arc(cx, cy, cr * 3.4, 0, Math.PI * 2);
      ctx.fill();

      // Disc
      if (!L.isMoon) {
        var sunTop = blendRgb([255, 253, 240], [255, 228, 160], L.nightFactor);
        var sunBot = blendRgb([255, 176, 74], [230, 66, 40], L.nightFactor);
        var sunGrad = ctx.createLinearGradient(cx, cy - cr, cx, cy + cr);
        sunGrad.addColorStop(0, rgbStr(sunTop));
        sunGrad.addColorStop(1, rgbStr(sunBot));
        ctx.fillStyle = sunGrad;
        ctx.beginPath();
        ctx.arc(cx, cy, cr, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillStyle = 'rgba(242,246,255,' + L.sunAlpha.toFixed(3) + ')';
        ctx.beginPath();
        ctx.arc(cx, cy, cr, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(203,214,232,' + (L.sunAlpha * 0.55).toFixed(3) + ')';
        ctx.beginPath();
        ctx.arc(cx - cr * 0.28, cy - cr * 0.15, cr * 0.26, 0, Math.PI * 2);
        ctx.arc(cx + cr * 0.22, cy + cr * 0.24, cr * 0.22, 0, Math.PI * 2);
        ctx.arc(cx - cr * 0.05, cy + cr * 0.32, cr * 0.16, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // 4. Wind Wisps
    if (this.motion) {
      ctx.lineWidth = 1.4 * k;
      ctx.lineCap = 'round';
      ctx.strokeStyle = L.isMoon ? 'rgba(224,236,255,0.12)' : 'rgba(255,255,255,0.15)';
      for (var wIdx = 0; wIdx < this.wisps.length; wIdx++) {
        var wisp = this.wisps[wIdx];
        var wx = ((time * (wisp.speed * k) + wisp.phase * 300 + skyShiftX * 0.7) % (w + 400 * k)) - 200 * k;
        var wy = wisp.y * h + skyShiftY * 0.5 + Math.sin(wx * 0.003 + time * 0.8 + wIdx) * 12 * k;
        var wlen = wisp.len * k;
        ctx.beginPath();
        ctx.moveTo(wx, wy);
        ctx.quadraticCurveTo(wx + wlen * 0.5, wy - 8 * k, wx + wlen, wy + Math.sin(time + wIdx) * 6 * k);
        ctx.stroke();
      }
    }

    // 5. Ocean Base
    var parHorizonY = horizonY - totalPy * 6 * k;
    var seaGrad = ctx.createLinearGradient(0, parHorizonY, 0, h);
    seaGrad.addColorStop(0, rgbStr(L.sea0));
    seaGrad.addColorStop(0.40, rgbStr(L.sea1));
    seaGrad.addColorStop(1, rgbStr(L.sea2));
    ctx.fillStyle = seaGrad;
    ctx.fillRect(0, parHorizonY, w, h);

    // 6. Reflection Corridor
    if (L.sunAlpha > 0.05) {
      var gleamBase = L.isMoon ? [216, 234, 255] : (L.nightFactor > 0.35 ? [255, 142, 50] : [255, 214, 110]);
      var gleamX = L.sunX - totalPx * 18 * k;
      ctx.lineCap = 'round';

      for (var ry = parHorizonY + 2 * k; ry < h; ry += 14 * k) {
        var prog = (ry - parHorizonY) / (h - parHorizonY);
        var spread = (20 + prog * 240) * k;
        var wobble = Math.sin(ry * 0.04 + time * 3.5) * 14 * k * prog;
        var shimmer = 0.35 + 0.65 * Math.sin(ry * 0.15 - time * 5);
        var alphaG = L.sunAlpha * (1 - prog * 0.65) * shimmer * 0.45;
        if (alphaG > 0.01) {
          ctx.strokeStyle = rgbStr(gleamBase, alphaG);
          ctx.lineWidth = (2.0 + prog * 3.5) * k;
          ctx.beginPath();
          ctx.moveTo(gleamX + wobble - spread * 0.5, ry);
          ctx.lineTo(gleamX + wobble + spread * 0.5, ry);
          ctx.stroke();
        }
      }
    }

    // 7. Layered Waves & Interactive Fish School
    var chop = this.isRaining ? 1.45 : 1.0;
    var speed = this.isRaining ? 1.4 : 1.0;

    // Distant wave ripple
    this.drawWave(parHorizonY + 16 * k, 0.006, 3.5 * k * chop, time * 1.1 * speed, 0, L.sea0, 0.40, -totalPx * 14 * k, false);

    // School of Fish gliding in the mid-depths of the water
    this.drawFishSchool(time, L, hour);

    // Midground ocean swell
    this.drawWave(parHorizonY + h * 0.12, 0.0042, 8.5 * k * chop, time * 1.5 * speed, 1.8, L.sea1, 0.70, -totalPx * 28 * k, true, L);

    // Foreground surf roll
    this.drawWave(parHorizonY + h * 0.26, 0.0028, 14.0 * k * chop, time * 1.9 * speed, 3.6, L.sea2, 0.95, -totalPx * 45 * k, true, L);

    // 8. Shoreline & Palm Trees (Completely static and stable)
    this.drawShoreAndPalms(time, L);

    // 9. Rain Overlay: Luminous drops with high contrast in any lighting
    if (this.isRaining) {
      this.drawRain(time, -totalPx * 60 * k, L, hour);
    }
  };

  /**
   * Renders an interactive school of vector fish gliding through the ocean water,
   * undulating their tails and swimming in the direction of D-Pad navigation.
   */
  OceanEngine.prototype.drawFishSchool = function (time, L, hour) {
    var ctx = this.ctx;
    var k = this.k;

    // Fish colors reacting to lighting
    var fishBodyCol, fishSheenCol;
    if (L.isMoon) {
      fishBodyCol = 'rgba(176,216,248,0.55)'; // Bioluminescent silver-cyan
      fishSheenCol = 'rgba(228,244,255,0.75)';
    } else if (hour >= 16.0 && hour <= 19.2) {
      fishBodyCol = 'rgba(246,184,148,0.60)'; // Sunset coral glow
      fishSheenCol = 'rgba(255,226,206,0.80)';
    } else if (hour >= 5.0 && hour < 7.0) {
      fishBodyCol = 'rgba(242,194,184,0.58)'; // Sunrise peach-rose sheen
      fishSheenCol = 'rgba(255,230,223,0.78)';
    } else {
      fishBodyCol = 'rgba(224,242,254,0.55)'; // Shimmering tropical aqua-silver
      fishSheenCol = 'rgba(240,249,255,0.75)';
    }

    for (var i = 0; i < this.fishCount; i++) {
      var fx = this.fishPosX[i];
      var fy = this.fishPosY[i];

      var facing = this.fishFacing[i];
      var absFacing = Math.abs(facing);
      var sign = facing >= 0 ? 1 : -1;

      // 3D perspective yaw scale: smoothly narrows to a thin sleek profile, then expands facing other way
      var drawScaleX = sign * Math.max(0.18, absFacing);
      // Vertical banking arch during the apex of turn
      var turnBank = (1 - absFacing * absFacing) * (sign * -6 * k);
      var drawScaleY = 1.0 + (1 - absFacing) * 0.16;

      var sc = this.fishScales[i] * k;
      var flen = 32 * sc;
      var fht = flen * 0.36;

      // Tail wagging speed: ambient wag + speed boost + turn flick
      var wagSpeed = (8.5 + (this.schoolSpeed / (this.cruiseSpeed + 1)) * 4.5) + (1 - absFacing) * 6.0;
      var tailWag = Math.sin(time * wagSpeed + this.fishWiggles[i]) * (flen * 0.24);

      ctx.save();
      ctx.translate(fx, fy + turnBank);
      ctx.rotate(this.fishPitch[i]);
      ctx.scale(drawScaleX, drawScaleY);

      // Streamlined fish body
      ctx.fillStyle = fishBodyCol;
      ctx.beginPath();
      ctx.moveTo(flen * 0.5, 0); // Head snout
      ctx.bezierCurveTo(flen * 0.20, -fht * 0.95, -flen * 0.25, -fht * 0.70, -flen * 0.45, 0);
      ctx.bezierCurveTo(-flen * 0.25, fht * 0.70, flen * 0.20, fht * 0.95, flen * 0.5, 0);
      ctx.closePath();
      ctx.fill();

      // Caudal fin (tail fin) swinging with harmonic undulation
      ctx.fillStyle = fishSheenCol;
      ctx.beginPath();
      ctx.moveTo(-flen * 0.42, 0);
      ctx.lineTo(-flen * 0.82, -fht * 0.85 + tailWag);
      ctx.quadraticCurveTo(-flen * 0.65, tailWag * 0.5, -flen * 0.82, fht * 0.85 + tailWag);
      ctx.closePath();
      ctx.fill();

      // Dorsal fin
      ctx.beginPath();
      ctx.moveTo(-flen * 0.05, -fht * 0.55);
      ctx.lineTo(-flen * 0.25, -fht * 1.15);
      ctx.lineTo(-flen * 0.30, -fht * 0.40);
      ctx.closePath();
      ctx.fill();

      // Glistening eye
      if (absFacing > 0.25) {
        ctx.fillStyle = 'rgba(255,255,255,0.88)';
        ctx.beginPath();
        ctx.arc(flen * 0.32, -fht * 0.18, 1.8 * sc, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.restore();
    }
  };

  OceanEngine.prototype.drawWave = function (baseY, freq, amp, phase, offset, color, alpha, pX, hasFoam, L) {
    var ctx = this.ctx;
    var w = this.w, h = this.h, k = this.k;
    var step = 20 * k;

    ctx.beginPath();
    ctx.moveTo(0, h);
    for (var x = 0; x <= w + step; x += step) {
      var y = baseY + Math.sin((x + pX) * freq + phase + offset) * amp
        + Math.cos((x + pX) * (freq * 1.8) - phase * 0.7) * (amp * 0.45);
      if (x === 0) ctx.lineTo(0, y);
      else ctx.lineTo(x, y);
    }
    ctx.lineTo(w, h);
    ctx.closePath();
    ctx.fillStyle = rgbStr(color, alpha);
    ctx.fill();

    if (hasFoam) {
      ctx.beginPath();
      for (var fx = 0; fx <= w + step; fx += step) {
        var fy = baseY + Math.sin((fx + pX) * freq + phase + offset) * amp
          + Math.cos((fx + pX) * (freq * 1.8) - phase * 0.7) * (amp * 0.45);
        if (fx === 0) ctx.moveTo(0, fy);
        else ctx.lineTo(fx, fy);
      }
      var foamAlpha = L && L.isMoon ? 0.35 : 0.50;
      var foamCol = L && L.isMoon ? 'rgba(232,244,255,' : (L && L.nightFactor > 0.4 ? 'rgba(255,222,192,' : 'rgba(255,255,255,');
      ctx.strokeStyle = foamCol + foamAlpha.toFixed(3) + ')';
      ctx.lineWidth = 2.2 * k;
      ctx.stroke();
    }
  };

  /** Shoreline and palm trees stay serenely static, rooted without camera movement. */
  OceanEngine.prototype.drawShoreAndPalms = function (time, L) {
    var ctx = this.ctx;
    var w = this.w, h = this.h, k = this.k;

    // Wet sand
    ctx.fillStyle = rgbStr(L.sand1);
    ctx.beginPath();
    ctx.moveTo(w * 0.48, h);
    ctx.bezierCurveTo(
      w * 0.62, h * 0.88,
      w * 0.75, h * 0.80,
      w, h * 0.76
    );
    ctx.lineTo(w, h);
    ctx.closePath();
    ctx.fill();

    // Main sand dune
    ctx.fillStyle = rgbStr(L.sand0);
    ctx.beginPath();
    ctx.moveTo(w * 0.56, h);
    ctx.bezierCurveTo(
      w * 0.68, h * 0.84,
      w * 0.80, h * 0.74,
      w, h * 0.70
    );
    ctx.lineTo(w, h);
    ctx.closePath();
    ctx.fill();

    // Two coconut palm trees (peaceful ambient wind sway)
    this.drawPalm(w * 0.83, h * 0.88, 460 * k, -0.18, time, L, 0);
    this.drawPalm(w * 0.93, h * 0.89, 380 * k, 0.12, time, L, 1);
  };

  OceanEngine.prototype.drawPalm = function (baseX, baseY, treeHeight, lean, time, L, seed) {
    var ctx = this.ctx;
    var k = this.k;
    var windSpeed = this.isRaining ? 2.5 : 1.5;
    var sway = Math.sin(time * windSpeed + seed * 1.5) * (this.isRaining ? 0.08 : 0.04);
    var totalLean = lean + sway;

    var crownX = baseX + Math.sin(totalLean) * treeHeight;
    var crownY = baseY - Math.cos(totalLean) * treeHeight;
    var midX = (baseX + crownX) * 0.5 + Math.sin(totalLean * 1.4) * (treeHeight * 0.12);
    var midY = (baseY + crownY) * 0.5;

    // Trunk
    var baseW = 20 * k;
    var crownW = 9 * k;
    ctx.fillStyle = rgbStr(L.palm);
    ctx.beginPath();
    ctx.moveTo(baseX - baseW * 0.5, baseY);
    ctx.quadraticCurveTo(midX - (baseW + crownW) * 0.28, midY, crownX - crownW * 0.5, crownY);
    ctx.lineTo(crownX + crownW * 0.5, crownY);
    ctx.quadraticCurveTo(midX + (baseW + crownW) * 0.28, midY, baseX + baseW * 0.5, baseY);
    ctx.closePath();
    ctx.fill();

    // Coconuts
    ctx.fillStyle = rgbStr(blendRgb(L.palm, [0, 0, 0], 0.25));
    ctx.beginPath();
    ctx.arc(crownX - 5 * k, crownY + 8 * k, 6.5 * k, 0, Math.PI * 2);
    ctx.arc(crownX + 5 * k, crownY + 9 * k, 6.0 * k, 0, Math.PI * 2);
    ctx.arc(crownX, crownY + 14 * k, 5.5 * k, 0, Math.PI * 2);
    ctx.fill();

    // 7 Fronds
    var frondAngles = [-2.4, -1.8, -1.1, -0.5, 0.2, 0.8, 1.4];
    var frondLens = [170, 210, 240, 220, 190, 160, 140];

    for (var i = 0; i < frondAngles.length; i++) {
      var frondSway = Math.sin(time * (windSpeed * 1.2) + i * 0.7 + seed) * (this.isRaining ? 0.12 : 0.06);
      var angle = totalLean + frondAngles[i] + frondSway;
      var fLen = frondLens[i] * k;

      var tipX = crownX + Math.cos(angle) * fLen;
      var tipY = crownY + Math.sin(angle) * fLen + (fLen * 0.22);
      var ctrlX = crownX + Math.cos(angle) * (fLen * 0.55);
      var ctrlY = crownY + Math.sin(angle) * (fLen * 0.45) - (fLen * 0.10);

      var leafW = 22 * k;
      var normX = -Math.sin(angle) * leafW;
      var normY = Math.cos(angle) * leafW;

      ctx.fillStyle = rgbStr(L.palm);
      ctx.beginPath();
      ctx.moveTo(crownX, crownY);
      ctx.quadraticCurveTo(ctrlX + normX, ctrlY + normY, tipX, tipY);
      ctx.quadraticCurveTo(ctrlX - normX * 0.6, ctrlY - normY * 0.6, crownX, crownY);
      ctx.closePath();
      ctx.fill();

      // Spine
      ctx.strokeStyle = rgbStr(blendRgb(L.palm, [255, 255, 255], 0.15));
      ctx.lineWidth = 1.6 * k;
      ctx.beginPath();
      ctx.moveTo(crownX, crownY);
      ctx.quadraticCurveTo(ctrlX, ctrlY, tipX, tipY);
      ctx.stroke();
    }
  };

  /**
   * Renders crisp, luminous silver-white raindrops with clear contrast across all daylight,
   * golden-hour, sunset, and nighttime conditions.
   */
  OceanEngine.prototype.drawRain = function (time, shift, L, hour) {
    var ctx = this.ctx;
    var w = this.w, h = this.h, k = this.k;
    var span = h + 240 * k;
    var slant = 0.16;
    var wrap = w + 300 * k;

    // Pick luminous water drop colors matching current lighting
    var fgColor, bgColor;
    if (L.isMoon) {
      fgColor = 'rgba(210,230,255,0.60)';
      bgColor = 'rgba(160,195,232,0.35)';
    } else if (hour >= 16.0 && hour <= 19.2) {
      fgColor = 'rgba(255,230,212,0.68)';
      bgColor = 'rgba(232,180,158,0.38)';
    } else if (hour >= 5.0 && hour < 7.0) {
      fgColor = 'rgba(248,228,223,0.68)';
      bgColor = 'rgba(228,178,170,0.38)';
    } else {
      fgColor = 'rgba(240,248,255,0.65)';
      bgColor = 'rgba(208,228,250,0.38)';
    }

    ctx.lineCap = 'round';

    // Draw background rain layer
    ctx.strokeStyle = bgColor;
    ctx.lineWidth = Math.max(1.0, 1.4 * k);
    ctx.beginPath();
    for (var i = 0; i < this.rain.length; i++) {
      var r = this.rain[i];
      if (r.isFg) continue;
      var y = ((r.phase + time * r.speed / span) % 1.0) * span - 120 * k;
      var x = r.x * wrap - y * slant + shift;
      x = ((x % wrap) + wrap) % wrap - 100 * k;
      ctx.moveTo(x, y);
      ctx.lineTo(x - r.len * k * slant * 0.75, y + r.len * k * 0.75);
    }
    ctx.stroke();

    // Draw foreground rain layer with higher contrast
    ctx.strokeStyle = fgColor;
    ctx.lineWidth = Math.max(1.6, 2.3 * k);
    ctx.beginPath();
    for (var j = 0; j < this.rain.length; j++) {
      var rf = this.rain[j];
      if (!rf.isFg) continue;
      var yf = ((rf.phase + time * rf.speed / span) % 1.0) * span - 120 * k;
      var xf = rf.x * wrap - yf * slant + shift;
      xf = ((xf % wrap) + wrap) % wrap - 100 * k;
      ctx.moveTo(xf, yf);
      ctx.lineTo(xf - rf.len * k * slant, yf + rf.len * k);
    }
    ctx.stroke();
  };

  global.OceanEngine = OceanEngine;
})(window);
