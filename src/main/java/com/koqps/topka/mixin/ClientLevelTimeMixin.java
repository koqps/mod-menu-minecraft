package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.AmbienceController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplies client-visible day time directly to the render path. Server packets
 * are still received and cached, but custom ambience no longer fights them by
 * rewriting the world every tick, eliminating visible time flicker.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    @Inject(method = "setTimeFromServer", at = @At("HEAD"))
    private void modmenu$captureServerTime(long dayTime, CallbackInfo ci) {
        AmbienceController.onServerTime(dayTime);
    }

    public long dayTime() {
        if (TopkaClient.MODULES.byId("ambience").enabled()) {
            return Math.floorMod(TopkaClient.CONFIG.get().ambienceTime, 24000L);
        }
        return AmbienceController.serverDayTime();
    }
}
