package com.xapps.media.xmusic.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.transition.MaterialContainerTransform;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.BuildConfig;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.FragmentAboutBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import java.util.Random;

public class AboutFragment extends BaseFragment {
    private FragmentAboutBinding binding;
    private MainActivity activity;
    
    private static final String[] ABOUT_QUOTES = {
        "This app exists because why not :)",
        "Built one bug at a time",
        "Works on my device",
        "Still faster than rewriting it in Compose",
        "If you’re reading this, the app didn’t crash",
        "Another day, another workaround.",
        "You weren’t supposed to be here this often.",
        "Yes, this is intentional.",
        "'It’s stable' Emotionally? No.",
        "This screen does nothing productive.",
        "An About screen that knows it’s an About screen.",
        "This app has opinions.",
        "Thanks for using the app. Seriously.",
        "Curiosity has consequences"
    };
    
    @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentAboutBinding.inflate(inflater, container, false);
        activity = (MainActivity) getActivity();
        init();
        return binding.getRoot();
	}

    private void init() {
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        int i = new Random().nextInt(ABOUT_QUOTES.length);
        binding.randomNote.setText(ABOUT_QUOTES[i]);

        binding.appbar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float progress = Math.abs(verticalOffset) / (float) totalScrollRange;
            progress = Math.min(1f, Math.max(0f, progress));
            binding.toolbarText.setAlpha(progress);

            binding.collapsingtoolbar.setScrimVisibleHeightTrigger(binding.collapsingtoolbar.getHeight());
            binding.collapsingtoolbar.setScrimAnimationDuration(0);
        });

        binding.versionText.setText(BuildConfig.VERSION_NAME);
        binding.buildText.setText(BuildConfig.BUILD_TYPE);
        
        
        
        
        /*MaterialContainerTransform exitTransform = new MaterialContainerTransform();
        exitTransform.setDuration(350);
        exitTransform.setScrimColor(Color.TRANSPARENT);
        exitTransform.setDrawingViewId(R.id.settings_frag);
        setExitTransition(exitTransform);*/
        
    }

    private void setupListeners() {
        binding.thirdItem.setOnClickListener(v -> {
            activity.showInfoDialog("Build Flavors", R.drawable.ic_info_outline, "XMusic has 3 different Build Flavors : release, debug, and preview.\n\n• Debug builds are the biggest in size and usually full of logging and debug stuff, that's why it's noticeably slower and dosen't reflect real app performance.\n\n• Preview builds are significantly smaller in size than debug builds, they are stripped from most of debug logic but not obfuscated, they should be much smoother and performant.\n\n• Release builds are the smallest in size and they're highly optimized and obfuscated, you'll usually be able to get this only from GitHub releases (when I make one :P).", "Got it");
        });
        binding.secondItem.setOnClickListener(v -> {
            activity.showInfoDialog("Release types", R.drawable.ic_info_outline, "XMusic has 3 different Build Flavors : Alpha, Beta, and Stable.\n\n• Alpha : Experimental builds with unfinished features.\nExpect bugs, crashes, and frequent changes.\n\n• Beta : Mostly stable with new features still being tested.\nMinor bugs and performance issues may occur.\n\n• Stable : Almost fully tested and optimized for daily use.\nBest performance and reliability.", "Got it");
        });
        binding.appLogo.setOnClickListener(v -> {
            //activity.HideBNV(true);
            //openLimbo();
        });
    }

    private void openLimbo() {
        requireActivity()
        .getSupportFragmentManager()
        .beginTransaction()
        .setReorderingAllowed(true)
        .addSharedElement(binding.appLogo, "shared_app_icon")
        .replace(R.id.settings_frag, new LimboFragment())
        .addToBackStack(null)
        .commit();
    }
}
