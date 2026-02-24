package com.xapps.media.xmusic.helper;

import com.xapps.media.xmusic.data.RuntimeData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class SongSearchHelper {

    public static ArrayList<HashMap<String, Object>> search(
            String query,
            boolean searchTitle,
            boolean searchArtist,
            boolean searchAlbum,
            boolean searchAlbumArtist) {
        ArrayList<HashMap<String, Object>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            results.addAll(RuntimeData.songsMap);
            return results;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (int i = 0; i < RuntimeData.songsMap.size(); i++) {
            HashMap<String, Object> song = RuntimeData.songsMap.get(i);
            String searchKey = (String) song.get("searchKey");
            if (searchKey == null) continue;

            boolean match = false;
            
            if (searchTitle && searchArtist && searchAlbum && searchAlbumArtist) {
                if (searchKey.toLowerCase(Locale.ROOT).contains(lowerQuery)) match = true;
            } else {
                if (searchTitle && extract(searchKey, "{t}", "{/t}").contains(lowerQuery))
                    match = true;
                if (!match
                        && searchArtist
                        && extract(searchKey, "{a}", "{/a}").contains(lowerQuery)) match = true;
                if (!match
                        && searchAlbum
                        && extract(searchKey, "{al}", "{/al}").contains(lowerQuery)) match = true;
                if (!match
                        && searchAlbumArtist
                        && extract(searchKey, "{aa}", "{/aa}").contains(lowerQuery)) match = true;
            }

            if (match) {
                results.add(song);
            }
        }
        return results;
    }

    private static String extract(String source, String start, String end) {
        int s = source.indexOf(start);
        if (s == -1) return "";
        s += start.length();
        int e = source.indexOf(end, s);
        if (e == -1) return "";
        return source.substring(s, e);
    }
}
