package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Overrides the client-visible day-time accessor instead of repeatedly writing
 * server time every tick. This avoids the one-frame snaps/flicker that happen
 * when incoming server time packets race the client tick loop.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    public long dayTime() {
        ClientLevel level = (ClientLevel) (Object) this;
        if (TopkaClient.MODULES.byId("ambience").enabled()) {
            return Math.floorMod(TopkaClient.CONFIG.get().ambienceTime, 24000L);
        }
        return level.getDayTime();
    }
}
