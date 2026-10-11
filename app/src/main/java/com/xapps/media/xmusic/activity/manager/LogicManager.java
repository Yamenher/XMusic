package com.xapps.media.xmusic.activity.manager;

import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.view.ViewKt;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSeekController;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.search.SearchView;
import com.google.android.material.slider.Slider;
import com.google.android.material.transition.MaterialFadeThrough;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.activity.controller.ActivityMediaController;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.databinding.LayoutSpeedTempoSheetBinding;

import java.util.Locale;
import com.xapps.media.xmusic.lyric.LyricsExtractor;
import com.xapps.media.xmusic.service.XPlayerService;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;
import com.xapps.media.xmusic.widget.RichTooltip;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import kotlin.Unit;

public class LogicManager {
    private final RootActivity activity;
    private final ActivityRootBinding binding;
    private final UIManager uiManager;
    private MediaController mediaController;
    private SessionToken sessionToken;
    private ActivityMediaController controller;

    public int mlfState = UIManager.LAYOUT_STATE_EXPOSE_TABS_BNV;
    public int srfState = UIManager.LAYOUT_STATE_EXPOSE_BNV;
    public int sgfState = UIManager.LAYOUT_STATE_EXPOSE_BNV;

    private boolean isUserSeeking;

    private boolean validCallback = false;

    private OnBackPressedCallback lyricsCallback;
    private TransitionSeekController seekController;

    public LogicManager(RootActivity activity, UIManager uiManager) {
        this.activity = activity;
        this.binding = activity.getBinding();
        this.uiManager = uiManager;
    }

    public void initLogic() {
        setupListeners();
        setupCallbacks();
    }

