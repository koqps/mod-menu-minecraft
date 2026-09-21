package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.CosmeticTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crash-safe cosmetic armor renderer.
 *
 * Uses the same world-render path as the other working cosmetics rather than a
 * renderer mixin. Each cosmetic slot is a separate Minecraft-proportioned shell
 * around the player's head, torso, arms and legs, aligned to body yaw.
 */
public final class ArmorCosmeticRenderer {
    private ArmorCosmeticRenderer() { }

    public static void render(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (!TopkaClient.MODULES.byId("armor_cosmetic").enabled()) return;

        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.armorCosmeticShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            double bodyY = box.minY + (player.isCrouching() ? -0.08D : 0.0D);
            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());

            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 origin = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    bodyY + cfg.armorCosmeticVerticalOffset,
                    (box.minZ + box.maxZ) * 0.5D
            );

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(CosmeticTextures.ARMOR),
                    (pose, vertices) -> emitArmor(
                            pose,
                            vertices,
                            origin,
                            right,
                            forward,
                            up,
                            cfg.armorCosmeticScale,
                            cfg.armorCosmeticTintArgb,
                            cfg.armorHelmetStyle,
                            cfg.armorChestStyle,
                            cfg.armorLeggingsStyle,
                            cfg.armorBootsStyle
                    )
            );

            poseStack.popPose();
        }
    }

    private static void emitArmor(
            PoseStack.Pose pose,
            VertexConsumer v,
            Vec3 origin,
            Vec3 right,
            Vec3 forward,
            Vec3 up,
            float scale,
            int tint,
            int helmet,
            int chest,
            int leggings,
            int boots
    ) {
        double s = Math.clamp(scale, 0.70F, 1.35F);

        // Helmet follows the player's head position and body yaw. Kept slightly
        // oversized like vanilla armor so it sits over the skin layer.
        if (helmet != 0) {
            box(pose, v, origin.add(up.scale(1.55D * s)),
                    right, forward, up, 0.58D * s, 0.56D * s, 0.58D * s,
                    tint, helmet);

            if (helmet == 1) {
                box(pose, v, origin.add(up.scale(1.91D * s)),
                        right, forward, up, 0.12D * s, 0.18D * s, 0.14D * s,
                        tint, helmet);
            } else {
                box(pose, v, origin.add(right.scale(-0.22D * s)).add(up.scale(1.92D * s)),
                        right, forward, up, 0.10D * s, 0.24D * s, 0.12D * s,
                        tint, helmet);
                box(pose, v, origin.add(right.scale(0.22D * s)).add(up.scale(1.92D * s)),
                        right, forward, up, 0.10D * s, 0.24D * s, 0.12D * s,
                        tint, helmet);
            }
        }

        if (chest != 0) {
            box(pose, v, origin.add(up.scale(1.08D * s)),
                    right, forward, up, 0.66D * s, 0.72D * s, 0.36D * s,
                    tint, chest);

            box(pose, v, origin.add(right.scale(-0.43D * s)).add(up.scale(1.10D * s)),
                    right, forward, up, 0.28D * s, 0.68D * s, 0.32D * s,
                    tint, chest);
            box(pose, v, origin.add(right.scale(0.43D * s)).add(up.scale(1.10D * s)),
                    right, forward, up, 0.28D * s, 0.68D * s, 0.32D * s,
                    tint, chest);

            // shoulder plates
            box(pose, v, origin.add(right.scale(-0.43D * s)).add(up.scale(1.43D * s)),
                    right, forward, up, 0.38D * s, 0.20D * s, 0.40D * s,
                    tint, chest);
            box(pose, v, origin.add(right.scale(0.43D * s)).add(up.scale(1.43D * s)),
                    right, forward, up, 0.38D * s, 0.20D * s, 0.40D * s,
                    tint, chest);
        }

        if (leggings != 0) {
            box(pose, v, origin.add(up.scale(0.75D * s)),
                    right, forward, up, 0.64D * s, 0.30D * s, 0.34D * s,
                    tint, leggings);

            box(pose, v, origin.add(right.scale(-0.15D * s)).add(up.scale(0.48D * s)),
                    right, forward, up, 0.27D * s, 0.54D * s, 0.30D * s,
                    tint, leggings);
            box(pose, v, origin.add(right.scale(0.15D * s)).add(up.scale(0.48D * s)),
                    right, forward, up, 0.27D * s, 0.54D * s, 0.30D * s,
                    tint, leggings);
        }

        if (boots != 0) {
            box(pose, v, origin.add(right.scale(-0.15D * s)).add(up.scale(0.18D * s)).add(forward.scale(0.025D * s)),
                    right, forward, up, 0.30D * s, 0.38D * s, 0.36D * s,
                    tint, boots);
            box(pose, v, origin.add(right.scale(0.15D * s)).add(up.scale(0.18D * s)).add(forward.scale(0.025D * s)),
                    right, forward, up, 0.30D * s, 0.38D * s, 0.36D * s,
                    tint, boots);
        }
    }

    private static void box(
            PoseStack.Pose pose,
            VertexConsumer v,
            Vec3 center,
            Vec3 right,
            Vec3 forward,
            Vec3 up,
            double width,
            double height,
            double depth,
            int tint,
            int style
    ) {
        Vec3 rx = right.scale(width * 0.5D);
        Vec3 fy = forward.scale(depth * 0.5D);
        Vec3 uy = up.scale(height * 0.5D);

        Vec3 p000 = center.subtract(rx).subtract(fy).subtract(uy);
        Vec3 p100 = center.add(rx).subtract(fy).subtract(uy);
        Vec3 p110 = center.add(rx).subtract(fy).add(uy);
        Vec3 p010 = center.subtract(rx).subtract(fy).add(uy);

        Vec3 p001 = center.subtract(rx).add(fy).subtract(uy);
        Vec3 p101 = center.add(rx).add(fy).subtract(uy);
        Vec3 p111 = center.add(rx).add(fy).add(uy);
        Vec3 p011 = center.subtract(rx).add(fy).add(uy);

        float u0 = style == 2 ? 0.50F : 0.00F;
        float u1 = style == 2 ? 1.00F : 0.50F;

        quad(pose, v, p000, p100, p110, p010, tint, u0, 0F, u1, 1F, forward.scale(-1.0D));
        quad(pose, v, p101, p001, p011, p111, tint, u0, 0F, u1, 1F, forward);
        quad(pose, v, p001, p000, p010, p011, tint, u0, 0F, u1, 1F, right.scale(-1.0D));
        quad(pose, v, p100, p101, p111, p110, tint, u0, 0F, u1, 1F, right);
        quad(pose, v, p001, p101, p100, p000, tint, u0, 0F, u1, 1F, up.scale(-1.0D));
        quad(pose, v, p010, p110, p111, p011, tint, u0, 0F, u1, 1F, up);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer v,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            int color,
            float u0, float v0, float u1, float v1,
            Vec3 normal
    ) {
        vertex(v, pose, a, color, u0, v0, normal);
        vertex(v, pose, b, color, u1, v0, normal);
        vertex(v, pose, c, color, u1, v1, normal);

        vertex(v, pose, a, color, u0, v0, normal);
        vertex(v, pose, c, color, u1, v1, normal);
        vertex(v, pose, d, color, u0, v1, normal);
    }

    private static void vertex(
            VertexConsumer v,
            PoseStack.Pose pose,
            Vec3 p,
            int color,
            float u,
            float texV,
            Vec3 normal
    ) {
        v.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(color)
                .setUv(u, texV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
