package com.xapps.media.xmusic.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialContainerTransform;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.databinding.FragmentLimboBinding;

public class LimboFragment extends Fragment {

    private FragmentLimboBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialContainerTransform enterTransform = new MaterialContainerTransform();
        enterTransform.setDuration(350);
        enterTransform.setScrimColor(Color.TRANSPARENT);
        enterTransform.setFadeMode(MaterialContainerTransform.FADE_MODE_OUT);
        enterTransform.setDrawingViewId(R.id.settings_frag);

        setSharedElementEnterTransition(enterTransform);

        MaterialContainerTransform returnTransform = new MaterialContainerTransform();
        returnTransform.setDuration(300);
        returnTransform.setScrimColor(Color.TRANSPARENT);
        returnTransform.setDrawingViewId(R.id.settings_frag);

        setSharedElementReturnTransition(returnTransform);

        postponeEnterTransition();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLimboBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        view.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}