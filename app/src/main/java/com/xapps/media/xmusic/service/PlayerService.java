package com.xapps.media.xmusic.service;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.media.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.*;
import androidx.media3.common.*;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSession.*;
import androidx.media3.session.MediaStyleNotificationHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.collect.ImmutableList;
import com.xapps.media.xmusic.*;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class PlayerService extends Service {
	
	private static final String CHANNEL_ID = "music_channel";  
	private static final int NOTIFICATION_ID = 1;  
	private static final String ACTION_PLAY = "ACTION_PLAY";  
	
	private ExoPlayer player;  
	private Handler handler;  
	private Runnable updateProgressRunnable;  
    private final Context c = this;
	
	private Bitmap icon;  
	private boolean isPlaying;  
	private int currentState;
	public static String currentTitle;  
	public static String currentArtist;  
	public static String currentCover;
	public static Bitmap currentArt;
	public static int lastProgress = 0;
	public static int lastMax = 0;
    private int currentPosition = 0;
    private boolean isBuilt = false;
    private boolean isNotifDead = true;
    private byte tb[];
    private MediaMetadata mt;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService executor2 = Executors.newSingleThreadScheduledExecutor();
    private boolean isExecutorStarted = false;
    private Bitmap current;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private ScheduledFuture<?> currentTask;
	private MediaSession mediaSession;  
    private List<MediaItem> mediaItems;
    public static boolean isReceiving = false;
    HandlerThread handlerThread = new HandlerThread("ExoPlayerThread");
	private Handler ExoPlayerHandler;
	private android.media.AudioManager audioManager;
    private android.media.AudioFocusRequest audioFocusRequest;
    private boolean wasPausedDueToFocus = false;

	@Override 
	public void onCreate() {  
		super.onCreate();  
        handlerThread.start();
        Looper backgroundLooper = handlerThread.getLooper();
		ExoPlayerHandler = new Handler(backgroundLooper);
		player = new ExoPlayer.Builder(this).setLooper(backgroundLooper).build();
		handler = new Handler(Looper.getMainLooper());  
        if (!isBuilt) {
		    setupMediaSession();
            isBuilt = true;
        }
        isNotifDead = false;
		createNotificationChannel();  
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setupAudioFocusRequest();
	}  
	
	@Nullable  
	@Override  
	public IBinder onBind(Intent intent) {  
		return null;  
	}  
	
	@Override  
	public int onStartCommand(Intent intent, int flags, int startId) {  
		if (intent != null && intent.getAction() != null ) {  
			if (intent.getAction().equals("ACTION_PLAY")) {  
				isPlaying = true;  
				String uri = intent.getStringExtra("uri");  
				String title = intent.getStringExtra("title");  
				String artist = intent.getStringExtra("artist");  
                int position = intent.getIntExtra("position", 0);
				String coverUri = intent.getStringExtra("cover");  
                currentPosition = position;
				if (uri != null) {   
					requestAudioFocus(uri, title, artist, coverUri, position);
				}  
			} else if (intent.getAction().equals("ACTION_NEXT")) {
                ExoPlayerHandler.post(() -> {
				    if (player != null && currentPosition+1 >= 0 && currentPosition+1 <= mediaItems.size()) {  
					    player.seekTo(currentPosition+1, 0);
				    }
                });
            } else if (intent.getAction().equals("ACTION_PREVIOUS")) {   
                ExoPlayerHandler.post(() -> {
				    if (player != null && currentPosition-1 >= 0 && currentPosition-1 <= mediaItems.size()) {  
					    player.seekTo(currentPosition-1, 0);
				    }
                });
            } else if (intent.getAction().equals("ACTION_STOP")) {  
				ExoPlayerHandler.post(() -> {
				    if (player != null && player.isPlaying()) {  
					    player.stop();  
					    player.clearMediaItems();
				    }  
				});
				if (mediaSession != null) {
                        mediaSession.release();
                        mediaSession = null;
                }
				stopForeground(true);
			} else if (intent.getAction().equals("ACTION_PAUSE")) {  
				isPlaying = false;  
                ExoPlayerHandler.post(() -> {
				    if (player != null && player.isPlaying()) {  
					    player.pause();  
				    }
                });
			} else if (intent.getAction().equals("ACTION_RESUME")) {  
				isPlaying = true;  
				ExoPlayerHandler.post(() -> {
				    if (player != null && player.getPlaybackState() == ExoPlayer.STATE_READY && !player.isPlaying()) {  
					    player.play();
				    }  
				});
			} else if (intent.getAction().equals("ACTION_SEEK")) {
                int position = intent.getIntExtra("progress", 0);
				ExoPlayerHandler.post(() -> {
                    player.seekTo(position);
				});
            } else if (intent.getAction().equals("ACTION_UPDATE")) {
                Log.i("Info", "Requested songs update");
                mediaItems = new ArrayList<>();
                ArrayList<HashMap<String, Object>> song = MainActivity.currentMap;
                    executor.execute(() -> {
                for (int i = 0; i < song.size(); i++) {
                    if (song.get(i).get("thumbnail") != null) {
                        try {
                            mt = new MediaMetadata.Builder().setTitle(song.get(i).get("title").toString()).setArtist(song.get(i).get("author").toString()).setArtworkUri(Uri.parse("file://"+song.get(i).get("thumbnail").toString())).build();
                            final int tmp_i = i;
                                Glide.with(c)
                                .load(Uri.parse("file://"+ song.get(tmp_i).get("thumbnail").toString()))
                                .apply(new RequestOptions()
                                .centerCrop()
                                .override(500, 500)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false))
                                .preload();
                        } catch (Exception e) { }
                    } else {
                        mt = new MediaMetadata.Builder().setTitle(song.get(i).get("title").toString()).setArtist(song.get(i).get("author").toString()).setArtworkData(tb).build();
                    }
                    String path = song.get(i).get("path").toString();
                    Uri uri2 = Uri.fromFile(new File(path));
                    MediaItem mediaItem = new MediaItem.Builder().setMediaMetadata(mt).setUri(uri2).build();
                    mediaItems.add(mediaItem);
                }
                });
            }
		}     
		return START_STICKY;  
	} 

    private void playMedia(String uri, String title, String artist, String coverUri, int position) {
		if (Build.VERSION.SDK_INT >= 33) {  
			startForeground(NOTIFICATION_ID, buildNotification("XMusic", "No song is playing", ""), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);  
		} else {  
			startForeground(NOTIFICATION_ID, buildNotification("XMusic", "No song is playing", ""));  
		}  
        updateNotification(title, artist, coverUri);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executor.execute(() -> {
            Bitmap transparentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.transparent); 
            tb = toByteArray(transparentBitmap);
            Bitmap bmp = loadBitmapFromPath(MainActivity.currentMap.get(position).get("thumbnail").toString());
            ColorPaletteUtils.generateFromBitmap(bmp, (light, dark) -> {
                Intent progressIntent = new Intent("PLAYER_COLORS");
                sendBroadcast(progressIntent);
            });
        });
        if (mediaItems == null) {
            BasicUtil.showMessage(getApplicationContext(), "fuck");
        } else {
			ExoPlayerHandler.post(() -> {
				player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setMediaItems(mediaItems);
                player.seekTo(position, 0);
                player.prepare();
                player.play();
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) {
                            long duration = player.getDuration();
                            lastMax = (int) duration;
                            startProgressUpdates();
                        }
                    }
                    @Override
                    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                        if (mediaItem != null) {
                            updateNotification(currentTitle, currentArtist, currentCover);
                        }
                    }
                });
		    });
        }
        currentTitle = title;
        currentArtist = artist;
        currentCover = coverUri;
    }
    
    private Bitmap loadBitmapFromPath(String uri) {
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(Uri.parse("file://"+uri));
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
        }
    }
    
    private byte[] toByteArray(Bitmap bitmap) {
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
    
    public void cancelProgressUpdates() {
        if (isExecutorStarted) {
            if (currentTask != null && !currentTask.isCancelled()) {
                currentTask.cancel(true);
            }
        }
    }

    private void startProgressUpdates() {
        if (isExecutorStarted) {
            if (currentTask != null && !currentTask.isCancelled()) {
                currentTask.cancel(true);
            }
        }

        currentTask = executor2.scheduleAtFixedRate(() -> {
            ExoPlayerHandler.post(() -> {
                if (player != null && player.isPlaying()) {
                    long pos = player.getCurrentPosition();
                    lastProgress = (int) pos;
                    Intent progressIntent = new Intent("PLAYER_PROGRESS");
                    progressIntent.putExtra("progress", (int) pos);
                    sendBroadcast(progressIntent);
                }
            });
        }, 0, 100, java.util.concurrent.TimeUnit.MILLISECONDS);

        isExecutorStarted = true;
    }
	
    
	private Notification buildNotification(String title, String artist, String cover) {
        if (mediaSession == null) {
            setupMediaSession();
        }
        MediaStyleNotificationHelper.MediaStyle mediaStyle = new MediaStyleNotificationHelper.MediaStyle(mediaSession)
        .setShowActionsInCompactView(0, 1, 2);
        Intent resumeIntent = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE);
	    int iconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
	    String actionText = isPlaying ? "Pause" : "Play";
	    String actionIntent = isPlaying ? "ACTION_PAUSE" : "ACTION_RESUME";
        return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setSmallIcon(android.R.drawable.ic_media_play)
		.setContentTitle(title)
		.setContentText(artist)
		.setLargeIcon(current)
		.addAction(new NotificationCompat.Action(iconRes, actionText, getServiceIntent(this, actionIntent)))
		.addAction(new NotificationCompat.Action(R.drawable.icon_prev, "Previous", getServiceIntent(this, "ACTION_PREVIOUS")))
		.addAction(new NotificationCompat.Action(R.drawable.ic_next, "Next", getServiceIntent(this, "ACTION_NEXT")))
		.setStyle(mediaStyle)
		.setOngoing(true)
        .setContentIntent(contentIntent)
		.setOnlyAlertOnce(true)
		.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		.build();
    }
    
    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    public void loadFromUri(String path, BitmapCallback callback) {
        executor.execute(() -> {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onBitmapLoaded(bmp);
            });
        });
    }
	
	private void createNotificationChannel() {  
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {  
			NotificationChannel channel = new NotificationChannel(  
			CHANNEL_ID,  
			"Music Playback",  
			NotificationManager.IMPORTANCE_LOW  
			);  
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
			if (manager != null) {  
				manager.createNotificationChannel(channel);  
			}  
		}  
	}  
	
	@Override  
	public void onDestroy() {  
		if (handler != null && updateProgressRunnable != null) handler.removeCallbacks(updateProgressRunnable);  
		ExoPlayerHandler.post(() -> {
            if (player != null) {
                player.release();
                player = null;
            }
	    });
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
        super.onDestroy();  
		abandonAudioFocus();
	}  
	
	private void updateNotification(String title, String artist, String cover) {
		Notification notification = buildNotification(title, artist, cover);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
			manager.notify(NOTIFICATION_ID, notification);
		}
	}
	
	private PendingIntent getServiceIntent(Context context, String action) {  
		Intent intent = new Intent(context, PlayerService.class);  
		intent.setAction(action);  
		return PendingIntent.getService(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);  
	}

    public void loadBitmapFromUri(Uri uri, Consumer<Bitmap> callback) {
        executor.execute(() -> {
            Bitmap result = null;
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);
                result = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {}
                }
            }
            Bitmap finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.accept(finalResult);
            });
        });

    }
	
	public static Bitmap cropToWide(Bitmap source) {
		if (source == null) throw new IllegalArgumentException("Bitmap is null, genius.");
		final float TARGET_RATIO = 2f;
		int srcWidth = source.getWidth();
		int srcHeight = source.getHeight();
		float srcRatio = (float) srcWidth / srcHeight;
		if (Math.abs(srcRatio - TARGET_RATIO) < 0.01f) {
			return source;
		}
		int cropWidth, cropHeight;
		if (srcRatio > TARGET_RATIO) {
			cropHeight = srcHeight;
			cropWidth = Math.round(TARGET_RATIO * cropHeight);
		} else {
			cropWidth = srcWidth;
			cropHeight = Math.round(cropWidth / TARGET_RATIO);
		}
		int x = Math.max((srcWidth - cropWidth) / 2, 0);
		int y = Math.max((srcHeight - cropHeight) / 2, 0);
		return Bitmap.createBitmap(source, x, y, cropWidth, cropHeight);
	}
	
	private void setupMediaSession() {
		if (mediaSession != null) {
			return;
		}
		CommandButton loopButton = new CommandButton.Builder()
        .setDisplayName("Loop")
        .setIconResId(R.drawable.ic_music_outlined)
        .setSessionCommand(new SessionCommand("CUSTOM_LOOP", Bundle.EMPTY))
        .build();
        CommandButton favButton = new CommandButton.Builder()
        .setDisplayName("Favorite")
        .setIconResId(R.drawable.ic_playlists_outlined)
        .setSessionCommand(new SessionCommand("CUSTOM_FAVORITE", Bundle.EMPTY))
        .build();
        mediaSession = new MediaSession.Builder(this, player).setId("XMusicMediaSessionPrivate").setCustomLayout(ImmutableList.of(loopButton, favButton)).build();
    }



