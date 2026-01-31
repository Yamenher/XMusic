package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.ColorInt;

public class StatusBarScrimView extends View {

    private Paint paint;
    private int statusBarHeight;
    private int scrimColor;
    private float alphaMultiplier = 1f;

    public StatusBarScrimView(Context context) {
        super(context);
        init(context, null);
    }

    public StatusBarScrimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatusBarScrimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        statusBarHeight = resId > 0 ? context.getResources().getDimensionPixelSize(resId) : 0;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, com.xapps.media.xmusic.R.styleable.StatusBarScrimView);
            scrimColor = ta.getColor(com.xapps.media.xmusic.R.styleable.StatusBarScrimView_scrimColor, getDefaultColor(context));
            ta.recycle();
        } else {
            scrimColor = getDefaultColor(context);
        }

        paint = new Paint();
    }

    private int getDefaultColor(Context context) {
        TypedArray themeAttrs = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorSurfaceContainer});
        int color = themeAttrs.getColor(0, 0xFF000000);
        themeAttrs.recycle();
        return color;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height;
        if (heightMode == MeasureSpec.AT_MOST) {
            height = statusBarHeight;
        } else {
            height = heightSize;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int alpha = (int) (alphaMultiplier * ((scrimColor >> 24) & 0xFF));
        int startColor = (alpha << 24) | (scrimColor & 0x00FFFFFF);
        
        LinearGradient gradient = new LinearGradient(0, 0, 0, getHeight(), startColor, 0x00000000, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    public void setScrimColor(@ColorInt int color) {
        if (this.scrimColor != color) {
            this.scrimColor = color;
            invalidate();
        }
    }

    public void setStatusBarHeight(int height) {
        if (this.statusBarHeight != height) {
            this.statusBarHeight = height;
            requestLayout();
        }
    }

    public void setAlphaMultiplier(float alpha) {
        alphaMultiplier = Math.max(0f, Math.min(1f, alpha));
        invalidate();
    }
}
