package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.data.LiveColors;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.R;

import com.xapps.media.xmusic.utils.XUtils;
import java.util.List;

public class LyricsAdapter extends RecyclerView.Adapter<LyricsViewHolder> {

    public static final int TYPE_PLAIN = 1;
    public static final int TYPE_SYNCED = 2;
    
    public int verticalPadding;
    public int endPadding;
    public int startPadding;

    private final List<LyricLine> lines;
    private PlaybackControlListener listener;

    private boolean syncedLyrics;
    private Typeface syncedTypeface;
    private int gravity;
    private float textSizeSp;
    
    private int lyricColor = LiveColors.onSurface;

    private int currentProgressMs = -1;
    private int activeLineIndex = -1;

    public LyricsAdapter(List<LyricLine> lines, Context c) {
        this.lines = lines;
        verticalPadding = XUtils.convertToPx(c, 16f);
        startPadding = XUtils.convertToPx(c, 24f);
        endPadding = XUtils.convertToPx(c, 60f);
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
    
    public void setLyricColor(int color) {
        lyricColor = color;
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

            LyricLineCanvasView v2 = holder.canvasLine;
            v2.setSkipNextAnimation(true);
            v2.setLyricLine(line);
            v2.setAlpha(1f);
            v2.setColor(lyricColor);
    
            if (line.vocalType == 2) {
                v2.setAlignment(LyricLineCanvasView.ALIGN_RIGHT);
            } else if (line.vocalType == 1) {
                v2.setAlignment(LyricLineCanvasView.ALIGN_LEFT);
            }

            if (line.isRomaji) {
                v2.setTextSizeSp(18f);
                if (line.vocalType == 1) {
                    v2.setPadding(
                        startPadding,
                        verticalPadding/2,
                        endPadding,
                        verticalPadding/2
                    );
                } else {
                    v2.setPadding(
                        endPadding,
                        verticalPadding/2,
                        startPadding,
                        verticalPadding/2
                    );
                }
            } else if (line.isBackground) {
                v2.setTextSizeSp(20f);
                if (line.vocalType == 1) {
                    v2.setPadding(
                        startPadding,
                        verticalPadding,
                        endPadding,
                        verticalPadding
                    );
                } else {
                    v2.setPadding(
                        endPadding,
                        verticalPadding,
                        startPadding,
                        verticalPadding
                    );
                }
            } else {
                v2.setTextSizeSp(30f);
                if (line.vocalType == 1) {
                    v2.setPadding(
                        startPadding,
                        verticalPadding,
                        endPadding,
                        verticalPadding
                    );
                } else {
                    v2.setPadding(
                        endPadding,
                        verticalPadding,
                        startPadding,
                        verticalPadding
                    );
                }
            }

            if (syncedTypeface != null) {
                v2.setTypeface(syncedTypeface);
            }

            if (currentProgressMs != -1) {
                v2.setCurrentProgress(currentProgressMs);
                v2.setCurrent(position == activeLineIndex, position);
            }

            holder.itemView
                .findViewById(R.id.lyricContainer)
                .setOnClickListener(c -> {
                    if (listener != null) {
                        listener.onSeekRequested((long) line.time);
                    }
                });

        } else {
            holder.textView.setText(line.line);
        }
    }

    @Override
    public void onViewRecycled(@NonNull LyricsViewHolder holder) {
        super.onViewRecycled(holder);
        try {
            int i = holder.getAdapterPosition();
            if (holder.canvasLine != null) holder.canvasLine.setCurrent(false, i);
        } catch (IndexOutOfBoundsException e) {}
    }
}
