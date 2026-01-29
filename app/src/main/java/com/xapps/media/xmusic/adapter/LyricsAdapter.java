package com.xapps.media.xmusic.widget;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.RecyclerView;
import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LyricsAdapter extends RecyclerView.Adapter<LyricsViewHolder> {

    public static final int TYPE_PLAIN = 1;
    public static final int TYPE_SYNCED = 2;

    private final List<LyricLine> lines;
    public final Set<LyricsViewHolder> attachedHolders = new HashSet<>();
    
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

    public void configureSynced(
            boolean synced,
            Typeface typeface,
            int gravity,
            float textSizeSp
    ) {
        this.syncedLyrics = synced;
        this.syncedTypeface = typeface;
        this.gravity = gravity;
        this.textSizeSp = textSizeSp;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_SYNCED;
    }

    @Override
    public LyricsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(LyricsViewHolder holder, int position) {
        attachedHolders.add(holder);

        LyricLine line = lines.get(position);

        if (getItemViewType(position) == TYPE_SYNCED) {
            LyricLineView v = holder.lineView;
            v.setGravity(gravity);
            v.setLyricLine(line);
            v.setAlpha(1f);
            if (syncedTypeface != null) v.setTypeface(syncedTypeface);
            
            if (currentProgressMs != -1) {
                v.setCurrentProgress(currentProgressMs);
                v.setCurrent(position == activeLineIndex);
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
        attachedHolders.remove(holder);

        if (holder.lineView != null) {
            //holder.lineView.setCurrent(false);
            //holder.lineView.
        }

        super.onViewRecycled(holder);
    }
}
