package com.xapps.media.xmusic.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.palette.graphics.Palette;
import com.google.android.material.color.utilities.CorePalette;
import com.google.android.material.color.utilities.TonalPalette;
import com.google.android.material.color.utilities.Hct;

import java.util.HashMap;
import java.util.Map;

public class ColorPaletteUtils {

    public static Map<String, Integer> lightColors;
    public static Map<String, Integer> darkColors;

    public interface ResultCallback {
        void onResult(Map<String, Integer> lightColors, Map<String, Integer> darkColors);
    }

    public static void generateFromBitmap(Bitmap bitmap, ResultCallback callback) {
        Palette.from(bitmap).generate(palette -> {
            int seedColor = palette.getDominantColor(0xFF326941);
            Hct h = Hct.fromInt(seedColor);
            CorePalette core = CorePalette.of(seedColor);

            lightColors = generateMaterialTones(h, false);
            darkColors = generateMaterialTones(h, true);

            callback.onResult(lightColors, darkColors);
        });
    }

    private static Map<String, Integer> extractColorRoles(CorePalette core, boolean isDark) {
        Map<String, Integer> roles = new HashMap<>();

        TonalPalette a1 = core.a1;
        TonalPalette n1 = core.n1;

        roles.put("primary", a1.tone(isDark ? 80 : 40));
        roles.put("onPrimary", a1.tone(isDark ? 20 : 100));
        roles.put("primaryContainer", a1.tone(isDark ? 30 : 90));
        roles.put("onPrimaryContainer", a1.tone(isDark ? 90 : 10));

        roles.put("surface", n1.tone(isDark ? 6 : 98));
        roles.put("onSurface", n1.tone(isDark ? 90 : 10));
        roles.put("surfaceContainer", n1.tone(isDark ? 12 : 94));
        roles.put("onSurfaceContainer", n1.tone(isDark ? 90 : 10));

        roles.put("background", n1.tone(isDark ? 6 : 98));
        roles.put("onBackground", n1.tone(isDark ? 90 : 10));
        roles.put("outline", n1.tone(isDark ? 60 : 50));

        return roles;
    }

    private static Map<String, Integer> generateMaterialTones(Hct hct, boolean isDark) {
    Map<String, Integer> tones = new HashMap<>();

    double hue = hct.getHue();
    double chroma = hct.getChroma(); // might want to clamp this to 48–64 for consistency

    tones.put("primary", Hct.from(hue, chroma, isDark ? 80 : 30).toInt());
    tones.put("onPrimary", Hct.from(hue, chroma, isDark ? 20 : 80).toInt());
	tones.put("tertiary", Hct.from(hue+25, chroma, isDark ? 80 : 40).toInt());
    tones.put("onTertiary", Hct.from(hue+25, chroma, isDark ? 20 : 80).toInt());
    tones.put("primaryContainer", Hct.from(hue, chroma, isDark ? 30 : 90).toInt());
    tones.put("onPrimaryContainer", Hct.from(hue, chroma, isDark ? 90 : 10).toInt());

    tones.put("surface", Hct.from(hue, chroma * 0.9, isDark ? 6 : 96).toInt());
    tones.put("onSurface", Hct.from(hue, chroma * 0.3, isDark ? 83 : 10).toInt());
    tones.put("surfaceContainer", Hct.from(hue, chroma * 0.15, isDark ? 12 : 94).toInt());
    tones.put("onSurfaceContainer", Hct.from(hue, chroma * 0.35, isDark ? 60 : 10).toInt());

    tones.put("background", Hct.from(hue, chroma * 0.1, isDark ? 6 : 98).toInt());
    tones.put("onBackground", Hct.from(hue, chroma * 0.2, isDark ? 90 : 10).toInt());
    tones.put("outline", Hct.from(hue, chroma * 0.6, isDark ? 30 : 70).toInt());

    return tones;
}
}