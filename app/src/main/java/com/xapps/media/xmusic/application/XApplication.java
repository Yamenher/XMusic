package com.xapps.media.xmusic.application;

import android.content.Intent;
import android.os.StrictMode;
import android.os.Build;
import android.app.Application;
import android.content.Context;
import com.google.android.material.color.DynamicColors;
import com.xapps.media.xmusic.activity.CrashActivity;

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
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
            intent.putExtra("error", throwable.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(2);
        });
    }
}