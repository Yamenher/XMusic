package com.xapps.media.xmusic.data;
import com.xapps.media.xmusic.common.SettingsItem;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.fragment.AppearanceFragment;
import java.util.ArrayList;
import java.util.List;

public final class SettingsData {
    public static List<SettingsItem> getNPEItems() {
        ArrayList<SettingsItem> list = new ArrayList<>();
        list.add(new SettingsItem(SettingsItem.TYPE_HEADER, "Components", "", null));
        list.add(new SettingsItem(SettingsItem.TYPE_NAV, "Adjust Seekbar", "Adjust Seekbar line and thumb to your needs", new AppearanceFragment()));
        return list;
    }
}
