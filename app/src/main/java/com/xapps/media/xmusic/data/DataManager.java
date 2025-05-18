package com.xapps.media.xmusic.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    private static final String PREF_NAME = "com.xapps.media.xmusic.songs_preferences";
    private static final String KEY_SONGS_LIST = "songs_list";
    private static final String KEY_SONGS_MAP = "songs_map";

    private static DataManager instance;
    private SharedPreferences encryptedSharedPreferences;
    private ArrayList<String> cachedSongsList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> cachedSongsMap = new ArrayList<>();

    private DataManager() {}

    public static DataManager getInstance() throws IOException {
        if (instance == null) {
            throw new IOException("DataManager not initialized. Call init(context) first.");
        }
        return instance;
    }

    public static void init(Context context) throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new DataManager();
            instance.setupEncryptedPrefs(context);
        }
    }

    private void setupEncryptedPrefs(Context context) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        encryptedSharedPreferences = EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        loadDataFromPrefs();
    }

    public void saveData() {
        Gson gson = new Gson();
        encryptedSharedPreferences.edit()
            .putString(KEY_SONGS_LIST, gson.toJson(cachedSongsList))
            .putString(KEY_SONGS_MAP, gson.toJson(cachedSongsMap))
            .apply();
    }

    public void loadDataFromPrefs() {
        Gson gson = new Gson();
        String songsListJson = encryptedSharedPreferences.getString(KEY_SONGS_LIST, null);
        String songsMapJson = encryptedSharedPreferences.getString(KEY_SONGS_MAP, null);

        if (songsListJson != null) {
            cachedSongsList = gson.fromJson(songsListJson, new TypeToken<ArrayList<String>>(){}.getType());
        }
        if (songsMapJson != null) {
            cachedSongsMap = gson.fromJson(songsMapJson, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        }
    }

    public ArrayList<String> getCachedSongsList() {
        return cachedSongsList;
    }

    public ArrayList<HashMap<String, Object>> getCachedSongsMap() {
        return cachedSongsMap;
    }

    public void setCachedSongsList(ArrayList<String> list) {
        cachedSongsList = list;
    }

    public void setCachedSongsMap(ArrayList<HashMap<String, Object>> map) {
        cachedSongsMap = map;
    }

    public void clearData() {
        encryptedSharedPreferences.edit().clear().apply();
        cachedSongsList.clear();
        cachedSongsMap.clear();
    }
}