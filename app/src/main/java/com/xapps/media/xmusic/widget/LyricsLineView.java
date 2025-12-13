package com.xapps.media.xmusic.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;

import java.util.Random;

public class LyricsLineView extends View {

    private String text = "This is a test phrase for lyrics view, and this is an extended text for testing";
    private TextPaint textPaint;
    private float progress = 0.3f;
    private StaticLayout layout;
    
    public LyricsLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricsLineView(Context context) {
        super(context);
        init();
    }

    private void init() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(80f);
        textPaint.setColor(0xFF000000);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setText(String text) {
        this.text = text;
        requestLayout();
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();
        int height = layout.getHeight();
        setMeasuredDimension(width, height);
    }

   @Override
protected void onDraw(Canvas canvas) {
    int lineCount = layout.getLineCount();

    float[] lineWidths = new float[lineCount];
    float totalWidth = 0;

    // Measure widths
    for (int i = 0; i < lineCount; i++) {
        int start = layout.getLineStart(i);
        int end = layout.getLineEnd(i);
        String lineText = text.substring(start, end);
        float lw = textPaint.measureText(lineText);
        lineWidths[i] = lw;
        totalWidth += lw;
    }

    float remaining = totalWidth * progress;

    for (int i = 0; i < lineCount; i++) {

        int start = layout.getLineStart(i);
        int end = layout.getLineEnd(i);
        String lineText = text.substring(start, end);
        float lineWidth = lineWidths[i];

        float highlightWidth = Math.min(remaining, lineWidth);
        float pct = highlightWidth / lineWidth;

        // Base fade amount
        float fade = 0.05f;

        // shrink fade smoothly when pct > 95%
        if (pct > 0.99f) {
            float t = (pct - 0.99f) / 0.01f;   // 0 → 1 as pct goes 0.95 → 1
            fade *= Math.max(0f, 1f - t);     // fade shrinks to 0
        }

        float fadeStart = Math.max(0f, pct - fade);
        float fadeEnd = pct;

        float y = layout.getLineBaseline(i);

        // Layer for text blend
        canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        // Draw base text (black)
        textPaint.setShader(null);
        textPaint.setColor(0xFF000000);
        canvas.drawText(lineText, 0, y, textPaint);

        if (highlightWidth > 0) {
            int solid = 0xFFFF0000;
            int fadeC = 0x00FF0000;

            float drawWidth = highlightWidth;

            float relFadeStart = fade > 0 ? (fadeStart / pct) : 1f;
            float relFadeEnd = 1f;

            LinearGradient shader = new LinearGradient(
                    0, 0,
                    drawWidth, 0,
                    new int[]{ solid, solid, fadeC },
                    new float[]{ 0f, relFadeStart, relFadeEnd },
                    Shader.TileMode.CLAMP
            );

            textPaint.setShader(shader);
            textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

            canvas.save();
            canvas.clipRect(0, y - textPaint.getTextSize(), drawWidth, y + textPaint.getTextSize());
            canvas.drawText(lineText, 0, y, textPaint);
            canvas.restore();

            textPaint.setXfermode(null);
        }

        canvas.restore();

        remaining -= lineWidth;
        if (remaining < 0) remaining = 0;
    }
}
    public void randomizeProgress() {
        Random r = new Random();
        setProgress(r.nextFloat());
    }

    public void demoSmoothProgress() {
    ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
    a.setDuration(10000); // 3 seconds
    a.setInterpolator(new LinearInterpolator());

    a.addUpdateListener(anim -> {
        this.progress = (float) anim.getAnimatedValue();
        invalidate();
    });

    a.start();
}
}