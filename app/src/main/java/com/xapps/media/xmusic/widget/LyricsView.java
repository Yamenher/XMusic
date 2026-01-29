package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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

public class LyricsView extends RecyclerView {

    private LyricsAdapter adapter;
    private final List<LyricLine> lines = new ArrayList<>();

    private List<Integer> currentActiveIndices = new ArrayList<>();
    private List<Integer> previousActiveIndices = new ArrayList<>();

    private long lastUserTouchTime = -1;
    private boolean isUserTouching = false;
    
    private static final long AUTO_SCROLL_DELAY_MS = 1500;

    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable snapBackRunnable = () -> {
        if (!currentActiveIndices.isEmpty()) {
            maybeAutoScroll(currentActiveIndices.get(0), false);
        }
    };

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayoutManager(new LinearLayoutManager(context));
        setItemAnimator(null);
        addItemDecoration(new LyricOffsetDecoration());
    }

    public void setLyrics(List<LyricLine> lyricLines) {
        lines.clear();
        lines.addAll(lyricLines);
        adapter = new LyricsAdapter(lines);
        setAdapter(adapter);

        currentActiveIndices.clear();
        previousActiveIndices.clear();

        scrollHandler.removeCallbacksAndMessages(null);
    }

    public void configureSyncedLyrics(boolean synced, int gravity, float textSizeSp) {
        if (adapter == null) return;
        adapter.configureSynced(synced, null, gravity, textSizeSp);
    }
    
    public void setOnSeekListener(PlaybackControlListener l) {
        adapter.setListener(l);
    }

    public void onProgress(int progressMs) {
        updateActiveLine(progressMs + 5, true);
    }
    
    public void onSeek(int progressMs) {
        updateActiveLine(progressMs + 5, false);
    }

    private void updateActiveLine(int progressMs, boolean isAutoUpdate) {
        if (adapter == null || lines.isEmpty()) return;

        List<Integer> newActiveIndices = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            long startTime = line.time;
            long endTime = getLineEndTime(line);

            if (progressMs >= startTime && progressMs <= endTime) {
                newActiveIndices.add(i);
            }
        }

        if (newActiveIndices.isEmpty()) {
            int fallbackIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).time <= progressMs) {
                    fallbackIndex = i;
                } else {
                    break; 
                }
            }
            if (fallbackIndex != -1) newActiveIndices.add(fallbackIndex);
        }

        boolean hasChanged = !newActiveIndices.equals(currentActiveIndices);

        if (!hasChanged && isAutoUpdate) {
            for (Integer index : currentActiveIndices) {
                LyricsViewHolder holder = findHolder(index);
                if (holder != null && holder.lineView != null) {
                    holder.lineView.setCurrentProgress(progressMs);
                }
            }
            return;
        }

        previousActiveIndices = new ArrayList<>(currentActiveIndices);
        currentActiveIndices = newActiveIndices;

        int primaryActive = currentActiveIndices.isEmpty() ? -1 : currentActiveIndices.get(0);
        adapter.updateProgressState(progressMs, primaryActive);
        
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder vh = getChildViewHolder(getChildAt(i));
            if (vh instanceof LyricsViewHolder) {
                LyricsViewHolder holder = (LyricsViewHolder) vh;
                if (holder.lineView != null) {
                    int pos = holder.getBindingAdapterPosition();
                    
                    if (currentActiveIndices.contains(pos)) {
                        holder.lineView.setCurrent(true);
                        holder.lineView.setCurrentProgress(progressMs);
                    } else {
                        holder.lineView.setCurrent(false);
                    }
                }
            }
        }

        if (!isUserTouching && !currentActiveIndices.isEmpty()) {
            int target = currentActiveIndices.get(0);
            int prevTarget = previousActiveIndices.isEmpty() ? -1 : previousActiveIndices.get(0);
            
            boolean isSequential = Math.abs(target - prevTarget) == 1;
            if (!isAutoUpdate) isSequential = false; 
            maybeAutoScroll(target, isSequential);
        }
    }

    private LyricsViewHolder findHolder(int position) {
        if (adapter == null) return null;
        for (LyricsViewHolder holder : adapter.attachedHolders) {
            if (holder.getBindingAdapterPosition() == position) {
                return holder;
            }
        }
        return null;
    }

    private long getLineEndTime(LyricLine line) {
        if (line.words == null || line.words.isEmpty()) {
            return line.time + 2000;
        }
        long max = 0;
        for (LyricWord w : line.words) {
            if (w.endTime > max) max = w.endTime;
        }
        return max;
    }

    private void maybeAutoScroll(int targetIndex, boolean isSequential) {
        if (targetIndex == -1) return;
        if (System.currentTimeMillis() - lastUserTouchTime < AUTO_SCROLL_DELAY_MS) return;

        LinearLayoutManager lm = (LinearLayoutManager) getLayoutManager();
        if (lm == null) return;

        int firstVisible = lm.findFirstVisibleItemPosition();
        int lastVisible = lm.findLastVisibleItemPosition();

        boolean isAtAbsoluteTop = !canScrollVertically(-1);
        boolean isAtAbsoluteBottom = !canScrollVertically(1);

        if (isAtAbsoluteTop && targetIndex <= firstVisible) return;
        if (isAtAbsoluteBottom && targetIndex >= lastVisible) return;

        smoothScrollWithDynamicSpeed(targetIndex, isSequential);
    }

    private void smoothScrollWithDynamicSpeed(int position, boolean isSequential) {
        LinearLayoutManager lm = (LinearLayoutManager) getLayoutManager();
        if (lm == null) return;

        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        boolean isOffScreen = position < first || position > last;
        boolean fastMode = !isSequential || isOffScreen;

        CenterSmoothScroller scroller = new CenterSmoothScroller(getContext(), fastMode);
        scroller.setTargetPosition(position);
        lm.startSmoothScroll(scroller);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            isUserTouching = true;
            scrollHandler.removeCallbacks(snapBackRunnable);
            lastUserTouchTime = System.currentTimeMillis();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isUserTouching = false;
            lastUserTouchTime = System.currentTimeMillis();
            scrollHandler.removeCallbacks(snapBackRunnable);
            scrollHandler.postDelayed(snapBackRunnable, AUTO_SCROLL_DELAY_MS);
        }
        return super.onTouchEvent(e);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        scrollHandler.removeCallbacksAndMessages(null);
    }

    private static class CenterSmoothScroller extends LinearSmoothScroller {
        private final boolean fastMode;
        CenterSmoothScroller(Context context, boolean fastMode) {
            super(context);
            this.fastMode = fastMode;
        }

        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
            int boxOneThird = boxStart + (boxEnd - boxStart) / 3;
            int viewCenter = viewStart + (viewEnd - viewStart) / 2;
            return boxOneThird - viewCenter;
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return (fastMode ? 40f : 300f) / displayMetrics.densityDpi;
        }
    }
}