private void setupAudioFocusRequest() {
    android.media.AudioFocusRequest.Builder builder = new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener(this::handleAudioFocusChange)
        .setAudioAttributes(new android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());

    audioFocusRequest = builder.build();
}

private void handleAudioFocusChange(int focusChange) {
    switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
		    ExoPlayerHandler.post(() -> {
				player.setVolume(1.0f);
                if (wasPausedDueToFocus) {
                    player.play();
                    wasPausedDueToFocus = false;
                }
			});
            break;
        case AudioManager.AUDIOFOCUS_LOSS:
		    ExoPlayerHandler.post(() -> {
                if (player.isPlaying()) {
                    player.pause();
                    wasPausedDueToFocus = true;
                }
			});
            break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		    ExoPlayerHandler.post(() -> {
                if (player.isPlaying()) {
                    player.pause();
                    wasPausedDueToFocus = true;
                }
			});
            break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
		    ExoPlayerHandler.post(() -> {
                if (player.isPlaying()) {
                    player.setVolume(0.2f);
                }
			});
			break;
    }
}

    public void requestAudioFocus(String uri, String title, String artist, String coverUri, int position) {
        int result = audioManager.requestAudioFocus(audioFocusRequest);
        if (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            playMedia(uri, title, artist, coverUri, position);   
        } else if (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			BasicUtil.showMessage(getApplicationContext(), "Unable to play songs at the moment");
		}
    }

    public void abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }
}
