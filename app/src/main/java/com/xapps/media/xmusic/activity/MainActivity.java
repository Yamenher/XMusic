package com.xapps.media.xmusic.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import androidx.core.content.res.ResourcesCompat;
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
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSeekController;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.resources.TypefaceUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.adapter.CustomPagerAdapter;
import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.fragment.MusicListFragment;
import com.xapps.media.xmusic.helper.ServiceCallback;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.lyric.LyricsExtractor;
import com.xapps.media.xmusic.lyric.LyricsParser;
import com.xapps.media.xmusic.models.BottomSheetBehavior;
import com.xapps.media.xmusic.models.CustomBottomSheetBehavior;
import com.xapps.media.xmusic.models.SquigglyProgress;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.viewmodel.MainActivityViewModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ServiceCallback, PlaybackControlListener {

    private MusicListFragment musicListFragment;
    private ActivityMainBinding binding;
    private MediaController mediaController;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private ArrayList<HashMap<String, Object>> songsMap = new ArrayList<>();
    private Context context = this;
    private Handler handler, backgroundHandler;
    private boolean isRestoring, wasAdjusted, seekbarFree, isBnvHidden, isColorAnimated, isAnimated, isBsInvisible, isOledTheme, isResuming = false;
    private ListenableFuture<MediaController> controllerFuture;
    private SessionToken sessionToken;
    private MainActivityViewModel viewmodel;
    public BottomSheetBehavior bottomSheetBehavior, innerBottomSheetBehavior;
    private HandlerThread handlerThread = new HandlerThread("BackgroundThread");
    private SquigglyProgress progressDrawable;
    private int bnvHeight, statusBarHeight, navBarHeight, bsbHeight, bottomSheetColor, tmpColor, playerSurface;
    private long lastClick;
    private float currentSlideOffset, tmpY;
    private OnBackPressedCallback callback, callback2, callback3;
    private TransitionSeekController controller;
    private CustomTarget<Drawable> coverTarget;
    private ValueAnimator colorAnimator;
    private Map<String, Integer> effectiveOldColors = new HashMap<>();

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
        XUtils.applyDynamicColors(this, DataManager.isOledThemeEnabled());
        if (XUtils.isDarkMode(this) && DataManager.isOledThemeEnabled())getTheme().applyStyle(R.style.ThemeOverlay_XMusic_OLED, true);
        super.onCreate(bundle);
        ServiceCallback.Hub.set(this);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
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
        isResuming = true;
        if (mediaController != null) {
            updateProgress(mediaController.getCurrentPosition());
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            binding.getRoot().post(() -> updateColors());
            if (!PlayerService.isPlaying && binding.toggleView.isAnimating()) {
                binding.toggleView.stopAnimation();
                progressDrawable.setAnimate(false);
            } else if (PlayerService.isPlaying && !binding.toggleView.isAnimating()) {
                binding.toggleView.startAnimation();
                progressDrawable.setAnimate(true);
            }
        }
        isResuming = false;
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
                try {
                    String songPath = RuntimeData.songsMap.get(mediaController.getCurrentMediaItemIndex()).get("path").toString();
                    LyricsExtractor.extract(songPath, lyrics -> {
                        if (lyrics != null) {
                            LyricsParser.parse(lyrics, result -> {
                                binding.lyricsView.setLyrics(result.lines);
                                binding.lyricsView.configureSyncedLyrics(result.isSynced, ResourcesCompat.getFont(context, R.font.product_sans_regular), Gravity.START, 17f);
                                binding.lyricsView.setOnSeekListener(MainActivity.this);
                            });
                        } else {
                            Log.e("LyricsExtractor", "could not find any lyrics for song : "+ RuntimeData.songsMap.get(mediaController.getCurrentMediaItemIndex()).get("title").toString());
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
        binding.currentSongTitle.setTypeface(binding.currentSongTitle.getTypeface(), Typeface.BOLD);
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
        loadSettings();
	}
    
    public void setSong(int position, String coverPath, Uri fileUri) {
        if (mediaController.getPlaybackState() == Player.STATE_BUFFERING) return;
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
        binding.songSeekbar.setProgress((int) position, false);
        binding.currentDurationText.setText(SongMetadataHelper.millisecondsToDuration(position));
    }
    
    private void updateCoverPager(int index) {
        if (RuntimeData.songsMap.isEmpty()) return;
        
        Object cover = RuntimeData.songsMap.get(index).get("thumbnail");
        if (coverTarget != null) {
            Glide.with(this).clear(coverTarget);
        }

        coverTarget = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable drawable, Transition<? super Drawable> transition) {
                crossfadeDrawable(binding.coversPager, drawable, 200);
            }

            @Override
            public void onLoadCleared(Drawable placeholder) {
                binding.coversPager.setImageDrawable(placeholder);
            }
        };
    
        Glide.with(this)
            .asDrawable()
            .load(cover == null ? R.drawable.placeholder : cover)
            .apply(new RequestOptions()
            .override(500, 500)
            .centerCrop()
            .priority(Priority.NORMAL))
            .into(coverTarget);
    }
    
    private void crossfadeDrawable(ImageView iv, Drawable next, int duration) {
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{
            new ColorDrawable(Color.TRANSPARENT),
            next
        });
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
                }, 100);
            } else {
                binding.bottomNavigation.postDelayed(() -> {
                    isBnvHidden = true;
                    HideBNV(false);
                }, 100);
            }
        } else if (PlayerService.isAnythingPlaying()) {
            ColorPaletteUtils.darkColors = PlayerService.darkColors;
            ColorPaletteUtils.lightColors = PlayerService.lightColors;
            syncPlayerUI(mediaController.getCurrentMediaItemIndex());
            updateColors();
            musicListFragment.updateActiveItem(mediaController.getMediaItemCount() > 0? mediaController.getCurrentMediaItemIndex() : -1);
            binding.bottomNavigation.postDelayed(() -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isBnvHidden = true;
                HideBNV(false);
            }, 100);
            updateProgress(mediaController.getContentPosition());
        } else {
            binding.bottomNavigation.postDelayed(() -> {
                isBnvHidden = true;
                HideBNV(false);
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
        
        binding.lyricsButton.setOnClickListener(v -> {
            boolean b = binding.lyricsButton.isChecked();
            if (binding.lyricsContainer.getVisibility() != View.GONE && !(binding.lyricsContainer.getVisibility() == View.VISIBLE && binding.lyricsContainer.getAlpha() == 1f)) {
                binding.lyricsButton.setChecked(!b);
                return;
            }
            callback3.setEnabled(b);
            callback.setEnabled(!b);
            innerBottomSheetBehavior.setDraggable(!b);
            bottomSheetBehavior.setDraggable(!b);
            binding.lyricsContainer.setClickable(b);
            binding.lyricsContainer.setFocusable(b);
            binding.lyricsContainer.setFocusableInTouchMode(b);
            if (b) {
                binding.lyricsContainer.setAlpha(0f);
                binding.lyricsContainer.setScaleX(1.1f);
                binding.lyricsContainer.setScaleY(1.1f);
                binding.lyricsContainer.animate().alpha(1f).setDuration(250).withStartAction(() -> {
                    binding.lyricsContainer.setVisibility(View.VISIBLE);
                }).start();
                binding.lyricsContainer.animate().scaleY(1f).scaleX(1f).setDuration(240).start();
            } else {
                binding.lyricsContainer.animate().alpha(0f).setDuration(250).withEndAction(() -> {
                    binding.lyricsContainer.setVisibility(View.GONE);
                }).start();
                binding.lyricsContainer.animate().scaleY(1.1f).scaleX(1.1f).setDuration(240).start();
            }
        });
        
        binding.miniPlayerBottomSheet.setOnTouchListener((v, event) -> {
            return true;
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
        
        callback3 = new OnBackPressedCallback(false) {
            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                binding.lyricsContainer.setAlpha(1f - 0.7f*backEvent.getProgress());
                binding.lyricsContainer.setScaleY(1f + 0.1f*backEvent.getProgress());
                binding.lyricsContainer.setScaleX(1f + 0.1f*backEvent.getProgress());
            }

            @Override
            public void handleOnBackPressed() {
                binding.lyricsContainer.animate().alpha(0f).setDuration(125).withEndAction(() -> {
                    binding.lyricsContainer.setVisibility(View.GONE);
                    binding.lyricsContainer.setAlpha(1f);
                    binding.lyricsContainer.setScaleY(1f);
                    binding.lyricsContainer.setScaleX(1f);
                }).start();
                binding.lyricsContainer.animate().scaleY(1.1f).scaleX(1.1f).setDuration(110).start();
                bottomSheetBehavior.setDraggable(true);
                innerBottomSheetBehavior.setDraggable(true);
                callback3.setEnabled(false);
                callback.setEnabled(true);
                binding.lyricsButton.setChecked(false);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.lyricsContainer.animate().alpha(1f).setDuration(100).start();
                binding.lyricsContainer.animate().scaleY(1f).scaleX(1f).setDuration(100).start();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback3);
        
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
                binding.secondCoordinator.animate().translationY(0).setDuration(200).start();
                innerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                callback2.setEnabled(false);
            }

            @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            @Override
            public void handleOnBackCancelled() {
                binding.secondCoordinator.animate().scaleX(1f).setDuration(300).start();
                binding.secondCoordinator.animate().translationY(0).setDuration(200).start();
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
                boolean b = binding.lyricsButton.isChecked();
                if (b) binding.lyricsButton.performClick();
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
                                if (PlayerService.isPlaying && !songs.isEmpty()) {
                                    updateCoverPager(PlayerService.currentPosition);
                                    binding.toggleView.startAnimation();
                                    syncPlayerUI(PlayerService.currentPosition);
                                }    
                            } else {
                                XUtils.showMessage(context, "no songs found");
                                MusicListFragment.fab.hide();
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
                        MusicListFragment.fab.hide();
                    } 
                });
            }
        });
    }

    private void syncPlayerUI(int position) {
        updateMaxValue(position);
        updateCoverPager(position);
        if (!isResuming) {
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
        } else {
            updateTexts(position);
        }
    }
    
    private void updateSongsQueue() {
        
    }
    
    public ActivityMainBinding getBinding() {
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
            binding.bottomNavigation.animate().alpha(0.5f).translationY(binding.bottomNavigation.getHeight()).setDuration(300).setInterpolator(interpolator).start();
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
            binding.miniPlayerBottomSheet.animate().translationY(bnvHeight).setDuration(300).setInterpolator(interpolator).start();
        } else {
            int extraInt = XUtils.convertToPx(context, 25);
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) { 
                binding.miniPlayerBottomSheet.animate().translationY(0).setDuration(300).setInterpolator(interpolator).start();
            }
            binding.bottomNavigation.animate().alpha(1f).translationY(0).setDuration(300).setInterpolator(interpolator).start();
        }
        isBnvHidden = hide;
    }
    
    public boolean isBNVHidden() {
        return isBnvHidden;
    }

    public void updateColors() {
        if (ColorPaletteUtils.lightColors == null && ColorPaletteUtils.darkColors == null) return;
        
        Map<String, Integer> colors = XUtils.isDarkMode(context) ? ColorPaletteUtils.darkColors : ColorPaletteUtils.lightColors;
        Map<String, Integer> oldColors = XUtils.isDarkMode(context) ? ColorPaletteUtils.oldDarkColors : ColorPaletteUtils.oldLightColors;
        
        effectiveOldColors = new HashMap<>(oldColors);
        
        int onTertiary = colors.get("onTertiary");
        int tertiary = colors.get("tertiary");
        int oldOnTertiary = effectiveOldColors.get("onTertiary");
        int oldTertiary = effectiveOldColors.get("tertiary");
        int surface = isOledTheme? 0xff000000 : colors.get("surface");
        int oldSurface = isOledTheme? 0xff000000 : effectiveOldColors.get("surface");
        int surfaceContainer = isOledTheme? 0xff050505 : colors.get("surfaceContainer");
        int oldSurfaceContainer = isOledTheme? 0xff050505 : effectiveOldColors.get("surfaceContainer");
        int outline = colors.get("outline");
        int oldOutline = effectiveOldColors.get("outline");
        int primary = colors.get("primary");
        int oldPrimary = effectiveOldColors.get("primary");
        int onPrimary = colors.get("onPrimary");
        int oldOnPrimary = effectiveOldColors.get("onPrimary");
        int onSurfaceContainer = isOledTheme? colors.get("onSurface") : colors.get("onSurfaceContainer");
        int oldOnSurfaceContainer = isOledTheme? effectiveOldColors.get("onSurface") : effectiveOldColors.get("onSurfaceContainer");
        int onSurface = colors.get("onSurface");
        int oldOnSurface = effectiveOldColors.get("onSurface");
        
        binding.mesh.setColors(surface, onPrimary, onTertiary);
        
        Drawable nextBg = binding.nextButton.getBackground();
        Drawable favBg  = binding.favoriteButton.getBackground();
        Drawable saveBg = binding.saveButton.getBackground();
        Drawable prevBg = binding.previousButton.getBackground();
        GradientDrawable background = (GradientDrawable) binding.miniPlayerBottomSheet.getBackground();
        
        Drawable d = binding.extendableLayout.getBackground();
        GradientDrawable gd = (GradientDrawable) d;
        
        GradientDrawable d2 = (GradientDrawable) binding.dragHandle.getBackground();
        
        SeekBar seekbar = binding.songSeekbar;
        
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(isResuming? 0 : 200);
        va.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            int iop = XUtils.interpolateColor(oldOnPrimary, onPrimary, f);
            int ip = XUtils.interpolateColor(oldPrimary, primary, f);
            int iot = XUtils.interpolateColor(oldOnTertiary, onTertiary, f);
            int it = XUtils.interpolateColor(oldTertiary, tertiary, f);
            int is = XUtils.interpolateColor(oldSurface, surface, f);
            int isc = XUtils.interpolateColor(oldSurfaceContainer, surfaceContainer, f);
            int io = XUtils.interpolateColor(oldOutline, outline, f);
            int iosc = XUtils.interpolateColor(oldOnSurfaceContainer, onSurfaceContainer, f);
            int ios = XUtils.interpolateColor(oldOnSurface, onSurface, f);
            
            binding.toggleView.setShapeColor(iop);
            binding.toggleView.setIconColor(ip);
            
            binding.nextButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.favoriteButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.saveButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.previousButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            
            nextBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            favBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            saveBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            prevBg.setColorFilter(iot, PorterDuff.Mode.SRC_IN);
            
            playerSurface = is;
            
            tmpColor = XUtils.interpolateColor(bottomSheetColor, playerSurface, currentSlideOffset);
            background.setColor(tmpColor);
            binding.lyricsContainer.setBackgroundColor(playerSurface);
            binding.lyricTopScrim.setScrimColor(playerSurface);
            
            gd.setColor(isc);
            
            d2.setColor(isOledTheme? 0xffbdbdbd : io);
            
            seekbar.setThumbTintList(ColorStateList.valueOf(ip)); 
            progressDrawable.setTint(ip);
            
            binding.lyricsButton.setIconTint(ColorStateList.valueOf(isOledTheme? 0xffbdbdbd : iosc));
            binding.lyricsButton.setRippleColor(ColorStateList.valueOf(io));
            
            binding.artistBigTitle.setTextColor(iosc);
            binding.songBigTitle.setTextColor(ios);
            binding.currentDurationText.setTextColor(iosc);
            binding.totalDurationText.setTextColor(iosc);
        });
        va.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled) {
                    effectiveOldColors = new HashMap<>(colors);
                }
            }
        });
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        colorAnimator = va;
        va.start();
    
    }

    @Override
    public void onServiceEvent(int callbackType) {
        if (handler == null) return;
        handler.post(() -> {
            if (callbackType == ServiceCallback.CALLBACK_COLORS_UPDATE) {
                updateColors();
            } else if (callbackType == ServiceCallback.CALLBACK_PROGRESS_UPDATE && seekbarFree) {
                updateProgress(RuntimeData.currentProgress);
                if (mediaController != null) binding.lyricsView.onProgress((int) RuntimeData.currentProgress);
            } else if (callbackType == ServiceCallback.CALLBACK_VUMETER_UPDATE && mediaController != null && mediaController.isPlaying()) {
                musicListFragment.updateActiveItem(mediaController.getCurrentMediaItemIndex());
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

    private void loadSettings() {
        isOledTheme = XUtils.isDarkMode(this) && DataManager.isOledThemeEnabled();
    }
    
    @Override
    public void onSeekRequested(long ms) {
        if (mediaController != null) {
            mediaController.seekTo(ms);
        }
    }

}
