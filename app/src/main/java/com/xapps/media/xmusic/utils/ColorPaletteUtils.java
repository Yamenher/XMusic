package com.xapps.media.xmusic.utils;

import android.graphics.Bitmap;

import com.google.android.material.color.utilities.CorePalette;
import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.QuantizerCelebi;
import com.google.android.material.color.utilities.Score;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false);

                int[] pixels = new int[scaled.getWidth() * scaled.getHeight()];
                scaled.getPixels(pixels, 0, scaled.getWidth(), 0, 0, scaled.getWidth(), scaled.getHeight());

                Map<Integer, Integer> colorMap = QuantizerCelebi.quantize(pixels, 128);
                List<Integer> ranked = Score.score(colorMap);
                int seedColor = ranked.isEmpty() ? 0xFF326941 : ranked.get(0);

                Hct h = Hct.fromInt(seedColor);
                CorePalette core = CorePalette.of(seedColor);

                lightColors = generateMaterialTones(h, false);
                darkColors = generateMaterialTones(h, true);

                if (callback != null) {
                    callback.onResult(lightColors, darkColors);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onResult(new HashMap<>(), new HashMap<>());
                }
            }
        });
    }

    private static Map<String, Integer> generateMaterialTones(Hct hct, boolean isDark) {
        Map<String, Integer> tones = new HashMap<>();

        double hue = hct.getHue();
        double chroma = hct.getChroma();

        tones.put("primary", Hct.from(hue, chroma, isDark ? 80 : 30).toInt());
        tones.put("onPrimary", Hct.from(hue, chroma, isDark ? 20 : 80).toInt());
        tones.put("tertiary", Hct.from(hue + 25, chroma, isDark ? 80 : 40).toInt());
        tones.put("onTertiary", Hct.from(hue + 25, chroma, isDark ? 20 : 80).toInt());
        tones.put("primaryContainer", Hct.from(hue, chroma, isDark ? 30 : 90).toInt());
        tones.put("onPrimaryContainer", Hct.from(hue, chroma, isDark ? 90 : 10).toInt());

        tones.put("surface", Hct.from(hue, chroma * 0.75, isDark ? 6 : 96).toInt());
        tones.put("onSurface", Hct.from(hue, chroma * 0.3, isDark ? 83 : 10).toInt());
        tones.put("surfaceContainer", Hct.from(hue, chroma * 0.15, isDark ? 12 : 94).toInt());
        tones.put("onSurfaceContainer", Hct.from(hue, chroma * 0.35, isDark ? 60 : 10).toInt());

        tones.put("background", Hct.from(hue, chroma * 0.1, isDark ? 6 : 98).toInt());
        tones.put("onBackground", Hct.from(hue, chroma * 0.2, isDark ? 90 : 10).toInt());
        tones.put("outline", Hct.from(hue, chroma * 0.6, isDark ? 30 : 70).toInt());

        return tones;
    }
}