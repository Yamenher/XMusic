package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.common.SettingsItem;
import java.util.ArrayList;
import java.util.List;

public class NowPlayingEditFragment extends BasePrefsFragment {
    
    @Override
    protected List<SettingsItem> provideItems() {
        List<SettingsItem> items = new ArrayList<>();

        items.add(new SettingsItem(
            SettingsItem.TYPE_HEADER,
            "",
            "Components","", 
            null
        ));
        
    

        /*items.add(new SettingsItem(
            SettingsItem.TYPE_NAV,
            "seekbar_customize",
            "Seekbar",
            "Customize your seekbar with a set of tweaks",
            null
        ));

        items.add(new SettingsItem(
            SettingsItem.TYPE_NAV,
            "toggle_customize",
            "Play/Pause toggle",
            "Customize states shapes and animation speed",
            new AppearanceFragment()
        ));*/
        
        items.add(new SettingsItem(
            SettingsItem.TYPE_SWITCH,
            "stable_colors",
            "Use Dynamic Colors for Now Playing UI",
            "All components in the Now Playing interface will use App Colors instead of Album Art's",
            null
        ));

        return items;
    }

    @Override
    protected void onNavigate(SettingsItem item) {

        try {
        Fragment f = item.destinationFragment;
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_frag, f)
                .addToBackStack(null)
                .commit();
        } catch (Exception ignored) {}
    }
}
