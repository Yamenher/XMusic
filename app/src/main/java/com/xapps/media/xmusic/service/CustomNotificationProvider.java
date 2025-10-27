package com.xapps.media.xmusic.service;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.Player;
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
    
    @Override
    public int[] addNotificationActions(MediaSession mediaSession, ImmutableList<CommandButton> mediaButtons, NotificationCompat.Builder builder, MediaNotification.ActionFactory actionFactory) {
        CommandButton cb = new CommandButton.Builder(R.drawable.ic_shuffle).setDisplayName("negro").setSessionCommand(new SessionCommand("action_nigga", Bundle.EMPTY)).setSlots(CommandButton.SLOT_BACK_SECONDARY).build();
        ImmutableList<CommandButton> l = ImmutableList.of(cb);
        NotificationCompat.Action action = actionFactory.createCustomAction(mediaSession, IconCompat.createWithResource(c, R.drawable.ic_shuffle), "test", "test", Bundle.EMPTY);
        builder.addAction(action);
        int[] i = super.addNotificationActions(mediaSession, l, builder, actionFactory);
        return i;
    } 
    
    @Override
    public ImmutableList<CommandButton> getMediaButtons(MediaSession mediaSession, Player.Commands commands, ImmutableList<CommandButton> mediaButtonPreferences, boolean z) {
        CommandButton cb = new CommandButton.Builder(R.drawable.ic_shuffle).setDisplayName("negro").setSessionCommand(new SessionCommand("action_nigga", Bundle.EMPTY)).setSlots(CommandButton.SLOT_BACK_SECONDARY).build();
        ImmutableList<CommandButton> l = ImmutableList.of(cb);
        return l;
    }

    private PendingIntent getSessionActivityPendingIntent(MediaSession session) {
        return session.getSessionActivity();
    }
}