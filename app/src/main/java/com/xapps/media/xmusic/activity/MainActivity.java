package com.xapps.media.xmusic.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.BackEventCompat;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.adapter.CustomPagerAdapter;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.MainBinding;
import com.xapps.media.xmusic.fragment.MusicListFragment;
import com.xapps.media.xmusic.helper.ServiceCallback;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.models.BottomSheetBehavior;
import com.xapps.media.xmusic.models.CustomBottomSheetBehavior;
import com.xapps.media.xmusic.models.SquigglyProgress;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.viewmodel.MainActivityViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ServiceCallback {

    private MusicListFragment musicListFragment;
    private MainBinding binding;
    private MediaController mediaController;
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private ArrayList<HashMap<String, Object>> songsMap = new ArrayList<>();
    private Context context = this;
    private Handler handler, backgroundHandler;
    private NavHostFragment navHostFragment;
    private NavController navController;
    private boolean isRestoring, wasAdjusted, seekbarFree, isBnvHidden, isColorAnimated, isAnimated, isBsInvisible = false;
    private ListenableFuture<MediaController> controllerFuture;
    private SessionToken sessionToken;
    private MainActivityViewModel viewmodel;
    public BottomSheetBehavior bottomSheetBehavior, innerBottomSheetBehavior;
    private HandlerThread handlerThread = new HandlerThread("BackgroundThread");
    private SquigglyProgress progressDrawable;
    private int bnvHeight, statusBarHeight, navBarHeight, bsbHeight, bottomSheetColor, tmpColor, playerSurface;
    private long lastClick;
    private float currentSlideOffset;
    private OnBackPressedCallback callback, callback2;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        saveUIState();
        super.onSaveInstanceState(outState);
    }

    @Override
	protected void onCreate(Bundle bundle) {
        XUtils.applyDynamicColors(this);
        super.onCreate(bundle);
        XUtils.updateTheme();
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer).getChildFragmentManager().getFragments().get(0);
        if (fragment instanceof MusicListFragment) musicListFragment = (MusicListFragment) fragment;
        loadSongs();
		initialize();
	}
    
    @Override
	protected void onResume() {
	    super.onResume();
        if (mediaController != null) {
            binding.musicProgress.setProgress((int) mediaController.getCurrentPosition());
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            if (!mediaController.isPlaying() && binding.toggleView.isAnimating()) {
                binding.toggleView.stopAnimation();
            } else if (mediaController.isPlaying() && !binding.toggleView.isAnimating()) {
                binding.toggleView.startAnimation();
            }
        }
    }
    
    @Override
	public void onPause() {
		super.onPause();
        //unregisterReceiver(multiReceiver);
	}
    
    @Override
    public void onStart() {
        super.onStart();
        ServiceCallback.Hub.set(this);
        setupReceivers(true);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentsContainer).getChildFragmentManager().getFragments().get(0);
            if (fragment instanceof MusicListFragment) {
                musicListFragment = (MusicListFragment) fragment;
                try {
                 //   musicListFragment.adjustUI();
                } catch (Exception e) {
                    
                }
            }
        });
        if (sessionToken == null) sessionToken = new SessionToken(context, new ComponentName(context, PlayerService.class));
        if (controllerFuture == null && mediaController == null) {
            controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
                controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();
                } catch (Exception e) {
                    showInfoDialog("Error", 0, e.toString(), "OK");
                }
                restoreStateIfPossible();
            }, MoreExecutors.directExecutor());
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceCallback.Hub.set(null);
        //executor.shutdown();
        //unregisterReceiver(focusReceiver);
    }
    
    private void initialize() {
        handler = new Handler(Looper.getMainLooper());
        viewmodel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
        binding.bottomNavigation.post(() -> {
            bnvHeight = binding.bottomNavigation.getHeight();
			XUtils.increaseMargins(binding.musicProgress, 0, 0, 0, navBarHeight);
			bottomSheetBehavior.setPeekHeight(bottomSheetBehavior.getPeekHeight() + navBarHeight);
			//int bottomHeight = binding.coversPager.getHeight() + binding.bottomNavigation.getHeight() + binding.musicProgress.getHeight() + XUtils.getMargin(binding.musicProgress, "top");
        });
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        backgroundHandler = new Handler(looper);
        MaterialColorUtils.initColors(this);
        setupUI();
		setupListeners();
        setupCallbacks();
    }
    
    private void setupUI() {
        progressDrawable = new SquigglyProgress();
        progressDrawable.setWaveLength(100);
		progressDrawable.setLineAmplitude(8);
		progressDrawable.setPhaseSpeed(25);
		progressDrawable.setStrokeWidth(XUtils.convertToPx(this, 4f));
		progressDrawable.setTransitionEnabled(true);
		progressDrawable.setAnimate(true);
		progressDrawable.setTint(MaterialColorUtils.colorPrimary);
        binding.songSeekbar.setProgressDrawable(progressDrawable);
        binding.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners_bottom_sheet));
        EdgeToEdge.enable(this);
        
        RecyclerView innerRecylcerView = (RecyclerView) binding.coversPager.getChildAt(0);
        if (innerRecylcerView != null) {
            innerRecylcerView.setNestedScrollingEnabled(false);
            innerRecylcerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        int pageGap = XUtils.convertToPx(this, 30);
        binding.coversPager.setClipToPadding(false);
        binding.coversPager.setClipChildren(false);
		bottomSheetColor = MaterialColorUtils.colorSurfaceContainer;
		int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        int resourceId2 = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            
		if (resourceId > 0 && resourceId2 > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId2);
            navBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}
        
        binding.extendableLayout.setPadding(XUtils.convertToPx(this, 16f), 0, XUtils.convertToPx(this, 16f), navBarHeight);
        XUtils.setMargins(binding.coversPager, 0, XUtils.getStatusBarHeight(this)*2, 0, 0);
		binding.songBigTitle.setSelected(true);
		binding.artistBigTitle.setSelected(true);
		binding.currentSongTitle.setSelected(true);
		binding.currentSongArtist.setSelected(true);
        bsbHeight = bottomSheetBehavior.getPeekHeight();
	}
    
    public void setSong(int position, String coverPath, Uri fileUri) {
        PlayerService.currentPosition = position;
        seekbarFree = false;
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(0));
        binding.songSeekbar.setProgress(0);
        binding.musicProgress.setProgressCompat(0, true);
        if (mediaController.getMediaItemCount() == 0) {
            mediaController.setMediaItems(PlayerService.mediaItems);
            mediaController.prepare();
        }
        mediaController.seekTo(position, 0);
        mediaController.play();
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        syncPlayerUI(position);
        saveUIState();
        backgroundHandler.postDelayed(() -> {
            seekbarFree = true;
        }, 150);
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            binding.miniPlayerBottomSheet.setProgress(1f);
        } else {
            binding.miniPlayerBottomSheet.setProgress(0f);
        }
	}
    
    private void updateProgress(long position) {
        binding.musicProgress.setProgressCompat((int) position, true);
        binding.songSeekbar.setProgress((int) position, true);
        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(position));
    }
    
    private void updateCoverPager(int index) {
        wasAdjusted = true;
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            binding.coversPager.setCurrentItem(index, true);
        else 
            binding.coversPager.setCurrentItem(index, false);
    }
    
    public void updateSongsQueue(ArrayList<HashMap<String, Object>> s) {
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction("ACTION_UPDATE");
        startService(playIntent);
    }
    
    public void openFragment(int layoutId) {
        try {
            navController.navigate(layoutId);
        } catch (IllegalArgumentException e) {
            
        }
    }
    
    private void saveUIState() {
        viewmodel.markDataAsSaved(true);
        viewmodel.setBNVAsHidden(isBNVHidden());
        if (mediaController != null) viewmodel.setLastPosition(mediaController.getCurrentMediaItemIndex());
        PlayerService.songsMap = RuntimeData.songsMap;
    }
    
    private void restoreStateIfPossible() {
        if (viewmodel.isDataSaved()) {
            isRestoring = true;
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            HideBNV(viewmodel.wasBNVHidden());
            if (PlayerService.isPlaying) {
                handler.postDelayed(() -> {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }, 500);
            }
        } else if (PlayerService.isPlaying) {
            ColorPaletteUtils.darkColors = PlayerService.darkColors;
            ColorPaletteUtils.lightColors = PlayerService.lightColors;
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            handler.postDelayed(() -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isBnvHidden = true;
                HideBNV(false);
            }, 500);
        }else {
            handler.postDelayed(() -> {
                isBnvHidden = true;
                HideBNV(false);
            }, 500);
        }
        isRestoring = false;
    }
    
    private void setupListeners() {
        binding.toggleView.setExtraOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (!binding.toggleView.isAnimating()) {
                    mediaController.pause();
                    progressDrawable.setAnimate(false);
				} else {
                    mediaController.play();
                    progressDrawable.setAnimate(true);
				}
			}
		});
        
        binding.miniPlayerBottomSheet.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        
        binding.favoriteButton.setOnClickListener(v -> {
            
        });
        
        binding.saveButton.setOnClickListener(v -> {
            
        });
        
        binding.placeholder1.setOnClickListener(v -> {
            binding.lyricsTestLine.demoSmoothProgress();
        });

        View.OnClickListener navClick = v -> {
            if (System.currentTimeMillis() - lastClick < 150) return;
            lastClick = System.currentTimeMillis();
            String placeholder = "android.resource://" + getPackageName() + "/" + R.drawable.placeholder;
            int index = mediaController.getCurrentMediaItemIndex();
            index += (v == binding.nextButton ? 1 : -1);
            HashMap<String, Object> song = RuntimeData.songsMap.get(index);
            setSong(index, song.get("thumbnail") == null? placeholder : song.get("thumbnail").toString(), Uri.parse("file://" + song.get("path").toString()));
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
                if (mediaController != null && i != mediaController.getCurrentMediaItemIndex() && !wasAdjusted) {
                    String placeholder = "android.resource://" + getPackageName() + "/" + R.drawable.placeholder;
                    HashMap<String, Object> song = RuntimeData.songsMap.get(i);
                    setSong(i, song.get("thumbnail") == null? placeholder : song.get("thumbnail").toString(), Uri.parse("file://" + song.get("path").toString()));
                }
            }
        });

        binding.nextButton.setOnClickListener(navClick);
        binding.previousButton.setOnClickListener(navClick);

        binding.songSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration((int)progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbarFree = false;
                progressDrawable.setAnimate(false);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaController.isPlaying()) progressDrawable.setAnimate(true);
                backgroundHandler.postDelayed(() -> {
					seekbarFree = true;
				}, 125);
                mediaController.seekTo(seekBar.getProgress());
            }
        });
    }
    
    private void setupCallbacks() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    innerBottomSheetBehavior.setDraggable(false);
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                     innerBottomSheetBehavior.setDraggable(true);
                     callback.setEnabled(true);
                     binding.miniPlayerBottomSheet.animate().translationY(0).setDuration(10).start();
				} else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    innerBottomSheetBehavior.setDraggable(false);
                    if (isBNVHidden()) {
                        binding.miniPlayerBottomSheet.animate().translationY(bnvHeight).setDuration(100).start();
                    }
                    callback.setEnabled(false);
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        musicListFragment.updateActiveItem(-1);
                        PlayerService.currentPosition = -1;
						mediaController.stop();
                        mediaController.clearMediaItems();
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
                        binding.miniPlayerBottomSheet.setTranslationY(bnvHeight - bnvHeight*slideOffset);
                    }
				    binding.fragmentsContainer.setTranslationY(-XUtils.convertToPx(context, 75f)*slideOffset);
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
						isColorAnimated = false;
						Drawable background = binding.miniPlayerBottomSheet.getBackground();
					    tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, slideOffset*2 - 1f);
						((GradientDrawable) background).setColor(tmpColor);
						binding.songSeekbar.setEnabled(true);
					} else {
						if (!isColorAnimated) {
							isColorAnimated = true;
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
        
        innerBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setDraggable(true);
                    callback.setEnabled(true);
                    callback2.setEnabled(false);
                } else {
                    bottomSheetBehavior.setDraggable(false);
                    callback.setEnabled(false);
                    callback2.setEnabled(true);
                }
            }
            
            @Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float prog = 1f - slideOffset;
                binding.extendableLayout.setTranslationY(statusBarHeight*slideOffset);
            }
        });
        
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
                binding.miniPlayerBottomSheet.setTranslationY((XUtils.convertToPx(context, 1000f)*0.05f)*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setScaleX(1f-0.1f*backEvent.getProgress());
                binding.miniPlayerBottomSheet.setScaleY(1f-0.1f*backEvent.getProgress());
                
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
                //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void loadSongs() {
        executor.execute(() -> {
            if (PlayerService.songsMap.isEmpty()) {
                SongMetadataHelper.getAllSongs(context, new SongLoadListener(){
                    @Override
                    public void onProgress(ArrayList<HashMap<String, Object>> songs, int count) {
                    
                    }
                    
                    @Override 
                    public void onComplete(ArrayList<HashMap<String, Object>> songs) {
                        RuntimeData.songsMap = songs;
                        PlayerService.songsMap = songs;
                        updateSongsQueue(songs);
                        CustomPagerAdapter customPagerAdapter = new CustomPagerAdapter(context, songs);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (songs.size() > 0) {
                                wasAdjusted = true;
                                binding.coversPager.setAdapter(customPagerAdapter);
                                if (PlayerService.isPlaying && !songs.isEmpty()) {
                                    updateCoverPager(PlayerService.currentPosition);
                                    binding.toggleView.startAnimation();
                                    syncPlayerUI(PlayerService.currentPosition);
                                }    
                            } else {
                                XUtils.showMessage(context, "no songs found");
                            } 
                        });
                    }
                });
            } else {
                RuntimeData.songsMap = PlayerService.songsMap;
                updateSongsQueue(RuntimeData.songsMap);
                CustomPagerAdapter customPagerAdapter = new CustomPagerAdapter(context, RuntimeData.songsMap);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (RuntimeData.songsMap.size() > 0) {
                        wasAdjusted = true;
                        binding.coversPager.setAdapter(customPagerAdapter);
                        if (PlayerService.isPlaying && !RuntimeData.songsMap.isEmpty()) {
                            updateCoverPager(PlayerService.currentPosition);
                            binding.toggleView.startAnimation();
                            syncPlayerUI(PlayerService.currentPosition);
                        }    
                    } else {
                        XUtils.showMessage(context, "no songs found");
                    } 
                });
            }
        });
    }

    private void syncPlayerUI(int position) {
        updateMaxValue(position);
        //updateColors();
        binding.musicProgress.setProgressCompat(0, true);
        updateCoverPager(position);
        if (!binding.toggleView.isAnimating()) binding.toggleView.startAnimation();
        binding.songSeekbar.setProgress(0);
        binding.artistBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.songBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.totalDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        binding.currentDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();
        handler = new Handler(Looper.getMainLooper());   
        handler.postDelayed(() -> {
            updateTexts(position);
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
        if (binding.toggleView.isAnimating()) binding.toggleView.startAnimation();
    }
    
    private void updateSongsQueue() {
        
    }
    
    private void setupReceivers(boolean initial) {
        /*if (initial) {
            /*IntentFilter focusFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(focusReceiver, focusFilter, Context.RECEIVER_EXPORTED);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAYER_PROGRESS");
        filter.addAction("PLAYER_COLORS");
        registerReceiver(multiReceiver, filter, Context.RECEIVER_EXPORTED);*/
    }
    
    public MainBinding getBinding() {
		return binding;
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
    
    public void updateTexts(int pos) {
        if (RuntimeData.songsMap.size() > 0 && mediaController != null) {
            int p =  mediaController.getCurrentMediaItemIndex();
            binding.totalDurationText.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("duration").toString());
            binding.artistBigTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("author").toString());
            binding.songBigTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongArtist.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("author").toString());
        } else if (isRestoring || PlayerService.isPlaying) {
            int p =  viewmodel.loadLastPosition();
            binding.totalDurationText.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("duration").toString());
            binding.artistBigTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("author").toString());
            binding.songBigTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongTitle.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("title").toString());
            binding.currentSongArtist.setText(RuntimeData.songsMap.get(pos == -1? p : pos).get("author").toString());
            isRestoring = false;
        }
    }
    
    public void updateMaxValue(int pos) {
        if (RuntimeData.songsMap.size() > 0 && mediaController != null) {
            int p = mediaController.getCurrentMediaItemIndex();
            int max = Integer.parseInt(RuntimeData.songsMap.get(pos == -1? p : pos).get("total").toString());
            binding.songSeekbar.setMax(max);
            binding.musicProgress.setMax(max);
        } else if (isRestoring || PlayerService.isPlaying) {
            int p = PlayerService.currentPosition;
            int max = Integer.parseInt(RuntimeData.songsMap.get(pos == -1? p : pos).get("total").toString());
            binding.songSeekbar.setMax(max);
            binding.musicProgress.setMax(max);
            isRestoring = false;
        }
    }
    
    public void HideBNV(boolean hide) {
        Interpolator interpolator = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
        if (hide) {
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(300).setInterpolator(interpolator)/*.withEndAction(() -> binding.bottomMixer.removeView(binding.bottomNavigation))*/.start();
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
            binding.miniPlayerBottomSheet.animate().translationY(bnvHeight).setDuration(300).setInterpolator(interpolator).start();
        } else {
            int extraInt = XUtils.convertToPx(context, 25);
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) { 
                binding.miniPlayerBottomSheet.animate().translationY(0).setDuration(300).setInterpolator(interpolator).start();
            }
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(300).setInterpolator(interpolator)/*.withStartAction(() -> binding.bottomMixer.addView(binding.bottomNavigation))*/.start();
        }
        isBnvHidden = hide;
    }
    
    public boolean isBNVHidden() {
        return isBnvHidden;
    }

    public void updateColors() {
        if (ColorPaletteUtils.lightColors == null && ColorPaletteUtils.darkColors == null) return;
        
        Map<String, Integer> colors = XUtils.isDarkMode(context) ? ColorPaletteUtils.darkColors : ColorPaletteUtils.lightColors;

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

        SeekBar seekbar = binding.songSeekbar;
        seekbar.setThumbTintList(ColorStateList.valueOf(colors.get("primary")));
        progressDrawable.setTint(colors.get("primary"));

        binding.artistBigTitle.setTextColor(colors.get("onSurfaceContainer"));
        binding.songBigTitle.setTextColor(colors.get("onSurface"));
        binding.currentDurationText.setTextColor(colors.get("onSurfaceContainer"));
        binding.totalDurationText.setTextColor(colors.get("onSurfaceContainer"));
    
        GradientDrawable background = (GradientDrawable) binding.miniPlayerBottomSheet.getBackground();
        tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);
        background.setColor(tmpColor);
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

    public void Start() {
        ViewCompat.setWindowInsetsAnimationCallback(binding.bottomNavigation, new WindowInsetsAnimationCompat.Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override
            public WindowInsetsCompat onProgress(WindowInsetsCompat insets, List<WindowInsetsAnimationCompat> runningAnimations) {
                int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                binding.bottomNavigation.setTranslationY(imeBottom < 0 ? 0 : -imeBottom);
                return insets;
            }
        });
    }

    @Override
    public void onServiceEvent(int callbackType) {
        if (callbackType == ServiceCallback.CALLBACK_COLORS_UPDATE) {
            updateColors();
        } else if (callbackType == ServiceCallback.CALLBACK_PROGRESS_UPDATE && seekbarFree) {
            updateProgress(RuntimeData.currentProgress);
        }
    }

    public MediaController getController() {
        return mediaController;
    }
}
