package com.xapps.media.xmusic.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.utils.XUtils;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 31) SplashScreen.installSplashScreen(this);
        if (!XUtils.areAllPermsGranted(this)) {
            startActivity(new Intent(this, WelcomeActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
