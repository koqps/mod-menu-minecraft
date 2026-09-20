package com.koqps.topka.mixin;

import com.koqps.topka.hud.AmbienceController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures authoritative server time. The actual custom visual time is applied
 * in SkyRendererTimeMixin because Minecraft 26.3 moved celestial angles to
 * EnvironmentAttributes/SkyRenderState.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    @Inject(method = "setTimeFromServer", at = @At("HEAD"), require = 1)
    private void modmenu$captureServerTime(long dayTime, CallbackInfo ci) {
        AmbienceController.onServerTime(dayTime);
    }
}
