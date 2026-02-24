package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xapps.media.xmusic.common.PlaybackControlListener;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LyricsView extends ScrollingView2 {

    private final List<LyricLine> lines = new ArrayList<>();
    private final List<LyricLineCanvasView> lineViews = new ArrayList<>();

    private final List<Integer> currentActiveIndices = new ArrayList<>();
    private final List<Integer> persistedActiveIndices = new ArrayList<>();

    private int[] lineTops = new int[0];
    private int contentHeight;

    private int lastTopActiveIndex = -1;
    private boolean allowAutoScroll = true;

    private static final long AUTO_SCROLL_DELAY_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private PlaybackControlListener seekListener;

    private int normalLineSpacingPx;
    private int horizontalPaddingPx;

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = context.getResources().getDisplayMetrics().density;
        normalLineSpacingPx = (int) (48 * d);
        horizontalPaddingPx = (int) (16 * d);
    }

    /* ---------------------------------------- */

    private final Runnable frameTick = new Runnable() {
        @Override
        public void run() {
            boolean needsRedraw = false;

            int scrollY = getScrollY();
            int h = getHeight();
    
            int max = Math.min(lineViews.size(), lineTops.length);

            for (int i = 0; i < max; i++) {
                int top = lineTops[i];
                int bottom = top + lineViews.get(i).getMeasuredHeight();

                if (bottom < scrollY || top > scrollY + h) continue;

                if (lineViews.get(i).consumeDirty()) {
                    needsRedraw = true;
                }
            }
            invalidate();
            postOnAnimation(this);
        }
    };

    /* --------------------------------------- */

    public void setLyrics(List<LyricLine> lyricLines) {
        lines.clear();
        lineViews.clear();
        currentActiveIndices.clear();
        persistedActiveIndices.clear();
        lastTopActiveIndex = -1;

        removeCallbacks(frameTick);
        handler.removeCallbacksAndMessages(null);

        if (lyricLines != null) {
            lines.addAll(lyricLines);
            for (LyricLine l : lines) {
                LyricLineCanvasView v = new LyricLineCanvasView(getContext(), null);
                boolean small = l.isRomaji || l.isBackground;
                v.setTextSizeSp(small ? 18f : 35f);
                v.setLyricLine(l);
                lineViews.add(v);
            }
        }

        requestLayout();
        invalidate();
        smoothScrollTo(0, 0, 350);
    }

    public void configureSyncedLyrics(boolean synced, Typeface tf, int gravity, float textSizeSp) {
        for (LyricLineCanvasView v : lineViews) {
            v.setTypeface(tf);
        }
        requestLayout();
        invalidate();
    }

    public void setLyricColor(int color) {
        for (LyricLineCanvasView v : lineViews) {
            v.setColor(color);
        }
        invalidate();
    }

    public void setOnSeekListener(PlaybackControlListener l) {
        seekListener = l;
    }

    public void onProgress(int progressMs) {
        updateActiveLines(progressMs);
    }

    /* ---------------------------------------- */

    private void updateActiveLines(int progressMs) {
        if (lines.isEmpty()) return;

        List<Integer> newActive = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            if (line.isRomaji) continue;

            long start = line.time;
            long end = getLineEndTime(i);

            if (progressMs >= start && progressMs <= end) {
                newActive.add(i);
            }
        }

        if (newActive.isEmpty() && !persistedActiveIndices.isEmpty()) {
            newActive.addAll(persistedActiveIndices);
        }

        currentActiveIndices.clear();
        currentActiveIndices.addAll(newActive);

        persistedActiveIndices.clear();
        persistedActiveIndices.addAll(newActive);

        int max = Math.min(
            Math.min(lines.size(), lineViews.size()),
            lineTops.length
        );

        for (int i = 0; i < max; i++) {
            LyricLineCanvasView v = lineViews.get(i);

            boolean isActive;
            if (lines.get(i).isRomaji) {
                isActive = i > 0 && currentActiveIndices.contains(i - 1);
            } else {
                isActive = currentActiveIndices.contains(i);
            }

            v.setCurrent(isActive, i);
            if (isActive) {
                v.setCurrentProgress(progressMs);
            }
        }

        maybeStartFrameLoop();
        maybeAutoScroll();
    }

    private void maybeStartFrameLoop() {
        int scrollY = getScrollY();
        int h = getHeight();

        int topsLen = lineTops.length;
        int viewsLen = lineViews.size();

        for (int idx : persistedActiveIndices) {
            if (idx < 0 || idx >= topsLen || idx >= viewsLen) continue;

            int top = lineTops[idx];
            int bottom = top + lineViews.get(idx).getMeasuredHeight();

            if (bottom >= scrollY && top <= scrollY + h) {
                removeCallbacks(frameTick);
                postOnAnimation(frameTick);
                return;
            }
        }
    }

    private void maybeAutoScroll() {
        if (!allowAutoScroll || persistedActiveIndices.isEmpty()) return;

        int top = Collections.min(persistedActiveIndices);
        if (top != lastTopActiveIndex) {
            lastTopActiveIndex = top;
            centerLine(top);
        }
    }

    /* -------------------------------------- */

    @Override
    protected void onMeasureForChild(int widthSpec, int heightSpec) {
        int fullWidth = MeasureSpec.getSize(widthSpec);
        int viewHeight = MeasureSpec.getSize(heightSpec);
        if (viewHeight <= 0) {
            viewHeight = getResources().getDisplayMetrics().heightPixels;
        }

        int width = fullWidth - horizontalPaddingPx * 2;
        int y = viewHeight / 3;

        lineTops = new int[lineViews.size()];

        for (int i = 0; i < lineViews.size(); i++) {
            if (!lines.get(i).isRomaji && i != 0) {
                y += normalLineSpacingPx;
            }

            LyricLineCanvasView v = lineViews.get(i);
            v.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );

            lineTops[i] = y;
            y += v.getMeasuredHeight();
        }

        contentHeight = y + viewHeight / 3;
        setChildMeasuredDimension(fullWidth, contentHeight);
    }

    @Override
    protected void onDrawForChild(@NonNull Canvas canvas) {
        int topVisible = getTopVisibleIndex();
        int bottomVisible = getBottomVisibleIndex();

        if (topVisible < 0 || bottomVisible < 0) return;

        int max = Math.min(lineViews.size(), lineTops.length);
        int start = Math.max(0, topVisible);
        int end = Math.min(bottomVisible, max - 1);

        for (int i = start; i <= end; i++) {
            int top = lineTops[i];

            canvas.save();
            canvas.translate(horizontalPaddingPx, top);
            lineViews.get(i).draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onLayoutForChild(int l, int t, int r, int b) {}
    
    private final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(getContext(), new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                handleLineClick(e.getX(), e.getY());
                return true;
            }
        }
    );
    
    private void handleLineClick(float x, float y) {
        if (lines.isEmpty() || lineTops.length == 0 || seekListener == null) return;

        int contentY = (int) y + getScrollY();

        int topIndex = getTopVisibleIndex();
        int bottomIndex = getBottomVisibleIndex();

        if (topIndex < 0 || bottomIndex < 0) return;

        int start = Math.max(0, topIndex);
        int end = Math.min(bottomIndex, lineTops.length - 1);

        for (int i = start; i <= end; i++) {
            int top = lineTops[i];
            int bottom = top + lineViews.get(i).getMeasuredHeight();

            if (contentY >= top && contentY <= bottom) {
                LyricLine clicked = lines.get(i);
                if (clicked.isRomaji && i > 0) {
                    seekListener.onSeekRequested(lines.get(i - 1).time);
                } else {
                    seekListener.onSeekRequested(clicked.time);
                }
                return;
            }
        }
    }

    /* --------------------------------------- */

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
            allowAutoScroll = false;
            handler.removeCallbacksAndMessages(null);
        } else if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            handler.postDelayed(() -> allowAutoScroll = true, AUTO_SCROLL_DELAY_MS);
        }
        return super.onTouchEvent(e);
    }
    
    @Override
    protected boolean onTouchEventForChild(@NonNull MotionEvent event) {
        return false;
    }

    /* ---------------------------------------- */

    private void centerLine(int index) {
        if (index < 0) return;
        if (index >= lineTops.length) return;
        if (index >= lineViews.size()) return;
    
        int target = lineTops[index] - getHeight() / 3;
        smoothScrollTo(0, Math.max(0, target));
    }
    
    private int getTopVisibleIndex() {
        int scrollY = getScrollY();

        int low = 0;
        int high = Math.min(lineTops.length, lineViews.size()) - 1;
        if (high < 0) return -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int bottom = lineTops[mid] + lineViews.get(mid).getMeasuredHeight();

            if (bottom <= scrollY) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low <= high + 1 ? low : -1;
    }
    
    private int getBottomVisibleIndex() {
        int viewportBottom = getScrollY() + getHeight();

        int low = 0;
        int high = Math.min(lineTops.length, lineViews.size()) - 1;
        if (high < 0) return -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
    
            if (lineTops[mid] < viewportBottom) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return high >= 0 ? high : -1;
    }

    private long getLineEndTime(int index) {
        if (index < 0 || index >= lines.size()) return 0;

        LyricLine line = lines.get(index);

        if (line.isSimpleLRC) {
            if (line.endTime > 0) return line.endTime -250;

            for (int i = index + 1; i < lines.size(); i++) {
                if (!lines.get(i).isRomaji) return lines.get(i).time;
            }
            return line.time + 10000;
        } else {
            long max = 0;
            for (LyricWord w : line.words) {
                max = Math.max(max, w.getEndTime());
            }
            return max;
        }
    }
}