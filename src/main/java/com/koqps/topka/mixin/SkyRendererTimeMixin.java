package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.3 moved sun/moon/star positioning into EnvironmentAttributes
 * which SkyRenderer copies into SkyRenderState each frame. Override the final
 * render state instead of mutating world time, so server packets cannot cause
 * one-frame snaps or flicker.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererTimeMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void modmenu$applyCustomTime(
            ClientLevel level,
            float partialTicks,
            Camera camera,
            SkyRenderState state,
            CallbackInfo ci
    ) {
        if (!TopkaClient.MODULES.byId("ambience").enabled()) return;

        long ticks = Math.floorMod(TopkaClient.CONFIG.get().ambienceTime, 24000L);
        float sun = (float) (((ticks - 6000L) / 24000.0D) * (Math.PI * 2.0D));
        state.sunAngle = sun;
        state.moonAngle = sun + (float) Math.PI;
        state.starAngle = sun;

        // Smooth deterministic night brightness: 0 at noon, 1 at midnight.
        double night = 0.5D - 0.5D * Math.cos((ticks - 6000L) / 24000.0D * Math.PI * 2.0D);
        float stars = (float) Math.clamp((night - 0.42D) / 0.58D, 0.0D, 1.0D);
        state.starBrightness = stars * state.rainBrightness;
    }
}
