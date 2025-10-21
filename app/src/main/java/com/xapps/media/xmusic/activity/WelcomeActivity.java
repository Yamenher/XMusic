package com.xapps.media.xmusic.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSeekController;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.databinding.ActivityWelcomeBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WelcomeActivity extends AppCompatActivity {
    
    private ActivityWelcomeBinding binding;
    private int currentPage = 1;
    private int MAX_PAGE_COUNT = 5;
    private OnBackPressedCallback callback1, callback2, callback3, callback4, nullcallback;
    private boolean notificationsAllowed, audiAccessAllowed, storageReadAllowed;
    private TransitionSeekController seekController;
    private TextView tv;
    private ActivityResultLauncher<String> requestPermissionLauncher;
            
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.progressBar.setProgress(0);
        MaterialColorUtils.initColors(this);
        setupInsets();
        setupClickListeners();
        setupCallbacks();
        setupTextViewEffect();
        setupPermsLaunchers();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 33) {
            boolean b1 = checkPermissionAllowed(this, Manifest.permission.READ_MEDIA_AUDIO);
            boolean b2 = checkPermissionAllowed(this, Manifest.permission.POST_NOTIFICATIONS);
            audiAccessAllowed = b1;
            binding.permission1Button.setEnabled(!b1);
            binding.permission1Button.setText(b1? "Permission Granted" : "Grant permission");
            notificationsAllowed = b2;
            binding.permission2Button.setEnabled(!b2);
            binding.permission2Button.setText(b2? "Permission Granted" : "Grant permission");
        } else {
            boolean b3 = checkPermissionAllowed(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            storageReadAllowed = b3;
            binding.permission1Button.setEnabled(!b3);
            binding.permission1Button.setText(b3? "Permission Granted" : "Grant permission");
            
        }
    }

    private void setupInsets() {
        binding.topWindow.setPadding(0, XUtils.getStatusBarHeight(this), 0, 0);
        XUtils.setMargins(binding.bottomCard, XUtils.getMargin(binding.bottomCard, "left"), 0, XUtils.getMargin(binding.bottomCard, "right"), XUtils.getNavigationBarHeight(this));
    }

    private void setupClickListeners() {
        binding.startButton.setOnClickListener(v -> {
            if (notificationsAllowed && audiAccessAllowed) {
                nullcallback.setEnabled(true);
                callback4.setEnabled(false);
                binding.topWindow.animate().translationYBy(-binding.topWindow.getHeight()).setDuration(200).start();
                binding.bottomWindow.animate().translationYBy(binding.bottomWindow.getHeight()).setDuration(200).start();
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
                msa.setDuration(800);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                binding.finalScreen.setVisibility(View.GONE);
                binding.part2View.setVisibility(View.VISIBLE);
                
                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                executor.execute(() -> {
                    SongMetadataHelper.getAllSongs(this, new SongLoadListener(){
                        @Override
                        public void onProgress(int count) {
                            runOnUiThread(() -> {
                                binding.loadingText.setText("Loading songs...($1 loaded)".replace("$1", String.valueOf(count)));
                            });
                        }
                
                        @Override
                        public void onComplete(java.util.ArrayList<HashMap<String, Object>> songs) {
                            try {
                                SerializationUtils.saveToFile(WelcomeActivity.this, songs, "songsList");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                binding.loadingText.setText("Complete! starting app...");
                            });
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                DataManager.setDataInitialized();
                                Intent i = new Intent();
                                i.setClass(WelcomeActivity.this, MainActivity.class);
                                startActivity(i);
                                finish();
                            }, 2500);
                        }
                    });
                });
            } else {
                Snackbar.make(WelcomeActivity.this, binding.coordinator, "Please allow all necessary permissions first", Snackbar.LENGTH_SHORT).setAnchorView(binding.bottomWindow).show(); 
            }
        });
            
        binding.joinButton.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://t.me/xmusiccommunity"));
            startActivity(i);
        });
        
        binding.permission1Button.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) { 
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }     
        });
        
        binding.permission2Button.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) { 
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
        
        binding.fabNext.setOnClickListener(v -> {
            MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, true);
            TransitionManager.beginDelayedTransition(binding.coordinator, msa);
            currentPage++;
            switch (currentPage) {
                case 2:
                binding.progressText.setText("Step 1/3");
                binding.progressBar.setProgressCompat(15, true);
                binding.firstScreen.setVisibility(View.GONE);
                binding.secondScreen.setVisibility(View.VISIBLE);
                break;
                
                case 3:
                binding.progressText.setText("Step 2/3");
                binding.progressBar.setProgressCompat(40, true);
                binding.secondScreen.setVisibility(View.GONE);
                binding.thirdScreen.setVisibility(View.VISIBLE);
                break;
                
                case 4:
                binding.progressText.setText("Step 3/3");
                binding.progressBar.setProgressCompat(70, true);
                binding.thirdScreen.setVisibility(View.GONE);
                binding.fourthScreen.setVisibility(View.VISIBLE);
                break;
                
                case 5:
                binding.progressText.setText("All set!");
                binding.progressBar.setProgressCompat(100, true);
                binding.fourthScreen.setVisibility(View.GONE);
                binding.finalScreen.setVisibility(View.VISIBLE);
                break;
            }
            checkPage();
        });
        
        binding.fabBack.setOnClickListener(v -> {
            MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
            TransitionManager.beginDelayedTransition(binding.coordinator, msa);
            currentPage--;
            switch (currentPage) {
                case 1:
                binding.progressText.setText("Let's start!");
                binding.firstScreen.setVisibility(View.VISIBLE);
                binding.secondScreen.setVisibility(View.GONE);
                binding.progressBar.setProgressCompat(0, true);
                break;
                
                case 2:
                binding.progressText.setText("Step 1/3");
                binding.secondScreen.setVisibility(View.VISIBLE);
                binding.thirdScreen.setVisibility(View.GONE);
                binding.progressBar.setProgressCompat(15, true);
                break;
                
                case 3:
                binding.progressText.setText("Step 2/3");
                binding.thirdScreen.setVisibility(View.VISIBLE);
                binding.fourthScreen.setVisibility(View.GONE);
                binding.progressBar.setProgressCompat(40, true);
                break;
                
                case 4:
                binding.progressText.setText("Step 3/3");
                binding.fourthScreen.setVisibility(View.VISIBLE);
                binding.finalScreen.setVisibility(View.GONE);
                binding.progressBar.setProgressCompat(70, true);
                break;
            }
            checkPage();
        });
    }

    private void checkPage() {
        if (currentPage == 1) {
            binding.fabBack.setEnabled(false);
            binding.fabNext.setEnabled(true);
        } else if (currentPage == MAX_PAGE_COUNT) {
            binding.fabNext.setEnabled(false);
            binding.fabBack.setEnabled(true);
        } else {
            binding.fabNext.setEnabled(true);
            binding.fabBack.setEnabled(true);
        }
        
        if (currentPage == 1) {
            callback1.setEnabled(false);
            callback2.setEnabled(false);
            callback3.setEnabled(false);
            callback4.setEnabled(false);
        } else if (currentPage == 2) {
            callback1.setEnabled(true);
            callback2.setEnabled(false);
            callback3.setEnabled(false);
            callback4.setEnabled(false);
        } else if (currentPage == 3) {
            callback2.setEnabled(true);
            callback1.setEnabled(false);
            callback3.setEnabled(false);
            callback4.setEnabled(false);
        } else if (currentPage == 4) {
            callback3.setEnabled(true);
            callback2.setEnabled(false);
            callback1.setEnabled(false);
            callback4.setEnabled(false);
        } else if (currentPage == 5) {
            callback3.setEnabled(false);
            callback2.setEnabled(false);
            callback1.setEnabled(false);
            callback4.setEnabled(true);
        }
    }

    private void setupCallbacks() {
        callback1 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                seekController = TransitionManager.controlDelayedTransition(binding.coordinator, new MaterialSharedAxis(MaterialSharedAxis.X, false));
                binding.firstScreen.setVisibility(View.VISIBLE);
                binding.secondScreen.setVisibility(View.GONE);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                if (seekController != null && seekController.isReady() && backEvent.getProgress() > 0.01f) {
                    float progress = backEvent.getProgress();
                    seekController.setCurrentFraction(progress);
                    binding.progressBar.setProgressCompat(Math.round(15-15*2*progress), true);
                }
            }

            @Override
            public void handleOnBackPressed() {
                if (seekController != null) {
                    seekController.animateToEnd();
                    seekController = null;
                }
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                currentPage--;
                checkPage();
                callback1.setEnabled(false);
                binding.progressBar.setProgressCompat(0, true);
                binding.progressText.setText("Let's start!");
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.progressBar.setProgressCompat(15, true);
                binding.firstScreen.setVisibility(View.INVISIBLE);
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                msa.setDuration(0);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                binding.firstScreen.setVisibility(View.GONE);
                binding.secondScreen.setVisibility(View.VISIBLE);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback1);
        
        callback2 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                seekController = TransitionManager.controlDelayedTransition(binding.coordinator, new MaterialSharedAxis(MaterialSharedAxis.X, false));
                binding.secondScreen.setVisibility(View.VISIBLE);
                binding.thirdScreen.setVisibility(View.GONE);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                if (seekController != null && seekController.isReady() && backEvent.getProgress() > 0.01f) {
                    float progress = backEvent.getProgress();
                    seekController.setCurrentFraction(progress);
                    binding.progressBar.setProgressCompat(Math.round(40-25*progress), true);
                }
            }

            @Override
            public void handleOnBackPressed() {
                if (seekController != null) {
                    seekController.animateToEnd();
                    seekController = null;
                }
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                currentPage--;
                checkPage();
                callback2.setEnabled(false);
                binding.progressBar.setProgressCompat(15, true);
                binding.progressText.setText("Step 1/3");
                    
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.progressBar.setProgressCompat(40, true);
                binding.secondScreen.setVisibility(View.INVISIBLE);
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                msa.setDuration(0);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                binding.secondScreen.setVisibility(View.GONE);
                binding.thirdScreen.setVisibility(View.VISIBLE);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback2);
        
        callback4 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                seekController = TransitionManager.controlDelayedTransition(binding.coordinator, new MaterialSharedAxis(MaterialSharedAxis.X, false));
                binding.fourthScreen.setVisibility(View.VISIBLE);
                binding.finalScreen.setVisibility(View.GONE);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                if (seekController != null && seekController.isReady() && backEvent.getProgress() > 0.01f) {
                    float progress = backEvent.getProgress();
                    seekController.setCurrentFraction(progress);
                    binding.progressBar.setProgressCompat(Math.round(100-30*progress), true);
                }
            }

            @Override
            public void handleOnBackPressed() {
                if (seekController != null) {
                    seekController.animateToEnd();
                    seekController = null;
                }
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                currentPage--;
                checkPage();
                callback4.setEnabled(false);
                binding.progressBar.setProgressCompat(70, true);
                binding.progressText.setText("Step 3/3");
                    
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.progressBar.setProgressCompat(100, true);
                binding.fourthScreen.setVisibility(View.INVISIBLE);
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                msa.setDuration(0);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                binding.fourthScreen.setVisibility(View.GONE);
                binding.finalScreen.setVisibility(View.VISIBLE);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback4);
        
        callback3 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                seekController = TransitionManager.controlDelayedTransition(binding.coordinator, new MaterialSharedAxis(MaterialSharedAxis.X, false));
                binding.thirdScreen.setVisibility(View.VISIBLE);
                binding.fourthScreen.setVisibility(View.GONE);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                if (seekController != null && seekController.isReady() && backEvent.getProgress() > 0.01f) {
                    float progress = backEvent.getProgress();
                    seekController.setCurrentFraction(progress);
                    binding.progressBar.setProgressCompat(Math.round(70-30*progress), true);
                }
            }

            @Override
            public void handleOnBackPressed() {
                if (seekController != null) {
                    seekController.animateToEnd();
                    seekController = null;
                }
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                currentPage--;
                checkPage();
                callback3.setEnabled(false);
                binding.progressBar.setProgressCompat(40, true);
                binding.progressText.setText("Step 2/3");
                    
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.progressBar.setProgressCompat(40, true);
                binding.thirdScreen.setVisibility(View.INVISIBLE);
                MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.X, false);
                msa.setDuration(0);
                TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                binding.thirdScreen.setVisibility(View.GONE);
                binding.fourthScreen.setVisibility(View.VISIBLE);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback3);
        
        nullcallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
            }
        };
        getOnBackPressedDispatcher().addCallback(this, nullcallback);
    }

    public void setupTextViewEffect() {
        binding.progressText.setFactory(() -> {
            tv = new TextView(this);
            tv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv.setText("Let's go!");
            tv.setTextSize(22f);
            tv.setGravity(Gravity.CENTER);
            return tv;
        });
    }

    public void setupPermsLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (Build.VERSION.SDK_INT >= 33) {
                boolean b1 = checkPermissionAllowed(this, Manifest.permission.READ_MEDIA_AUDIO);
                boolean b2 = checkPermissionAllowed(this, Manifest.permission.POST_NOTIFICATIONS);
                audiAccessAllowed = b1;
                binding.permission1Button.setEnabled(!b1);
                binding.permission1Button.setText(b1? "Permission Granted" : "Grant permission");
                notificationsAllowed = b2;
                binding.permission2Button.setEnabled(!b2);
                binding.permission2Button.setText(b2? "Permission Granted" : "Grant permission");
            } else {
                boolean b3 = checkPermissionAllowed(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                storageReadAllowed = b3;
                binding.permission1Button.setEnabled(!b3);
                binding.permission1Button.setText(b3? "Permission Granted" : "Grant permission");
            
            }
        });
    }


    public boolean checkPermissionAllowed(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
}
