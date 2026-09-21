package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.CosmeticTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Cosmetic armor rendered directly on Minecraft's animated player bones.
 *
 * This deliberately uses Minecraft-proportioned armor shells instead of trying
 * to keep an imported full-body OBJ rigidly wrapped around the player. Each
 * slot follows the same head/body/arm/leg ModelPart transforms as the player.
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

        var cfg = TopkaClient.CONFIG.get();
        PlayerModel model = getParentModel();
        float scale = Math.clamp(cfg.armorCosmeticScale, 0.70F, 1.35F);
        int tint = cfg.armorCosmeticTintArgb;

        poseStack.pushPose();
        poseStack.translate(0.0F, -cfg.armorCosmeticVerticalOffset, 0.0F);

        // Helmet: actual head bone.
        if (cfg.armorHelmetStyle != 0) {
            submitBox(model.head, cfg.armorHelmetStyle, poseStack, collector, light, tint, scale,
                    -0.270F, -0.535F, -0.270F,
                     0.270F,  0.020F,  0.270F);

            // Small set-specific crown/horn details, still attached to the head.
            if (cfg.armorHelmetStyle == 1) {
                submitBox(model.head, 1, poseStack, collector, light, tint, scale,
                        -0.055F, -0.640F, -0.070F,
                         0.055F, -0.520F,  0.070F);
            } else {
                submitBox(model.head, 2, poseStack, collector, light, tint, scale,
                        -0.255F, -0.675F, -0.060F,
                        -0.145F, -0.505F,  0.060F);
                submitBox(model.head, 2, poseStack, collector, light, tint, scale,
                         0.145F, -0.675F, -0.060F,
                         0.255F, -0.505F,  0.060F);
            }
        }

        // Chestplate: torso and both animated arms.
        if (cfg.armorChestStyle != 0) {
            submitBox(model.body, cfg.armorChestStyle, poseStack, collector, light, tint, scale,
                    -0.285F, -0.020F, -0.165F,
                     0.285F,  0.770F,  0.165F);

            submitBox(model.leftArm, cfg.armorChestStyle, poseStack, collector, light, tint, scale,
                    -0.160F, -0.145F, -0.160F,
                     0.160F,  0.765F,  0.160F);
            submitBox(model.rightArm, cfg.armorChestStyle, poseStack, collector, light, tint, scale,
                    -0.160F, -0.145F, -0.160F,
                     0.160F,  0.765F,  0.160F);

            // Shoulder caps.
            submitBox(model.leftArm, cfg.armorChestStyle, poseStack, collector, light, tint, scale,
                    -0.205F, -0.170F, -0.205F,
                     0.205F,  0.120F,  0.205F);
            submitBox(model.rightArm, cfg.armorChestStyle, poseStack, collector, light, tint, scale,
                    -0.205F, -0.170F, -0.205F,
                     0.205F,  0.120F,  0.205F);
        }

        // Leggings: waist plus upper legs.
        if (cfg.armorLeggingsStyle != 0) {
            submitBox(model.body, cfg.armorLeggingsStyle, poseStack, collector, light, tint, scale,
                    -0.290F, 0.510F, -0.170F,
                     0.290F, 0.790F,  0.170F);

            submitBox(model.leftLeg, cfg.armorLeggingsStyle, poseStack, collector, light, tint, scale,
                    -0.155F, -0.020F, -0.155F,
                     0.155F,  0.500F,  0.155F);
            submitBox(model.rightLeg, cfg.armorLeggingsStyle, poseStack, collector, light, tint, scale,
                    -0.155F, -0.020F, -0.155F,
                     0.155F,  0.500F,  0.155F);
        }

        // Boots: lower half of each animated leg.
        if (cfg.armorBootsStyle != 0) {
            submitBox(model.leftLeg, cfg.armorBootsStyle, poseStack, collector, light, tint, scale,
                    -0.170F, 0.390F, -0.185F,
                     0.170F, 0.790F,  0.185F);
            submitBox(model.rightLeg, cfg.armorBootsStyle, poseStack, collector, light, tint, scale,
                    -0.170F, 0.390F, -0.185F,
                     0.170F, 0.790F,  0.185F);
        }

        poseStack.popPose();
    }

    private static void submitBox(
            ModelPart part,
            int style,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int tint,
            float scale,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        poseStack.scale(scale, scale, scale);

        float u0 = style == 2 ? 0.50F : 0.00F;
        float u1 = style == 2 ? 1.00F : 0.50F;

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucent(CosmeticTextures.ARMOR),
                (pose, vertices) -> emitCuboid(
                        pose,
                        vertices,
                        light,
                        tint,
                        u0,
                        u1,
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )
        );
        poseStack.popPose();
    }

    private static void emitCuboid(
            PoseStack.Pose pose,
            VertexConsumer v,
            int light,
            int tint,
            float u0,
            float u1,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        // front / back
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                0F, 0F, -1F);
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                0F, 0F, 1F);

        // left / right
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ,
                -1F, 0F, 0F);
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                1F, 0F, 0F);

        // top / bottom
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                0F, -1F, 0F);
        quad(v, pose, light, tint, u0, 0F, u1, 1F,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                0F, 1F, 0F);
    }

    private static void quad(
            VertexConsumer v,
            PoseStack.Pose pose,
            int light,
            int tint,
            float u0,
            float v0,
            float u1,
            float v1,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz,
            float nx, float ny, float nz
    ) {
        vertex(v, pose, ax, ay, az, tint, u0, v0, light, nx, ny, nz);
        vertex(v, pose, bx, by, bz, tint, u1, v0, light, nx, ny, nz);
        vertex(v, pose, cx, cy, cz, tint, u1, v1, light, nx, ny, nz);

        vertex(v, pose, ax, ay, az, tint, u0, v0, light, nx, ny, nz);
        vertex(v, pose, cx, cy, cz, tint, u1, v1, light, nx, ny, nz);
        vertex(v, pose, dx, dy, dz, tint, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(
            VertexConsumer v,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int tint,
            float u,
            float texV,
            int light,
            float nx,
            float ny,
            float nz
    ) {
        v.addVertex(pose, x, y, z)
                .setColor(tint)
                .setUv(u, texV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
