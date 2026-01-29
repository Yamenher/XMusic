package com.xapps.media.xmusic.helper;

import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.widget.SeekBar;

import java.util.HashMap;
import java.util.Map;

import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import com.xapps.media.xmusic.utils.XUtils;

public final class PlayerColorAnimator {

    private final Context context;
    private final ActivityMainBinding binding;

    private Map<String, Integer> effectiveOldColors = new HashMap<>();
    private ValueAnimator colorAnimator;

    private int playerSurface;
    private int bottomSheetColor;
    private float currentSlideOffset;
    private Drawable progressDrawable;

    public PlayerColorAnimator(
            Context context,
            ActivityMainBinding binding,
            Drawable progressDrawable
    ) {
        this.context = context;
        this.binding = binding;
        this.progressDrawable = progressDrawable;
    }

    public void setBottomSheetState(int bottomSheetColor, float slideOffset) {
        this.bottomSheetColor = bottomSheetColor;
        this.currentSlideOffset = slideOffset;
    }

    public void updateColors() {
        if (ColorPaletteUtils.lightColors == null && ColorPaletteUtils.darkColors == null) return;

        Map<String, Integer> colors =
                XUtils.isDarkMode(context)
                        ? ColorPaletteUtils.darkColors
                        : ColorPaletteUtils.lightColors;

        Map<String, Integer> oldColors =
                XUtils.isDarkMode(context)
                        ? ColorPaletteUtils.oldDarkColors
                        : ColorPaletteUtils.oldLightColors;

        if (effectiveOldColors.isEmpty()) {
            effectiveOldColors = new HashMap<>(oldColors);
        }

        int onTertiary = colors.get("onTertiary");
        int tertiary = colors.get("tertiary");
        int surface = colors.get("surface");
        int surfaceContainer = colors.get("surfaceContainer");
        int outline = colors.get("outline");
        int primary = colors.get("primary");
        int onPrimary = colors.get("onPrimary");
        int onSurfaceContainer = colors.get("onSurfaceContainer");
        int onSurface = colors.get("onSurface");

        int oldOnTertiary = effectiveOldColors.get("onTertiary");
        int oldTertiary = effectiveOldColors.get("tertiary");
        int oldSurface = effectiveOldColors.get("surface");
        int oldSurfaceContainer = effectiveOldColors.get("surfaceContainer");
        int oldOutline = effectiveOldColors.get("outline");
        int oldPrimary = effectiveOldColors.get("primary");
        int oldOnPrimary = effectiveOldColors.get("onPrimary");
        int oldOnSurfaceContainer = effectiveOldColors.get("onSurfaceContainer");
        int oldOnSurface = effectiveOldColors.get("onSurface");

        Drawable nextBg = binding.nextButton.getBackground();
        Drawable favBg  = binding.favoriteButton.getBackground();
        Drawable saveBg = binding.saveButton.getBackground();
        Drawable prevBg = binding.previousButton.getBackground();

        GradientDrawable bottomSheetBg =
                (GradientDrawable) binding.miniPlayerBottomSheet.getBackground();

        GradientDrawable extendableBg =
                (GradientDrawable) binding.extendableLayout.getBackground();

        GradientDrawable handleBg =
                (GradientDrawable) binding.dragHandle.getBackground();

        SeekBar seekBar = binding.songSeekbar;

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(200);

        va.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();

            int iop  = XUtils.interpolateColor(oldOnPrimary, onPrimary, f);
            int ip   = XUtils.interpolateColor(oldPrimary, primary, f);
            int iot  = XUtils.interpolateColor(oldOnTertiary, onTertiary, f);
            int it   = XUtils.interpolateColor(oldTertiary, tertiary, f);
            int is   = XUtils.interpolateColor(oldSurface, surface, f);
            int isc  = XUtils.interpolateColor(oldSurfaceContainer, surfaceContainer, f);
            int io   = XUtils.interpolateColor(oldOutline, outline, f);
            int iosc = XUtils.interpolateColor(oldOnSurfaceContainer, onSurfaceContainer, f);
            int ios  = XUtils.interpolateColor(oldOnSurface, onSurface, f);

            binding.toggleView.setShapeColor(iop);
            binding.toggleView.setIconColor(ip);

            binding.nextButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.favoriteButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.saveButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.previousButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);

            nextBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            favBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            saveBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            prevBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);

            playerSurface = is;

            int blended =
                    XUtils.interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);

            bottomSheetBg.setColor(blended);
            extendableBg.setColor(isc);
            handleBg.setColor(io);

            seekBar.setThumbTintList(ColorStateList.valueOf(ip));
            progressDrawable.setTint(ip);

            binding.placeholder1.setIconTint(ColorStateList.valueOf(iosc));

            binding.artistBigTitle.setTextColor(iosc);
            binding.songBigTitle.setTextColor(ios);
            binding.currentDurationText.setTextColor(iosc);
            binding.totalDurationText.setTextColor(iosc);
        });

        va.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled) {
                    effectiveOldColors = new HashMap<>(colors);
                }
            }
        });

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        colorAnimator = va;
        va.start();
    }
}
