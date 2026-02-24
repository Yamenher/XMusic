package com.xapps.media.xmusic.data;
import android.content.Context;
import android.content.SharedPreferences;

public class DataManager {
    public static SharedPreferences sp;

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

    public static void setOledTheme(boolean b) {
        sp.edit().putBoolean("oledTheme", b).apply();
    }

    public static boolean isOledThemeEnabled() {
        return sp.getBoolean("oledTheme", false);
    }

    public static void setNewIconEnabled(boolean b) {
        sp.edit().putBoolean("newIcon", b).apply();
    }

    public static boolean isNewIconEnabled() {
        return sp.getBoolean("newIcon", false);
    }

    public static void setStableColors(boolean b) {
        sp.edit().putBoolean("stable_colors", b).apply();
    }

    public static boolean areStableColors() {
        return sp.getBoolean("stable_colors", false);
    }

    public static void saveLatestRepeatMode(String s) {
        sp.edit().putString("repeatMode", s).apply();
    }

    public static String getLatestRepeatMode() {
        return sp.getString("repeatMode", "LOOP_OFF");
    }

    public static void saveLatestShuffleMode(String s) {
        sp.edit().putString("shuffleMode", s).apply();
    }

    public static String getLatestShuffleMode() {
        return sp.getString("shuffleMode", "SHUFFLE_OFF");
    }

    public static void saveItemsList(String s) {
        sp.edit().putString("listData", s).apply();
    }

    public static String loadItemsList() {
        return sp.getString("listData", "");
    }
}
