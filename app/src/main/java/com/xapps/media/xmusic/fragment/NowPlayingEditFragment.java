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
            "Components","", 
            null
    ));

    items.add(new SettingsItem(
            SettingsItem.TYPE_NAV,
            "Seekbar",
            "Customize your seekbar with a set of tweaks",
            null
    ));

    items.add(new SettingsItem(
            SettingsItem.TYPE_NAV,
            "Play/Pause toggle",
            "Customize states shapes and animation speed",
            new AppearanceFragment()
    ));

    return items;
}

@Override
protected void onNavigate(SettingsItem item) {
    if (item.destinationFragment == null) return;

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
