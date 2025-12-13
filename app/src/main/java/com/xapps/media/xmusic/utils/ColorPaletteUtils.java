package com.xapps.media.xmusic.utils;

import android.graphics.Bitmap;

import com.google.android.material.color.utilities.CorePalette;
import com.google.android.material.color.utilities.DynamicScheme;
import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.QuantizerCelebi;
import com.google.android.material.color.utilities.Scheme;
import com.google.android.material.color.utilities.Score;
import com.google.android.material.color.utilities.TonalPalette;
import com.google.android.material.color.utilities.Variant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ColorPaletteUtils {

    public static Map<String, Integer> lightColors;
    public static Map<String, Integer> darkColors;

    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public interface ResultCallback {
        void onResult(Map<String, Integer> lightColors, Map<String, Integer> darkColors);
    }

    public static void generateFromBitmap(Bitmap bitmap, ResultCallback callback) {
    executor.execute(() -> {
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 128, 128, false);

            int[] pixels = new int[scaled.getWidth() * scaled.getHeight()];
            scaled.getPixels(pixels, 0, scaled.getWidth(), 0, 0, scaled.getWidth(), scaled.getHeight());

            Map<Integer, Integer> colorMap = QuantizerCelebi.quantize(pixels, 16);
            int totalPixels = scaled.getWidth() * scaled.getHeight();

            int bestColor = 0;
            double bestScore = -1;

            for (Map.Entry<Integer, Integer> e : colorMap.entrySet()) {
                int color = e.getKey();
                int count = e.getValue();

                Hct h = Hct.fromInt(color);

                double dominance = (double) count / totalPixels;
                double chroma = h.getChroma();
                double tone = h.getTone();

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

            if (callback != null) callback.onResult(lightColors, darkColors);

        } catch (Exception e) {
            if (callback != null) callback.onResult(new HashMap<>(), new HashMap<>());
        }
    });
}

    private static Map<String, Integer> generateMaterialTones(Hct hct, boolean isDark) {
        Map<String, Integer> tones = new HashMap<>();

        double hue = hct.getHue();
        double chroma = hct.getChroma();
        
        boolean chromaTooLow = chroma < 10;

        tones.put("primary", Hct.from(hue, chromaTooLow? chroma : 40, isDark ? 80 : 30).toInt());
        tones.put("onPrimary", Hct.from(hue, chromaTooLow? chroma : 40, isDark ? 20 : 80).toInt());
        tones.put("tertiary", Hct.from(hue + 25, chromaTooLow? chroma : 40, isDark ? 80 : 40).toInt());
        tones.put("onTertiary", Hct.from(hue + 25, chromaTooLow? chroma : 40, isDark ? 20 : 80).toInt());
        tones.put("primaryContainer", Hct.from(hue, chroma, isDark ? 30 : 90).toInt());
        tones.put("onPrimaryContainer", Hct.from(hue, chroma, isDark ? 90 : 10).toInt());

        tones.put("surface", Hct.from(hue, chromaTooLow? chroma : 25, isDark ? 7 : 95).toInt());
        tones.put("onSurface", Hct.from(hue, chromaTooLow? chroma : 30, isDark ? 75 : 10).toInt());
        tones.put("surfaceContainer", Hct.from(hue, chromaTooLow? chroma : 25, isDark ? 12 : 94).toInt());
        tones.put("onSurfaceContainer", Hct.from(hue, chromaTooLow? chroma : 30, isDark ? 60 : 10).toInt());
        
        tones.put("outline", Hct.from(hue, chromaTooLow? chroma : 25, isDark ? 30 : 70).toInt());

        return tones;
    }

    public static Scheme generateCustomScheme(int seedColor, boolean isDarkMode) {
        if (isDarkMode) {
            return Scheme.dark(seedColor);
        } else {
            return Scheme.light(seedColor);
        }
    }
}