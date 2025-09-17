package com.xapps.media.xmusic;
import android.os.*;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.xapps.media.xmusic.databinding.SettingsBinding;

public class settingsFragment extends Fragment {
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
    }

    private void setupListeners() {
        
    }
}
