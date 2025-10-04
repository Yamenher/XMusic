/*
 *     Copyright (C) 2022 The Android Open Source Project
 *                   2024 Akane Foundation
 *
 *     XMusic is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     XMusic is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.xapps.media.xmusic.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.animation.PathInterpolator;

public class SquigglyProgress extends Drawable {

    private static final float TWO_PI = (float) (Math.PI * 2f);
    private static final int DISABLED_ALPHA = 77;

    private final Paint wavePaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Path path = new Path();

    private float heightFraction = 0f;
    private ValueAnimator heightAnimator;
    private float phaseOffset = 0f;
    private long lastFrameTime = -1L;

    private final float transitionPeriods = 1.5f;
    private final float minWaveEndpoint = 0f;
    private final float matchedWaveEndpoint = 1f;

    public float waveLength = 0f;
    public float lineAmplitude = 0f;
    public float phaseSpeed = 0f;

    private float strokeWidth = 0f;
    public boolean transitionEnabled = true;
    public boolean animate = false;

    public SquigglyProgress() {
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAlpha(DISABLED_ALPHA);
    }

    public void setStrokeWidth(float value) {
        if (this.strokeWidth == value) return;
        this.strokeWidth = value;
        wavePaint.setStrokeWidth(value);
        linePaint.setStrokeWidth(value);
    }

    public void setTransitionEnabled(boolean value) {
        this.transitionEnabled = value;
        invalidateSelf();
    }

    public void setAnimate(boolean value) {
        if (this.animate == value) return;
        this.animate = value;

        if (value) {
            lastFrameTime = SystemClock.uptimeMillis();
        }
        if (heightAnimator != null) {
            heightAnimator.cancel();
        }

        heightAnimator = ValueAnimator.ofFloat(heightFraction, value ? 1f : 0f);
        if (value) {
            heightAnimator.setStartDelay(60);
            heightAnimator.setDuration(800);
            heightAnimator.setInterpolator(new PathInterpolator(0.05f, 0.7f, 0.1f, 1f));
        } else {
            heightAnimator.setDuration(550);
            heightAnimator.setInterpolator(new PathInterpolator(0f, 0f, 0f, 1f));
        }

        heightAnimator.addUpdateListener(anim -> {
            heightFraction = (float) anim.getAnimatedValue();
            invalidateSelf();
        });

        heightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                heightAnimator = null;
            }
        });

        heightAnimator.start();
    }

    @Override
    public void draw(Canvas canvas) {
        if (animate) {
            invalidateSelf();
            long now = SystemClock.uptimeMillis();
            phaseOffset += (now - lastFrameTime) / 1000f * phaseSpeed;
            phaseOffset %= waveLength;
            lastFrameTime = now;
        }

        float progress = getLevel() / 10000f;
        float totalWidth = getBounds().width();
        float totalProgressPx = totalWidth * progress;
        float waveProgressPx;

        if (!transitionEnabled || progress > matchedWaveEndpoint) {
            waveProgressPx = totalWidth * progress;
        } else {
            waveProgressPx = totalWidth * CalculationUtils.lerp(
                    minWaveEndpoint,
                    matchedWaveEndpoint,
                    CalculationUtils.lerpInv(0f, matchedWaveEndpoint, progress)
            );
        }

        float waveStart = -phaseOffset - waveLength / 2f;
        float waveEnd = transitionEnabled ? totalWidth : waveProgressPx;

        path.rewind();
        path.moveTo(waveStart, 0f);

        float currentX = waveStart;
        float waveSign = 1f;
        float dist = waveLength / 2f;

        float currentAmp = computeAmplitude(currentX, waveSign, waveProgressPx);
        while (currentX < waveEnd) {
            waveSign = -waveSign;
            float nextX = currentX + dist;
            float midX = currentX + dist / 2f;
            float nextAmp = computeAmplitude(nextX, waveSign, waveProgressPx);
            path.cubicTo(midX, currentAmp, midX, nextAmp, nextX, nextAmp);
            currentAmp = nextAmp;
            currentX = nextX;
        }

        float clipTop = lineAmplitude + strokeWidth;
        canvas.save();
        canvas.translate(getBounds().left, getBounds().centerY());

        canvas.save();
        canvas.clipRect(0f, -clipTop, totalProgressPx, clipTop);
        canvas.drawPath(path, wavePaint);
        canvas.restore();

        if (transitionEnabled) {
            canvas.save();
            canvas.clipRect(totalProgressPx, -clipTop, totalWidth, clipTop);
            canvas.drawPath(path, linePaint);
            canvas.restore();
        } else {
            canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint);
        }

        float startAmp = (float) Math.cos(Math.abs(waveStart) / waveLength * TWO_PI);
        canvas.drawPoint(0f, startAmp * lineAmplitude * heightFraction, wavePaint);

        canvas.restore();
    }

    private float computeAmplitude(float x, float sign, float waveProgressPx) {
        if (transitionEnabled) {
            float length = transitionPeriods * waveLength;
            float coeff = CalculationUtils.lerpInvSat(
                    waveProgressPx + length / 2f,
                    waveProgressPx - length / 2f,
                    x
            );
            return sign * heightFraction * lineAmplitude * coeff;
        } else {
            return sign * heightFraction * lineAmplitude;
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        wavePaint.setColorFilter(colorFilter);
        linePaint.setColorFilter(colorFilter);
    }

    @Override
    public void setAlpha(int alpha) {
        updateColors(wavePaint.getColor(), alpha);
    }

    @Override
    public int getAlpha() {
        return wavePaint.getAlpha();
    }

    @Override
    public void setTint(int tintColor) {
        updateColors(tintColor, getAlpha());
    }

    @Override
    protected boolean onLevelChange(int level) {
        return animate;
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint == null) return;
        updateColors(tint.getDefaultColor(), getAlpha());
    }

    private void updateColors(int tintColor, int alpha) {
        wavePaint.setColor(CalculationUtils.setAlphaComponent(tintColor, alpha));
        int lineAlpha = (int) (DISABLED_ALPHA * (alpha / 255f));
        linePaint.setColor(CalculationUtils.setAlphaComponent(tintColor, lineAlpha));
    }

    private static class CalculationUtils {
        static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        static float lerpInv(float a, float b, float v) {
            return (v - a) / (b - a);
        }

        static float lerpInvSat(float a, float b, float v) {
            return clamp((v - a) / (b - a), 0f, 1f);
        }

        static float clamp(float x, float min, float max) {
            return Math.max(min, Math.min(max, x));
        }

        static int setAlphaComponent(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
    }
}
