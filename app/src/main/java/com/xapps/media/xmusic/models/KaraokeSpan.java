package com.xapps.media.xmusic.widget;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.style.ReplacementSpan;
import com.xapps.media.xmusic.data.LiveColors;

public class KaraokeSpan extends ReplacementSpan {

    public float progress = -1.0f;
    public float alpha = 0.4f;
    public float glowAlpha = 0.0f;
    public boolean shouldBounce = false;

    private static final float FIXED_FADE_WIDTH = 200f;

    private final Matrix matrix = new Matrix();
    private LinearGradient shader;

    public float externalWordDrop = 0f;
    public boolean wordCompleted = false;
    public boolean freezeDrop = false;
    public float resetVelocity = 0f;

    private int cachedColor = 0;
    private int cachedInactiveColor = 0;
    private final RectF layerRect = new RectF();
    private final Paint layerPaint = new Paint();

    public KaraokeSpan(float initialAlpha) {
        this.alpha = initialAlpha;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, Paint paint) {

        int originalColor = paint.getColor();
        int originalAlpha = paint.getAlpha();
        float clamped = Math.max(0f, Math.min(1f, progress));
        float exitDecay = Math.max(0f, Math.min(1f, (1f - clamped) * 10f));

        boolean isRtl = false;
        for (int i = start; i < end; i++) {
            byte dir = Character.getDirectionality(text.charAt(i));
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                isRtl = true;
                break;
            }
        }

        float textWidth = paint.measureText(text, start, end);

        if (shader == null || cachedColor != originalColor) {
            cachedColor = originalColor;
            cachedInactiveColor = originalColor & 0x00FFFFFF;

            float gradStart = isRtl ? x + textWidth : x - FIXED_FADE_WIDTH;
            float gradEnd = isRtl ? x + textWidth + FIXED_FADE_WIDTH : x;

            int[] colors = isRtl
                    ? new int[]{ cachedInactiveColor, originalColor, originalColor }
                    : new int[]{ originalColor, originalColor, cachedInactiveColor };

            float[] positions = isRtl
                    ? new float[]{ 0f, 0.33f, 1f }
                    : new float[]{ 0f, 0.67f, 1f };

            shader = new LinearGradient(
                    gradStart, y,
                    gradEnd, y,
                    colors,
                    positions,
                    Shader.TileMode.CLAMP
            );
        }

        float travel = (FIXED_FADE_WIDTH + textWidth) * clamped;

        matrix.reset();
        matrix.setTranslate(isRtl ? -travel : travel, 0f);
        shader.setLocalMatrix(matrix);

        if (isRtl) {
            wordCompleted = (x + textWidth - travel) <= x;
        } else {
            wordCompleted = (x + travel) >= (x + textWidth);
        }

        if (glowAlpha > 0.02f && clamped > 0.01f && clamped < 0.99f) {
            paint.setShader(shader);
            paint.setAlpha(0);

            float rawRadius = 12f + 23f * (float) Math.sin(Math.PI * clamped);
            float snappedRadius = Math.round(rawRadius / 2f) * 2f;
            int alphaBits = Math.min(255, Math.max(0, (int) (glowAlpha * 255)));
            int snappedAlpha = (alphaBits / 10) * 10;
            int glowColor = (originalColor & 0x00FFFFFF) | (snappedAlpha << 24);

            paint.setShadowLayer(snappedRadius, 0, 0, glowColor);

            canvas.drawText(text, start, end, x, y + externalWordDrop, paint);
            paint.clearShadowLayer();
            paint.setAlpha(originalAlpha);
        }

        layerRect.set(x - 10, top - 50, x + textWidth + 10, bottom + 50);
        layerPaint.setAlpha(100);
        int sc = canvas.saveLayer(layerRect, layerPaint);

        paint.setShader(null);
        paint.setColor(LiveColors.outline);
        paint.setAlpha(255);
        drawStaggeredText(canvas, text, start, end, x, y, paint, clamped, exitDecay, externalWordDrop);

        canvas.restoreToCount(sc);

        if (progress > -0.99f) {
            paint.setShader(shader);
            paint.setAlpha((int) (255 * alpha));
            drawStaggeredText(canvas, text, start, end, x, y, paint, clamped, exitDecay, externalWordDrop);
        }

        paint.setShader(null);
        paint.setAlpha(originalAlpha);
        paint.setColor(originalColor);
    }

    private void drawStaggeredText(Canvas canvas, CharSequence text, int start, int end,
                                   float x, float y, Paint paint,
                                   float clamped, float exitDecay, float wordDrop) {
        canvas.drawText(text, start, end, x, y + wordDrop, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        return (int) paint.measureText(text, start, end);
    }
}