    private void setupListeners() {
        binding.collapsedPlayer.cover.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                binding.collapsedPlayer.cover.captureCollapsedBounds();
            }
        });

        binding.maximumSizeView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {

                int size = binding.maximumSizeView.getMaximumSize();

                View v2 = binding.maximumSizeView;
                int leftNow = v2.getLeft();
                int topNow = v2.getTop();
                int x = leftNow + (v2.getWidth() - size) / 2;

                binding.collapsedPlayer.cover.setExpandedBounds(x, topNow, x + size, topNow + size);

                binding.collapsedPlayer.cover.setExpansionProgress(Math.max(0f, binding.miniPlayer.getSlideOffset()));
            }
        });

        binding.expandedPlayer.songSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.expandedPlayer.currentDurationText.setText(XUtils.millisecondsToDuration(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> isUserSeeking = false, 100);
                mediaController.seekTo(seekBar.getProgress());
            }
        });

        binding.expandedPlayer.songSeekbar.setOnClickListener(v -> {});

        binding.expandedPlayer.previousButton.setOnHoldListener(R.drawable.ic_rewind_10, () -> {
            mediaController.seekTo(mediaController.getCurrentPosition() - 10000);
        });

        binding.expandedPlayer.nextButton.setOnHoldListener(R.drawable.ic_fast_forward_10, () -> {
            mediaController.seekTo(mediaController.getCurrentPosition() + 10000);
        });

        binding.expandedPlayer.previousButton.setOnClickListener(v -> {
            mediaController.seekToPrevious();
        });

        binding.expandedPlayer.nextButton.setOnClickListener(v -> {
            mediaController.seekToNext();
        });

        binding.expandedPlayer.repeatModeButton.setOnClickListener(v -> handleRepeatButtonClick());
        binding.expandedPlayer.shuffleModeButton.setOnClickListener(v -> handleShuffleButtonClick());

        binding.expandedPlayer.toggleView.setExtraOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (!binding.expandedPlayer.toggleView.isAnimating()) {
                    mediaController.pause();
                    binding.expandedPlayer.songSeekbar.setAnimate(false);
                } else {
                    mediaController.play();
                    binding.expandedPlayer.songSeekbar.setAnimate(true);
                }
            }
        });

        binding.collapsedPlayer.action.setOnClickListener(v -> {
            if (mediaController.isPlaying()) {
                mediaController.pause();
            } else {
                mediaController.play();
            }
            binding.collapsedPlayer.action.setIconResource(mediaController.isPlaying()? R.drawable.ic_pause : R.drawable.ic_play);
        });

        MaterialFadeThrough transition = new MaterialFadeThrough();
        transition.setDuration(500);
        transition.excludeTarget(binding.bottomNavigation, true);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {

            uiManager.viewModel.saveBNVPosition(activity.getBinding().bottomNavigation.getSelectedItemId());
            if (CallbackInterface.srFrag() == null) return false;
            if (CallbackInterface.srFrag().getSearchViewState() == SearchView.TransitionState.SHOWING) return false;
            int id = item.getItemId();
            CallbackInterface.srFrag().hideSearchView();
            if (id == R.id.menuHomeFragment) {
                TransitionManager.beginDelayedTransition(binding.Coordinator, transition);
                binding.searchCard.setVisibility(View.GONE);
                binding.settingsCard.setVisibility(View.GONE);
                binding.rootCard.setVisibility(View.VISIBLE);
                binding.tabLayout.setEnabled(true);
                binding.tabLayout.setVisibility(View.VISIBLE);

                if (CallbackInterface.srFrag() != null) CallbackInterface.srFrag().freeze(true);
                if (CallbackInterface.sgFrag() != null) CallbackInterface.sgFrag().freeze(true);
                if (CallbackInterface.mlFrag() != null) CallbackInterface.mlFrag().freeze(false);

                return true;
            } else if (id == R.id.menuSearchFragment) {
                TransitionManager.beginDelayedTransition(binding.Coordinator, transition);
                binding.searchCard.setVisibility(View.VISIBLE);
                binding.rootCard.setVisibility(View.GONE);
                binding.tabLayout.setVisibility(View.INVISIBLE);
                binding.tabLayout.setEnabled(false);
                binding.settingsCard.setVisibility(View.GONE);

                if (CallbackInterface.srFrag() != null) CallbackInterface.srFrag().freeze(false);
                if (CallbackInterface.sgFrag() != null) CallbackInterface.sgFrag().freeze(true);
                if (CallbackInterface.mlFrag() != null) CallbackInterface.mlFrag().freeze(true);

                return true;
            } else if (id == R.id.menuSettingsFragment) {
                TransitionManager.beginDelayedTransition(binding.Coordinator, transition);
                binding.searchCard.setVisibility(View.GONE);
                binding.rootCard.setVisibility(View.GONE);
                binding.tabLayout.setVisibility(View.INVISIBLE);
                binding.tabLayout.setEnabled(false);
                binding.settingsCard.setVisibility(View.VISIBLE);

                if (CallbackInterface.srFrag() != null) CallbackInterface.srFrag().freeze(true);
                if (CallbackInterface.sgFrag() != null) CallbackInterface.sgFrag().freeze(false);
                if (CallbackInterface.mlFrag() != null) CallbackInterface.mlFrag().freeze(true);

                return true;
            }

            return false;
        });

        ViewKt.doOnLayout(binding.bottomNavigation, v -> {
            binding.bottomNavigation.setSelectedItemId(uiManager.viewModel.loadBNVPosition());
            uiManager.viewModel.saveBNVPosition(binding.bottomNavigation.getSelectedItemId());

            return Unit.INSTANCE;
        });

        binding.expandedPlayer.lyricsButton.setOnClickListener(v -> {
            boolean checked = binding.expandedPlayer.lyricsButton.isChecked();
            if (binding.lyricsContainer.getVisibility() != View.GONE && !(binding.lyricsContainer.getVisibility() == View.VISIBLE && binding.lyricsContainer.getAlpha() == 1f)) {
                binding.expandedPlayer.lyricsButton.setChecked(!checked);
                return;
            }

            if (checked) {
                binding.miniPlayer.setDraggable(false);
                if (DataManager.getKeepScreenAwakeState()) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                binding.lyricsContainer.animate().alpha(1f).translationY(0f).setDuration(300L).withStartAction(() -> {
                    if (binding.lyricsContainer.getParent() == null) binding.miniPlayer.addView(binding.lyricsContainer);
                    binding.lyricsContainer.setVisibility(View.VISIBLE);
                }).start();
            } else {
                if (DataManager.getKeepScreenAwakeState()) activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                binding.miniPlayer.setDraggable(true);
                binding.lyricsContainer.animate().alpha(0f).translationY(150f).setDuration(300L).withEndAction(() -> {
                    binding.lyricsContainer.setVisibility(View.GONE);
                    if (binding.lyricsContainer.getParent() != null) binding.miniPlayer.removeView(binding.lyricsContainer);
                }).start();
            }

            lyricsCallback.setEnabled(checked);
        });

        binding.expandedPlayer.speedButton.setOnClickListener(v -> showSpeedDialog());
        binding.collapsedPlayer.getRoot().setOnClickListener(v -> binding.miniPlayer.setState(ExpressiveSliderLayout.STATE_EXPANDED));
    }

    public void applySavedPlaybackParameters() {
        if (activity != null && activity.getController() != null) {
            float speed = DataManager.getPlaybackSpeed();
            float pitch = DataManager.getPlaybackPitch();
            boolean locked = DataManager.isSpeedTempoLocked();
            if (locked) {
                pitch = speed;
            }
            activity.getController().setPlaybackParameters(new PlaybackParameters(speed, pitch));
        }
    }

    private void applyPlaybackParameters(float speed, float pitch) {
        if (activity != null && activity.getController() != null) {
            activity.getController().setPlaybackParameters(new PlaybackParameters(speed, pitch));
        }
    }

    private static void setContainerEnabled(ViewGroup container, boolean enabled) {
        container.setEnabled(enabled);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                setContainerEnabled((ViewGroup) child, enabled);
            }
        }
    }

    private static class SpeedTempoDialogState {
        float speed;
        float pitch;
        boolean isLocked;
        boolean isConfirmed;
    }

    @SuppressLint("SetTextI18n")
    private void showSpeedDialog() {
        if (activity == null) return;

        float activeSpeed = (activity.getController() != null) ? activity.getController().getPlaybackParameters().speed : DataManager.getPlaybackSpeed();
        float activePitch = (activity.getController() != null) ? activity.getController().getPlaybackParameters().pitch : DataManager.getPlaybackPitch();
        boolean activeLocked = DataManager.isSpeedTempoLocked();

        final float originalSpeed = activeSpeed;
        final float originalPitch = activePitch;

        SpeedTempoDialogState state = new SpeedTempoDialogState();
        state.speed = Math.max(0.25f, Math.min(4.0f, originalSpeed));
        state.isLocked = activeLocked;
        state.pitch = state.isLocked ? state.speed : Math.max(0.25f, Math.min(4.0f, originalPitch));
        state.isConfirmed = false;

        LayoutSpeedTempoSheetBinding sheetBinding = LayoutSpeedTempoSheetBinding.inflate(activity.getLayoutInflater());
        BottomSheetDialog bs = new BottomSheetDialog(activity);
        bs.getBehavior().setFitToContents(true);
        bs.getBehavior().setSkipCollapsed(true);
        bs.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        bs.setContentView(sheetBinding.getRoot());

        bs.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(true);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        sheetBinding.speedSlider.setValue(state.speed);
        sheetBinding.speedSlider.setLabelBehavior(LABEL_GONE);
        sheetBinding.speedValueText.setText(String.format(Locale.US, "%.2fx", state.speed));

        sheetBinding.tempoSlider.setValue(state.pitch);
        sheetBinding.tempoSlider.setLabelBehavior(LABEL_GONE);
        sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", state.pitch));

        sheetBinding.lockSwitch.setChecked(state.isLocked);
        sheetBinding.tempoSlider.setEnabled(!state.isLocked);
        sheetBinding.tempoPresetsContainer.setAlpha(state.isLocked ? 0.5f : 1.0f);
        setContainerEnabled(sheetBinding.tempoPresetsContainer, !state.isLocked);

        sheetBinding.lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            state.isLocked = isChecked;
            sheetBinding.tempoSlider.setEnabled(!isChecked);
            sheetBinding.tempoPresetsContainer.setAlpha(isChecked ? 0.5f : 1.0f);
            setContainerEnabled(sheetBinding.tempoPresetsContainer, !isChecked);

            if (isChecked) {
                state.pitch = state.speed;
                sheetBinding.tempoSlider.setValue(state.speed);
                sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", state.speed));
                applyPlaybackParameters(state.speed, state.pitch);
            }
        });

        sheetBinding.speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            state.speed = value;
            sheetBinding.speedValueText.setText(String.format(Locale.US, "%.2fx", value));
            if (state.isLocked) {
                state.pitch = value;
                sheetBinding.tempoSlider.setValue(value);
                sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", value));
            }
            if (fromUser) {
                applyPlaybackParameters(state.speed, state.pitch);
            }
        });

        sheetBinding.speedSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                applyPlaybackParameters(state.speed, state.pitch);
            }
        });

        sheetBinding.tempoSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!state.isLocked) {
                state.pitch = value;
                sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", value));
                if (fromUser) {
                    applyPlaybackParameters(state.speed, state.pitch);
                }
            }
        });

        sheetBinding.tempoSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (!state.isLocked) {
                    applyPlaybackParameters(state.speed, state.pitch);
                }
            }
        });

        View.OnClickListener speedPresetListener = v -> {
            float val = 1.0f;
            int id = v.getId();
            if (id == R.id.btn_speed_05) val = 0.5f;
            else if (id == R.id.btn_speed_075) val = 0.75f;
            else if (id == R.id.btn_speed_10) val = 1.0f;
            else if (id == R.id.btn_speed_125) val = 1.25f;
            else if (id == R.id.btn_speed_15) val = 1.5f;
            else if (id == R.id.btn_speed_20) val = 2.0f;

            sheetBinding.speedSlider.setValue(val);
            state.speed = val;
            sheetBinding.speedValueText.setText(String.format(Locale.US, "%.2fx", val));

            if (state.isLocked) {
                state.pitch = val;
                sheetBinding.tempoSlider.setValue(val);
                sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", val));
            }
            applyPlaybackParameters(state.speed, state.pitch);
        };

        sheetBinding.btnSpeed05.setOnClickListener(speedPresetListener);
        sheetBinding.btnSpeed075.setOnClickListener(speedPresetListener);
        sheetBinding.btnSpeed10.setOnClickListener(speedPresetListener);
        sheetBinding.btnSpeed125.setOnClickListener(speedPresetListener);
        sheetBinding.btnSpeed15.setOnClickListener(speedPresetListener);
        sheetBinding.btnSpeed20.setOnClickListener(speedPresetListener);

        View.OnClickListener tempoPresetListener = v -> {
            if (state.isLocked) return;
            float val = 1.0f;
            int id = v.getId();
            if (id == R.id.btn_tempo_05) val = 0.5f;
            else if (id == R.id.btn_tempo_075) val = 0.75f;
            else if (id == R.id.btn_tempo_10) val = 1.0f;
            else if (id == R.id.btn_tempo_125) val = 1.25f;
            else if (id == R.id.btn_tempo_15) val = 1.5f;
            else if (id == R.id.btn_tempo_20) val = 2.0f;

            sheetBinding.tempoSlider.setValue(val);
            state.pitch = val;
            sheetBinding.tempoValueText.setText(String.format(Locale.US, "%.2fx", val));
            applyPlaybackParameters(state.speed, state.pitch);
        };

        sheetBinding.btnTempo05.setOnClickListener(tempoPresetListener);
        sheetBinding.btnTempo075.setOnClickListener(tempoPresetListener);
        sheetBinding.btnTempo10.setOnClickListener(tempoPresetListener);
        sheetBinding.btnTempo125.setOnClickListener(tempoPresetListener);
        sheetBinding.btnTempo15.setOnClickListener(tempoPresetListener);
        sheetBinding.btnTempo20.setOnClickListener(tempoPresetListener);

        sheetBinding.btnCancel.setOnClickListener(v -> bs.dismiss());

        sheetBinding.btnSave.setOnClickListener(v -> {
            state.isConfirmed = true;
            DataManager.setPlaybackSpeed(state.speed);
            DataManager.setPlaybackPitch(state.pitch);
            DataManager.setSpeedTempoLocked(state.isLocked);
            applyPlaybackParameters(state.speed, state.pitch);
            bs.dismiss();
        });

        sheetBinding.lockCard.setOnClickListener(v -> sheetBinding.lockSwitch.toggle());

        bs.setOnDismissListener(dialog -> {
            if (!state.isConfirmed) {
                applyPlaybackParameters(originalSpeed, originalPitch);
            }
        });


        bs.show();
    }

    private void handleShuffleButtonClick() {
        if (activity == null || activity.getController() == null || CallbackInterface.service() == null) return;
        activity.getController().setShuffleModeEnabled(!activity.getController().getShuffleModeEnabled());
        binding.expandedPlayer.shuffleModeButton.setIconResource(activity.getController().getShuffleModeEnabled()? R.drawable.ic_shuffle : R.drawable.ic_shuffle_off);
        binding.expandedPlayer.shuffleModeButton.setChecked(activity.getController().getShuffleModeEnabled());
    }

    private void handleRepeatButtonClick() {
        if (activity == null || activity.getController() == null || CallbackInterface.service() == null) return;
        switch (activity.getController().getRepeatMode()) {
            case Player.REPEAT_MODE_ALL -> {
                activity.getController().setRepeatMode(Player.REPEAT_MODE_ONE);
                binding.expandedPlayer.repeatModeButton.setIconResource(R.drawable.ic_repeat_one);
                binding.expandedPlayer.repeatModeButton.setChecked(true);
            }
            case Player.REPEAT_MODE_ONE -> {
                activity.getController().setRepeatMode(Player.REPEAT_MODE_OFF);
                binding.expandedPlayer.repeatModeButton.setIconResource(R.drawable.ic_repeat_off);
                binding.expandedPlayer.repeatModeButton.setChecked(false);
            }
            case Player.REPEAT_MODE_OFF -> {
                activity.getController().setRepeatMode(Player.REPEAT_MODE_ALL);
                binding.expandedPlayer.repeatModeButton.setIconResource(R.drawable.ic_repeat);
                binding.expandedPlayer.repeatModeButton.setChecked(true);
            }
            default -> {
                throw new IllegalStateException("Player repeat mode:" + activity.getController().getRepeatMode() + " not handled");
            }
        }
    }

    private void setupCallbacks() {
        binding.miniPlayer.setupPredictiveBack(activity);
        binding.miniPlayer.addSliderCallback(new ExpressiveSliderLayout.SliderCallback() {
            @Override
            public void onStateChanged(int state) {
                binding.miniPlayer.getPredictiveBackCallback().setEnabled(!(state == ExpressiveSliderLayout.STATE_COLLAPSED || state == ExpressiveSliderLayout.STATE_HIDDEN));
                if (state == ExpressiveSliderLayout.STATE_HIDDEN) {
                    uiManager.onPlayerHidden();
                    mediaController.stop();
                    mediaController.clearMediaItems();
                } else if (state == ExpressiveSliderLayout.STATE_COLLAPSED) {
                    DataManager.increasePlayerShowCount();
                    if (DataManager.getPlayerSHowCount() >= 5 && !DataManager.isPlayerTipShown()) {
                        new RichTooltip.Builder(activity)
                                .setTitle("Did you know?")
                                .setMessage("You can swipe the mini player the left or the right to seek between tracks")
                                .setIconRes(R.drawable.lightbulb_24px)
                                .setStyle(RichTooltip.TooltipStyle.PRIMARY)
                                .setPrimaryAction("Got it", RichTooltip.ButtonStyle.PRIMARY, null)
                                .setCancelable(false)
                                .build()
                                .show(binding.collapsedPlayer.getRoot());
                        DataManager.markPlayerTipAsShown();
                    }
                } else if (state == ExpressiveSliderLayout.STATE_EXPANDED) {
                    if (DataManager.getPlayerSHowCount() >= 8 && !DataManager.isSeekTipShown()) {
                        new RichTooltip.Builder(activity)
                                .setTitle("Pro tip")
                                .setMessage("Hold on the seek buttons to seek the song forward or backward by 10s")
                                .setIconRes(R.drawable.lightbulb_24px)
                                .setStyle(RichTooltip.TooltipStyle.TERTIARY)
                                .setPrimaryAction("Got it", RichTooltip.ButtonStyle.TERTIARY, null)
                                .setCancelable(false)
                                .build()
                                .show(binding.expandedPlayer.nextButton);
                        DataManager.markSeekTipAsShown();
                    }
                }
            }

            @Override
            public void onSwipe(boolean toRight) {
                if (!toRight) mediaController.seekToNext();
                else mediaController.seekToPrevious();
            }

            @Override
            public void onSlide(float offset) {
                binding.layoutScrim.setAlpha(Math.max(0f, offset) * 0.7f);
                uiManager.updateTopProgress(Math.max(0f, offset));
                updateImageSize(offset);
                binding.collapsedPlayer.cover.setExpansionProgress(Math.max(0f, offset));
            }
        });



        lyricsCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                validCallback = binding.lyricsContainer.getAlpha() == 1f;
            }

            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                if (!validCallback) return;

                binding.lyricsContainer.setAlpha(1f - backEvent.getProgress());
                binding.lyricsContainer.setTranslationY(150f*backEvent.getProgress());
            }

            @Override
            public void handleOnBackPressed() {
                if (!validCallback && XUtils.predictiveBackSupported()) return;
                binding.lyricsContainer.animate().alpha(0f).translationY(150f).setDuration((XUtils.predictiveBackSupported()? 150L : 300L)).withEndAction(() -> {
                    binding.lyricsContainer.setVisibility(View.GONE);
                    if (binding.lyricsContainer.getParent() != null) binding.miniPlayer.removeView(binding.lyricsContainer);
                }).start();
                lyricsCallback.setEnabled(false);
                binding.expandedPlayer.lyricsButton.setChecked(false);
                binding.miniPlayer.setDraggable(true);

                if (DataManager.getKeepScreenAwakeState()) activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            @Override
            public void handleOnBackCancelled() {
                binding.lyricsContainer.animate().alpha(1f).translationY(0f).setDuration(100L).withStartAction(() -> binding.lyricsContainer.setVisibility(View.VISIBLE)).start();
            }
        };

        activity.getOnBackPressedDispatcher().addCallback(activity, lyricsCallback);
    }

    @OptIn(markerClass = UnstableApi.class)
    public void initController(FragmentActivity activity, Consumer<MediaController> onReady, Consumer<Throwable> onError, Runnable onRestore) {

        if (sessionToken == null) {
            sessionToken = new SessionToken(activity, new ComponentName(activity, XPlayerService.class));
        }

        controller = new ActivityMediaController(activity, sessionToken);

        controller.initialize(c -> {
            mediaController = c;
            controller.setupListener((RootActivity) activity);
            applySavedPlaybackParameters();
            onReady.accept(c);
        }, onError::accept, () -> {
            applySavedPlaybackParameters();
            onRestore.run();
        });
    }

    public void playSong(int position) {
        if (mediaController.getPlaybackState() == Player.STATE_BUFFERING) return;

        String songPath = RuntimeData.songs.get(position).path;
        uiManager.loadLyrics(songPath);
        if (!samePlaylistByPath(mediaController, CallbackInterface.service().getMediaItems())) {
            mediaController.setMediaItems(CallbackInterface.service().getMediaItems(), position, 0);
            mediaController.play();
        } else {
            mediaController.seekTo(position, 0);
            mediaController.play();
        }
        binding.expandedPlayer.toggleView.forcePlayState();
    }

    private static boolean samePlaylistByPath(MediaController controller, List<MediaItem> serviceItems) {
        int count = controller.getMediaItemCount();
        if (count != serviceItems.size()) return false;

        for (int i = 0; i < count; i++) {
            MediaItem cItem = controller.getMediaItemAt(i);
            MediaItem sItem = serviceItems.get(i);

            assert cItem.localConfiguration != null;
            String cPath = cItem.localConfiguration.uri.getPath();
            assert sItem.localConfiguration != null;
            String sPath = sItem.localConfiguration.uri.getPath();

            if (!Objects.equals(cPath, sPath)) return false;
        }
        return true;
    }

    public void updateVumeters(boolean isPlaying) {
        activity.runOnUiThread(() -> {
            if (CallbackInterface.mlFrag() != null) CallbackInterface.mlFrag().updateVumeter(isPlaying);
            if (CallbackInterface.srFrag() != null) CallbackInterface.srFrag().updateVumeter(isPlaying);
        });
    }

    private void updateImageSize(float offset) {
        float clampedOffset = Math.max(0f, offset);
    }

    public void handleProgress(long progress) {
        if (true) activity.runOnUiThread(() -> updateProgress(progress));
        activity.runOnUiThread(() -> binding.lyricsView.updateLyricsProgress((int) progress, false));
    }

    public void updateProgress(long position) {
        binding.collapsedPlayer.musicProgress.setProgressCompat((int) position, true);
        if (!isUserSeeking) binding.expandedPlayer.songSeekbar.setProgress((int) position, false);
    }
}
