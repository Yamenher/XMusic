package com.xapps.media.xmusic.utils;

import android.content.Context;
import android.graphics.Color;
import com.google.android.material.color.MaterialColors;

public class MaterialColorUtils {

    public static boolean isAlreadyInitialized = false;

    public static int colorPrimary = Color.WHITE;
    public static int colorOnPrimary = Color.WHITE;
    public static int colorPrimaryContainer = Color.WHITE;
    public static int colorOnPrimaryContainer = Color.WHITE;
    public static int colorSecondary = Color.WHITE;
    public static int colorOnSecondary = Color.WHITE;
    public static int colorSecondaryContainer = Color.WHITE;
    public static int colorOnSecondaryContainer = Color.WHITE;
    public static int colorTertiary = Color.WHITE;
    public static int colorOnTertiary = Color.WHITE;
    public static int colorTertiaryContainer = Color.WHITE;
    public static int colorOnTertiaryContainer = Color.WHITE;
    public static int colorSurface = Color.WHITE;
    public static int colorOnSurface = Color.WHITE;
    public static int colorSurfaceDim = Color.WHITE;
    public static int colorSurfaceBright = Color.WHITE;
    public static int colorSurfaceContainer = Color.WHITE;
    public static int colorSurfaceContainerHigh = Color.WHITE;
    public static int colorSurfaceContainerLow = Color.WHITE;
    public static int colorSurfaceContainerHighest = Color.WHITE;
    public static int colorSurfaceContainerLowest = Color.WHITE;
    public static int colorOutline = Color.WHITE;

    public static void initColors(Context c) {
        colorPrimary = MaterialColors.getColor(c, android.R.attr.colorPrimary, Color.WHITE);
        colorOnPrimary = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        colorPrimaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorPrimaryContainer, Color.WHITE);
        colorOnPrimaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE);
        colorSecondary = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSecondary, Color.WHITE);
        colorOnSecondary = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSecondary, Color.WHITE);
        colorSecondaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSecondaryContainer, Color.WHITE);
        colorOnSecondaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.WHITE);
        colorTertiary = MaterialColors.getColor(c, com.google.android.material.R.attr.colorTertiary, Color.WHITE);
        colorOnTertiary = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnTertiary, Color.WHITE);
        colorTertiaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorTertiaryContainer, Color.WHITE);
        colorOnTertiaryContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnTertiaryContainer, Color.WHITE);
        colorSurface = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurface, Color.WHITE);
        colorOnSurface = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        colorSurfaceDim = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceDim, Color.WHITE);
        colorSurfaceBright = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceBright, Color.WHITE);
        colorSurfaceContainer = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainer, Color.WHITE);
        colorSurfaceContainerHigh = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.WHITE);
        colorSurfaceContainerLow = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerLow, Color.WHITE);
        colorSurfaceContainerHighest = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerHighest, Color.WHITE);
        colorSurfaceContainerLowest = MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurfaceContainerLowest, Color.WHITE);
        colorOutline = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOutline, Color.WHITE);
    }
}