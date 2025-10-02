package com.xapps.media.xmusic.application;

import android.os.StrictMode;
import android.os.Build;
import android.app.Application;
import android.content.Context;
import com.google.android.material.color.DynamicColors;

public class XApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        if (true) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .penaltyFlashScreen()
        .build());

    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build());
}
    }
}