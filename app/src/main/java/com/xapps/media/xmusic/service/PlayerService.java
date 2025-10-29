package com.xapps.media.xmusic.service;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.media.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.service.controls.actions.CommandAction;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.*;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSession.*;
import androidx.media3.session.MediaStyleNotificationHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.collect.ImmutableList;
import com.xapps.media.xmusic.activity.*;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;

public class PlayerService extends MediaSessionService {
	
	private static final String CHANNEL_ID = "music_channel";  
	private static final int NOTIFICATION_ID = 1;  
	public static ExoPlayer player;   
	private Handler handler;  
	private Runnable updateProgressRunnable;  
    private final Context c = this;
	
	private Bitmap icon;  
	public static boolean isPlaying;  
	private int currentState;
	public static String currentTitle;  
	public static String currentArtist;  
	public static String currentCover;
	public static Bitmap currentArt;
	public static int lastProgress = 0;
	public static int lastMax = 0;
    public static int currentPosition = 0;
    private boolean isBuilt = false;
    private boolean isNotifDead = true;
    public static boolean isColorChanged = false;
    private byte tb[];
    private MediaMetadata mt;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService executor2 = Executors.newSingleThreadScheduledExecutor();
    private boolean isExecutorStarted = false;
    private Bitmap current;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
	private ScheduledFuture<?> currentTask;
	private MediaSession mediaSession;  
    private List<MediaItem> mediaItems;
    public static boolean isReceiving = false;
    HandlerThread handlerThread = new HandlerThread("ExoPlayerThread");
	private static Handler ExoPlayerHandler;
	private android.media.AudioManager audioManager;
    private android.media.AudioFocusRequest audioFocusRequest;
    private boolean wasPausedDueToFocus = false;
    
    private final IBinder binder = new LocalBinder();
    private Callback callback;
    
    private int resId = R.drawable.placeholder;
    private Uri fallbackUri;
    
