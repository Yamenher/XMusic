package com.xapps.media.xmusic.fragment;
import android.os.*;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.xapps.media.xmusic.R;
import dev.trindadedev.fastui.preferences.withicon.Preference;
import com.xapps.media.xmusic.databinding.SettingsBinding;

import java.util.Objects;

public class SettingsFragment extends BaseFragment {
    private SettingsBinding binding; 
        
    @NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = SettingsBinding.inflate(inflater, container, false);
		initializeLogic();
		return binding.getRoot();
	}

    private void initializeLogic() {

        setupListeners();

        setupItems();
    }

    private void setupItems() {
        binding.linear1.addView(appearanceSetting());
        binding.linear1.addView(placeHolderSetting());
        binding.linear1.addView(placeHolder2Setting());
        binding.linear1.addView(placeHolder3Setting());
    }

    private Preference appearanceSetting() {
        Preference pref = new Preference(requireContext());
        pref.setIcon(R.drawable.ic_palette);
        pref.setTitle("Appearance");
        pref.setDescription("Customize XMusic UI");
        pref.setBackgroundType(0);
        return pref;


    }

    private Preference placeHolderSetting() {
        Preference prefs = new Preference(requireContext());
        prefs.setIcon(R.drawable.ic_android);
        prefs.setTitle("PlaceHolder Title");
        prefs.setDescription("PlaceHolder Description");
        prefs.setBackgroundType(1);
        return prefs;


    }

    private Preference placeHolder2Setting() {
        Preference prefs = new Preference(requireContext());
        prefs.setIcon(R.drawable.ic_android);
        prefs.setTitle("PlaceHolder2 Title");
        prefs.setDescription("PlaceHolder2 Description");
        prefs.setBackgroundType(1);
        return prefs;


    }

    private Preference placeHolder3Setting() {
        Preference prefs = new Preference(requireContext());
        prefs.setIcon(R.drawable.ic_android);
        prefs.setTitle("PlaceHolder3 Title");
        prefs.setDescription("PlaceHolder3 Description");
        prefs.setBackgroundType(2);
        return prefs;


    }

    private void setupListeners() {
        
    }
}
