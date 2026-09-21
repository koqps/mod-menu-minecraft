package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.TopkaConfig;
import com.koqps.topka.hud.CosmeticTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Segmented cloth-style cape renderer.
 * Uses multiple articulated textured strips with a small material thickness
 * instead of a single flat banner.
 */
public final class CapeCosmeticRenderer {
    private CapeCosmeticRenderer() { }

    public static void render(LevelRenderContext context, Vec3 camera, Minecraft client) {
        TopkaConfig cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.capeShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            CosmeticAnchor anchor = CosmeticAnchor.forBack(player, -0.38D, 0.105D);
            CosmeticAnimation animation = CosmeticAnimation.sample(player, 0.62F, 0.08F);

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(CosmeticTextures.WINGS),
                    (pose, vertices) -> renderCape(pose, vertices, anchor, animation, cfg)
            );

            poseStack.popPose();
        }
    }

    private static void renderCape(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            CosmeticAnchor a,
            CosmeticAnimation anim,
            TopkaConfig cfg
    ) {
        int style = Math.floorMod(cfg.capeStyle, 4);
        int segments = style == 2 ? 8 : 6;
        double width = Math.clamp(cfg.capeWidth, 0.30F, 1.20F);
        double height = Math.clamp(cfg.capeHeight, 0.45F, 1.60F);
        double halfW = width * 0.5D;
        double thickness = 0.028D + (style == 3 ? 0.018D : 0.0D);
        int alpha = Math.clamp(cfg.capeOpacity, 40, 255);

        int topColor = color(cfg.capeColorArgb, cfg.capeRainbow, 0.0F, alpha);
        int lowerColor = color(cfg.capeColorArgb, cfg.capeRainbow, 0.18F, Math.max(45, alpha - 20));

        Vec3 previousLeft = a.local(-halfW, 0.06D, 0.0D);
        Vec3 previousRight = a.local(halfW, 0.06D, 0.0D);

        for (int i = 0; i < segments; i++) {
            double t = (i + 1.0D) / segments;
            double y = -height * t;
            double taper = switch (style) {
                case 1 -> 1.0D - 0.22D * t; // split cloak
                case 2 -> 1.0D - 0.10D * t; // royal
                case 3 -> 1.0D - 0.34D * t; // energy
                default -> 1.0D - 0.16D * t;
            };

            double motion = (0.05D + t * 0.15D) * Math.min(1.0D, Math.abs(anim.sway()) * 4.0D + 0.22D);
            double back = t * t * (0.10D + anim.spreadBoost() * 0.42D) + motion;
            double sideSway = Math.sin(System.currentTimeMillis() / 360.0D + t * 2.6D) * 0.018D * t;

            Vec3 left = a.local(-halfW * taper + sideSway, y, back);
            Vec3 right = a.local(halfW * taper + sideSway, y, back);

            int segmentColor = (i & 1) == 0 ? topColor : lowerColor;

            if (style == 1) {
                // Split cloak: two separated articulated strips.
                Vec3 pMid0 = lerp(previousLeft, previousRight, 0.47D);
                Vec3 pMid1 = lerp(left, right, 0.47D);
                Vec3 nMid0 = lerp(previousLeft, previousRight, 0.53D);
                Vec3 nMid1 = lerp(left, right, 0.53D);
                extrudedPanel(pose, vertices, previousLeft, pMid0, pMid1, left, a.back(), thickness, segmentColor);
                extrudedPanel(pose, vertices, nMid0, previousRight, right, nMid1, a.back(), thickness, shade(segmentColor, 0.92F));
            } else {
                extrudedPanel(pose, vertices, previousLeft, previousRight, right, left, a.back(), thickness, segmentColor);
            }

            // Royal and energy variants receive an extra center accent strip.
            if (style == 2 || style == 3) {
                double stripe = style == 2 ? 0.10D : 0.065D;
                Vec3 s0L = lerp(previousLeft, previousRight, 0.5D - stripe);
                Vec3 s0R = lerp(previousLeft, previousRight, 0.5D + stripe);
                Vec3 s1R = lerp(left, right, 0.5D + stripe);
                Vec3 s1L = lerp(left, right, 0.5D - stripe);
                int accent = shade(segmentColor, style == 3 ? 1.18F : 0.70F);
                extrudedPanel(pose, vertices, s0L, s0R, s1R, s1L, a.back(), thickness * 1.20D, accent);
            }

            previousLeft = left;
            previousRight = right;
        }
    }

    private static void extrudedPanel(
            PoseStack.Pose pose,
            VertexConsumer v,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            Vec3 depthAxis,
            double thickness,
            int color
    ) {
        Vec3 off = depthAxis.normalize().scale(thickness * 0.5D);
        Vec3 af = a.add(off), bf = b.add(off), cf = c.add(off), df = d.add(off);
        Vec3 ab = a.subtract(off), bb = b.subtract(off), cb = c.subtract(off), db = d.subtract(off);

        quad(pose, v, af, bf, cf, df, color);
        quad(pose, v, db, cb, bb, ab, shade(color, 0.72F));

        int edge = shade(color, 0.58F);
        quad(pose, v, af, ab, bb, bf, edge);
        quad(pose, v, bf, bb, cb, cf, shade(edge, 0.90F));
        quad(pose, v, cf, cb, db, df, shade(edge, 0.76F));
        quad(pose, v, df, db, ab, af, shade(edge, 0.84F));
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        // Use the fabric/feather tile as a cloth grain. Color tinting supplies the actual cape theme.
        vertex(pose, v, a, color, 0.00F, 0.00F);
        vertex(pose, v, b, color, 0.50F, 0.00F);
        vertex(pose, v, c, color, 0.50F, 0.50F);
        vertex(pose, v, d, color, 0.00F, 0.50F);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer v, Vec3 p, int color, float u, float vv) {
        v.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(color)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    private static int shade(int argb, float multiplier) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.clamp(Math.round(((argb >>> 16) & 0xFF) * multiplier), 0, 255);
        int g = Math.clamp(Math.round(((argb >>> 8) & 0xFF) * multiplier), 0, 255);
        int b = Math.clamp(Math.round((argb & 0xFF) * multiplier), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int color(int baseArgb, boolean rainbow, float offset, int alpha) {
        int rgb = baseArgb & 0x00FFFFFF;
        if (rainbow) {
            float hue = (System.currentTimeMillis() / 3200.0F + offset) % 1.0F;
            rgb = java.awt.Color.HSBtoRGB(hue, 0.72F, 1.0F) & 0x00FFFFFF;
        }
        return (Math.clamp(alpha, 0, 255) << 24) | rgb;
    }
}
