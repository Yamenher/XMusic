package com.xapps.media.xmusic.activity;

import android.Manifest;
import android.animation.*;
import android.content.*;
import android.content.pm.PackageManager; 
import android.content.res.*;
import android.content.res.loader.ResourcesLoader;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.util.Log;
import android.view.*;
import android.view.View.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.webkit.*;
import android.widget.*;

import androidx.activity.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.resources.*;
import androidx.constraintlayout.motion.widget.MotionScene;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.splashscreen.*;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.emoji2.*;
import androidx.fragment.app.*;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.process.*;
import androidx.lifecycle.runtime.*;
import androidx.lifecycle.viewmodel.*;
import androidx.lifecycle.viewmodel.savedstate.*;
import androidx.media.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.palette.*;
import androidx.profileinstaller.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.savedstate.*;
import androidx.startup.*;
import androidx.transition.*;

import androidx.viewpager2.widget.ViewPager2;
import com.appbroker.roundedimageview.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import com.google.android.material.*;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.ColorResourcesOverride;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.MaterialColorUtilitiesHelper;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.color.ThemeUtils;
import com.google.android.material.color.utilities.DynamicScheme;
import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.Scheme;
import com.google.android.material.color.utilities.SchemeContent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.fragment.SettingsFragment;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.adapter.CustomPagerAdapter;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.databinding.MainBinding;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.*;
import com.xapps.media.xmusic.fragment.MusicListFragment;

