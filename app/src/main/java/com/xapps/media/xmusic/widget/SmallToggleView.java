package com.xapps.media.xmusic.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatImageView;

public class SmallToggleView extends AppCompatImageView {

    private boolean pressedInside = false;

    public SmallToggleView(Context c) {
        super(c);
        init();
    }

    public SmallToggleView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public SmallToggleView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        init();
    }

    private void init() {
        setClickable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedInside = true;
                animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start();
                return true;

            case MotionEvent.ACTION_MOVE:
                /*if (!isInside(e)) {
                    pressedInside = false;
                    animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                }*/
                return true;

            case MotionEvent.ACTION_UP:
                animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                if (pressedInside && isInside(e)) performClick();
                pressedInside = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                pressedInside = false;
                animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private boolean isInside(MotionEvent e) {
        return e.getX() >= 0 && e.getX() <= getWidth()
                && e.getY() >= 0 && e.getY() <= getHeight();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}