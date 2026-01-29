package com.xapps.media.xmusic.widget;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.style.ReplacementSpan;

public class KaraokeSpan extends ReplacementSpan {
    public float progress = -1.0f;
    public float alpha = 0.18f;
    public float glowAlpha = 0.0f;

    private final float gradientWidthFactor;
    private final Matrix matrix = new Matrix();
    private LinearGradient shader;
    
    private int cachedColor = 0;
    private int cachedInactiveColor = 0;
    
    public KaraokeSpan(float initialAlpha) {
        this.alpha = initialAlpha;
        this.gradientWidthFactor = ((1f - 0.67f) / 0.67f) + 1f;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        int originalColor = paint.getColor();
        int originalAlpha = paint.getAlpha();

        boolean isRtl = false;
        if (end > start) {
            for (int i = start; i < end; i++) {
                byte dir = Character.getDirectionality(text.charAt(i));
                if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || 
                    dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                    isRtl = true;
                    break; 
                }
            }
        }

        paint.setShader(null);
        paint.clearShadowLayer();
        paint.setAlpha((int) (255 * 0.18f)); 
        canvas.drawText(text, start, end, x, (float) y, paint);

        if (progress > -0.99f) {
            paint.setAlpha((int) (255 * alpha));
            
            int textWidth = (int) paint.measureText(text, start, end);

            if (shader == null || cachedColor != originalColor) {
                cachedColor = originalColor;
                cachedInactiveColor = (originalColor & 0x00FFFFFF); 
                
                float gradStart, gradEnd;
                int[] colors;
                float[] positions;

                if (isRtl) {
                    gradStart = x + textWidth;
                    gradEnd = x + textWidth + (textWidth * gradientWidthFactor);
                    colors = new int[]{cachedInactiveColor, originalColor, originalColor};
                    positions = new float[]{0.0f, 0.33f, 1.0f};
                } else {
                    gradStart = x - (textWidth * gradientWidthFactor);
                    gradEnd = x;
                    colors = new int[]{originalColor, originalColor, cachedInactiveColor};
                    positions = new float[]{0.0f, 0.67f, 1.0f};
                }
                shader = new LinearGradient(gradStart, (float) y, gradEnd, (float) y, colors, positions, Shader.TileMode.CLAMP);
            }

            matrix.reset();
            float shaderProgress = Math.min(1.0f, Math.max(0.0f, progress));
            
            if (isRtl) {
                matrix.setTranslate(-(gradientWidthFactor * textWidth) * shaderProgress, 0.0f);
            } else {
                matrix.setTranslate((gradientWidthFactor * textWidth) * shaderProgress, 0.0f);
            }
            
            shader.setLocalMatrix(matrix);
            paint.setShader(shader);

            if (glowAlpha > 0.01f && progress >= 0.0f && progress < 1.0f) {
                float finalShadowAlpha = Math.max(0, Math.min(1, glowAlpha)); 
                paint.setShadowLayer(35.0f, 0, 0, (originalColor & 0x00FFFFFF) | ((int) (finalShadowAlpha * 255) << 24));
            } else {
                paint.clearShadowLayer();
            }
            
            canvas.drawText(text, start, end, x, (float) y, paint);
        }

        paint.setShader(null);
        paint.clearShadowLayer();
        paint.setAlpha(originalAlpha); 
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) paint.measureText(text, start, end);
    }
}
