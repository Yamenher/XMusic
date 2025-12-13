package com.xapps.media.xmusic.models;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.lang.reflect.Field;

public class CustomBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    public CustomBottomSheetBehavior() {
        super();
    }

    public CustomBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {}
}