    Player.Commands playerCommands = new Player.Commands.Builder().addAllCommands().build();
    
    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull ControllerInfo controllerInfo) {
        return mediaSession; 
    }
    
    @Override
    public void onTaskRemoved(Intent i) {
        XUtils.showMessage(this, "okay?");
        ExoPlayerHandler.post(() -> {
            if (!player.isPlaying()) stopSelf();
        });
    }

	@Override 
	public void onCreate() {  
		super.onCreate();  
        CustomNotificationProvider cnp = new CustomNotificationProvider(this);
        cnp.setSmallIcon(R.drawable.service_icon);
        setMediaNotificationProvider(cnp);
        handlerThread.start();
        Looper backgroundLooper = handlerThread.getLooper();
		ExoPlayerHandler = new Handler(backgroundLooper);
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
		player = new ExoPlayer.Builder(this, renderersFactory).setLooper(backgroundLooper).build();
        if (!isBuilt) {
		    setupMediaSession();
            isBuilt = true;
        }
        fallbackUri = Uri.parse("android.resource://" + this.getPackageName() + "/" + resId);
        androidx.media3.common.AudioAttributes attrs = new androidx.media3.common.AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build();
        ExoPlayerHandler.post(() -> {
            player.setAudioAttributes(attrs, true);
        });
		handler = new Handler(Looper.getMainLooper());  
        isNotifDead = false;
		createNotificationChannel();  
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setupAudioFocusRequest();
	}  
	
	@Nullable  
	@Override  
	public IBinder onBind(Intent intent) {  
        if (intent.getAction() == MediaSessionService.SERVICE_INTERFACE) {
            return super.onBind(intent);
        }
		return binder;
	}  
	
	@Override  
	public int onStartCommand(Intent intent, int flags, int startId) {  
		if (intent != null && intent.getAction() != null ) {  
            if (Build.VERSION.SDK_INT >= 33) {  
                startForeground(NOTIFICATION_ID, buildNotification("XMusic", "No song is playing", "") , ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);  
            } else {  
                startForeground(NOTIFICATION_ID, buildNotification("XMusic", "No song is playing", ""));  
            }  
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
            } else if (intent.getAction().equals("ACTION_STOP")) {  
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
				ExoPlayerHandler.post(() -> {
				    if (player != null && player.isPlaying()) {  
					    player.stop();  
					    player.clearMediaItems();
				    }  
				});
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
                mediaItems = new ArrayList<>();
                ArrayList<HashMap<String, Object>> song = MainActivity.currentMap;
                executor.execute(() -> {
                    for (int i = 0; i < song.size(); i++) {
                        if (song.get(i).get("thumbnail") != null) {
                            mt = new MediaMetadata.Builder().setTitle(song.get(i).get("title").toString()).setArtist(song.get(i).get("author").toString()).setArtworkUri(Uri.parse("file://"+song.get(i).get("thumbnail").toString())).build();
                        } else {
                            mt = new MediaMetadata.Builder().setTitle(song.get(i).get("title").toString()).setArtist(song.get(i).get("author").toString()).setArtworkUri(fallbackUri).build();
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
        ExoPlayerHandler.post(() -> {
            if (player.isPlaying()) {
                player.pause();
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executor.execute(() -> {
            isColorChanged = true;
            Bitmap transparentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.transparent); 
            tb = toByteArray(transparentBitmap);
            Bitmap bmp = MainActivity.currentMap.get(position).get("thumbnail") == null? transparentBitmap : loadBitmapFromPath(MainActivity.currentMap.get(position).get("thumbnail").toString());
            ColorPaletteUtils.generateFromBitmap(bmp, (light, dark) -> {
                sendDataToActivity("colors");
            });
        });
        if (mediaItems == null) {
            XUtils.showMessage(getApplicationContext(), "no songs were found on this device");
        } else {
			ExoPlayerHandler.post(() -> {
                player.setPlayWhenReady(false);
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
                            Log.d("DecoderInfo", player.getAudioFormat().toString());
                        }
                    }
                    @Override
                    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                        if (mediaItem != null) {
                            currentPosition = player.getCurrentMediaItemIndex();
                        }
                        if (!isColorChanged) {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                Bitmap transparentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.transparent); 
                                tb = toByteArray(transparentBitmap);
                                Bitmap bmp = loadBitmapFromPath(MainActivity.currentMap.get(position).get("thumbnail").toString());
                                ColorPaletteUtils.generateFromBitmap(bmp, (light, dark) -> {
                                    sendDataToActivity("colors");
                                });
                            });
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

        ExoPlayerHandler.postDelayed(new Runnable() {
            @Override
            public void run(){
                if (player != null && player.isPlaying()) {
                    long pos = player.getCurrentPosition();
                    lastProgress = (int) pos;
                    Intent progressIntent = new Intent("PLAYER_PROGRESS");
                    progressIntent.putExtra("progress", (int) pos);
                    sendBroadcast(progressIntent);
                    ExoPlayerHandler.postDelayed(this, 125);
                }
            }
        }, 125);

        isExecutorStarted = true;
    }
	
    
	private Notification buildNotification(String title, String artist, String cover) {
        if (mediaSession == null) {
            setupMediaSession();
        }
        MediaStyleNotificationHelper.MediaStyle mediaStyle = new MediaStyleNotificationHelper.MediaStyle(mediaSession).setShowActionsInCompactView(3 , 2 , 1, 0);
        Intent resumeIntent = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setSmallIcon(R.drawable.service_icon)
		.setContentTitle(title)
		.setContentText(artist)
		.setLargeIcon(current)
    	.setStyle(mediaStyle)
		.setOngoing(true)
        .setContentIntent(contentIntent)
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
        mediaSession = new androidx.media3.session.MediaSession.Builder(this, player).setId("XMusicMediaSessionPrivate").setCallback(new CustomCallback()).build();
    }
    
    public class CustomCallback implements MediaSession.Callback {
        private int loopIcon = R.drawable.ic_repeat_one;
        private String loopMode = "LOOP_ONCE";
        private String shuffleMode = "SHUFFLE_ON";
        private String nextLoopAction = "LOOP_OFF";
     
        SessionCommand loopCommand = new SessionCommand(nextLoopAction, Bundle.EMPTY);
        CommandButton loopButton = new CommandButton.Builder().setIconResId(loopIcon).setDisplayName("repeat").setSessionCommand(loopCommand).setSlots(CommandButton.SLOT_FORWARD_SECONDARY).build();
        List<CommandButton> commandsList = ImmutableList.of(loopButton);
        SessionCommands sessionCommands = SessionCommands.EMPTY.buildUpon().add(new SessionCommand("LOOP_ALL", Bundle.EMPTY)).add(new SessionCommand("LOOP_ONCE", Bundle.EMPTY)).add(new SessionCommand("LOOP_OFF", Bundle.EMPTY)).build();
        @Override
            public ConnectionResult onConnect(MediaSession mediaSession, ControllerInfo controllerInfo) {
                return ConnectionResult.accept(sessionCommands, playerCommands);
            }
            
            @Override
            public void onPostConnect(MediaSession mediaSession, ControllerInfo controllerInfo) {
                mediaSession.setAvailableCommands(controllerInfo, sessionCommands, playerCommands);
                mediaSession.setCustomLayout(controllerInfo, commandsList);
            }
            
            @Override
            public ListenableFuture<SessionResult> onCustomCommand(MediaSession mediaSession, ControllerInfo controllerInfo, SessionCommand sessionCommand, Bundle bundle) {
                ExoPlayerHandler.post(() -> {
                    nextLoopAction = switch (sessionCommand.customAction) {
                        case "LOOP_ALL" -> {
                            loopIcon = R.drawable.ic_repeat_one;
                            player.setRepeatMode(Player.REPEAT_MODE_ONE);
                            yield "LOOP_ONCE"; 
                        }
                        case "LOOP_ONCE" -> {
                            player.setPauseAtEndOfMediaItems(true);
                            loopIcon = R.drawable.ic_repeat_off;
                            player.setRepeatMode(Player.REPEAT_MODE_OFF);
                            yield "LOOP_OFF";
                        }
                        case "LOOP_OFF" -> {
                            player.setPauseAtEndOfMediaItems(false);
                            player.setRepeatMode(Player.REPEAT_MODE_ALL);
                            loopIcon = R.drawable.ic_repeat;
                            yield "LOOP_ALL"; 
                        }
                        default -> {
                            loopIcon = R.drawable.ic_repeat_one;
                            yield "LOOP_ONCE";
                        }
                    };
                });
                loopCommand = new SessionCommand(nextLoopAction, Bundle.EMPTY);
                loopButton = new CommandButton.Builder().setIconResId(loopIcon).setDisplayName("repeat").setSessionCommand(loopCommand).setSlots(CommandButton.SLOT_FORWARD_SECONDARY).build();
                commandsList = ImmutableList.of(loopButton);
                mediaSession.setCustomLayout(controllerInfo, commandsList);
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            }
        
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
        } else {
			XUtils.showMessage(getApplicationContext(), "Unable to play songs at the moment");
		}
    }

    public void abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }

    public interface Callback {
        void onData(String data);
    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void sendDataToActivity(String data) {
        if (callback != null) callback.onData(data);
    }
}
