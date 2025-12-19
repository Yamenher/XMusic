package com.xapps.media.xmusic.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.shape.MaterialShapes;

import com.xapps.media.xmusic.R;

import androidx.annotation.ColorInt;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;
import com.xapps.media.xmusic.utils.XUtils;

public class NewPlayerToggle extends LinearLayout {

    
    public static final int SHAPE_SQUARE      = 0;
    public static final int SHAPE_SOFT_BURST  = 1;
    public static final int SHAPE_COOKIE_9    = 2;
    public static final int SHAPE_PENTAGON    = 3;
    public static final int SHAPE_PILL        = 4;
    public static final int SHAPE_SUNNY       = 5;
    public static final int SHAPE_COOKIE_4    = 6;
    public static final int SHAPE_OVAL        = 7;
    public static final int SHAPE_CIRCLE      = 8;

    private long clickDelay = 200;
    private long morphDuration = 450;
    private long lastClickTime = 0;

    private boolean scaleToFit = true;
    private boolean autoMorph = false;
    private int indicatorSize = 180;
    private int containerWidth = 180;
    private int containerHeight = 180;

    private @ColorInt int indicatorColor = Color.WHITE;
    private int avdColor = Color.BLACK;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final Rect bounds = new Rect();
    private ValueAnimator animator;

    private float progress = 1f;
    private ValueAnimator rotateAnimator;
    private float shapeRotation = 0f;
    private int currentShape = SHAPE_SQUARE;
    private int targetShape = SHAPES.length - SHAPES.length;
    private int defaultStartShape = SHAPE_SQUARE;
    private int defaultEndShape = SHAPE_COOKIE_9;
    private boolean isMorphing = false;
    private boolean isAnimating;
    
    private ImageView image;
    
    private ObjectAnimator rotateAnimation;

    private OnClickListener extraClickListener;

    private float pressScale = 1f;
    private ValueAnimator scaleAnim;

    private static final RoundedPolygon[] SHAPES = {
        MaterialShapes.normalize(MaterialShapes.SQUARE,    true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.SOFT_BURST,true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.COOKIE_9,  true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.PENTAGON,  true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.PILL,      true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.SUNNY,     true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.COOKIE_4,  true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.OVAL,      true,new RectF(-1,-1,1,1)),
        MaterialShapes.normalize(MaterialShapes.CIRCLE,    true,new RectF(-1,-1,1,1))
    };

    public NewPlayerToggle(Context c){ super(c); init(c); }
    public NewPlayerToggle(Context c, AttributeSet a){ super(c,a); init(c); }

    private void init(Context context){
        setWillNotDraw(false);
        setClickable(true);

        image = new ImageView(context);
        LayoutParams lp = new LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
        );
        lp.gravity = Gravity.CENTER;
        image.setLayoutParams(lp);
        int p = XUtils.convertToPx(context, 20f);
		image.setPadding(p, p, p, p);
        
        image.setImageDrawable(newAvd(R.drawable.pause_avd));
        ((AnimatedVectorDrawable) image.getDrawable()).start();
        
        addView(image);

