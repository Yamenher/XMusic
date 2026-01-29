package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.TypefaceCompat;
import com.xapps.media.xmusic.R;

public class RoundTextView extends TextView {

    private Typeface typeface;

    public RoundTextView(Context context) {
        super(context);
        //typeface = ResourcesCompat.getFont(context, R.font.google_sans_shared);
        apply();
    }

    public RoundTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //typeface = ResourcesCompat.getFont(context, R.font.google_sans_shared);
        apply();
    }

    public RoundTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //typeface = ResourcesCompat.getFont(context, R.font.google_sans_shared);
        apply();
    }

    private void apply() {
        setTypeface(typeface);
        setFontVariationSettings("'ROND' 100");
    }

    public void setCharsWeight(int weight) {
        setFontVariationSettings("'wght' " + String.valueOf(weight) + ",'ROND' 100");
    }
}
