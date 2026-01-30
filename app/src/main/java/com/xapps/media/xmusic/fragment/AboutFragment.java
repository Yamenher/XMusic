package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xapps.media.xmusic.BuildConfig;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.FragmentAboutBinding;
import com.xapps.media.xmusic.utils.XUtils;

public class AboutFragment extends BaseFragment {
    private FragmentAboutBinding binding;
    private MainActivity activity;
    
    private static final String[] ABOUT_QUOTES = {
        "This app exists because I am unemployed",
        "Built one bug at a time",
        "Works on my device",
        "Still faster than rewriting it in Compose",
        "If you’re reading this, the app didn’t crash",
        "Some assembly required. Emotionally.",
        "Another day, another workaround.",
        "Powered by caffeine and bad decisions.",
        "You weren’t supposed to be here this often.",
        "Yes, this is intentional.",
        "Touching things again, I see.",
        "It’s stable. Emotionally? No.",
        "This screen does nothing productive.",
        "An About screen that knows it’s an About screen.",
        "Information density: low. Vibes: acceptable.",
        "This app has opinions.",
        "Minimal effort, maximum overthinking.",
        "Designed. Debated. Shipped.",
        "Thanks for using the app. Seriously.",
        "Made with care. Debugged with spite.",
        "Don’t tap the logo too much",
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
    }

    private void setupUI() {
        binding.appbar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
    binding.collapsingtoolbar.setScrimVisibleHeightTrigger(binding.collapsingtoolbar.getHeight());
    binding.collapsingtoolbar.setScrimAnimationDuration(0);
});

binding.versionText.setText(BuildConfig.VERSION_NAME);
        binding.buildText.setText(BuildConfig.BUILD_TYPE);
    }
}
