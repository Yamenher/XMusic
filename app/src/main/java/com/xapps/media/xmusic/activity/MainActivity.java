package com.xapps.media.xmusic.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.ImageView;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
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
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private ArrayList<HashMap<String, Object>> songsMap = new ArrayList<>();
    private Context context = this;
    private Handler handler, backgroundHandler;
    private boolean isRestoring, wasAdjusted, seekbarFree, isBnvHidden, isColorAnimated, isAnimated, isBsInvisible = false;
    private ListenableFuture<MediaController> controllerFuture;
    private SessionToken sessionToken;
    private MainActivityViewModel viewmodel;
    public BottomSheetBehavior bottomSheetBehavior, innerBottomSheetBehavior;
    private HandlerThread handlerThread = new HandlerThread("BackgroundThread");
    private SquigglyProgress progressDrawable;
    private int bnvHeight, statusBarHeight, navBarHeight, bsbHeight, bottomSheetColor, tmpColor, playerSurface;
    private long lastClick;
    private float currentSlideOffset, normalFabY, playingFabY;
    private OnBackPressedCallback callback, callback2;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        saveUIState();
        super.onSaveInstanceState(outState);
    }

    @Override
	protected void onCreate(Bundle bundle) {
        EdgeToEdge.enable(this);
        getWindow().setNavigationBarContrastEnforced(false);
        XUtils.updateTheme();
        super.onCreate(bundle);
        XUtils.applyDynamicColors(this);
        ServiceCallback.Hub.set(this);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        int resourceId2 = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0 && resourceId2 > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId2);
            navBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}
        loadSongs();
		initialize();
	}
    
    @Override
	protected void onResume() {
	    super.onResume();
        if (mediaController != null) {
            updateProgress(mediaController.getCurrentPosition());
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            updateColors();
            if (!PlayerService.isPlaying && binding.toggleView.isAnimating()) {
                binding.toggleView.stopAnimation();
                progressDrawable.setAnimate(false);
            } else if (PlayerService.isPlaying && !binding.toggleView.isAnimating()) {
                binding.toggleView.startAnimation();
                progressDrawable.setAnimate(true);
            }
        }
    }
    
    @Override
	public void onPause() {
		super.onPause();
	}
    
    @Override
    public void onStart() {
        super.onStart();
        if (sessionToken == null) sessionToken = new SessionToken(context, new ComponentName(context, PlayerService.class));
        if (controllerFuture == null && mediaController == null) {
            controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
                controllerFuture.addListener(() -> {
                try {
                    if (!XUtils.isDarkMode(this)) setDarkStatusBar(getWindow(), true);
                    mediaController = controllerFuture.get();
                    progressDrawable.setAnimate(mediaController.isPlaying());
                    setupControllerListener();
                } catch (Exception e) {
                    showInfoDialog("Error", 0, e.toString(), "OK");
                }
                restoreStateIfPossible();
            }, MoreExecutors.directExecutor());
        }
    }
    
    public void setupControllerListener() {
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null) {
                    int position = mediaController.getCurrentMediaItemIndex();
                    musicListFragment.updateActiveItem(position);
                    progressDrawable.setAnimate(true);
                    if (!binding.toggleView.isAnimating()) binding.toggleView.startAnimation();
                    PlayerService.currentPosition = position;
                    seekbarFree = false;
                    binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(0));
                    binding.songSeekbar.setProgress(0, true);
                    binding.musicProgress.setProgressCompat(0, true);
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
                    if (position == 0) {
                        binding.previousButton.setActive(false);
                        binding.nextButton.setActive(true);
                    } else if (position == RuntimeData.songsMap.size() - 1) {
                        binding.previousButton.setActive(true);
                        binding.nextButton.setActive(false);
                    } else {
                        binding.previousButton.setActive(true);
                        binding.nextButton.setActive(true);
                    }
                }
            }
            
            @Override
            public void onPositionDiscontinuity(Player.PositionInfo positionInfo, Player.PositionInfo positionInfo2, int i) {
                updateProgress(mediaController.getCurrentPosition());
            }            
            
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                    if (playWhenReady) {
                        binding.toggleView.startAnimation();
                        progressDrawable.setAnimate(true);
                    } else {
                        binding.toggleView.stopAnimation();
                        progressDrawable.setAnimate(false);
                    }
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        saveUIState();
        super.onDestroy();
        ServiceCallback.Hub.set(null);
        mediaController.release();
        //executor.shutdown();
        //unregisterReceiver(focusReceiver);
    }
    
    private void initialize() {
        viewmodel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        handler = new Handler(Looper.getMainLooper());
        bottomSheetBehavior = BottomSheetBehavior.from(binding.miniPlayerBottomSheet);
        binding.bottomNavigation.post(() -> {
            binding.bottomNavigation.setSelectedItemId(viewmodel.loadBNVPosition());
            bnvHeight = binding.bottomNavigation.getHeight();
			XUtils.increaseMargins(binding.musicProgress, 0, 0, 0, navBarHeight);
			bottomSheetBehavior.setPeekHeight(bottomSheetBehavior.getPeekHeight() + navBarHeight);
			playingFabY = -(binding.bottomNavigation.getHeight() + binding.miniPlayerDetailsLayout.getHeight() + XUtils.getMargin(binding.coversPager, "bottom")*2 + binding.musicProgress.getHeight());
            normalFabY = -(binding.bottomNavigation.getHeight() + binding.musicProgress.getHeight());
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
		bottomSheetColor = MaterialColorUtils.colorSurfaceContainer;
        binding.extendableLayout.setPadding(XUtils.convertToPx(this, 16f), 0, XUtils.convertToPx(this, 16f), navBarHeight);
        XUtils.setMargins(binding.coversPager, 0, XUtils.getStatusBarHeight(this)*5, 0, 0);
		binding.songBigTitle.setSelected(true);
		binding.artistBigTitle.setSelected(true);
		binding.currentSongTitle.setSelected(true);
		binding.currentSongArtist.setSelected(true);
        bsbHeight = bottomSheetBehavior.getPeekHeight();
	}
    
    public void setSong(int position, String coverPath, Uri fileUri) {
        if (mediaController.getPlaybackState() == Player.STATE_BUFFERING) return;
        MusicListFragment.fab.animate().translationY(playingFabY).setDuration(300).start();
        if (mediaController.getMediaItemCount() == 0) {
            mediaController.setMediaItems(PlayerService.mediaItems);
            PlayerService.areMediaItemsEmpty = false;
            mediaController.prepare();
        }
        mediaController.seekTo(position, 0);
        mediaController.play();
	}
    
    private void updateProgress(long position) {
        binding.musicProgress.setProgressCompat((int) position, true);
        binding.songSeekbar.setProgress((int) position, true);
        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(position));
    }
    
    private void updateCoverPager(int index) {
        if (RuntimeData.songsMap.size() == 0) return;
        wasAdjusted = true;
        Object cover = RuntimeData.songsMap.get(index).get("thumbnail");
        Glide.with(this)
            .asDrawable()
            .load(cover == null? ContextCompat.getDrawable(this, R.drawable.placeholder) : cover.toString())
            .apply(new RequestOptions()
            .override(500, 500)
            .fallback(ContextCompat.getDrawable(this, R.drawable.placeholder))
            .centerCrop()
            .priority(Priority.NORMAL)
            .skipMemoryCache(false))
            .into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(Drawable drawable, Transition<? super Drawable> transition) {
                    crossfadeDrawable(binding.coversPager, drawable, 200);
                }

                @Override
                public void onLoadCleared(Drawable placeholder) {}
            });
    }
    
    private void crossfadeDrawable(ImageView iv, Drawable next, int duration) {
        Drawable current = iv.getDrawable();
        if (current == null) {
            iv.setImageDrawable(next);
            return;
        }
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{current, next});
        td.setCrossFadeEnabled(true);
        iv.setImageDrawable(td);
        td.startTransition(duration);
    }
    
    public void updateSongsQueue(ArrayList<HashMap<String, Object>> s) {
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction("ACTION_UPDATE");
        startService(playIntent);
    }
    
    private void saveUIState() {
        viewmodel.markDataAsSaved(true);
        viewmodel.setBNVAsHidden(isBnvHidden);
        viewmodel.saveBNVPosition(binding.bottomNavigation.getSelectedItemId());
        if (mediaController != null) viewmodel.setLastPosition(mediaController.getCurrentMediaItemIndex());
        PlayerService.songsMap = RuntimeData.songsMap;
    }
    
    private void restoreStateIfPossible() {
        seekbarFree = true;
        if (viewmodel.isDataSaved()) {
            ColorPaletteUtils.darkColors = PlayerService.darkColors;
            ColorPaletteUtils.lightColors = PlayerService.lightColors;
            isRestoring = true;
            int index = mediaController.getCurrentMediaItemIndex();
            if (index >= 0 && ColorPaletteUtils.lightColors != null && ColorPaletteUtils.darkColors != null) {
                updateColors();
            }
            if (mediaController.getMediaItemCount() > 0) {
                musicListFragment.updateActiveItem(index);
                syncPlayerUI(mediaController.getCurrentMediaItemIndex());
                updateProgress(mediaController.getCurrentPosition());
                updateColors();
                binding.bottomNavigation.postDelayed(() -> {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    isBnvHidden = true;
                    HideBNV(false);
                    MusicListFragment.fab.animate().translationY(playingFabY).setDuration(300).start();
                }, 100);
            } else {
                binding.bottomNavigation.postDelayed(() -> {
                    isBnvHidden = true;
                    HideBNV(false);
                    MusicListFragment.fab.animate().translationY(normalFabY).setDuration(300).start();
                }, 100);
            }
        } else if (PlayerService.isAnythingPlaying()) {
            ColorPaletteUtils.darkColors = PlayerService.darkColors;
            ColorPaletteUtils.lightColors = PlayerService.lightColors;
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            updateColors();
            musicListFragment.updateActiveItem(mediaController.getMediaItemCount() > 0? mediaController.getCurrentMediaItemIndex() : -1);
            binding.bottomNavigation.postDelayed(() -> {
                MusicListFragment.fab.animate().translationY(playingFabY).setDuration(300).start();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isBnvHidden = true;
                HideBNV(false);
            }, 100);
            updateProgress(mediaController.getContentPosition());
        } else {
            binding.bottomNavigation.postDelayed(() -> {
                isBnvHidden = true;
                HideBNV(false);
                MusicListFragment.fab.animate().translationYBy(normalFabY).setDuration(300).start();
            }, 100);
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
            binding.musicProgress.setAlpha(0f);
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

        binding.nextButton.setOnClickListener(navClick);
        binding.previousButton.setOnClickListener(navClick);

        binding.songSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration((int)progress));
                    binding.musicProgress.setProgress(progress);
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
        
            
        MaterialFadeThrough transition = new MaterialFadeThrough();
        transition.setDuration(500);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            TransitionManager.beginDelayedTransition(binding.Coordinator, transition);
            int id = item.getItemId();

            if (id == R.id.menuHomeFragment) {
                binding.searchFrag.setVisibility(View.GONE);
                binding.settingsFrag.setVisibility(View.GONE);
                binding.mainFrag.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.menuSearchFragment) {
                binding.searchFrag.setVisibility(View.VISIBLE);
                binding.mainFrag.setVisibility(View.GONE);
                binding.settingsFrag.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.menuSettingsFragment) {
                binding.searchFrag.setVisibility(View.GONE);
                binding.mainFrag.setVisibility(View.GONE);
                binding.settingsFrag.setVisibility(View.VISIBLE);
                return true;
            }

            return false;
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
                        ColorPaletteUtils.lightColors = null;
                        ColorPaletteUtils.darkColors = null;
                        PlayerService.currentPosition = -1;
						mediaController.stop();
                        mediaController.clearMediaItems();
                        PlayerService.areMediaItemsEmpty = true;
                        MusicListFragment.fab.animate().translationY(normalFabY).setDuration(300).start();
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
                                //binding.coversPager.setAdapter(customPagerAdapter);
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (RuntimeData.songsMap.size() > 0) {
                        wasAdjusted = true;
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
        updateCoverPager(position);
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
    }
    
    private void updateSongsQueue() {
        
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
        if (handler == null) return;
        handler.post(() -> {
            if (callbackType == ServiceCallback.CALLBACK_COLORS_UPDATE) {
                updateColors();
            } else if (callbackType == ServiceCallback.CALLBACK_PROGRESS_UPDATE && seekbarFree) {
                updateProgress(RuntimeData.currentProgress);
            }
        });
    }

    public MediaController getController() {
        return mediaController;
    }

    public void setMusicListFragmentInstance(MusicListFragment f) {
        musicListFragment = f;
    }

    public void setDarkStatusBar(Window window, boolean dark) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller == null) return;

            int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            controller.setSystemBarsAppearance(dark ? appearance : 0, appearance);
        } else {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();

            if (dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            decor.setSystemUiVisibility(flags);
        }
    }
}
