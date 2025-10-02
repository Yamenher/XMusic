package com.xapps.media.xmusic.common;
import java.util.HashMap;

public interface SongLoadListener {
    void onProgress(int count);
    void onComplete(java.util.ArrayList<HashMap<String, Object>> songs);
}
