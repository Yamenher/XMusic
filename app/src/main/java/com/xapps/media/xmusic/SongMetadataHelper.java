package com.xapps.media.xmusic;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.xapps.media.xmusic.common.SongLoadListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SongMetadataHelper {
	
	private static final String CACHE_DIR_NAME = "covers";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
	
	public static void getAllSongs(Context context, SongLoadListener listener) {

    ArrayList<HashMap<String, Object>> songListMap = new ArrayList<>();
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
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
        MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

    if (cursor != null) {
        List<Future<HashMap<String, Object>>> futures = new ArrayList<>();
        int count = 0;

        while (cursor.moveToNext()) {
            final String path = cursor.getString(6);
            final long songId = cursor.getLong(0);
            final String title = cursor.getString(1);
            final String artist = cursor.getString(2);
            final String artistId = cursor.getString(3);
            final String album = cursor.getString(4);
            final String albumArtist = cursor.getString(5);
            final String data = cursor.getString(6);
            final String year = cursor.getString(7);
            final String albumId = cursor.getString(8);
            final String mimeType = cursor.getString(9);
            final String track = cursor.getString(10);
            final long duration = cursor.getLong(11);
            final String dateAdded = cursor.getString(12);
            final String dateModified = cursor.getString(13);

            futures.add(executorService.submit(() -> {
                HashMap<String, Object> map = new HashMap<>();
                map.put("path", path);
                map.put("id", songId);
                map.put("title", title != null && !title.isEmpty() ? title : "Unknown Title");
                map.put("author", artist != null && !artist.isEmpty() ? artist.trim() : "Unknown Artist");
                map.put("artistId", artistId);
                map.put("album", album);
                map.put("albumArtist", albumArtist);
                map.put("data", data);
                map.put("year", year);
                map.put("albumId", albumId);
                map.put("mimeType", mimeType);
                map.put("track", track);
                map.put("duration", duration > 0 ? millisecondsToDuration(duration) : "00:00");
                map.put("total", String.valueOf((int) duration));
                map.put("dateAdded", dateAdded);
                map.put("dateModified", dateModified);
                map.put("thumbnail", getSongCover(context, path));
                return map;
            }));

            count++;
            if (listener != null) {
                listener.onProgress(count);
            }
        }
        cursor.close();

        for (Future<HashMap<String, Object>> future : futures) {
            try {
                HashMap<String, Object> songMap = future.get();
                songListMap.add(songMap);
                if (listener != null) {
                    listener.onProgress(songListMap.size());
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("SongMetadataHelper", "Error processing song", e);
            }
        }
    }

    if (listener != null) {
        listener.onComplete(songListMap);
    }
}
	
	public static String getSongCover(Context context, String songFilePath) {
		try {
			String cachedCoverPath = getCachedCoverPath(context, songFilePath);
			if (cachedCoverPath != null) {
				return cachedCoverPath;
			}
			
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(songFilePath);

            byte[] coverArt = retriever.getEmbeddedPicture();
            if (coverArt != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(coverArt, 0, coverArt.length, options);
                options.inSampleSize = calculateInSampleSize(options, 1000, 1000);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeByteArray(coverArt, 0, coverArt.length, options);
                if (bitmap != null) {
                    try {
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        int edge = Math.min(width, height);
                        Bitmap square = Bitmap.createBitmap(bitmap, (width - edge) / 2, (height - edge) / 2, edge, edge);
                        Bitmap finalBitmap = Bitmap.createScaledBitmap(square, 1000, 1000, true);
                        if (square != finalBitmap) {
                            square.recycle();
                        }
                        bitmap.recycle();
                        String cacheFilePath = saveCoverToCache(context, songFilePath, finalBitmap);
                        finalBitmap.recycle();
                        return cacheFilePath;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;

            }
		} catch (Exception e) {
			Log.e("SongMetadataHelper", "Error retrieving song cover", e);
		}
		
		return null;
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
