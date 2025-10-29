package com.xapps.media.xmusic.data;
import android.content.Context;
import android.content.SharedPreferences;

public class DataManager {
    private static SharedPreferences sp;

    public static void init(Context c) {
        sp = c.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public static void setDataInitialized() {
        sp.edit().putBoolean("isDataInitialized", true).apply();
    }

    public static boolean isDataLoaded() {
        return sp.getBoolean("isDataInitialized", false);
    }

    public static void setDynamicColorsEnabled(boolean b) {
        sp.edit().putBoolean("isDynamicColorsOn", b).apply();
    }

    public static boolean isDynamicColorsOn() {
        return sp.getBoolean("isDynamicColorsOn", false);
    }

    public static void setCustomColorsEnabled(boolean b) {
        sp.edit().putBoolean("isCustomColorsOn", b).apply();
    }

    public static boolean isCustomColorsOn() {
        return sp.getBoolean("isCustomColorsOn", false);
    }

    public static void setCustomColor(int c) {
        sp.edit().putInt("customColor", c).apply();
    }

    public static int getCustomColor() {
        return sp.getInt("customColor", 0xFFFF7AAE);
    }

    public static void setProgress(int i) {
        sp.edit().putInt("progress", i).apply();
    }

    public static int getProgress() {
        return sp.getInt("progress", 0);
    }

    public static void setThemeMode(int mode) {
        sp.edit().putInt("theme", mode).apply();
    }

    public static int getThemeMode() {
        return sp.getInt("theme", 0);
    }

    public static void saveFabMargin(int i) {
        sp.edit().putInt("fabMargin", i).apply();
    }

    public static int getFabMargin() {
        return sp.getInt("fabMargin", 0);
    }

    public static void saveNormalFabMargin(int i) {
        sp.edit().putInt("fabMarginNormal", i).apply();
    }

    public static int getNormalFabMargin() {
        return sp.getInt("fabMarginNormal", 0);
    }
}