        setOnClickListener(v -> {
            if(isMorphing) return;
            isAnimating = !isAnimating;
            if (autoMorph) {
               morphTo((currentShape+1) % SHAPES.length); 
            } else {
                if (isAnimating) {
                    morphTo(defaultEndShape);
                } else {
                    morphTo(defaultStartShape);
                }
            }
            if (isAnimating) {
                image.setImageDrawable(newAvd(R.drawable.resume_avd));
                ((AnimatedVectorDrawable) image.getDrawable()).start();
                startRotation();
            } else {
                image.setImageDrawable(newAvd(R.drawable.pause_avd));
                ((AnimatedVectorDrawable) image.getDrawable()).start();
                stopRotation();
            }
            if(extraClickListener != null) extraClickListener.onClick(this);
        });
    }
    
    private void startRotation() {
        if (rotateAnimator != null && rotateAnimator.isRunning()) return;

        rotateAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotateAnimator.setDuration(7500);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.addUpdateListener(a -> {
            shapeRotation = (float) a.getAnimatedValue();
            invalidate();
        });
        rotateAnimator.start();
    }

    private void stopRotation() {
        isAnimating = false;

        if (rotateAnimator != null && rotateAnimator.isRunning()) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }

        float current = shapeRotation;
        float target = ((int) (current / 90) + 1) * 90;
        if (target > 360) target = 0;

        ValueAnimator snap = ValueAnimator.ofFloat(current, target);
        snap.setDuration(600);
        snap.setInterpolator(new android.view.animation.DecelerateInterpolator());
        snap.addUpdateListener(v -> {
            shapeRotation = (float) v.getAnimatedValue();
            invalidate();
        });
        snap.start();
    }
    
    public void stopAnimation() {
        if (!isAnimating || currentShape == defaultStartShape) return;
        isAnimating = false;
        stopRotation();
        morphTo(defaultStartShape);
        image.setImageDrawable(newAvd(R.drawable.pause_avd));
        ((AnimatedVectorDrawable) image.getDrawable()).start();
    }
    
    public void startAnimation() {
        if (isAnimating || currentShape == defaultEndShape) return;
        isAnimating = true;
        startRotation();
        morphTo(defaultEndShape);
        image.setImageDrawable(newAvd(R.drawable.resume_avd));
        ((AnimatedVectorDrawable) image.getDrawable()).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getDrawingRect(bounds);
        canvas.save();
        canvas.translate(bounds.centerX(), bounds.centerY());
        canvas.rotate(shapeRotation);
        if (scaleToFit) {
            float s = Math.min(bounds.width()/(float)containerWidth,
                           bounds.height()/(float)containerHeight);
            canvas.scale(s, s);
        }
        path.rewind();
        Shapes_androidKt.toPath(
            new Morph(SHAPES[currentShape], SHAPES[targetShape]),
            progress,
            path
        );
        matrix.setScale(indicatorSize/2f, indicatorSize/2f);
        path.transform(matrix);
        paint.setColor(indicatorColor);
        canvas.drawPath(path, paint);
    
        canvas.restore();
    }

    private void animateScale(float target){
        if(scaleAnim != null) scaleAnim.cancel();
        scaleAnim = ValueAnimator.ofFloat(pressScale, target);
        scaleAnim.setDuration(120);
        scaleAnim.addUpdateListener(v -> {
            pressScale = (float) v.getAnimatedValue();
            setScaleX(pressScale);
            setScaleY(pressScale);
        });
        scaleAnim.start();
    }
    
    public boolean isStillMorphing() {
        return isMorphing;
    }

    private void morphTo(int shape){
        if(isMorphing) return;
        if(shape < 0 || shape >= SHAPES.length) return;
        if(shape == currentShape && progress == 1f) return;

        targetShape = shape;
        progress = 0f;
        isMorphing = true;

        if(animator != null) animator.cancel();

        animator = ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(morphDuration);
        animator.addUpdateListener(v -> {
            progress = (float)v.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new Animator.AnimatorListener(){
            @Override public void onAnimationEnd(Animator a){
                currentShape = targetShape;
                progress = 1f;
                isMorphing = false;
                invalidate();
            }
            @Override public void onAnimationStart(Animator a){}
            @Override public void onAnimationCancel(Animator a){ isMorphing = false; }
            @Override public void onAnimationRepeat(Animator a){}
        });
        animator.start();
    }
    
    public void setTargetShape(int shape) {
        if (8 >= shape && shape >= 0) {
            defaultEndShape = shape;
        } else {
            throw new IllegalArgumentException("Shape Int must be between 0 and 8.");
        }
        invalidate();
    }
    
    public void setStartShape(int shape) {
        if (8 >= shape && shape >= 0) {
            defaultStartShape = shape;
        } else {
            throw new IllegalArgumentException("Shape Int must be between 0 and 8.");
        }
        invalidate();
    }
    
    public void morphToInstant(int shape){
        if(shape < 0 || shape >= SHAPES.length) return;
        if(animator != null) animator.cancel();
        currentShape = targetShape = shape;
        progress = 1f;
        isMorphing = false;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        int x = (int)e.getX();
        int y = (int)e.getY();
        boolean inside = x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight();

        switch(e.getAction()){
            case MotionEvent.ACTION_DOWN:
                animateScale(0.9f);
                return true;
            case MotionEvent.ACTION_MOVE:
                if(!inside) animateScale(1f);
                return true;
            case MotionEvent.ACTION_UP:
                if(inside) performClick();
                animateScale(1f);
                return true;
            case MotionEvent.ACTION_CANCEL:
                animateScale(1f);
                return true;
        }
        return super.onTouchEvent(e);
    }

    public void setExtraOnClickListener(OnClickListener l){
        extraClickListener = l;
    }
    
    public void setAutoMorphEnabled(boolean b) {
        autoMorph = b;
    }
    
    public boolean isAnimating() {
        return isAnimating;
    }

    public void setMorphDuration(long ms){ morphDuration = Math.max(ms,0); }

    public void setShapeColor(int color) {
		indicatorColor = color;
        invalidate();
	}

	public void setIconColor(int color) {
        avdColor = color;
        Drawable d = image.getDrawable();
        if (d != null) d.setTint(color);
    }

    private AnimatedVectorDrawable newAvd(int resId) {
        Drawable d = ContextCompat.getDrawable(getContext(), resId);
        if (!(d instanceof AnimatedVectorDrawable)) return null;

        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d.mutate();
        avd.setTint(avdColor);
        return avd;
    }
}