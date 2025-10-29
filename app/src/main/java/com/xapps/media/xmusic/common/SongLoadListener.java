package com.xapps.media.xmusic.common;
import java.util.HashMap;

public interface SongLoadListener {
    void onProgress(java.util.ArrayList<HashMap<String, Object>> songs, int count);
    void onComplete(java.util.ArrayList<HashMap<String, Object>> songs);
}
