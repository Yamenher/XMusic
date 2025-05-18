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
        .detectAll() // Detects everything: disk reads/writes, network, etc.
        .penaltyLog() // Logs violations to Logcat
        .penaltyFlashScreen() // Makes your app flash red when you mess up UI thread
        .build());

    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectAll() // Leaks, unclosed resources, etc.
        .penaltyLog()
        .build());
}
    }
}