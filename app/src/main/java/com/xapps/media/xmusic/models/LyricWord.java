package com.xapps.media.xmusic.models;
import android.util.Log;

public class LyricWord {

    public final int timestamp;
    public final String word;
    public final int startIndex;
    public int endTime;

    public LyricWord(int timestamp, String word, int startIndex, int endTime) {
        this.timestamp = timestamp;
        this.word = word;
        this.startIndex = startIndex;
        this.endTime = endTime;
        Log.d("LyricWord", "Start="+String.valueOf(timestamp) + " ,end="+String.valueOf(endTime));
    }

    public LyricWord(int timestamp, String word, int startIndex) {
        this(timestamp, word, startIndex, timestamp + 1000);
    }
}