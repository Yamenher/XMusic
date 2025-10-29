package com.xapps.media.xmusic.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.Player;
import androidx.media3.common.util.Assertions;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import com.xapps.media.xmusic.R;
import com.google.common.collect.ImmutableList;

public class CustomNotificationProvider extends DefaultMediaNotificationProvider {

    private Context c;
    public CustomNotificationProvider(Context context) {
        super(context);
        c = context;
    }
}