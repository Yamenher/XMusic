package com.xapps.media.xmusic.service;

import android.content.Context;
import androidx.media3.session.DefaultMediaNotificationProvider;

public class CustomNotificationProvider extends DefaultMediaNotificationProvider {
    private Context c;

    public CustomNotificationProvider(Context context) {
        super(context);
        this.c = context;
    }
}



