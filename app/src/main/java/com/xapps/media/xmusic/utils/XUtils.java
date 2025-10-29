package com.xapps.media.xmusic.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import java.lang.reflect.Field;

public class XUtils {
	
	public static void increaseMarginsSmoothly(View view, int increaseLeft, int increaseTop, int increaseRight, int increaseBottom, long duration) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		int startLeft = params.leftMargin;
		int startTop = params.topMargin;
		int startRight = params.rightMargin;
		int startBottom = params.bottomMargin;
		int endLeft = startLeft + increaseLeft;
		int endTop = startTop + increaseTop;
		int endRight = startRight + increaseRight;
		int endBottom = startBottom + increaseBottom;
		ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
		animator.setDuration(duration);
		animator.addUpdateListener(animation -> {
			float progress = (float) animation.getAnimatedValue();
			params.leftMargin = (int) (startLeft + progress * (endLeft - startLeft));
			params.topMargin = (int) (startTop + progress * (endTop - startTop));
			params.rightMargin = (int) (startRight + progress * (endRight - startRight));
			params.bottomMargin = (int) (startBottom + progress * (endBottom - startBottom));
			view.setLayoutParams(params);
		});
		
		animator.start();
	}
	
	public static void setMargins(View view, int left, int top, int right, int bottom) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		params.setMargins(left, top, right, bottom);
		view.setLayoutParams(params);
	}
	
	public static void increaseMargins(View view, int increaseLeft, int increaseTop, int increaseRight, int increaseBottom) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		params.leftMargin += increaseLeft;
		params.topMargin += increaseTop;
		params.rightMargin += increaseRight;
		params.bottomMargin += increaseBottom;
		view.setLayoutParams(params);
	}
	
	public static int getStatusBarHeight(Context c) {
		Resources resources = c.getResources();
		int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			return resources.getDimensionPixelSize(resourceId);
		}
		return 0;
	}
    
    public static int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
	
	public static int convertToPx(Context context, float dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
	}
    
    public static float convertSpToPx(Context context, float sp) {
        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = context.getResources().getConfiguration().fontScale;
        return sp * density * fontScale;
    }
	
	public static int getMargin(View view, String side) {
		if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
			switch (side.toLowerCase()) {
				case "left":
				return params.leftMargin;
				case "top":
				return params.topMargin;
				case "right":
				return params.rightMargin;
				case "bottom":
				return params.bottomMargin;
				default:
				throw new IllegalArgumentException("Invalid margin side: " + side);
			}
		}
		return 0;
	}
    
    public static void showMessage(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
    
    public static void openSettings(Context c) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", c.getPackageName(), null);
        intent.setData(uri);
        c.startActivity(intent);
    }
    
    public static void animateColor(int fromColor, int toColor, ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(fromColor, toColor);
		colorAnimator.setDuration(0);
		colorAnimator.addUpdateListener(listener);
		colorAnimator.start();
	}
    
    public static Integer extractColor(View view) {
		Drawable background = view.getBackground();
		if (background instanceof GradientDrawable) {
			try {
				Field stateField = GradientDrawable.class.getDeclaredField("mFillPaint");
				stateField.setAccessible(true);
				Paint fillPaint = (Paint) stateField.get(background);
				return fillPaint.getColor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
    
    public static int interpolateColor(int startColor, int endColor, float percentage) {
			return (int) new android.animation.ArgbEvaluator().evaluate(percentage, startColor, endColor);
	}

    public static float clamp(float value, float min, float max) {
			return Math.max(min, Math.min(max, value));
	}

    public static boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
    
    public static void setThemeMode(String mode) {
        switch (mode) {
            case "light":
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            break;
            case "dark":
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            break;
            case "auto":
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            break;
        }
    }

    public static int normalizeColor(int color) {
        return color | 0xFF000000;
    }
}
