package com.xapps.media.xmusic;

import android.graphics.Bitmap;

public class SongMetadata {
    public String title;
    public String artist;
    public long duration;
    public Bitmap albumArt;

    public SongMetadata(String title, String artist, long duration, Bitmap albumArt) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.albumArt = albumArt;
    }
}
