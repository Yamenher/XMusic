package com.xapps.media.xmusic.widget;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.R;

import java.util.List;

public class LyricsAdapter extends RecyclerView.Adapter<LyricsViewHolder> {

    public static final int TYPE_PLAIN = 1;
    public static final int TYPE_SYNCED = 2;

    private final List<LyricLine> lines;
    private PlaybackControlListener listener;

    private boolean syncedLyrics;
    private Typeface syncedTypeface;
    private int gravity;
    private float textSizeSp;

    private int currentProgressMs = -1;
    private int activeLineIndex = -1;

    public LyricsAdapter(List<LyricLine> lines) {
        this.lines = lines;
    }

    public void updateProgressState(int progressMs, int activeIndex) {
        this.currentProgressMs = progressMs;
        this.activeLineIndex = activeIndex;
    }
    
    public void setListener(PlaybackControlListener l) {
        listener = l;
    }

    public void configureSynced(boolean s, Typeface t, int g, float ts) {
        this.syncedLyrics = s;
        this.syncedTypeface = t;
        this.gravity = g;
        this.textSizeSp = ts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    @Override
    public int getItemViewType(int position) {
        return syncedLyrics ? TYPE_SYNCED : TYPE_PLAIN;
    }

    @NonNull
    @Override
    public LyricsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == TYPE_SYNCED) {
            view = inflater.inflate(R.layout.item_lyric_synced, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_lyric_plain, parent, false);
        }
        return new LyricsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricsViewHolder holder, int position) {
        LyricLine line = lines.get(position);

        if (getItemViewType(position) == TYPE_SYNCED) {
            LyricLineView v = holder.lineView;
            v.setLyricLine(line);
            v.setAlpha(1f);
            v.setGravity(gravity);
            if (syncedTypeface != null) v.setTypeface(syncedTypeface);
            
            if (currentProgressMs != -1) {
                v.setCurrentProgress(currentProgressMs);
                v.setCurrent(position == activeLineIndex, position);
            }
            
            if (line.vocalType == 2) {
                v.setGravity(Gravity.RIGHT);
            } else if (line.vocalType == 1) {
                v.setGravity(Gravity.LEFT);
            }
            
            if (line.isBackground) {
                v.setTextSize(20f);
            } else {
                v.setTextSize(32f);
            }
            
            holder.itemView.findViewById(R.id.lyricContainer).setOnClickListener(v2 -> {
                if (listener != null) listener.onSeekRequested((long) line.time);
            });
        } else {
            holder.textView.setText(line.line);
        }
    }

    @Override
    public void onViewRecycled(@NonNull LyricsViewHolder holder) {
        super.onViewRecycled(holder);
    }
}
