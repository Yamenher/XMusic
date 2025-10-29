package com.xapps.media.xmusic.application;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Process;
import android.os.StrictMode;
import android.os.Build;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;
import com.google.android.material.color.ColorResourcesOverride;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.MaterialColorUtilitiesHelper;
import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.SchemeContent;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.data.DataManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import com.xapps.media.xmusic.BuildConfig;
import com.xapps.media.xmusic.R;

public class XApplication extends Application {
    
    @Override
    public void onCreate() {
        DataManager.init(this);
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String report = buildReport(thread, throwable);

            try {
                sendToTelegram(report, 634);
            } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Crash report failed", Toast.LENGTH_SHORT).show();
            }

            Process.killProcess(Process.myPid());
            System.exit(1);
        });
        
        super.onCreate();
        
        
        
        if (false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyFlashScreen()
            .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .setClassInstanceLimit(MainActivity.class, 1)
            .build());
        }
    }

    private void sendToTelegram(String text, long threadId) throws Exception {
        int maxLen = 4096;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            String chunk = text.substring(start, end);

            String urlStr = "https://api.telegram.org/bot" + BuildConfig.TG_BOT_TOKEN + "/sendMessage";
            String payload = "chat_id=" + BuildConfig.LOG_CHAT_ID +
                         "&text=" + URLEncoder.encode(chunk, "UTF-8");

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.getOutputStream().write(payload.getBytes());
            conn.getResponseCode();
            conn.disconnect();

            start = end;
        }
    }    

    private String buildReport(Thread thread, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Thread: " + thread.getName());
        t.printStackTrace(pw);
        pw.flush();

        return "App: " + BuildConfig.APPLICATION_ID + "\n" +
                "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n" +
                "Android: " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT + "\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n\n" +
                sw.toString();
    }

}