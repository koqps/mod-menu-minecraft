package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.mixin.OverlayTextureAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.lang.reflect.Method;

public final class HitColorController {
    private static int lastColor = Integer.MIN_VALUE;
    private static boolean lastEnabled;
    private static OverlayTexture overlayTexture;

    private HitColorController() { }

    public static void tick() {
        boolean enabled = TopkaClient.MODULES.byId("hit_color").enabled();
        int color = enabled ? TopkaClient.CONFIG.get().hitColorArgb : 0xB2FF0000;
        if (enabled == lastEnabled && color == lastColor) return;

        OverlayTexture overlay = findOverlayTexture();
        if (overlay == null) return;

        DynamicTexture texture = ((OverlayTextureAccessor) overlay).topka$getTexture();
        NativeImage image = texture.getPixels();
        if (image == null) return;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                image.setPixelABGR(x, y, color);
            }
        }
        texture.upload();
        lastEnabled = enabled;
        lastColor = color;
    }

    private static OverlayTexture findOverlayTexture() {
        if (overlayTexture != null) return overlayTexture;
        Object renderer = Minecraft.getInstance().gameRenderer;
        for (Method method : renderer.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == OverlayTexture.class) {
                try {
                    overlayTexture = (OverlayTexture) method.invoke(renderer);
                    return overlayTexture;
                } catch (ReflectiveOperationException ignored) { }
            }
        }
        return null;
    }
}
