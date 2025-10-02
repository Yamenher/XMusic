package com.xapps.media.xmusic;

import android.Manifest;
import android.animation.*;
import android.app.*;
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
import androidx.core.view.WindowCompat;
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
import androidx.palette.*;
import androidx.profileinstaller.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.savedstate.*;
import androidx.startup.*;
import androidx.transition.*;

import com.appbroker.roundedimageview.*;
import com.bumptech.glide.Glide;
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
import com.xapps.media.xmusic.adapters.CustomPagerAdapter;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.databinding.MainBinding;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.*;

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
    private boolean hasPausedFirst = false; 
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
    private MusicListFragmentActivity mlfa; 
	
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
	public static ArrayList<HashMap<String, Object>> currentMap = new ArrayList<>();
    
    private ActivityResultLauncher<String> requestPermissionLauncher;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
        /*if (Build.VERSION.SDK_INT >= 31) {
		    final SplashScreen splash = SplashScreen.installSplashScreen(this);
            splash.setKeepOnScreenCondition(() -> {
                if (isDataLoaded) {
                    if (SongsMap.size() == 0) {
                       MusicListFragmentActivity.fab.hide();
                    }
                }
                return !isDataLoaded;
            });
        }*/
        DynamicColors.applyIfAvailable(this);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
        mlfa = (MusicListFragmentActivity) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        if (Build.VERSION.SDK_INT >= 32) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
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
                    try {
                        SerializationUtils.saveToFile(c, songs, "songsList");
                    } catch (Exception e) {
                        XUtils.showMessage(c, "failed to save songs"); 
                    }
                }
            });
        });
	}
	
	private void initialize(Bundle _savedInstanceState) {
        MaterialColorUtils.initColors(this);
        binding.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners_bottom_sheet));
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
            MusicListFragmentActivity.fab.hide();
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
		bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
					if (!isBNVHidden()) MusicListFragmentActivity.fab.hide();
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
				} else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						Intent playIntent = new Intent(c, PlayerService.class);
						playIntent.setAction("ACTION_STOP");
						startService(playIntent);
                        Intent playIntent2 = new Intent("ACTION_STOP_FRAGMENT");
						sendBroadcast(playIntent2);
						isBsInvisible = true;
                        XUtils.increaseMargins(MusicListFragmentActivity.fab, 0, 0, 0, -(binding.coversPager.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2));
				    } else {
						isBsInvisible = false;
					}
					if (!isBNVHidden()) MusicListFragmentActivity.fab.show();
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
            
		
            
		/*binding.miniPlayerBottomSheet.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
			public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
		    }
			@Override
			public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            
			}
			@Override
			    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
			}
			@Override
				public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
			}
		});*/
		
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
    
    public void addFragmentWithTransition(androidx.fragment.app.Fragment fragment) {
        fragment.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        fragment.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        androidx.fragment.app.Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        current.setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        current.setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentsContainer, fragment)
        .addToBackStack(null)
        .commit();
    }
	
	public MainBinding getBinding() {
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
        if (hasPausedFirst) {
            if (Build.VERSION.SDK_INT >= 32) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    MaterialAlertDialogBuilder m = new MaterialAlertDialogBuilder(this);
                    m.setTitle("Permission needed to continue");
                    m.setMessage("To access and play your music, this app requires permission to read audio files on your device. This permission is used solely for identifying and playing your songs—nothing else.");
                    m.setPositiveButton("Grant", (dialog, which) -> { XUtils.openSettings(c); });
                    m.setNegativeButton("Cancel", (dialog, which) -> { finishAffinity(); });
                    m.setCancelable(false);
                    m.show();
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    MaterialAlertDialogBuilder m = new MaterialAlertDialogBuilder(this);
                    m.setTitle("Permission needed to continue");
                    m.setMessage("To access and play your music, this app requires permission to read audio files on your device. This permission is used solely for identifying and playing your songs—nothing else.");
                    m.setPositiveButton("Grant", (dialog, which) -> { XUtils.openSettings(c); });
                    m.setNegativeButton("Cancel", (dialog, which) -> { finishAffinity(); });
                    m.setCancelable(false);
                    m.show();
                }
		    }
        }
        hasPausedFirst = false;
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
        hasPausedFirst = true;
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
        
	public void _setSong(final int _position, final String _coverPath, final Uri _fileUri) {
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            Intent playIntent = new Intent(this, PlayerService.class);
            playIntent.setAction("ACTION_PLAY");
            playIntent.putExtra("uri", _fileUri.toString());
            playIntent.putExtra("title", currentMap.get(_position).get("title").toString());
            playIntent.putExtra("artist", currentMap.get(_position).get("author").toString());
            playIntent.putExtra("cover", _coverPath);
            playIntent.putExtra("position", _position);
            startService(playIntent);
        }, 10);
        binding.coversPager.setCurrentItem(_position, bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED? true : false);
        currentPosition = _position;
        binding.musicProgress.setProgressCompat(0, true);
        binding.songSeekbar.setValue(0);
        if (isBsInvisible) {
			XUtils.increaseMarginsSmoothly(MusicListFragmentActivity.fab, 0, 0, 0, (binding.coversPager.getHeight() + binding.miniPlayerBottomSheet.getPaddingTop()*2), 200L);
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
        MusicListFragmentActivity frag = (MusicListFragmentActivity) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        if (frag != null) {
            currentMap = frag.getSongsMap();
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
				updateColors();
			} else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
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
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(180).withEndAction(() -> binding.bottomMixer.removeView(binding.bottomNavigation)).start();
            ValueAnimator animator = ValueAnimator.ofFloat(bsh, 0f);
		    animator.setDuration(300);
		    animator.addUpdateListener(animation -> {
			    int progress = Math.round((float) animation.getAnimatedValue());
                bottomSheetBehavior.setPeekHeight(progress);
            });
		    animator.start();
            if (!fabWasHidden) {
                MusicListFragmentActivity.fab.hide();
            }
        } else {
            int extraInt = XUtils.convertToPx(c, 25);
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(350).withStartAction(() -> binding.bottomMixer.addView(binding.bottomNavigation)).start();
            ValueAnimator animator = ValueAnimator.ofFloat(0f, bsh);
		    animator.setDuration(300);
		    animator.addUpdateListener(animation -> {
			    int progress = Math.round((float) animation.getAnimatedValue());
                bottomSheetBehavior.setPeekHeight(progress + extraInt);
            });
		    animator.start();
            if (SongsMap.size() != 0 && !fabWasHidden) {
                MusicListFragmentActivity.fab.show();
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
            binding.toggleView.setShapeColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("primary") : ColorPaletteUtils.lightColors.get("onPrimary"));
		    playerSurface = XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("surface") : ColorPaletteUtils.lightColors.get("surface");
		    binding.toggleView.setIconColor(XUtils.isDarkMode(c)? ColorPaletteUtils.darkColors.get("onPrimary") : ColorPaletteUtils.lightColors.get("primary"));
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
            binding.coversPager.setCurrentItem(pos == -1? PlayerService.currentPosition : pos, false);
        }
    }
    
    public void updateMaxValue(int pos) {
        if (currentMap.size() > 0) {
            int max = Integer.parseInt(currentMap.get(pos == -1? PlayerService.currentPosition : pos).get("total").toString());
            binding.songSeekbar.setValueTo(max);
            binding.musicProgress.setMax(max);
        }
    }
}