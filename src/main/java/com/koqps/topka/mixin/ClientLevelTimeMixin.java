package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.AmbienceController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flicker-free client ambience override.
 *
 * Minecraft 26.3's celestial rendering reads LevelTimeAccess#getTimeOfDay,
 * which in turn depends on dayTime(). Both accessors are provided directly on
 * ClientLevel here so every sky consumer sees the same stable custom value.
 * Incoming server time is still cached for instant vanilla restoration.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    @Inject(method = "setTimeFromServer", at = @At("HEAD"), require = 1)
    private void modmenu$captureServerTime(long dayTime, CallbackInfo ci) {
        AmbienceController.onServerTime(dayTime);
    }

    public long dayTime() {
        if (TopkaClient.MODULES.byId("ambience").enabled()) {
            return Math.floorMod(TopkaClient.CONFIG.get().ambienceTime, 24000L);
        }
        return AmbienceController.serverDayTime();
    }

    public float getTimeOfDay(float partialTick) {
        ClientLevel self = (ClientLevel) (Object) this;
        return self.dimensionType().timeOfDay(this.dayTime());
    }
}
