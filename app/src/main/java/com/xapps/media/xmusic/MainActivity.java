package com.xapps.media.xmusic;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.*;
import android.widget.*;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.resources.*;
import androidx.core.splashscreen.*;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.emoji2.*;
import androidx.fragment.app.*;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
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
import com.bumptech.glide.Glide;
import com.google.android.material.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xapps.media.xmusic.databinding.MainBinding;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.core.splashscreen.SplashScreen;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import android.net.Uri;
import androidx.media3.common.Player;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import java.lang.reflect.Field;
import com.xapps.media.xmusic.XUtils;
import androidx.constraintlayout.motion.widget.MotionLayout;
import com.xapps.media.xmusic.widget.PlayerToggle;
import com.google.android.material.slider.Slider;
import androidx.core.view.WindowCompat;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import androidx.media3.common.*;
import androidx.media3.common.util.Util;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

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
    private int playbackState, playerSurface, bottomSheetColor, tmpColor, navBarHeight, currentPosition;
	public ExoPlayer player;
	public BottomSheetBehavior bottomSheetBehavior;
	private Handler handler, seekController = new Handler(Looper.getMainLooper());
	private Runnable updateProgressRunnable;
	private MediaSessionCompat mediaSession;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private float currentSlideOffset;
    private boolean isPlaying = false;
    private Handler actionSender = new Handler(Looper.getMainLooper());
    private boolean seekAllowed = true;
	
	public static ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
	private ArrayList<String> SongsList = new ArrayList<>();
	public static ArrayList<HashMap<String, Object>> currentMap = new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
        executor.execute(() -> {
            SongsMap = SongMetadataHelper.getAllSongs(c);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		    final SplashScreen splash = SplashScreen.installSplashScreen(this);
            splash.setKeepOnScreenCondition(() -> {
                return !isDataLoaded;
            });
        }
		WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
        IntentFilter filter = new IntentFilter();
		filter.addAction("PLAYER_PROGRESS");
		filter.addAction("PLAYER_COLORS");
		registerReceiver(multiReceiver, filter, Context.RECEIVER_EXPORTED);
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
            binding.Fab.hide();
            binding.musicProgress.setAlpha(0f);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        
        binding.favoriteButton.setOnClickListener(v -> {
            
        });
        
        binding.saveButton.setOnClickListener(v -> {
            
        });
        
        binding.nextButton.setOnClickListener(v -> {
            if (seekAllowed) {
                currentPosition++;
                _setSong(currentPosition, SongsMap.get(currentPosition).get("thumbnail").toString(), Uri.parse("file://"+SongsMap.get(currentPosition).get("path").toString()));
                seekAllowed = false;
                seekController.postDelayed(() -> {
                    seekAllowed = true;
                }, 150);
            }
        });
        binding.previousButton.setOnClickListener(v -> {
            if (seekAllowed) {
                currentPosition--;
                _setSong(currentPosition, SongsMap.get(currentPosition).get("thumbnail").toString(), Uri.parse("file://"+SongsMap.get(currentPosition).get("path").toString()));
                seekAllowed = false;
                seekController.postDelayed(() -> {
                    seekAllowed = true;
                }, 150);
            }
        });
    }
   
	
	private void initializeLogic() {
		bottomSheetColor = extractColor(binding.miniPlayerBottomSheet);
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
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
		addFragment("com.xapps.media.xmusic.MusicListFragmentActivity");
		bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
		bottomSheetBehavior.setHideable(true);
        bsh = bottomSheetBehavior.getPeekHeight();
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomNavigation.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isRun) {
                targetMargin = binding.bottomNavigation.getHeight() ;
			    XUtils.increaseMargins(binding.musicProgress, 0, 0, 0, navBarHeight);
			    bottomSheetBehavior.setPeekHeight(bottomSheetBehavior.getPeekHeight() + navBarHeight);
			    int bottomHeight = binding.imageContainer.getHeight() + binding.bottomNavigation.getHeight() + binding.musicProgress.getHeight() + XUtils.getMargin(binding.musicProgress, "top");
			    XUtils.increaseMargins(binding.Fab, 0, 0, 0, (int)(navBarHeight*1.4f));
            }
            isRun = true;
        });
		bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
					if (!isBNVHidden()) binding.Fab.hide();
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
				} else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    
						Intent playIntent = new Intent(c, PlayerService.class);
						playIntent.setAction("ACTION_STOP");
						startService(playIntent);
                        Intent playIntent2 = new Intent("ACTION_STOP");
						sendBroadcast(playIntent2);
						isBsInvisible = true;
						XUtils.increaseMargins(binding.Fab, 0, 0, 0, -(binding.miniPlayerThumbnail.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2));
				    } else {
						isBsInvisible = false;
					}
						if (!isBNVHidden()) binding.Fab.show();
						binding.musicProgress.animate().alpha(1f).setDuration(100).start();
			    } else {
						isBsInvisible = false;
				}
			}
				
			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                currentSlideOffset = slideOffset;
				if (0f <= slideOffset) {
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
					    tmpColor = interpolateColor(bottomSheetColor, playerSurface, slideOffset*2 - 1f);
						((GradientDrawable) background).setColor(tmpColor);
						binding.songSeekbar.setEnabled(true);
					} else if (slideOffset <= 0.5f && slideOffset > 0f) {
						if (!IsColorAnimated) {
							IsColorAnimated = true;
							animateColor(tmpColor, bottomSheetColor, 100, animation -> {
								int animatedColor = (int) animation.getAnimatedValue();
								Drawable background = binding.miniPlayerBottomSheet.getBackground();
								((GradientDrawable) background).setColor(animatedColor);
						    
                            });
                        }
                        binding.songSeekbar.setEnabled(false);
				    }
				}
			}
		});
		
		if (Build.VERSION.SDK_INT >= 32) {
            requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_AUDIO}, 1);
		
		} else {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		
		}
		binding.miniPlayerBottomSheet.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
			public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
		    }
			@Override
			public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
				binding.miniPlayerThumbnail.setRadius(XUtils.convertToPx(c, 8f) + XUtils.convertToPx(c, 16f*progress));
			}
			@Override
			    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
			}
			@Override
				public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
			}
		});
		
	}
	
	public void addFragment(String fragmentName) {
		androidx.fragment.app.Fragment fragment = null;
		try {
		    fragment = (androidx.fragment.app.Fragment) Class.forName(fragmentName).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fragment != null) {
			androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			androidx.fragment.app.Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(fragmentName);
			if (existingFragment == null) {
			    com.google.android.material.transition.MaterialSharedAxis enterTransition = new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, true);
				com.google.android.material.transition.MaterialSharedAxis exitTransition = new com.google.android.material.transition.MaterialSharedAxis(com.google.android.material.transition.MaterialSharedAxis.X, false);
				fragment.setEnterTransition(enterTransition);
				fragment.setExitTransition(exitTransition);		
				transaction.add(R.id.fragmentsContainer, fragment, fragmentName);
				transaction.addToBackStack(null);
			} else {
				transaction.show(existingFragment);
			}		
                transaction.commit();
			}
	}
	
	public MainBinding getBinding() {
			return binding;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction("PLAYER_PROGRESS");
		filter.addAction("PLAYER_COLORS");
		filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		registerReceiver(multiReceiver, filter, Context.RECEIVER_EXPORTED);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(multiReceiver);
	}
        
	public void _setSong(final int _position, final String _coverPath, final Uri _fileUri) {
        currentPosition = _position;
        binding.musicProgress.setProgressCompat(0, true);
        binding.songSeekbar.setValue(0);
        if (isBsInvisible) {
			XUtils.increaseMarginsSmoothly(binding.Fab, 0, 0, 0, (binding.miniPlayerThumbnail.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2), 200L);
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
        MusicListFragmentActivity frag = (MusicListFragmentActivity) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        if (frag != null) {
            currentMap = frag.getSongsMap();
        }
        binding.artistBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.songBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        handler = new Handler(Looper.getMainLooper());   
        handler.postDelayed(() -> {
            binding.artistBigTitle.setTranslationX(20f);
            binding.songBigTitle.setTranslationX(20f);
            binding.totalDurationText.setText(currentMap.get(_position).get("duration").toString());
            binding.artistBigTitle.setText(currentMap.get(_position).get("author").toString());
            binding.songBigTitle.setText(currentMap.get(_position).get("title").toString());
            binding.currentSongTitle.setText(currentMap.get(_position).get("title").toString());
            binding.currentSongArtist.setText(currentMap.get(_position).get("author").toString());
        }, 110);
        handler.postDelayed(() -> {
            binding.artistBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
            binding.songBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
        }, 120);
        if (!isPlaying) binding.toggleView.startAnimation();
        isPlaying = true;    
        Glide.with(c)
        .load(Uri.parse("file://" + _coverPath))
        .apply(new RequestOptions()
        .centerCrop()
        .override(800, 800)
        .skipMemoryCache(true))
        .thumbnail(1f)    
        .transition(DrawableTransitionOptions.withCrossFade(300))
        .into(binding.miniPlayerThumbnail);
        int max = Integer.parseInt(currentMap.get(_position).get("total").toString());
        binding.songSeekbar.setValueTo(max);
        binding.musicProgress.setMax(max);
        handler.postDelayed(() -> {
            Intent playIntent = new Intent(this, PlayerService.class);
            playIntent.setAction("ACTION_PLAY");
            playIntent.putExtra("uri", _fileUri.toString());
            playIntent.putExtra("title", currentMap.get((int)_position).get("title").toString());
            playIntent.putExtra("artist", currentMap.get((int)_position).get("author").toString());
            playIntent.putExtra("cover", _coverPath);
            playIntent.putExtra("position", _position);
            startService(playIntent);
        }, 10);
	}
	
	private final BroadcastReceiver multiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ("PLAYER_PROGRESS".equals(action) && intent.hasExtra("progress")) {
				int currentPosition = intent.getIntExtra("progress", 0);
					if (currentPosition < binding.songSeekbar.getValueTo() && seekbarFree) {
						binding.musicProgress.setProgressCompat(currentPosition, true);
						binding.songSeekbar.setValue(currentPosition);
                        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(currentPosition));
					}
			} else if ("PLAYER_COLORS".equals(action)) {
				binding.toggleView.setShapeColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onPrimary") : ColorPaletteUtils.lightColors.get("onPrimary"));
				playerSurface = isDarkMode(c)? ColorPaletteUtils.darkColors.get("surface") : ColorPaletteUtils.lightColors.get("surface");
				binding.toggleView.setIconColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary"));
                binding.nextButton.getBackground().setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
                binding.favoriteButton.getBackground().setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
                binding.saveButton.getBackground().setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
                binding.previousButton.getBackground().setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onTertiary") : ColorPaletteUtils.lightColors.get("onTertiary"), PorterDuff.Mode.SRC_IN);
                binding.nextButton.setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
                binding.favoriteButton.setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
                binding.saveButton.setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
                binding.previousButton.setColorFilter(isDarkMode(c)? ColorPaletteUtils.darkColors.get("tertiary") : ColorPaletteUtils.lightColors.get("tertiary"), PorterDuff.Mode.SRC_IN);
                Slider slider = binding.songSeekbar;
                slider.setTrackInactiveTintList(ColorStateList.valueOf(isDarkMode(c)? ColorPaletteUtils.darkColors.get("outline") : ColorPaletteUtils.lightColors.get("outline")));
                slider.setTrackActiveTintList(ColorStateList.valueOf(isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary")));
                slider.setThumbTintList(ColorStateList.valueOf(isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("primary")));
                slider.setHaloTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.artistBigTitle.setTextColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
                binding.songBigTitle.setTextColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurface") : ColorPaletteUtils.lightColors.get("onSurface"));
                binding.currentDurationText.setTextColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
                binding.totalDurationText.setTextColor(isDarkMode(c)? ColorPaletteUtils.darkColors.get("onSurfaceContainer") : ColorPaletteUtils.lightColors.get("onSurfaceContainer"));
                Drawable background = binding.miniPlayerBottomSheet.getBackground();
				tmpColor = interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);
				((GradientDrawable) background).setColor(tmpColor);
			} else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
				Intent stopIntent = new Intent(c, PlayerService.class);
				stopIntent.setAction("ACTION_PAUSE");
				startService(stopIntent);
			}
		}
	};
	
	public static int interpolateColor(int startColor, int endColor, float percentage) {
			return (int) new android.animation.ArgbEvaluator().evaluate(percentage, startColor, endColor);
	}
    
    public static boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
	
	private static float clamp(float value, float min, float max) {
			return Math.max(min, Math.min(max, value));
	}
	
	public static Integer extractColor(View view) {
		Drawable background = view.getBackground();
		if (background instanceof GradientDrawable) {
			try {
				Field stateField = GradientDrawable.class.getDeclaredField("mFillPaint");
				stateField.setAccessible(true);
				Paint fillPaint = (Paint) stateField.get(background);
				return fillPaint.getColor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
			return null;
	}
	
	public void animateColor(int fromColor, int toColor, long duration, ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(fromColor, toColor);
		colorAnimator.setDuration(duration);
		colorAnimator.addUpdateListener(listener);
		colorAnimator.start();
	}
	
    public void updateSongs(ArrayList<HashMap<String, Object>> s) {
	    currentMap = s;
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction("ACTION_UPDATE");
        startService(playIntent);
    }
        
    public void HideBNV(boolean hide) {
        if (hide) {
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(180).withEndAction(() -> binding.bottomMixer.removeView(binding.bottomNavigation)).start();
            ValueAnimator animator = ValueAnimator.ofFloat(bsh, 0f);
		    animator.setDuration(300);
		    animator.addUpdateListener(animation -> {
			    int progress = Math.round((float) animation.getAnimatedValue());
                bottomSheetBehavior.setPeekHeight(progress);
            });
		    animator.start();
            binding.Fab.hide();
        } else {
            int extraInt = XUtils.convertToPx(c, 15);
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(350).withStartAction(() -> binding.bottomMixer.addView(binding.bottomNavigation)).start();
            ValueAnimator animator = ValueAnimator.ofFloat(0f, bsh);
		    animator.setDuration(300);
		    animator.addUpdateListener(animation -> {
			    int progress = Math.round((float) animation.getAnimatedValue());
                bottomSheetBehavior.setPeekHeight(progress + extraInt);
            });
		    animator.start();
            binding.Fab.show();
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
	
}