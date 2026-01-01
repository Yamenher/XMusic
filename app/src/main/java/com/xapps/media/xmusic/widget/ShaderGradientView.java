package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import com.xapps.media.xmusic.R;

import java.io.InputStream;
import java.util.Scanner;

public class ShaderGradientView extends View {

    private static final boolean SUPPORTED = Build.VERSION.SDK_INT >= 31;

    private RuntimeShader shader;
    private Paint paint;

    private boolean visible = false;

    // ---- animation clock (loop-based) ----
    private float phase = 0f;
    private float speed = 0.5f;
    private static final float BASE_SPEED = 0.016f; // ~60fps normalized step

    // ---- color interpolation ----
    private float[] currentColors;
    private float[] startColors;
    private float[] targetColors;
    private float colorT = 1f;
    private float colorSpeed = 0.8f; // seconds-ish

    private final float[] uPoints = {
            0f, 0f, 1.5f,
            1f, 0f, 1.5f,
            0f, 1f, 1.5f,
            1f, 1f, 1.5f
    };

    private final float[] defaultColors = {
            0.0f, 0.588f, 0.533f, 1.0f,
            0.0f, 0.427f, 0.384f, 1.0f,
            0.0f, 0.588f, 0.533f, 1.0f,
            0.0f, 0.427f, 0.384f, 1.0f
    };

    private final float[] uBound = {0f, 0f, 1f, 1f};

    private final Choreographer.FrameCallback frameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {

                    if (visible && shader != null) {

                        // ---- advance phase smoothly ----
                        phase += BASE_SPEED * speed;
                        shader.setFloatUniform("uAnimTime", phase);

                        // ---- color interpolation ----
                        if (colorT < 1f && startColors != null && targetColors != null) {
                            colorT += BASE_SPEED * colorSpeed;
                            float t = Math.min(1f, colorT);
                            float e = 1f - (1f - t) * (1f - t); // ease-out

                            for (int i = 0; i < currentColors.length; i++) {
                                currentColors[i] =
                                        startColors[i] + (targetColors[i] - startColors[i]) * e;
                            }

                            shader.setFloatUniform("uColors", currentColors);

                            if (t >= 1f) {
                                startColors = null;
                                targetColors = null;
                            }
                        }

                        invalidate();
                    }

                    Choreographer.getInstance().postFrameCallback(this);
                }
            };

    public ShaderGradientView(Context context) {
        super(context);
        init();
    }

    public ShaderGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShaderGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (!SUPPORTED) return;

        shader = new RuntimeShader(loadShader(R.raw.bg_frag));

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(shader);

        currentColors = defaultColors.clone();

        shader.setFloatUniform("uPoints", uPoints);
        shader.setFloatUniform("uColors", currentColors);
        shader.setFloatUniform("uBound", uBound);
        shader.setFloatUniform("uNoiseScale", 1.5f);
        shader.setFloatUniform("uPointOffset", 0.1f);
        shader.setFloatUniform("uPointRadiusMulti", 1f);
        shader.setFloatUniform("uAlphaMulti", 1f);
        shader.setFloatUniform("uSaturateOffset", 0.2f);
        shader.setFloatUniform("uLightOffset", 0.1f);
        shader.setFloatUniform("uTranslateY", 0f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        visible = true;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        visible = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        visible = visibility == VISIBLE;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (shader != null && w > 0 && h > 0) {
            shader.setFloatUniform("uResolution", w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    // ---- PUBLIC API ----

    public void setAnimationSpeed(float s) {
        speed = Math.max(0f, s);
    }

    public void setColors(int c1, int c2, int c3, int c4) {
        startColors = currentColors.clone();
        targetColors = colorsFromInts(c1, c2, c3, c4);
        colorT = 0f;
    }

    // ---- utils ----

    private static float[] colorsFromInts(int c1, int c2, int c3, int c4) {
        return new float[]{
                ((c1 >> 16) & 0xFF) / 255f, ((c1 >> 8) & 0xFF) / 255f, (c1 & 0xFF) / 255f, ((c1 >> 24) & 0xFF) / 255f,
                ((c2 >> 16) & 0xFF) / 255f, ((c2 >> 8) & 0xFF) / 255f, (c2 & 0xFF) / 255f, ((c2 >> 24) & 0xFF) / 255f,
                ((c3 >> 16) & 0xFF) / 255f, ((c3 >> 8) & 0xFF) / 255f, (c3 & 0xFF) / 255f, ((c3 >> 24) & 0xFF) / 255f,
                ((c4 >> 16) & 0xFF) / 255f, ((c4 >> 8) & 0xFF) / 255f, (c4 & 0xFF) / 255f, ((c4 >> 24) & 0xFF) / 255f
        };
    }

    private String loadShader(int resId) {
        try (InputStream is = getResources().openRawResource(resId);
             Scanner sc = new Scanner(is)) {
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine()).append('\n');
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}