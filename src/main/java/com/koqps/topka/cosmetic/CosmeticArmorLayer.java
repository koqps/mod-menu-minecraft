package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders uploaded cosmetic armor on the real animated Minecraft player bones.
 *
 * Unlike the previous rigid full-body OBJ pass, every piece is transformed by
 * the same ModelPart that vanilla player/armor rendering uses. Head rotation,
 * arm swings, crouching and leg movement therefore carry the cosmetic geometry.
 */
public final class CosmeticArmorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public CosmeticArmorLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            AvatarRenderState state,
            float limbAngle,
            float limbDistance
    ) {
        if (!TopkaClient.MODULES.byId("armor_cosmetic").enabled()) return;
        if (state.isSpectator) return;

        Minecraft client = Minecraft.getInstance();
        var cfg = TopkaClient.CONFIG.get();

        boolean self = client.player != null && state.id == client.player.getId();
        if (!self && !cfg.armorCosmeticShowOthers) return;
        if (self && client.options.getCameraType().isFirstPerson()) return;

        PlayerModel model = getParentModel();

        poseStack.pushPose();
        // Minecraft's humanoid render space points down in +Y, so a positive
        // user vertical offset is inverted here.
        poseStack.translate(0.0F, -cfg.armorCosmeticVerticalOffset, 0.0F);

        renderSlot(
                cfg.armorHelmetStyle,
                "helmet",
                model.head,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );

        renderSlot(
                cfg.armorChestStyle,
                "body",
                model.body,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );
        renderSlot(
                cfg.armorChestStyle,
                "left_arm",
                model.leftArm,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );
        renderSlot(
                cfg.armorChestStyle,
                "right_arm",
                model.rightArm,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );

        renderSlot(
                cfg.armorLeggingsStyle,
                "waist",
                model.body,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );
        renderSlot(
                cfg.armorLeggingsStyle,
                "left_leg",
                model.leftLeg,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );
        renderSlot(
                cfg.armorLeggingsStyle,
                "right_leg",
                model.rightLeg,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );

        renderSlot(
                cfg.armorBootsStyle,
                "left_boot",
                model.leftLeg,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );
        renderSlot(
                cfg.armorBootsStyle,
                "right_boot",
                model.rightLeg,
                poseStack,
                collector,
                light,
                cfg.armorCosmeticTintArgb,
                cfg.armorCosmeticScale
        );

        poseStack.popPose();
    }

    private static void renderSlot(
            int style,
            String modelName,
            ModelPart bodyPart,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int tintArgb,
            float scale
    ) {
        PackedMeshLibrary.Pack pack = packForStyle(style);
        if (pack == null) return;

        poseStack.pushPose();
        bodyPart.translateAndRotate(poseStack);
        float fittedScale = Math.clamp(scale, 0.70F, 1.35F);
        poseStack.scale(fittedScale, fittedScale, fittedScale);

        PackedMeshSubmitter.submit(
                PackedMeshLibrary.get(pack, modelName),
                poseStack,
                collector,
                light,
                OverlayTexture.NO_OVERLAY,
                tintArgb
        );
        poseStack.popPose();
    }

    private static PackedMeshLibrary.Pack packForStyle(int style) {
        return switch (style) {
            case 1 -> PackedMeshLibrary.Pack.VALKYRIE_ARMOR;
            case 2 -> PackedMeshLibrary.Pack.DEMONIC_ARMOR;
            default -> null;
        };
    }
}
