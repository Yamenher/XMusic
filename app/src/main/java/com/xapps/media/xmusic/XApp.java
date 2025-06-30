package com.xapps.media.xmusic;

import android.os.StrictMode;
import android.os.Build;
import android.app.Application;
import android.content.Context;

public class XApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        if (true) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .penaltyFlashScreen()
        .build());

    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectAll() // Leaks, unclosed resources, etc.
        .penaltyLog()
        .build());
}
    }
}