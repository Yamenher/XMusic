package com.xapps.media.xmusic.activity;

import android.Manifest;
import android.animation.*;
import android.content.*;
import android.content.pm.PackageManager; 
import android.content.res.*;
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
import android.webkit.*;
import android.widget.*;

import androidx.activity.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.resources.*;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.splashscreen.*;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.emoji2.*;
import androidx.fragment.app.*;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.process.*;
import androidx.lifecycle.runtime.*;
import androidx.lifecycle.viewmodel.*;
import androidx.lifecycle.viewmodel.savedstate.*;
import androidx.media.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;
import androidx.palette.*;
import androidx.profileinstaller.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.savedstate.*;
import androidx.startup.*;
import androidx.transition.*;

import com.appbroker.roundedimageview.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import com.google.android.material.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.adapter.CustomPagerAdapter;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.databinding.MainBinding;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.*;
import com.xapps.media.xmusic.fragment.MusicListFragment;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private MainBinding binding;
    private final Context c = this;
	public float targetMargin, bsh;
    private boolean receiveProgress = false;
	private boolean isAnimated = false;
    private boolean isBsInvisible = true;
    private boolean isRun = false;
    private boolean seekbarFree = true;
    private boolean IsColorAnimated = false;
    public boolean isbnvHidden, isDataLoaded = false;
    private int playbackState, playerSurface, bottomSheetColor, tmpColor , currentPosition;
    public int navBarHeight;
    private long lastClick = 0;
	public ExoPlayer player;
	public BottomSheetBehavior bottomSheetBehavior;
	private Handler handler = new Handler(Looper.getMainLooper());
	private Runnable updateProgressRunnable;
	private MediaSessionCompat mediaSession;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private float currentSlideOffset;
    public boolean isPlaying = false;
    private Handler actionSender = new Handler(Looper.getMainLooper());
    private boolean seekAllowed = true;
    private boolean progressAllowed = false;
    private MusicListFragment mlfa; 
    private OnBackPressedCallback callback;
    
    private HandlerThread handlerThread = new HandlerThread("BackgroundThread");
    private Handler bgHandler;
	
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
	public static ArrayList<HashMap<String, Object>> currentMap = new ArrayList<>();
    
    private NavHostFragment navHostFragment;
    private NavController navController;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
        handlerThread.start();
        Looper l = handlerThread.getLooper();
        bgHandler = new Handler(l);
        setupReceivers(true);
		EdgeToEdge.enable(this);
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
        setupCallbacks();
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer).getChildFragmentManager().getFragments().get(0);
        if (f instanceof MusicListFragment) {
            mlfa = (MusicListFragment) f;
        }
        executor.execute(() -> {
            SongMetadataHelper.getAllSongs(c, new SongLoadListener(){
                @Override
                public void onProgress(int count) {
                    
                }
                
                @Override 
                public void onComplete(java.util.ArrayList<HashMap<String, Object>> songs) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        SongsMap = songs;
                        updateSongs(SongsMap);
                        if (songs.size() > 0) {
                            CustomPagerAdapter cpa = new CustomPagerAdapter(c, songs);
                            binding.coversPager.setAdapter(cpa);
                        } else {
                            XUtils.showMessage(c, "no songs found");
                        } 
                    });
                }
            });
        });
	}
	
	private void initialize(Bundle _savedInstanceState) {
        MaterialColorUtils.initColors(this);
        binding.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners_bottom_sheet));
		binding.toggleView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (!binding.toggleView.isAnimating()) {
					Intent playIntent = new Intent(c, PlayerService.class);
					playIntent.setAction("ACTION_PAUSE");
                    isPlaying = false; 
					startService(playIntent);
				} else {
					Intent playIntent = new Intent(c, PlayerService.class);
					playIntent.setAction("ACTION_RESUME");
                    isPlaying = true;
					startService(playIntent);
				}
			}
		});
        
        binding.miniPlayerDetailsLayout.setOnClickListener(v -> {
            MusicListFragment.fab.hide();
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
            currentPosition += (v == binding.nextButton ? 1 : -1);
            HashMap<String, Object> song = SongsMap.get(currentPosition);
            _setSong(currentPosition, song.get("thumbnail") == null? placeholder : song.get("thumbnail").toString(), Uri.parse("file://" + song.get("path").toString()));
        };

        binding.nextButton.setOnClickListener(navClick);
        binding.previousButton.setOnClickListener(navClick);
        
    }    
   
	
	private void initializeLogic() {
        binding.coversPager.setUserInputEnabled(false);
		bottomSheetColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer, 0xff000000);
		int resourceId = c.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            
		if (resourceId > 0) {
            navBarHeight = c.getResources().getDimensionPixelSize(resourceId);
		}
            
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
        
		binding.songBigTitle.setSelected(true);
		binding.artistBigTitle.setSelected(true);
		binding.currentSongTitle.setSelected(true);
		binding.currentSongArtist.setSelected(true);
		bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
					if (!isBNVHidden()) MusicListFragment.fab.hide();
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                     callback.setEnabled(true);
				} else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    callback.setEnabled(false);
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						Intent playIntent = new Intent(c, PlayerService.class);
						playIntent.setAction("ACTION_STOP");
						startService(playIntent);
                        Intent playIntent2 = new Intent("ACTION_STOP_FRAGMENT");
						sendBroadcast(playIntent2);
						isBsInvisible = true;
                        XUtils.increaseMargins(MusicListFragment.fab, 0, 0, 0, -(binding.coversPager.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2));
				    } else {
						isBsInvisible = false;
					}
					if (!isBNVHidden()) MusicListFragment.fab.show();
					binding.musicProgress.animate().alpha(1f).setDuration(100).start();
			    } else {
					isBsInvisible = false;
				}
			}
				
			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                currentSlideOffset = slideOffset;
				if (0f < slideOffset) {
				    binding.fragmentsContainer.setTranslationY(-XUtils.convertToPx(c, 75f)*slideOffset);
				    binding.Scrim.setAlpha(slideOffset*0.7f);
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
    
    public void addFragmentWithTransition(androidx.fragment.app.Fragment fragment) {
        navController.navigate(R.id.action_open_settings);
    }
	
	public MainBinding getBinding() {
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
            navController = navHostFragment.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.homeFragment) {
                mlfa.adjustUI();
            }
        });
		return binding;
	}
	
	@Override
	public void onResume() {
	    super.onResume();
        currentPosition = PlayerService.currentPosition;
        updateColors();
        updateTexts(-1);
        updateMaxValue(-1);
        try {
            binding.musicProgress.setProgress(PlayerService.lastProgress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setupReceivers(false);
    }
	
	@Override
	public void onPause() {
		super.onPause();
        unregisterReceiver(multiReceiver);
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        unregisterReceiver(focusReceiver);
    }
        
	public void _setSong(final int _position, final String _coverPath, final Uri _fileUri) {
        progressAllowed = false;
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
        binding.coversPager.setCurrentItem(_position, false);
        binding.coversPager.setAlpha(0f);
        binding.coversPager.animate().alpha(1f).setDuration(300).start();
        currentPosition = _position;
        binding.musicProgress.setProgressCompat(0, true);
        binding.songSeekbar.setValue(0);
        if (isBsInvisible) {
			XUtils.increaseMarginsSmoothly(MusicListFragment.fab, 0, 0, 0, (binding.coversPager.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2), 200L);
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
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            binding.miniPlayerBottomSheet.setProgress(0f);
        } else {
            binding.miniPlayerBottomSheet.setProgress(1f);
        }
        bgHandler.postDelayed(() -> {
            progressAllowed = true;
        }, 250);
        
	}
	
	private final BroadcastReceiver multiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
            if (progressAllowed) {
			    if ("PLAYER_PROGRESS".equals(action) && intent.hasExtra("progress")) {
				    int currentPosition = intent.getIntExtra("progress", 0);
					    if (currentPosition < binding.songSeekbar.getValueTo() && seekbarFree) {
						    binding.musicProgress.setProgressCompat(currentPosition, true);
						    binding.songSeekbar.setValue(currentPosition);
                            binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(currentPosition));
					    }
			    } else if ("PLAYER_COLORS".equals(action)) {
				    updateColors();
			    }
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
        if (hide) {
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(100).withEndAction(() -> binding.bottomMixer.removeView(binding.bottomNavigation)).start();
            bottomSheetBehavior.setPeekHeight(0, true);
            MusicListFragment.fab.hide();
        } else {
            int extraInt = XUtils.convertToPx(c, 25);
            bottomSheetBehavior.setPeekHeight(Math.round(bsh + binding.bottomNavigation.getPaddingBottom()), true);
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(100).withStartAction(() -> binding.bottomMixer.addView(binding.bottomNavigation)).start();
            if (SongsMap.size() != 0) {
                MusicListFragment.fab.show();
            }
        }
        isbnvHidden = hide;
    }
    
    public boolean isBNVHidden() {
        return isbnvHidden;
    }
    
    public void Start() {
        isDataLoaded = true;
    }
    
    public ArrayList<HashMap<String, Object>> getSongsMap() {
        return SongsMap;
    }
    
	public void updateColors() {
        if (ColorPaletteUtils.lightColors != null || ColorPaletteUtils.darkColors != null) {
            binding.toggleView.setShapeColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onPrimary") : ColorPaletteUtils.lightColors.get("onPrimary"));
		    playerSurface = XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("surface") : ColorPaletteUtils.lightColors.get("surface");
		    binding.toggleView.setIconColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary"));
            binding.nextButton.getBackground().setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
            binding.favoriteButton.getBackground().setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
            binding.saveButton.getBackground().setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
            binding.previousButton.getBackground().setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
            binding.nextButton.setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
            binding.favoriteButton.setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
            binding.saveButton.setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
            binding.previousButton.setColorFilter(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
            Slider slider = binding.songSeekbar;
            slider.setTrackInactiveTintList(ColorStateList.valueOf(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("outline") : ColorPaletteUtils.lightColors.get("outline")));
            slider.setTrackActiveTintList(ColorStateList.valueOf(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary")));
            slider.setThumbTintList(ColorStateList.valueOf(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary")));
            slider.setHaloTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.artistBigTitle.setTextColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
            binding.songBigTitle.setTextColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurface") : ColorPaletteUtils.lightColors.get("onSurface"));
            binding.currentDurationText.setTextColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
            binding.totalDurationText.setTextColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
            Drawable background = binding.miniPlayerBottomSheet.getBackground();
		    tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);
		    ((GradientDrawable) background).setColor(tmpColor);
        }
    }
    
    public void updateTexts(int pos) {
        if (currentMap.size() > 0) {
            binding.totalDurationText.setText(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("duration").toString());
            binding.artistBigTitle.setText(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("author").toString());
            binding.songBigTitle.setText(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("title").toString());
            binding.currentSongTitle.setText(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("title").toString());
            binding.currentSongArtist.setText(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("author").toString());
        }
    }
    
    public void updateMaxValue(int pos) {
        if (currentMap.size() > 0) {
            int max = Integer.parseInt(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("total").toString());
            binding.songSeekbar.setValueTo(max);
            binding.musicProgress.setMax(max);
        }
    }

    public void setupCallbacks() {
        callback = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                binding.miniPlayerBottomSheet.setScaleX(1f-0.1f*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setScaleY(1f-0.1f*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setTranslationY((binding.miniPlayerBottomSheet.getHeight()*0.05f)*backEvent.getProgress());
                
            }

            @Override
            public void handleOnBackPressed() {
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
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
}