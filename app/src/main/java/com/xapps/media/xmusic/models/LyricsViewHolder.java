package com.xapps.media.xmusic.widget;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xapps.media.xmusic.R;

public class LyricsViewHolder extends RecyclerView.ViewHolder {

    public final TextView textView;
    public final LyricLineView lineView;

    public LyricsViewHolder(@NonNull View itemView) {
        super(itemView);

        textView = itemView.findViewById(R.id.lyricText);

        if (textView instanceof LyricLineView) {
            lineView = (LyricLineView) textView;
        } else {
            lineView = null;
        }
    }
}