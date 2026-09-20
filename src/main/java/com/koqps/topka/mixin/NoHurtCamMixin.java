package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public final class NoHurtCamMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void topka$disableHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (TopkaClient.MODULES.byId("no_hurt_cam").enabled()) {
            ci.cancel();
        }
    }
}
