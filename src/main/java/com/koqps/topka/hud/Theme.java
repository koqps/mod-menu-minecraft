package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;

public final class Theme {
    private Theme() { }

    public static int accent() {
        var cfg = TopkaClient.CONFIG.get();
        if (cfg.rainbowTheme) {
            return rainbow(cfg.themeAnimationSpeed, 0.0F);
        }
        if (cfg.gradientTheme) {
            float phase = animationPhase(cfg.themeAnimationSpeed);
            return lerpColor(cfg.accentArgb, cfg.secondaryAccentArgb, pingPong(phase));
        }
        return cfg.accentArgb;
    }

    public static int secondary() {
        var cfg = TopkaClient.CONFIG.get();
        if (cfg.rainbowTheme) {
            return rainbow(cfg.themeAnimationSpeed, 0.22F);
        }
        if (cfg.gradientTheme) {
            float phase = animationPhase(cfg.themeAnimationSpeed);
            return lerpColor(cfg.secondaryAccentArgb, cfg.accentArgb, pingPong(phase));
        }
        return cfg.secondaryAccentArgb;
    }

    public static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    private static float animationPhase(float configuredSpeed) {
        float speed = Math.clamp(configuredSpeed, 0.05F, 2.0F);
        return (System.currentTimeMillis() / 1000.0F) * speed;
    }

    private static float pingPong(float value) {
        float wrapped = value - (float) Math.floor(value / 2.0F) * 2.0F;
        return wrapped <= 1.0F ? wrapped : 2.0F - wrapped;
    }

    private static int rainbow(float configuredSpeed, float offset) {
        float speed = Math.clamp(configuredSpeed, 0.05F, 2.0F);
        float hue = ((System.currentTimeMillis() / 1000.0F) * speed * 0.16F + offset) % 1.0F;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.72F, 1.0F) & 0x00FFFFFF);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.clamp(t, 0.0F, 1.0F);
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int oa = Math.round(aa + (ba - aa) * t);
        int or = Math.round(ar + (br - ar) * t);
        int og = Math.round(ag + (bg - ag) * t);
        int ob = Math.round(ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }
}
