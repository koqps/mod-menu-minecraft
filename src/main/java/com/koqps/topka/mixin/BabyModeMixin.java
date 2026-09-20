package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public final class BabyModeMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void modmenu$babyScale(
            AvatarlikeEntity avatar,
            AvatarRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!TopkaClient.MODULES.byId("baby_mode").enabled()) return;

        Minecraft client = Minecraft.getInstance();
        boolean isSelf = avatar == client.player;
        if (!isSelf && !TopkaClient.CONFIG.get().babyModeOthers) return;

        float scale = Math.clamp(TopkaClient.CONFIG.get().babyScale, 0.35F, 1.0F);
        state.scale *= scale;
        state.boundingBoxWidth *= scale;
        state.boundingBoxHeight *= scale;
    }
}
