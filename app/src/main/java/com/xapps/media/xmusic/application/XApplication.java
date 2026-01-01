package com.xapps.media.xmusic.application;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import com.xapps.media.xmusic.BuildConfig;
import com.xapps.media.xmusic.activity.CrashReportActivity;
import com.xapps.media.xmusic.data.DataManager;

import com.xapps.media.xmusic.utils.XUtils;
import java.io.PrintWriter;
import java.io.StringWriter;

public class XApplication extends Application {

    public static boolean isForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        DataManager.init(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String report = buildReport(thread, throwable);

            Intent intent = new Intent(this, CrashReportActivity.class);
            intent.putExtra("error", report);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            );

            startActivity(intent);

            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            Process.killProcess(Process.myPid());
            System.exit(10);
        });
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
                    
            @Override
            public void onStart(@NonNull LifecycleOwner owner) { isForeground = true; }
        
            @Override
            public void onStop(@NonNull LifecycleOwner owner) { isForeground = false; }
        });
    }

    private String buildReport(Thread thread, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Thread: " + thread.getName());
        t.printStackTrace(pw);
        pw.flush();

        return
                "App: " + BuildConfig.APPLICATION_ID + "\n" +
                "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n" +
                "Android: " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT + "\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n\n" +
                sw.toString();
    }

    public void tryit() {
        XUtils.updateTheme();
        XUtils.applyDynamicColors(this);
    }
}