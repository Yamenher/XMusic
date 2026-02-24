package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricOffsetDecoration;
import com.xapps.media.xmusic.models.LyricWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LyricsViewLegacy extends RecyclerView {

    private LyricsAdapter adapter;
    private final List<LyricLine> lines = new ArrayList<>();
    private List<Integer> currentActiveIndices = new ArrayList<>();
    private int lastTopActiveIndex = -1;
    private long lastUserTouchTime = -1;

    private boolean allowAutoScroll = true;

    private static final long AUTO_SCROLL_DELAY_MS = 2000;
    private final Handler scrollHandler = new Handler(Looper.getMainLooper());

    private final Runnable enableAutoScrollRunnable = () -> allowAutoScroll = true;

    private final Runnable snapBackRunnable = () -> {
        if (!allowAutoScroll) return;
        if (!currentActiveIndices.isEmpty()) {
            int topIndex = Collections.min(currentActiveIndices);
            performDynamicScroll(topIndex);
        }
    };

    public LyricsViewLegacy(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayoutManager(new LinearLayoutManager(context));
        setItemAnimator(null);
        addItemDecoration(new LyricOffsetDecoration());
        setItemViewCacheSize(0);
    }

    public void setLyricColor(int color) {
        if (adapter != null) {
            adapter.setLyricColor(color);
            adapter.notifyDataSetChanged();
        }
    }

    public void setLyrics(List<LyricLine> lyricLines) {
        lines.clear();
        lines.addAll(lyricLines);
        adapter = new LyricsAdapter(lines, getContext());
        setAdapter(adapter);
        currentActiveIndices.clear();
        lastTopActiveIndex = -1;
        scrollHandler.removeCallbacksAndMessages(null);
        allowAutoScroll = true;
    }

    public void configureSyncedLyrics(boolean synced, Typeface t, int gravity, float textSizeSp) {
        if (adapter == null) return;
        adapter.configureSynced(synced, t, gravity, textSizeSp);
    }

    public void setOnSeekListener(PlaybackControlListener l) {
        if (adapter != null) adapter.setListener(l);
    }

    public void onProgress(int progressMs) {
        updateActiveLine(progressMs);
    }

    private void updateActiveLine(int progressMs) {
    if (adapter == null || lines.isEmpty()) return;

    final int EXIT_ANTICIPATION_MS = 10;
    List<Integer> newActiveIndices = new ArrayList<>();

    for (int i = 0; i < lines.size(); i++) {
        LyricLine line = lines.get(i);
        if (line.isRomaji) continue;

        long startTime = line.time;
        long actualWordEnd = getLineEndTime(i);
        long disappearTime = actualWordEnd - EXIT_ANTICIPATION_MS;
        

        if (progressMs >= startTime && progressMs <= disappearTime) {
            newActiveIndices.add(i);
        }
    }

    if (newActiveIndices.isEmpty() && !currentActiveIndices.isEmpty()) {
        newActiveIndices.addAll(currentActiveIndices);
    }

    List<Integer> toDeactivate = new ArrayList<>(currentActiveIndices);
    toDeactivate.removeAll(newActiveIndices);
    currentActiveIndices = newActiveIndices;

    if (!currentActiveIndices.isEmpty()) {
        int currentTopIndex = Collections.min(currentActiveIndices);
        if (currentTopIndex != lastTopActiveIndex) {
            lastTopActiveIndex = currentTopIndex;

            LinearLayoutManager lm = (LinearLayoutManager) getLayoutManager();
            if (lm != null && allowAutoScroll) {
                int first = lm.findFirstVisibleItemPosition();
                int last = lm.findLastVisibleItemPosition();
                boolean isFarAway = currentTopIndex < first || currentTopIndex > last;

                if (isFarAway) {
                    scrollHandler.removeCallbacks(snapBackRunnable);
                    scrollHandler.postDelayed(snapBackRunnable, 10);
                } else if (System.currentTimeMillis() - lastUserTouchTime > AUTO_SCROLL_DELAY_MS) {
                    scrollHandler.removeCallbacks(snapBackRunnable);
                    performDynamicScroll(currentTopIndex);
                }
            }
        }
    }

    for (int i = 0; i < getChildCount(); i++) {
        ViewHolder vh = getChildViewHolder(getChildAt(i));
        if (vh instanceof LyricsViewHolder) {
            LyricsViewHolder holder = (LyricsViewHolder) vh;
            int pos = holder.getBindingAdapterPosition();
            if (pos < 0) continue;
            
            boolean isActive;
            if (lines.get(pos).isRomaji) {
                isActive = (pos > 0 && currentActiveIndices.contains(pos - 1));
            } else {
                isActive = currentActiveIndices.contains(pos);
            }

            if (isActive) {
                holder.canvasLine.setCurrent(true, pos);
                holder.canvasLine.setCurrentProgress(progressMs);
            } else if (toDeactivate.contains(pos) || (lines.get(pos).isRomaji && pos > 0 && toDeactivate.contains(pos - 1))) {
                holder.canvasLine.setCurrent(false, pos);
            }
        }
    }
}

    private void performDynamicScroll(int targetIndex) {
        if (!allowAutoScroll) return;

        LinearLayoutManager lm = (LinearLayoutManager) getLayoutManager();
        if (lm == null) return;

        int firstVisible = lm.findFirstVisibleItemPosition();
        int distance = Math.abs(targetIndex - firstVisible);

        CenterSmoothScroller scroller = new CenterSmoothScroller(getContext(), distance);
        scroller.setTargetPosition(targetIndex);
        lm.startSmoothScroll(scroller);
    }

    private long getLineEndTime(int index) {
    LyricLine line = lines.get(index);
    if (line.isSimpleLRC) {
        if (line.endTime > 0) return line.endTime;
        
        for (int i = index + 1; i < lines.size(); i++) {
            if (!lines.get(i).isRomaji) return lines.get(i).time;
        }
        return line.time + 99999;
    } else {
        long max = 0;
        for (LyricWord w : line.words) {
            int wEnd = w.getEndTime();
            if (wEnd > max) max = wEnd;
        }
        return max;
    }
}





    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();

        if (action == MotionEvent.ACTION_MOVE) {
            allowAutoScroll = false;
            scrollHandler.removeCallbacks(enableAutoScrollRunnable);
            lastUserTouchTime = System.currentTimeMillis();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            lastUserTouchTime = System.currentTimeMillis();
            scrollHandler.removeCallbacks(enableAutoScrollRunnable);
            scrollHandler.postDelayed(enableAutoScrollRunnable, AUTO_SCROLL_DELAY_MS);
        }

        return super.onTouchEvent(e);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        scrollHandler.removeCallbacksAndMessages(null);
    }

    private static class CenterSmoothScroller extends LinearSmoothScroller {

        private final int itemDistance;

        CenterSmoothScroller(Context context, int itemDistance) {
            super(context);
            this.itemDistance = itemDistance;
        }

        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
            int boxOneThird = boxStart + (boxEnd - boxStart) / 3;
            int viewCenter = viewStart + (viewEnd - viewStart) / 2;
            return boxOneThird - viewCenter;
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            float baseMillisPerPixel = 150f;
            float speedReduction = itemDistance * 12f;
            float finalSpeed = Math.max(20f, baseMillisPerPixel - speedReduction);
            return finalSpeed / displayMetrics.densityDpi;
        }
    }
}