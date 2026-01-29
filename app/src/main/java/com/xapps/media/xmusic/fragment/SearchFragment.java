package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.FragmentSearchBinding;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.utils.XUtils;

public class SearchFragment extends BaseFragment {
    
    private FragmentSearchBinding binding;
    private ActivityMainBinding activityBinding;
    
    @NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentSearchBinding.inflate(inflater, container, false);
		initializeLogic();
        setupListeners();
        setupInsets();
		return binding.getRoot();
	}

    private void initializeLogic() {
        activityBinding = ((MainActivity) getActivity()).getBinding();
    }

    private void setupListeners() {
        
    }

    private void setupInsets() {
        XUtils.setMargins(binding.searchBar, 0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
    }
}
