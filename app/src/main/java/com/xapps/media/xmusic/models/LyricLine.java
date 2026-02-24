package com.xapps.media.xmusic.models;

import android.text.Spannable;
import java.util.Collections;
import java.util.List;

public class LyricLine {
    public int time;
    public int endTime;
    public Spannable line;
    public final List<LyricWord> words;
    
    public int vocalType = 1; 
    public boolean isBackground = false; 
    public boolean isRomaji = false;
    public boolean isSimpleLRC;

    public LyricLine(int time, Spannable line, List<LyricWord> words) {
        this.time = time;
        this.line = line;
        this.words = words != null ? words : Collections.emptyList();
    }

    public LyricLine(Spannable line) {
        this(-1, line, Collections.emptyList());
    }
}
