package com.xapps.media.xmusic;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.resources.*;
import androidx.core.*;
import androidx.core.ktx.*;
import androidx.core.splashscreen.*;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.emoji2.*;
import androidx.emoji2.viewsintegration.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.process.*;
import androidx.lifecycle.runtime.*;
import androidx.lifecycle.viewmodel.*;
import androidx.lifecycle.viewmodel.savedstate.*;
import androidx.media.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.palette.*;
import androidx.profileinstaller.*;
import androidx.savedstate.*;
import androidx.security.*;
import androidx.startup.*;
import androidx.transition.*;
import com.appbroker.roundedimageview.*;
import com.google.android.material.*;
import com.xapps.media.xmusic.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class SplashActivity extends AppCompatActivity {
	
	private SplashBinding binding;
	private boolean isDataLoaded = false;
	private final Context c = this;
	
	private Intent intent = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		binding = SplashBinding.inflate(getLayoutInflater());
		final SplashScreen splash = SplashScreen.installSplashScreen(this);
		splash.setKeepOnScreenCondition(() -> {
			return !isDataLoaded;
		});
        }
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
	}
	
	private void initializeLogic() {
		HandlerThread handlerThread = new HandlerThread("SplashBackgroundThread");
		handlerThread.start();
		Handler backgroundHandler = new Handler(handlerThread.getLooper());
		Handler mainHandler = new Handler(Looper.getMainLooper());
		backgroundHandler.post(new Runnable() {
				@Override
				public void run() {
						try {
								com.xapps.media.xmusic.data.DataManager.init(c);
								mainHandler.post(() -> {
										isDataLoaded = true;
								});
						} catch (Exception e) {
								android.util.Log.e("Fucking error!", e.toString());
						}
						handlerThread.quitSafely();
						return;
				}
		});
		
		intent.setClass(getApplicationContext(), MainActivity.class);
		startActivity(intent);
		finish();
	}
	
}