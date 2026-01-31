package com.xapps.media.xmusic.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.text.LineBreaker;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import androidx.appcompat.widget.AppCompatTextView;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricWord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LyricLineView extends AppCompatTextView {

    private static final float INACTIVE_ALPHA = 0.18f;
    private static final float ACTIVE_ALPHA = 1.0f;
    private static final float ACTIVE_SCALE = 1.05f;
    private static final float INACTIVE_SCALE = 1.0f;

    private static final int FADE_DURATION_MS = 250;
    private static final int MIN_WORD_DURATION_MS = 150;
    private static final int MAX_WORD_DURATION_MS = 2000;
    private static final float MIN_GLOW_INTENSITY = 0.1f;
    private static final float MAX_GLOW_INTENSITY = 3.0f;

    private static final int MIN_SPLIT_THRESHOLD = 20;
    private static final int DEFAULT_SPLIT_CHARS = 20;

    private final Map<Integer, KaraokeSpan> spanMap = new HashMap<>();
    private final Map<Integer, Float> spanPeakGlowMap = new HashMap<>();
    private final Map<Integer, SpanTiming> spanTimingMap = new HashMap<>();

    private LyricLine lyricLine;
    private boolean isUpdating = false;
    private boolean isActiveLine = false;
    private boolean isFadedOut = true;

    private int currentPos = -1;

    private long lastUpdateTime = 0;
    private int lastProgressMs = 0;
    private long lineEndTime = 0;

    private ValueAnimator spanAlphaAnimator;

    private static class SpanTiming {
        long start;
        long end;

        SpanTiming(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class VirtualWord {
        String text;
        long start;
        long end;
        int startIndex;

        VirtualWord(String text, long start, long end, int startIndex) {
            this.text = text;
            this.start = start;
            this.end = end;
            this.startIndex = startIndex;
        }
    }

    private final Runnable updateRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (
                    /*isUpdating*/ false) {
                        long elapsed = System.currentTimeMillis() - lastUpdateTime;
                        updateSpanProgress(lastProgressMs + (int) elapsed);
                        invalidate();
                        postOnAnimation(this);
                    }
                }
            };

    public LyricLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipToOutline(false);
    }

    public void setLyricLine(LyricLine line) {
        removeCallbacks(updateRunnable);
        animate().cancel();
        if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();

        isUpdating = false;
        isActiveLine = false;
        isFadedOut = true;

        setAlpha(INACTIVE_ALPHA);
        setScaleX(INACTIVE_SCALE);
        setScaleY(INACTIVE_SCALE);

        spanMap.clear();
        spanPeakGlowMap.clear();
        spanTimingMap.clear();
        this.lyricLine = line;
        this.lineEndTime = 0;

        if (line.words == null || line.words.isEmpty()) {
            CharSequence raw = line.line;
            String text = raw.toString();
            SpannableString spannable = new SpannableString(text);
            TextPaint paint = getPaint();

            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            if (width <= 0) {
                post(() -> setLyricLine(line));
                return;
            }

            StaticLayout layout =
                    StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setIncludePad(false)
                            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                            .setHyphenationFrequency(LineBreaker.HYPHENATION_FREQUENCY_NONE)
                            .build();

            for (int i = 0; i < layout.getLineCount(); i++) {
                int start = layout.getLineStart(i);
                int end = layout.getLineEnd(i);

                if (start >= end) continue;

                String lineText = text.substring(start, end);
                int actualEnd = end;
                if (lineText.endsWith(" ") && end > start) {
                    actualEnd = end - 1;
                }

                createAndAttachSpan(
                        spannable, start, actualEnd, line.time, line.time, MIN_GLOW_INTENSITY);
            }

            for (KaraokeSpan span : spanMap.values()) {
                span.progress = 1f;
                span.alpha = 0f;
            }

            setText(spannable, BufferType.SPANNABLE);
            return;
        }

        for (LyricWord w : line.words) {
            if (w.getEndTime() > lineEndTime) lineEndTime = w.getEndTime();
        }

        boolean isFirstWordRtl = false;
        String firstWord = line.words.get(0).getText();
        if (firstWord.length() > 0) {
            byte dir = Character.getDirectionality(firstWord.charAt(0));
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                isFirstWordRtl = true;
            }
        }

        if (isFirstWordRtl) {
            setTextDirection(View.TEXT_DIRECTION_RTL);
            setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        } else {
            setTextDirection(View.TEXT_DIRECTION_LTR);
            setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        SpannableString spannable = new SpannableString(line.line);
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        Paint paint = getPaint();

        for (LyricWord w : line.words) {
            List<VirtualWord> virtualWords = splitMixedScripts(w);

            for (VirtualWord vw : virtualWords) {
                float peakIntensity = calculatePeakIntensity(vw.end - vw.start);
                String wordStr = vw.text;
                int len = wordStr.length();

                boolean needsSplit = false;
                int splitSize = DEFAULT_SPLIT_CHARS;

                if (len >= MIN_SPLIT_THRESHOLD) {
                    if (viewWidth > 0) {
                        float wordWidth = paint.measureText(wordStr);
                        if (wordWidth > viewWidth) {
                            needsSplit = true;
                            float avgCharWidth = wordWidth / len;
                            int charsThatFitScreen = (int) (viewWidth / avgCharWidth);
                            splitSize = Math.max(MIN_SPLIT_THRESHOLD, charsThatFitScreen - 2);
                        }
                    } else {
                        needsSplit = true;
                        splitSize = 20;
                    }
                }

                if (!needsSplit) {
                    createAndAttachSpan(
                            spannable,
                            vw.startIndex,
                            vw.startIndex + len,
                            vw.start,
                            vw.end,
                            peakIntensity);
                } else {
                    int chunks = (int) Math.ceil((double) len / splitSize);
                    long totalDuration = vw.end - vw.start;

                    for (int i = 0; i < chunks; i++) {
                        int chunkStartOffset = i * splitSize;
                        int chunkEndOffset = Math.min((i + 1) * splitSize, len);
                        int absStart = vw.startIndex + chunkStartOffset;
                        int absEnd = vw.startIndex + chunkEndOffset;
                        double startFraction = (double) chunkStartOffset / len;
                        double endFraction = (double) chunkEndOffset / len;
                        long chunkStartTime = vw.start + (long) (totalDuration * startFraction);
                        long chunkEndTime = vw.start + (long) (totalDuration * endFraction);

                        createAndAttachSpan(
                                spannable,
                                absStart,
                                absEnd,
                                chunkStartTime,
                                chunkEndTime,
                                peakIntensity);
                    }
                }
            }
        }
        setText(spannable, BufferType.SPANNABLE);
    }

    private List<VirtualWord> splitMixedScripts(LyricWord w) {
        List<VirtualWord> result = new ArrayList<>();
        String text = w.getText();
        if (text.isEmpty()) return result;

        int len = text.length();
        int lastType = getScriptType(text.charAt(0));
        int start = 0;

        for (int i = 1; i < len; i++) {
            char c = text.charAt(i);
            int currentType = getScriptType(c);
            if (currentType == 0) currentType = lastType;

            if (currentType != 0 && currentType != lastType) {
                addVirtualWord(result, w, text.substring(start, i), start);
                start = i;
                lastType = currentType;
            }
        }
        addVirtualWord(result, w, text.substring(start), start);
        return result;
    }

    private void addVirtualWord(
            List<VirtualWord> list, LyricWord parent, String subText, int relStart) {
        if (subText.isEmpty()) return;
        long totalDuration = parent.getEndTime() - parent.getStartTime();
        int parentLen = parent.getText().length();
        int subLen = subText.length();
        long startOffset = (long) (totalDuration * ((double) relStart / parentLen));
        long duration = (long) (totalDuration * ((double) subLen / parentLen));
        long subStart = parent.getStartTime() + startOffset;
        long subEnd = subStart + duration;
        list.add(new VirtualWord(subText, subStart, subEnd, parent.startIndex + relStart));
    }

    private int getScriptType(char c) {
        byte dir = Character.getDirectionality(c);
        if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) return 1;
        if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) return -1;
        return 0;
    }

    private void createAndAttachSpan(
            SpannableString spannable,
            int start,
            int end,
            long startTime,
            long endTime,
            float peakIntensity) {
        if (start >= spannable.length()) return;
        if (end > spannable.length()) end = spannable.length();
        KaraokeSpan span = new KaraokeSpan(ACTIVE_ALPHA);
        spanMap.put(start, span);
        spanPeakGlowMap.put(start, peakIntensity);
        spanTimingMap.put(start, new SpanTiming(startTime, endTime));
        spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private float calculatePeakIntensity(long duration) {
        if (duration <= MIN_WORD_DURATION_MS) return MIN_GLOW_INTENSITY;
        if (duration >= MAX_WORD_DURATION_MS) return MAX_GLOW_INTENSITY;
        float progress =
                (float) (duration - MIN_WORD_DURATION_MS)
                        / (MAX_WORD_DURATION_MS - MIN_WORD_DURATION_MS);
        return MIN_GLOW_INTENSITY + (progress * (MAX_GLOW_INTENSITY - MIN_GLOW_INTENSITY));
    }

    public void setCurrent(boolean isCurrent, int position) {
        this.currentPos = position;
        this.isActiveLine = isCurrent;

        if (!isCurrent) {
            if (isUpdating) {
                isUpdating = false;
                removeCallbacks(updateRunnable);
            }
            animateFadeOut(position);
            return;
        }

        if (this.isUpdating == isCurrent) return;
        this.isUpdating = isCurrent;

        if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();

        boolean isStandardLrc = (lyricLine.words == null || lyricLine.words.isEmpty());
        for (KaraokeSpan span : spanMap.values()) {
            span.progress = isStandardLrc ? 1.0f : -1.0f;
            span.glowAlpha = 0f;
            span.alpha = 0f;
        }
        isFadedOut = false;
        setAlpha(1.0f);

        spanAlphaAnimator = ValueAnimator.ofFloat(0f, ACTIVE_ALPHA);
        spanAlphaAnimator.setDuration(FADE_DURATION_MS);
        spanAlphaAnimator.addUpdateListener(
                animation -> {
                    float val = (float) animation.getAnimatedValue();
                    for (KaraokeSpan span : spanMap.values()) {
                        span.alpha = val;
                    }
                    invalidate();
                });
        spanAlphaAnimator.start();

        if (!isStandardLrc) {
            lastUpdateTime = System.currentTimeMillis();
            postOnAnimation(updateRunnable);
        }

        if ((getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
            setPivotX(getWidth());
        } else {
            setPivotX(0f);
        }
        setPivotY(getHeight() / 2f);

        animate().scaleX(ACTIVE_SCALE).scaleY(ACTIVE_SCALE).setDuration(FADE_DURATION_MS).start();
    }

    private void animateFadeOut(int nextActivePos) {
        if (isFadedOut) return;

        boolean isWordedLine =
                lyricLine != null
                        && !lyricLine.isRomaji
                        && lyricLine.words != null
                        && !lyricLine.words.isEmpty();

        if (isWordedLine && (nextActivePos > currentPos && nextActivePos <= currentPos + 2)) {
            if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();

            animate()
                    .alpha(1.0f)
                    .scaleX(INACTIVE_SCALE)
                    .scaleY(INACTIVE_SCALE)
                    .setDuration(FADE_DURATION_MS)
                    .start();

            for (KaraokeSpan span : spanMap.values()) {
                span.glowAlpha = 0f;
                span.alpha = ACTIVE_ALPHA;
                span.progress = 1.0f;
            }
            invalidate();
            return;
        }

        isFadedOut = true;
        if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();

        spanAlphaAnimator = ValueAnimator.ofFloat(ACTIVE_ALPHA, 0.0f);
        spanAlphaAnimator.setDuration(FADE_DURATION_MS);
        spanAlphaAnimator.addUpdateListener(
                animation -> {
                    float val = (float) animation.getAnimatedValue();
                    for (KaraokeSpan span : spanMap.values()) {
                        span.alpha = val;
                        span.glowAlpha = Math.min(span.glowAlpha, val);
                    }
                    invalidate();
                });

        animate()
                .scaleX(INACTIVE_SCALE)
                .scaleY(INACTIVE_SCALE)
                .setDuration(FADE_DURATION_MS)
                .withEndAction(
                        () -> {
                            for (KaraokeSpan span : spanMap.values()) {
                                span.progress = -1.0f;
                                span.alpha = 0f;
                            }
                            invalidate();
                        })
                .start();

        spanAlphaAnimator.start();
    }

    public void setCurrentProgress(int progressMs) {
        if (!isActiveLine) return;
        this.lastProgressMs = progressMs;
        this.lastUpdateTime = System.currentTimeMillis();
        updateSpanProgress(progressMs);
        invalidate();
    }

    private void updateSpanProgress(int progressMs) {
        if (!isActiveLine) return;
        if (spanMap.isEmpty()) return;

        for (Map.Entry<Integer, KaraokeSpan> entry : spanMap.entrySet()) {
            int startIndex = entry.getKey();
            KaraokeSpan span = entry.getValue();
            SpanTiming timing = spanTimingMap.get(startIndex);
            if (timing == null) continue;

            Float peakRef = spanPeakGlowMap.get(startIndex);
            float peakIntensity = (peakRef != null) ? peakRef : MIN_GLOW_INTENSITY;

            if (progressMs < timing.start) {
                span.progress = -1.0f;
                span.glowAlpha = 0.0f;
                span.alpha = ACTIVE_ALPHA;
            } else if (progressMs >= timing.end) {
                span.progress = 1.0f;
                span.glowAlpha = 0.0f;
                span.alpha = ACTIVE_ALPHA;
            } else {
                float wordProgress =
                        (float) (progressMs - timing.start) / (timing.end - timing.start);
                wordProgress = Math.max(0f, Math.min(1f, wordProgress));
                span.progress = wordProgress;
                float sineCurve = (float) Math.sin(wordProgress * Math.PI);
                span.glowAlpha = sineCurve * peakIntensity;
                span.alpha = ACTIVE_ALPHA;
            }
        }
    }

    /*private void animateFadeOut(int p) {
        if (isFadedOut) return;
        isFadedOut = true;

        if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();
        spanAlphaAnimator = ValueAnimator.ofFloat(ACTIVE_ALPHA, 0.0f);
        spanAlphaAnimator.setDuration(FADE_DURATION_MS);
        spanAlphaAnimator.addUpdateListener(
                animation -> {
                    float val = (float) animation.getAnimatedValue();
                    for (KaraokeSpan span : spanMap.values()) {
                        span.alpha = val;
                        span.glowAlpha = Math.min(span.glowAlpha, val);
                    }
                    invalidate();
                });

        animate()
                .alpha(ACTIVE_ALPHA)
                .scaleX(INACTIVE_SCALE)
                .scaleY(INACTIVE_SCALE)
                .setDuration(FADE_DURATION_MS)
                .withEndAction(
                        () -> {
                            for (KaraokeSpan span : spanMap.values()) {
                                span.progress = -1.0f;
                                span.alpha = 0f;
                            }
                            invalidate();
                        })
                .start();

        spanAlphaAnimator.start();
    }*/

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(updateRunnable);
        if (spanAlphaAnimator != null) spanAlphaAnimator.cancel();
        isUpdating = false;
        setCurrent(false, currentPos);
    }
}
