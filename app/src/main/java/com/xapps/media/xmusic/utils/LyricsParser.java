package com.xapps.media.xmusic.lyric;

import android.text.SpannableString;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricWord;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsParser {

    private static final Pattern OFFSET_PATTERN = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_IGNORE_PATTERN = Pattern.compile("^\\[(by|ar|ti|al|au|length|re):.*\\]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
    private static final Pattern WORD_TIME_PATTERN = Pattern.compile("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)");

    public static List<LyricLine> parse(InputStream inputStream) {
        try (PushbackInputStream pb = new PushbackInputStream(inputStream, 20)) {
            int firstByte = pb.read();
            if (firstByte == -1) return Collections.emptyList();
            pb.unread(firstByte);
            return firstByte == '<' ? handleTtml(pb) : parseLrcStream(pb);
        } catch (Exception e) { return Collections.emptyList(); }
    }

    private static List<LyricLine> handleTtml(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        byte[] bytes = TtmlToElrc.convert(sb.toString()).getBytes(StandardCharsets.UTF_8);
        return parseLrcStream(new ByteArrayInputStream(bytes));
    }

    private static List<LyricLine> parseLrcStream(InputStream in) {
        List<LyricLine> result = new ArrayList<>();
        long globalOffset = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = br.readLine()) != null) {
                String line = rawLine.trim();
                if (line.isEmpty() || METADATA_IGNORE_PATTERN.matcher(line).matches()) continue;
                
                Matcher om = OFFSET_PATTERN.matcher(line);
                if (om.find()) { 
                    globalOffset = Long.parseLong(om.group(1)); 
                    continue; 
                }

                if (line.startsWith("[bg:") && line.endsWith("]")) {
                    String inner = line.substring(4, line.length() - 1).trim();
                    Matcher wm = WORD_TIME_PATTERN.matcher(inner);
                    if (wm.find()) {
                        long startTime = parseTimestamp(wm.group(1), wm.group(2), wm.group(3)) + globalOffset;
                        LyricLine lyricLine = processContent("bg: " + inner, startTime, globalOffset);
                        if (lyricLine != null) result.add(lyricLine);
                    }
                    continue;
                }

                Matcher lm = LINE_TIME_PATTERN.matcher(line);
                if (lm.find()) {
                    long startTime = parseTimestamp(lm.group(1), lm.group(2), lm.group(3)) + globalOffset;
                    LyricLine lyricLine = processContent(lm.group(4).trim(), startTime, globalOffset);
                    if (lyricLine != null) result.add(lyricLine);
                }
            }
            if (!result.isEmpty()) {
                result.sort(Comparator.comparingLong(l -> l.time));
                finalizeTimings(result);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static LyricLine processContent(String content, long lineStartTime, long globalOffset) {
        List<LyricWord> words = new ArrayList<>();
        int vocalType = 1; 
        boolean isBackground = false; 
        String t = content;

        if (t.startsWith("bg:")) {
            isBackground = true;
            t = t.substring(3).trim();
        } else if (t.startsWith("[bg:") && t.endsWith("]")) {
            isBackground = true;
            t = t.substring(4, t.length() - 1).trim();
        }
        
        String lower = t.toLowerCase();
        if (lower.startsWith("v1:")) { vocalType = 1; t = t.substring(3).trim(); }
        else if (lower.startsWith("v2:")) { vocalType = 2; t = t.substring(3).trim(); }

        Matcher wm = WORD_TIME_PATTERN.matcher(t);
        StringBuilder rawTextBuilder = new StringBuilder();
        int cursor = 0;
        
        while (wm.find()) {
            long wordStart = parseTimestamp(wm.group(1), wm.group(2), wm.group(3)) + globalOffset;
            String wordText = wm.group(4);
            rawTextBuilder.append(wordText);
            words.add(new LyricWord((int)wordStart, wordText, cursor, 0));
            cursor += wordText.length();
        }

        String finalLyrics = words.isEmpty() ? t : rawTextBuilder.toString();
        LyricLine line = new LyricLine((int)lineStartTime, new SpannableString(finalLyrics), words);
        line.vocalType = vocalType;
        line.isBackground = isBackground;
        return line;
    }

    private static long parseTimestamp(String min, String sec, String msStr) {
        int m = Integer.parseInt(min);
        int s = Integer.parseInt(sec);
        int ms = Integer.parseInt(msStr) * (msStr.length() == 2 ? 10 : 1);
        return (m * 60L + s) * 1000L + ms;
    }

    private static void finalizeTimings(List<LyricLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            LyricLine curr = lines.get(i);
            long nextStart = (i + 1 < lines.size()) ? lines.get(i + 1).time : curr.time + 3000;
            if (!curr.words.isEmpty()) {
                for (int w = 0; w < curr.words.size(); w++) {
                    LyricWord currentWord = curr.words.get(w);
                    if (w + 1 < curr.words.size()) {
                        currentWord.endTime = curr.words.get(w + 1).timestamp-10;
                    } else {
                        currentWord.endTime = (int) nextStart-10;
                    }
                }
            }
        }
    }
}
