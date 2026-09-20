package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonViewMixin {
    @Unique
    private static final String ITEM_SUBMIT =
            "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V";

    @Inject(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = ITEM_SUBMIT, shift = At.Shift.BEFORE)
    )
    private void modmenu$beforeItem(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!active()) return;

        var cfg = TopkaClient.CONFIG.get();
        poseStack.pushPose();

        if (TopkaClient.MODULES.byId("viewmodel").enabled()) {
            boolean main = hand == InteractionHand.MAIN_HAND;
            double x = main ? cfg.viewMainX : cfg.viewOffX;
            double y = main ? cfg.viewMainY : cfg.viewOffY;
            double z = main ? cfg.viewMainZ : cfg.viewOffZ;
            poseStack.translate(x, y, z);

            float scale = Math.clamp(cfg.viewScale, 0.35F, 2.0F);
            poseStack.scale(scale, scale, scale);

            float side = main ? 1.0F : -1.0F;
            poseStack.rotate(Axis.XP.rotationDegrees(cfg.viewPitch));
            poseStack.rotate(Axis.YP.rotationDegrees(cfg.viewYaw * side));
            poseStack.rotate(Axis.ZP.rotationDegrees(cfg.viewRoll * side));
        }

        if (TopkaClient.MODULES.byId("swing_animations").enabled() && attack > 0.001F) {
            applySwing(poseStack, hand, attack, cfg.swingMode, cfg.swingStrength);
        }
    }

    @Inject(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = ITEM_SUBMIT, shift = At.Shift.AFTER)
    )
    private void modmenu$afterItem(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (active()) poseStack.popPose();
    }

    @Unique
    private static boolean active() {
        return TopkaClient.MODULES.byId("viewmodel").enabled()
                || TopkaClient.MODULES.byId("swing_animations").enabled();
    }

    @Unique
    private static void applySwing(PoseStack poseStack, InteractionHand hand, float attack, int mode, float strength) {
        float side = hand == InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
        float s = (float) Math.sin(Math.sqrt(Math.clamp(attack, 0.0F, 1.0F)) * Math.PI);
        float amount = Math.clamp(strength, 0.0F, 2.0F) * s;

        switch (Math.floorMod(mode, 4)) {
            case 0 -> {
                poseStack.rotate(Axis.ZP.rotationDegrees(-12.0F * amount * side));
                poseStack.rotate(Axis.YP.rotationDegrees(8.0F * amount * side));
            }
            case 1 -> {
                poseStack.translate(0.0D, -0.06D * amount, 0.10D * amount);
                poseStack.rotate(Axis.XP.rotationDegrees(-18.0F * amount));
            }
            case 2 -> {
                poseStack.rotate(Axis.YP.rotationDegrees(38.0F * amount * side));
                poseStack.rotate(Axis.ZP.rotationDegrees(-18.0F * amount * side));
            }
            default -> {
                poseStack.translate(0.10D * amount * side, -0.08D * amount, 0.0D);
                poseStack.rotate(Axis.ZP.rotationDegrees(28.0F * amount * side));
                poseStack.rotate(Axis.XP.rotationDegrees(-14.0F * amount));
            }
        }
    }
}