import com.xapps.media.xmusic.viewmodel.MainActivityViewModel;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends AppCompatActivity implements PlayerService.Callback {
	
	private MainBinding binding;
    private final Context c = this;
	public float targetMargin, bsh;
    private boolean receiveProgress = false;
	private boolean isAnimated = false;
    private boolean isBsInvisible = true;
    private boolean isRun = false;
    private boolean seekbarFree = true;
    private boolean IsColorAnimated = false;
    public boolean isbnvHidden, isDataLoaded, wasAdjusted = false;
    private int playbackState, playerSurface, bottomSheetColor, tmpColor , currentPosition, currentProgress;
    public int navBarHeight, statusBarHeight;
    private long lastClick = 0;
	public ExoPlayer player;
	public BottomSheetBehavior bottomSheetBehavior, innerBottomSheetBehavior;
	private Handler handler = new Handler(Looper.getMainLooper());
	private Runnable updateProgressRunnable;
	private MediaSessionCompat mediaSession;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()/2);
    private float currentSlideOffset;
    private float bst;
    public boolean isPlaying = false;
    private Handler actionSender = new Handler(Looper.getMainLooper());
    private boolean seekAllowed = true;
    private boolean progressAllowed = false;
    private MusicListFragment mlfa; 
    private OnBackPressedCallback callback, callback2;
    public static boolean fabHandlerRunning;
    
    private HandlerThread handlerThread = new HandlerThread("BackgroundThread");
    private Handler bgHandler;
	
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
	public static ArrayList<HashMap<String, Object>> currentMap = new ArrayList<>();
    
    private NavHostFragment navHostFragment;
    private NavController navController;
    
    public MediaController mediaController;
    private SessionToken sessionToken;
    
    private MainActivityViewModel viewmodel;
    
    private PlayerService playerService;
    private boolean bound = false;
    private ListenableFuture<MediaController> controllerFuture;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            playerService = binder.getService();
            playerService.setCallback(MainActivity.this);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            playerService = null;
        }
    };
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        saveUIState();
        super.onSaveInstanceState(outState);
    }
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
        applyDynamicColors();
        super.onCreate(_savedInstanceState);
        updateTheme();
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer).getChildFragmentManager().getFragments().get(0);
        if (f instanceof MusicListFragment) {
            mlfa = (MusicListFragment) f;
        }
        loadSongs();
		initialize();
	}
    
    @Override
	public void onResume() {
	    super.onResume();
        if (mediaController != null) {
            binding.musicProgress.setProgress((int) mediaController.getCurrentPosition());
            if (!mediaController.isPlaying() && binding.toggleView.isAnimating()) {
                binding.toggleView.stopAnimation();
            } else if (mediaController.isPlaying() && !binding.toggleView.isAnimating()) {
                binding.toggleView.startAnimation();
            }
            currentPosition = mediaController.getCurrentMediaItemIndex();
            updateColors();
            updateTexts(-1);
            updateMaxValue(-1);
        }
    }
	
	@Override
	public void onPause() {
		super.onPause();
        unregisterReceiver(multiReceiver);
	}
    
    @Override
    public void onStart() {
        super.onStart();
        setupReceivers(true);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer).getChildFragmentManager().getFragments().get(0);
            if (f instanceof MusicListFragment) {
                mlfa = (MusicListFragment) f;
                try {
                    mlfa.adjustUI();
                } catch (Exception e) {
                    
                }
            }
        });
        bindService(new Intent(this, PlayerService.class), connection, Context.BIND_AUTO_CREATE);
        if (sessionToken == null) sessionToken = new SessionToken(c, new ComponentName(c, PlayerService.class));
        if (controllerFuture == null && mediaController == null) {
            controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
                controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();
                    if (isRestoring) {
                        progressAllowed = true;
                        isPlaying = mediaController.isPlaying();
                        if (isPlaying) binding.toggleView.startAnimation();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, MoreExecutors.directExecutor());
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bound) {
            playerService.setCallback(null);
            unbindService(connection);
            bound = false;
        }
        executor.shutdown();
        unregisterReceiver(focusReceiver);
    }
	
	private void initialize() {
        viewmodel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
        restoreStateIfPossible();
        Intent i = new Intent(this, PlayerService.class);
        startService(i);
        handlerThread.start();
        Looper l = handlerThread.getLooper();
        bgHandler = new Handler(l);
        MaterialColorUtils.initColors(this);
        setupUI();
		setupCallbacksAndEvents();
    }    
   
	private void setupUI() {
        binding.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners_bottom_sheet));
        EdgeToEdge.enable(this);
        binding.bottomNavigation.post(() -> {
            bst = binding.bottomNavigation.getHeight();
        });
        RecyclerView innerRecylcerView = (RecyclerView) binding.coversPager.getChildAt(0);
        if (innerRecylcerView != null) {
            innerRecylcerView.setNestedScrollingEnabled(false);
            innerRecylcerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        int pageGap = XUtils.convertToPx(this, 30);
        binding.coversPager.setClipToPadding(false);
        binding.coversPager.setClipChildren(false);
		bottomSheetColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer, 0xff000000);
		int resourceId = c.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        int resourceId2 = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
            
		if (resourceId > 0 && resourceId2 > 0) {
            statusBarHeight = c.getResources().getDimensionPixelSize(resourceId2);
            navBarHeight = c.getResources().getDimensionPixelSize(resourceId);
		}
            
        int p = XUtils.convertToPx(this, 16f);
        binding.extendableLayout.setPadding(p, 0, p, navBarHeight);
        XUtils.setMargins(binding.coversPager, 0, XUtils.getStatusBarHeight(this)*2, 0, 0);
		binding.songBigTitle.setSelected(true);
		binding.artistBigTitle.setSelected(true);
		binding.currentSongTitle.setSelected(true);
		binding.currentSongArtist.setSelected(true);
		bottomSheetBehavior.setHideable(true);
        bsh = bottomSheetBehavior.getPeekHeight();
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomNavigation.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isRun) {
                targetMargin = binding.bottomNavigation.getHeight() ;
			    XUtils.increaseMargins(binding.musicProgress, 0, 0, 0, navBarHeight);
			    bottomSheetBehavior.setPeekHeight(bottomSheetBehavior.getPeekHeight() + navBarHeight);
			    int bottomHeight = binding.coversPager.getHeight() + binding.bottomNavigation.getHeight() + binding.musicProgress.getHeight() + XUtils.getMargin(binding.musicProgress, "top");
            }
            isRun = true;
        });
	}
    
    public void openFragment(int layoutId) {
        try {
            navController.navigate(layoutId);
        } catch (Exception e) {}
    }
	
	public MainBinding getBinding() {
		return binding;
	}
        
	public void _setSong(final int _position, final String _coverPath, final Uri _fileUri) {
        progressAllowed = false;
        currentPosition = _position;
        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(0));
        binding.songSeekbar.setValue(0f);
        binding.musicProgress.setProgressCompat(0, true);
        updateMaxValue(_position);
        bgHandler.postDelayed(() -> {
            Intent playIntent = new Intent(this, PlayerService.class);
            playIntent.setAction("ACTION_PLAY");
            playIntent.putExtra("uri", _fileUri.toString());
            playIntent.putExtra("title", currentMap.get(_position).get("title").toString());
            playIntent.putExtra("artist", currentMap.get(_position).get("author").toString());
            playIntent.putExtra("cover", _coverPath);
            playIntent.putExtra("position", _position);
            startService(playIntent);
        }, 10);
        binding.musicProgress.setProgressCompat(0, true);
        updateCoverPager(_position);
        binding.songSeekbar.setValue(0);
        if (isBsInvisible) {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
        binding.artistBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.songBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.totalDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.currentDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        handler = new Handler(Looper.getMainLooper());   
        handler.postDelayed(() -> {
            updateTexts(_position);
            binding.totalDurationText.setTranslationX(20f);
            binding.currentDurationText.setTranslationX(20f);
            binding.songBigTitle.setTranslationX(20f);
            binding.artistBigTitle.setTranslationX(20f);
        }, 110);
        handler.postDelayed(() -> {
            binding.artistBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
            binding.songBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
            binding.currentDurationText.animate().alpha(1f).translationX(0f).setDuration(120).start();
            binding.totalDurationText.animate().alpha(1f).translationX(0f).setDuration(120).start();
        }, 120);
        if (!isPlaying) binding.toggleView.startAnimation();
        isPlaying = true;
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            binding.miniPlayerBottomSheet.setProgress(1f);
        } else {
            binding.miniPlayerBottomSheet.setProgress(0f);
        }
        bgHandler.postDelayed(() -> {
            progressAllowed = true;
        }, 250);
	}
	
	private final BroadcastReceiver multiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
            if ("PLAYER_PROGRESS".equals(action) && intent.hasExtra("progress") && progressAllowed) {
                currentProgress = intent.getIntExtra("progress", 0);
                if (currentProgress < binding.songSeekbar.getValueTo() && seekbarFree) {
                    binding.musicProgress.setProgressCompat(currentProgress, true);
                    binding.songSeekbar.setValue(currentProgress);
                    binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(currentProgress));
                }
            }
            if ("PLAYER_COLORS".equals(action)) {
                Log.i("PlayerColors", "Received broadcast from the service");
                handler.post(() -> {
                    Log.i("PlayerColors", "update colors started");
                    updateColors();
                    Log.i("PlayerColors", "colors updates executed");
                });
            }
		}
	};
    
    private final BroadcastReceiver focusReceiver = new BroadcastReceiver() {
        @Override
		public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				Intent stopIntent = new Intent(c, PlayerService.class);
				stopIntent.setAction("ACTION_PAUSE");
				startService(stopIntent);
			}
        }
    };
	
    public void updateSongs(ArrayList<HashMap<String, Object>> s) {
	    currentMap = s;
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction("ACTION_UPDATE");
        startService(playIntent);
    }
        
    public void HideBNV(boolean hide) {
        Interpolator interpolator = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
        if (hide) {
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(300).setInterpolator(interpolator).withEndAction(() -> binding.bottomMixer.removeView(binding.bottomNavigation)).start();
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
            binding.miniPlayerBottomSheet.animate().translationY(bst).setDuration(300).setInterpolator(interpolator).start();
        } else {
            int extraInt = XUtils.convertToPx(c, 25);
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) { 
                binding.miniPlayerBottomSheet.animate().translationY(0).setDuration(300).setInterpolator(interpolator).start();
            }
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(300).setInterpolator(interpolator).withStartAction(() -> binding.bottomMixer.addView(binding.bottomNavigation)).start();
        }
        isbnvHidden = hide;
    }
    
    public boolean isBNVHidden() {
        return isbnvHidden;
    }
    
    public void Start() {
        isDataLoaded = true;
        ViewCompat.setWindowInsetsAnimationCallback(binding.bottomNavigation, new WindowInsetsAnimationCompat.Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override
            public WindowInsetsCompat onProgress(WindowInsetsCompat insets, List<WindowInsetsAnimationCompat> runningAnimations) {
                int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                binding.bottomNavigation.setTranslationY(imeBottom < 0 ? 0 : -imeBottom);
                return insets;
            }
        });
    }
    
    public ArrayList<HashMap<String, Object>> getSongsMap() {
        return SongsMap;
    }
    
	public void updateColors() {
        if (ColorPaletteUtils.lightColors == null && ColorPaletteUtils.darkColors == null) return;
        
        Map<String, Integer> colors = XUtils.isDarkMode(c) ? ColorPaletteUtils.darkColors : ColorPaletteUtils.lightColors;

        playerSurface = colors.get("surface");
        
        Drawable d = binding.extendableLayout.getBackground();
        GradientDrawable gd = (GradientDrawable) d;
        gd.setColor(colors.get("surfaceContainer"));
        
        GradientDrawable d2 = (GradientDrawable) binding.dragHandle.getBackground();
        d2.setColor(colors.get("outline"));
        
        binding.placeholder1.setIconTint(ColorStateList.valueOf(colors.get("onSurfaceContainer")));

        binding.toggleView.setShapeColor(colors.get("onPrimary"));
        binding.toggleView.setIconColor(colors.get("primary"));

        Drawable nextBg = binding.nextButton.getBackground();
        Drawable favBg = binding.favoriteButton.getBackground();
        Drawable saveBg = binding.saveButton.getBackground();
        Drawable prevBg = binding.previousButton.getBackground();

        int onTertiary = colors.get("onTertiary");
        int tertiary = colors.get("tertiary");

        nextBg.setColorFilter(onTertiary, PorterDuff.Mode.SRC_IN);
        favBg.setColorFilter(onTertiary, PorterDuff.Mode.SRC_IN);
        saveBg.setColorFilter(onTertiary, PorterDuff.Mode.SRC_IN);
        prevBg.setColorFilter(onTertiary, PorterDuff.Mode.SRC_IN);

        binding.nextButton.setColorFilter(tertiary, PorterDuff.Mode.SRC_IN);
        binding.favoriteButton.setColorFilter(tertiary, PorterDuff.Mode.SRC_IN);
        binding.saveButton.setColorFilter(tertiary, PorterDuff.Mode.SRC_IN);
        binding.previousButton.setColorFilter(tertiary, PorterDuff.Mode.SRC_IN);

        Slider slider = binding.songSeekbar;
        slider.setTrackInactiveTintList(ColorStateList.valueOf(colors.get("outline")));
        slider.setTrackActiveTintList(ColorStateList.valueOf(colors.get("primary")));
        slider.setThumbTintList(ColorStateList.valueOf(colors.get("primary")));
        slider.setHaloTintList(ColorStateList.valueOf(Color.TRANSPARENT));

        binding.artistBigTitle.setTextColor(colors.get("onSurfaceContainer"));
        binding.songBigTitle.setTextColor(colors.get("onSurface"));
        binding.currentDurationText.setTextColor(colors.get("onSurfaceContainer"));
        binding.totalDurationText.setTextColor(colors.get("onSurfaceContainer"));
    
        GradientDrawable background = (GradientDrawable) binding.miniPlayerBottomSheet.getBackground();
        tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);
        background.setColor(tmpColor);
    }
    
    public void updateTexts(int pos) {
        if (currentMap.size() > 0 && mediaController != null) {
            int p =  mediaController.getCurrentMediaItemIndex();
            binding.totalDurationText.setText(currentMap.get(pos == -1? p : pos).get("duration").toString());
            binding.artistBigTitle.setText(currentMap.get(pos == -1? p : pos).get("author").toString());
            binding.songBigTitle.setText(currentMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongTitle.setText(currentMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongArtist.setText(currentMap.get(pos == -1? p : pos).get("author").toString());
        } else if (isRestoring || PlayerService.isPlaying) {
            int p =  currentPosition;
            binding.totalDurationText.setText(currentMap.get(pos == -1? p : pos).get("duration").toString());
            binding.artistBigTitle.setText(currentMap.get(pos == -1? p : pos).get("author").toString());
            binding.songBigTitle.setText(currentMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongTitle.setText(currentMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongArtist.setText(currentMap.get(pos == -1? p : pos).get("author").toString());
            isRestoring = false;
        }
    }
    
    public void updateMaxValue(int pos) {
        if (currentMap.size() > 0 && mediaController != null) {
            int p = mediaController.getCurrentMediaItemIndex();
            int max = Integer.parseInt(currentMap.get(pos == -1? p : pos).get("total").toString());
            binding.songSeekbar.setValueTo(max);
            binding.musicProgress.setMax(max);
        } else if (isRestoring || PlayerService.isPlaying) {
            int p = currentPosition;
            int max = Integer.parseInt(currentMap.get(pos == -1? p : pos).get("total").toString());
            binding.songSeekbar.setValueTo(max);
            binding.musicProgress.setMax(max);
            isRestoring = false;
        }
    }

    public void setupCallbacks() {
        callback2 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {

            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                binding.secondCoordinator.setScaleX(1f-0.1f*backEvent.getProgress());
                binding.secondCoordinator.setScaleY(1f-0.1f*backEvent.getProgress());
                binding.secondCoordinator.setTranslationY((binding.secondCoordinator.getHeight()*0.05f)*backEvent.getProgress());
            }

            @Override
            public void handleOnBackPressed() {
                binding.miniPlayerDetailsLayout.setAlpha(0);
                binding.secondCoordinator.animate().scaleX(1f).setDuration(200).start();
                binding.secondCoordinator.animate().scaleY(1f).setDuration(200).start();
                binding.secondCoordinator.animate().translationY(0f).setDuration(200).start();
                innerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                callback2.setEnabled(false);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.secondCoordinator.animate().scaleX(1f).setDuration(300).start();
                binding.secondCoordinator.animate().translationY(0f).setDuration(200).start();
                binding.secondCoordinator.animate().scaleY(1f).setDuration(300).start();
                binding.miniPlayerDetailsLayout.animate().alpha(1).setDuration(200).start();
                innerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback2);
        
        callback = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                binding.miniPlayerBottomSheet.setTranslationY((XUtils.convertToPx(c, 1000f)*0.05f)*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setScaleX(1f-0.1f*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setScaleY(1f-0.1f*backEvent.getProgress());
                
            }

            @Override
            public void handleOnBackPressed() {
                binding.miniPlayerBottomSheet.setTransition(R.id.start_mid);
                binding.miniPlayerBottomSheet.animate().scaleX(1f).setDuration(200).start();
                binding.miniPlayerBottomSheet.animate().scaleY(1f).setDuration(200).start();
                binding.miniPlayerBottomSheet.animate().translationY(0f).setDuration(200).start();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                callback.setEnabled(false);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.miniPlayerBottomSheet.animate().scaleX(1f).setDuration(300).start();
                binding.miniPlayerBottomSheet.animate().scaleY(1f).setDuration(300).start();
                //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupReceivers(boolean initial) {
        if (initial) {
            IntentFilter focusFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(focusReceiver, focusFilter, Context.RECEIVER_EXPORTED);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAYER_PROGRESS");
        filter.addAction("PLAYER_COLORS");
        registerReceiver(multiReceiver, filter, Context.RECEIVER_EXPORTED);
    }
    
    public void showInfoDialog(String title, int icon, String Desc, String button) {
        MaterialAlertDialogBuilder m = new MaterialAlertDialogBuilder(this);
        m.setTitle(title);
        if (icon != 0) m.setIcon(icon);
        m.setMessage(Desc);
        m.setPositiveButton(button, (dialog, which) -> {
            dialog.dismiss();
        });
        m.show();
    }

    @Override
    public void onData(String data) {
        if (data.equals("update")) {
            updateTexts(-1);
            updateMaxValue(-1);
            updateColors();
        } else if (data.equals("update-cover")) {
            updateColors();
            if (mlfa != null) mlfa.adjustUI();
            currentPosition = PlayerService.currentPosition;
            updateTexts(-1);
            updateMaxValue(-1);
            updateCoverPager(currentPosition);
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                binding.miniPlayerBottomSheet.setProgress(1f);
            } else {
                binding.miniPlayerBottomSheet.setProgress(0f);
            }
        }
    }
    
    public void showSnackbar(@NonNull View parent, String text, String btntext, @Nullable View.OnClickListener actionListener) {
        Snackbar snack = Snackbar.make(parent, "", Snackbar.LENGTH_LONG);
        View defaultText = snack.getView().findViewById(
            com.google.android.material.R.id.snackbar_text
        );
        if (defaultText != null) defaultText.setVisibility(View.INVISIBLE);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View custom = inflater.inflate(R.layout.custom_snackbar, null);
        
        TextView t = custom.findViewById(R.id.text);
        t.setText(text);

        Button btn = custom.findViewById(R.id.button);
        btn.setText(btntext);
        if (actionListener != null) {
            btn.setOnClickListener(actionListener);
        } else {
            btn.setOnClickListener(v -> {
                snack.dismiss();
            });
        }
        
        Snackbar.SnackbarLayout snackLayout = (Snackbar.SnackbarLayout) snack.getView();
        snackLayout.setBackground(null);
        snackLayout.setPadding(0, 0, 0, 0);
        snackLayout.addView(custom, 0);

        snack.show();
    }
    
    private void updateCoverPager(int position) {
        wasAdjusted = true;
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
        binding.coversPager.setCurrentItem(position, true);
        else 
        binding.coversPager.setCurrentItem(position, false);
    }

    private void saveUIState() {
        viewmodel.markDataAsSaved(true);
        viewmodel.setBNVAsHidden(isBNVHidden());
        viewmodel.setLastPosition(currentPosition);
        viewmodel.saveSongsMap(currentMap); 
    }

    private boolean isRestoring;

    private void restoreStateIfPossible() {
        if (viewmodel.isDataSaved()) {
            isRestoring = true;
        
            try {
                currentMap = viewmodel.loadSongsMap(); 
                int oldPosition = viewmodel.loadLastPosition();
                currentPosition = oldPosition;
                isPlaying = true;
                progressAllowed = true;
                if (!currentMap.isEmpty()) {
                    updateTexts(-1);
                    isRestoring = true;
                    updateMaxValue(-1);
                    updateColors();
                }
            } catch (Exception e) {

            }
            if (PlayerService.isPlaying) {
                handler.postDelayed(() -> {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }, 500);
            }
        
            //if (viewmodel.wasBNVHidden()) HideBNV(true);
        }
        
    }



    public void setupCallbacksAndEvents() {
        binding.toggleView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (!binding.toggleView.isAnimating()) {
                    isPlaying = false; 
                    mediaController.pause();
				} else {
                    isPlaying = true;
                    mediaController.play();
				}
			}
		});
        
        binding.miniPlayerDetailsLayout.setOnClickListener(v -> {
            binding.musicProgress.setAlpha(0f);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        
        binding.favoriteButton.setOnClickListener(v -> {
            
        });
        
        binding.saveButton.setOnClickListener(v -> {
            
        });

        View.OnClickListener navClick = v -> {
            if (System.currentTimeMillis() - lastClick < 150) return;
            lastClick = System.currentTimeMillis();
            int resId = R.drawable.placeholder;
            String placeholder = "android.resource://" + getPackageName() + "/" + resId;
            currentPosition = PlayerService.currentPosition;
            currentPosition += (v == binding.nextButton ? 1 : -1);
            HashMap<String, Object> song = SongsMap.get(currentPosition);
            _setSong(currentPosition, song.get("thumbnail") == null? placeholder : song.get("thumbnail").toString(), Uri.parse("file://" + song.get("path").toString()));
        };
        
        binding.coversPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            
            @Override
            public void onPageScrollStateChanged(int i) {
            }
            
            @Override
            public void onPageScrolled(int i, float f, int i2) {
                wasAdjusted = false;
            }
        
            @Override
            public void onPageSelected(int i) {
                if (i != currentPosition && !wasAdjusted) {
                    int resId = R.drawable.placeholder;
                    String placeholder = "android.resource://" + getPackageName() + "/" + resId;
                    HashMap<String, Object> song = SongsMap.get(i);
                    _setSong(i, song.get("thumbnail") == null? placeholder : song.get("thumbnail").toString(), Uri.parse("file://" + song.get("path").toString()));
                }
            }
        });

        binding.nextButton.setOnClickListener(navClick);
        binding.previousButton.setOnClickListener(navClick);
        
        binding.songSeekbar.addOnChangeListener((slider, progress, isManual) -> {
            if (isManual) {
                binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration((int)progress));
            }
        });
            
		binding.songSeekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
			@Override
		    public void onStartTrackingTouch(Slider slider) {
				seekbarFree = false;
			}
			@Override
			public void onStopTrackingTouch(Slider slider) {
				bgHandler.postDelayed(() -> {
					seekbarFree = true;
				}, 101);
                Intent playIntent = new Intent(c, PlayerService.class);
				playIntent.setAction("ACTION_SEEK");
                playIntent.putExtra("progress", (int)binding.songSeekbar.getValue());
				startService(playIntent);
			}
		});
        
        setupCallbacks();
        
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    binding.miniPlayerBottomSheet.setTransition(R.id.start_mid);
                    innerBottomSheetBehavior.setDraggable(false);
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                     innerBottomSheetBehavior.setDraggable(true);
                     callback.setEnabled(true);
                     binding.miniPlayerBottomSheet.setTransition(R.id.mid_end);
                     binding.miniPlayerBottomSheet.animate().translationY(0).setDuration(10).start();
				} else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    innerBottomSheetBehavior.setDraggable(false);
                    if (isBNVHidden()) {
                        binding.miniPlayerBottomSheet.animate().translationY(bst).setDuration(100).start();
                    }
                    callback.setEnabled(false);
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						Intent playIntent = new Intent(c, PlayerService.class);
						playIntent.setAction("ACTION_STOP");
						startService(playIntent);
                        Intent playIntent2 = new Intent("ACTION_STOP_FRAGMENT");
						sendBroadcast(playIntent2);
						isBsInvisible = true;
				    } else {
						isBsInvisible = false;
					}
					binding.musicProgress.animate().alpha(1f).setDuration(100).start();
			    } else {
                    
                    innerBottomSheetBehavior.setDraggable(false);
					isBsInvisible = false;
				}
			}
				
			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                currentSlideOffset = slideOffset;
				if (0f < slideOffset) {
                    if (isBNVHidden()) {
                        binding.miniPlayerBottomSheet.setTranslationY(bst - bst*slideOffset);
                    }
				    binding.fragmentsContainer.setTranslationY(-XUtils.convertToPx(c, 75f)*slideOffset);
				    binding.Scrim.setAlpha(slideOffset*0.8f);
					binding.miniPlayerBottomSheet.setProgress(slideOffset);
                    if (!isBNVHidden()) binding.bottomNavigation.setTranslationY(binding.bottomNavigation.getHeight()*slideOffset*2.5f);
					if (slideOffset <= 0.05f) {
						binding.miniPlayerDetailsLayout.setAlpha(1f - slideOffset*20);
						if (isAnimated) {
							isAnimated = false;
                        }
                    } else {
                        if (!isAnimated) {
							binding.miniPlayerDetailsLayout.animate().alpha(0f).setDuration(80).start();
							isAnimated = true;
						}
					}
					if (slideOffset >= 0.5f) {
						IsColorAnimated = false;
						Drawable background = binding.miniPlayerBottomSheet.getBackground();
					    tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, slideOffset*2 - 1f);
						((GradientDrawable) background).setColor(tmpColor);
						binding.songSeekbar.setEnabled(true);
					} else {
						if (!IsColorAnimated) {
							IsColorAnimated = true;
							XUtils.animateColor(tmpColor, bottomSheetColor, animation -> {
								int animatedColor = (int) animation.getAnimatedValue();
								Drawable background = binding.miniPlayerBottomSheet.getBackground();
								((GradientDrawable) background).setColor(animatedColor);
						    
                            });
                        }
                        binding.songSeekbar.setEnabled(false);
				    }
				} else {
                    XUtils.animateColor(tmpColor, bottomSheetColor, animation -> {
						int animatedColor = (int) animation.getAnimatedValue();
						Drawable background = binding.miniPlayerBottomSheet.getBackground();
						((GradientDrawable) background).setColor(animatedColor);
						    
                    });
                }
			}
		});
        
        innerBottomSheetBehavior = BottomSheetBehavior.from(binding.extendableLayout);
        innerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        innerBottomSheetBehavior.setDraggable(true);
        int cm = XUtils.getMargin(binding.coversPager, "top");
        
        innerBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState != BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setDraggable(false);
                    callback.setEnabled(false);
                    callback2.setEnabled(true);
                } else {
                    bottomSheetBehavior.setDraggable(true);
                    callback.setEnabled(true);
                    callback2.setEnabled(false);
                }
                
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.miniPlayerDetailsLayout.animate().alpha(1).setDuration(200).start();
                } else {
                    binding.miniPlayerDetailsLayout.animate().alpha(0).setDuration(200).start();
                }
            }
            
            @Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float prog = 1f - slideOffset;
                binding.extendableLayout.setTranslationY((binding.coversPager.getHeight() + XUtils.convertToPx(c, 16f)*2)*slideOffset);
                binding.miniPlayerBottomSheet.post(() ->
                    binding.miniPlayerBottomSheet.setProgress(1f - slideOffset)
                );
                /* if (slideOffset >= 0.8f) {
                    binding.miniPlayerDetailsLayout.setAlpha(1f - (1f-slideOffset)*5); 
                }/* else if (binding.miniPlayerDetailsLayout.getAlpha() > 0 && slideOffset < 0.8f) {
                    binding.miniPlayerDetailsLayout.setAlpha(0f);
                }*/
            }
        });
    }
        
    public void applyDynamicColors() {
        if (DataManager.isDynamicColorsOn() && Build.VERSION.SDK_INT >= 31) {
            if (DataManager.isCustomColorsOn()) {
                DynamicColorsOptions.Builder options = new DynamicColorsOptions.Builder();
                options.setContentBasedSource(DataManager.getCustomColor());
                options.setThemeOverlay(R.style.AppTheme);
                DynamicColors.applyToActivityIfAvailable(this, options.build());
            } else {
                DynamicColors.applyToActivityIfAvailable(this);
            }
        }
    }
    
    public void updateTheme() {
        switch(DataManager.getThemeMode()) {
            case 0:
                XUtils.setThemeMode("auto");
            break;
            case 1:
                XUtils.setThemeMode("dark");
            break;
            case 2:
                XUtils.setThemeMode("light");
            break;
        }
    }
    
    public void loadSongs() {
        executor.execute(() -> {
            SongMetadataHelper.getAllSongs(c, new SongLoadListener(){
                @Override
                public void onProgress(java.util.ArrayList<HashMap<String, Object>> songs, int count) {
                    
                }
                
                @Override 
                public void onComplete(java.util.ArrayList<HashMap<String, Object>> songs) {
                    SongsMap = songs;
                    updateSongs(SongsMap);
                    CustomPagerAdapter cpa = new CustomPagerAdapter(c, songs);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (songs.size() > 0) {
                            wasAdjusted = true;
                            binding.coversPager.setAdapter(cpa);
                            if (PlayerService.isPlaying && !songs.isEmpty()) {
                                updateCoverPager(PlayerService.currentPosition);
                                binding.toggleView.startAnimation();
                                currentPosition = PlayerService.getCurrentPos();
                                progressAllowed = true;
                                updateMaxValue(-1);
                                updateTexts(-1);
                                updateColors();
                                handler.postDelayed(() -> {
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                }, 500);
                            }    
                        } else {
                            XUtils.showMessage(c, "no songs found");
                        } 
                    });
                }
            });
        });
    }

}