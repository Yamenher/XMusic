package com.xapps.media.xmusic.helper;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.xapps.media.xmusic.common.SongLoadListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.concurrent.atomic.AtomicInteger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public class SongMetadataHelper {
	
	private static final String CACHE_DIR_NAME = "covers";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public static void getAllSongs(Context context, SongLoadListener listener) {

    ArrayList<HashMap<String, Object>> songListMap = new ArrayList<>();
    Object lock = new Object();

    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + getListOfMediaTypes();
    String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
    };

    Cursor cursor = context.getContentResolver().query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " COLLATE NOCASE"
    );

    if (cursor == null) {
        if (listener != null) listener.onComplete(songListMap);
        return;
    }

    int total = cursor.getCount();
    if (total == 0) {
        cursor.close();
        if (listener != null) listener.onComplete(songListMap);
        return;
    }

    for (int i = 0; i < total; i++) {
        songListMap.add(null);
    }

    CountDownLatch latch = new CountDownLatch(total);
    AtomicInteger progress = new AtomicInteger(0);

    while (cursor.moveToNext()) {

        final int index = cursor.getPosition();

        final long songId = cursor.getLong(0);
        final String title = cursor.getString(1);
        final String artist = cursor.getString(2);
        final String artistId = cursor.getString(3);
        final String album = cursor.getString(4);
        final String albumArtist = cursor.getString(5);
        final String path = cursor.getString(6);
        final String year = cursor.getString(7);
        final String albumId = cursor.getString(8);
        final String mimeType = cursor.getString(9);
        final String track = cursor.getString(10);
        final long duration = cursor.getLong(11);
        final String dateAdded = cursor.getString(12);
        final String dateModified = cursor.getString(13);

        executorService.execute(() -> {
            try {
                HashMap<String, Object> map = new HashMap<>();
                map.put("path", path);
                map.put("id", songId);
                map.put("title", title != null && !title.isEmpty() ? title : "Unknown Title");
                map.put("author", artist != null && !artist.isEmpty() ? artist.trim() : "Unknown Artist");
                map.put("artistId", artistId);
                map.put("album", album);
                map.put("albumArtist", albumArtist);
                map.put("data", path);
                map.put("year", year);
                map.put("albumId", albumId);
                map.put("mimeType", mimeType);
                map.put("track", track);
                map.put("duration", duration > 0 ? millisecondsToDuration(duration) : "00:00");
                map.put("total", String.valueOf((int) duration));
                map.put("dateAdded", dateAdded);
                map.put("dateModified", dateModified);
                map.put("thumbnail", getSongCover(context, path, mimeType, songId));

                synchronized (lock) {
                    songListMap.set(index, map);
                }

                int current = progress.incrementAndGet();
                if (listener != null) {
                    listener.onProgress(songListMap, current);
                }

            } finally {
                latch.countDown();
            }
        });
    }

    cursor.close();

    executorService.execute(() -> {
        try {
            latch.await();
        } catch (InterruptedException ignored) {}
        if (listener != null) {
            listener.onComplete(songListMap);
        }
    });
}
	
	public static String getSongCover(Context context, String songFilePath, String mimeType, long id) {
    String cached = getCachedCoverPath(context, songFilePath);
    if (cached != null) return cached;

    Long audioId = id;
    if (audioId != null) {
        try {
            Uri uri = ContentUris.appendId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(),
                    audioId
            ).appendPath("albumart").build();

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                Bitmap b = BitmapFactory.decodeStream(is);
                is.close();
                if (b != null) {
                    String path = saveCoverToCache(context, songFilePath, b);
                    b.recycle();
                    return path;
                }
            }
        } catch (Exception ignored) {}
    }

    boolean useJAudioTagger =
            mimeType == null ||
            mimeType.equals("audio/flac") ||
            mimeType.equals("audio/ogg") ||
            mimeType.equals("audio/opus") ||
            mimeType.equals("audio/x-flac");

    if (!useJAudioTagger) {
        try {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            r.setDataSource(songFilePath);
            byte[] art = r.getEmbeddedPicture();
            r.release();
            if (art != null && art.length > 0) {
                Bitmap b = BitmapFactory.decodeByteArray(art, 0, art.length);
                if (b != null) {
                    String path = saveCoverToCache(context, songFilePath, b);
                    b.recycle();
                    return path;
                }
            }
        } catch (Exception ignored) {}
    }

    try {
        AudioFile audioFile = AudioFileIO.read(new File(songFilePath));
        Tag tag = audioFile.getTag();
        if (tag == null) return null;
        Artwork artwork = tag.getFirstArtwork();
        if (artwork == null) return null;

        byte[] art = artwork.getBinaryData();
        if (art == null || art.length == 0) return null;

        Bitmap b = BitmapFactory.decodeByteArray(art, 0, art.length);
        if (b == null) return null;

        String path = saveCoverToCache(context, songFilePath, b);
        b.recycle();
        return path;
    } catch (Exception ignored) {
        return null;
    }
}
    
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
	
	private static String getCachedCoverPath(Context context, String songFilePath) {
		String hashedFileName = hashFilePath(songFilePath);
		File cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		
		File coverFile = new File(cacheDir, hashedFileName + ".jpg");
		if (coverFile.exists()) {
			return coverFile.getAbsolutePath();
		}
		
		return null;
	}
	
	private static String saveCoverToCache(Context context, String songFilePath, Bitmap bitmap) {
		String hashedFileName = hashFilePath(songFilePath);
		File cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		
		File coverFile = new File(cacheDir, hashedFileName + ".jpg");
		try (FileOutputStream fos = new FileOutputStream(coverFile)) {
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			return coverFile.getAbsolutePath();
		} catch (IOException e) {
			Log.e("SongMetadataHelper", "Error saving cover to cache", e);
		}
		
		return null;
	}
	
	private static String hashFilePath(String filePath) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(filePath.getBytes());
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				hexString.append(String.format("%02x", b));
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e("SongMetadataHelper", "Error hashing file path", e);
		}
		return String.valueOf(filePath.hashCode());
	}
	
	private static String getListOfMediaTypes() {
		List<String> mediaTypes = List.of("audio/x-wav", "audio/ogg", "audio/aac", "audio/midi", "audio/flac");
		StringBuilder stringBuilder = new StringBuilder();
		for (String mediaType : mediaTypes) {
			stringBuilder.append(" or ").append(MediaStore.Audio.Media.MIME_TYPE).append(" = '").append(mediaType).append("'");
		}
		return stringBuilder.toString();
	}
	
	public static String millisecondsToDuration(long milliseconds) {
		long seconds = milliseconds / 1000;
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long remainingSeconds = seconds % 60;
		
		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
		} else {
			return String.format("%02d:%02d", minutes, remainingSeconds);
		}
	}

}
