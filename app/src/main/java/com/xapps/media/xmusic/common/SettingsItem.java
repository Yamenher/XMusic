package com.xapps.media.xmusic.common;
import androidx.fragment.app.Fragment;

public class SettingsItem {
    
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NAV = 1;
    public static final int TYPE_SWITCH = 2;

    public int type;
    public String title;
    public String id;
    public String description;
    public Fragment destinationFragment;

    public SettingsItem(int itemType, String id, String title, String desc, Fragment frag) {
        this.id = id;
        this.type = itemType;
        this.title = title;
        this.description = desc;
        this.destinationFragment = frag;
    }
}
