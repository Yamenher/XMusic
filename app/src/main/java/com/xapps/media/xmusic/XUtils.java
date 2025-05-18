package com.xapps.media.xmusic;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;

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
	
	public static int convertToPx(Context context, float dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
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
}
