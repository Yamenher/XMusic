package com.xapps.media.xmusic.models;

import java.util.ArrayList;
import java.util.List;

public class LyricWord {

    public final int startIndex;
    public final List<LyricSyllable> syllables = new ArrayList<>();

    public LyricWord(int startIndex) {
        this.startIndex = startIndex;
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (LyricSyllable s : syllables) sb.append(s.text);
        return sb.toString();
    }

    public int getStartTime() {
        return syllables.get(0).startTime;
    }

    public int getEndTime() {
        return syllables.get(syllables.size() - 1).endTime;
    }
}