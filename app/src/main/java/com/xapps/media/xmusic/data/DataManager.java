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
}
