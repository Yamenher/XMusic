package com.xapps.media.xmusic.utils;

import android.graphics.Bitmap;

import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.QuantizerCelebi;
import com.google.android.material.color.utilities.Scheme;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LegacyColorPaletteUtils {

    public static Map<String, Integer> lightColors;
    public static Map<String, Integer> darkColors;

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static volatile long lastBitmapHash = Long.MIN_VALUE;

    public interface ResultCallback {
        void onResult(Map<String, Integer> lightColors, Map<String, Integer> darkColors);
    }

    public static void generateFromBitmap(Bitmap bitmap, ResultCallback callback) {
        if (bitmap == null) return;

        executor.execute(() -> {
            try {
                long hash = hashBitmap(bitmap);

                if (hash == lastBitmapHash) {
                    if (callback != null && lightColors != null && darkColors != null) {
                        callback.onResult(lightColors, darkColors);
                    }
                    return;
                }

                lastBitmapHash = hash;

                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 128, 128, false);

                int[] pixels = new int[scaled.getWidth() * scaled.getHeight()];
                scaled.getPixels(
                        pixels, 0, scaled.getWidth(),
                        0, 0, scaled.getWidth(), scaled.getHeight()
                );

                Map<Integer, Integer> colorMap = QuantizerCelebi.quantize(pixels, 16);
                int totalPixels = pixels.length;

                int bestColor = 0;
                double bestScore = -1;

                for (Map.Entry<Integer, Integer> e : colorMap.entrySet()) {
                    int color = e.getKey();
                    int count = e.getValue();

                    Hct hct = Hct.fromInt(color);

                    double dominance = (double) count / totalPixels;
                    double chroma = hct.getChroma();
                    double tone = hct.getTone();

                    double penalty = chroma > 50 ? (chroma - 30) * 0.4 : 0;

                    double score =
                            (dominance * 100) +
                            (chroma * 0.4) -
                            penalty +
                            (20 - Math.abs(tone - 60));

                    if (score > bestScore) {
                        bestScore = score;
                        bestColor = color;
                    }
                }

                Hct seed = Hct.fromInt(bestColor);

                lightColors = generateMaterialTones(seed, false);
                darkColors = generateMaterialTones(seed, true);

                if (callback != null) {
                    callback.onResult(lightColors, darkColors);
                }

            } catch (Exception ignored) {
                if (callback != null) {
                    callback.onResult(new HashMap<>(), new HashMap<>());
                }
            }
        });
    }

    private static long hashBitmap(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false);

        int[] pixels = new int[scaled.getWidth() * scaled.getHeight()];
        scaled.getPixels(
                pixels, 0, scaled.getWidth(),
                0, 0, scaled.getWidth(), scaled.getHeight()
        );

        long hash = 1125899906842597L;
        for (int p : pixels) {
            hash = 31 * hash + p;
        }
        return hash;
    }

    private static Map<String, Integer> generateMaterialTones(Hct hct, boolean isDark) {
        Map<String, Integer> tones = new HashMap<>();

        double hue = hct.getHue();
        double chroma = hct.getChroma();
        boolean lowChroma = chroma < 10;

        tones.put("primary", Hct.from(hue, lowChroma ? chroma : 40, isDark ? 80 : 30).toInt());
        tones.put("onPrimary", Hct.from(hue, lowChroma ? chroma : 40, isDark ? 20 : 80).toInt());

        tones.put("tertiary", Hct.from(hue + 25, lowChroma ? chroma : 40, isDark ? 80 : 40).toInt());
        tones.put("onTertiary", Hct.from(hue + 25, lowChroma ? chroma : 40, isDark ? 20 : 80).toInt());

        tones.put("primaryContainer", Hct.from(hue, chroma, isDark ? 30 : 90).toInt());
        tones.put("onPrimaryContainer", Hct.from(hue, chroma, isDark ? 90 : 10).toInt());

        tones.put("surface", Hct.from(hue, lowChroma ? chroma : 25, isDark ? 7 : 95).toInt());
        tones.put("onSurface", Hct.from(hue, lowChroma ? chroma : 30, isDark ? 75 : 10).toInt());

        tones.put("surfaceContainer", Hct.from(hue, lowChroma ? chroma : 25, isDark ? 12 : 94).toInt());
        tones.put("onSurfaceContainer", Hct.from(hue, lowChroma ? chroma : 30, isDark ? 60 : 10).toInt());

        tones.put("outline", Hct.from(hue, lowChroma ? chroma : 25, isDark ? 30 : 70).toInt());

        return tones;
    }

    public static Scheme generateCustomScheme(int seedColor, boolean isDarkMode) {
        return isDarkMode ? Scheme.dark(seedColor) : Scheme.light(seedColor);
    }
}