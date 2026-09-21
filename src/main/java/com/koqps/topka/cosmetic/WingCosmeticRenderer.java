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
 * Modeled wing renderer.
 *
 * Unlike the old sheet renderer, every visible wing piece is an extruded
 * double-sided solid with front/back/edge faces. Large single-span wing planes
 * are intentionally avoided.
 */
public final class WingCosmeticRenderer {
    private static final float[][] UV_TILES = {
            {0.00F, 0.00F, 0.50F, 0.50F}, // angel
            {0.50F, 0.00F, 1.00F, 0.50F}, // demon
            {0.00F, 0.50F, 0.50F, 1.00F}, // crystal
            {0.50F, 0.50F, 1.00F, 1.00F}  // dragon
    };

    private WingCosmeticRenderer() { }

    public static void render(LevelRenderContext context, Vec3 camera, Minecraft client) {
        TopkaConfig cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.wingsShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            CosmeticAnchor anchor = CosmeticAnchor.forBack(
                    player,
                    -0.58D + cfg.wingsVerticalOffset,
                    cfg.wingsBackOffset
            );
            CosmeticAnimation animation = CosmeticAnimation.sample(player, cfg.wingsFlapSpeed, cfg.wingsFlapAmount);

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            int style = Math.floorMod(cfg.wingsStyle, 5);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(style == 0 ? CosmeticTextures.ANGEL_BASE : CosmeticTextures.WINGS),
                    (pose, vertices) -> {
                        renderSide(pose, vertices, anchor, animation, cfg, -1.0D, 0.0F);
                        renderSide(pose, vertices, anchor, animation, cfg, 1.0D, 0.5F);
                        renderBackMount(pose, vertices, anchor, cfg);
                    }
            );

            if (cfg.wingsGlow) {
                context.submitNodeCollector().submitCustomGeometry(
                        poseStack,
                        RenderTypes.linesTranslucent(),
                        (pose, vertices) -> {
                            renderBones(pose, vertices, anchor, animation, cfg, -1.0D, 0.0F);
                            renderBones(pose, vertices, anchor, animation, cfg, 1.0D, 0.5F);
                        }
                );
            }

            poseStack.popPose();
        }
    }

    private static void renderSide(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            CosmeticAnchor a,
            CosmeticAnimation anim,
            TopkaConfig cfg,
            double side,
            float hueOffset
    ) {
        int style = Math.floorMod(cfg.wingsStyle, 5);
        float scale = Math.clamp(cfg.wingsScale, 0.45F, 2.25F);
        float spread = Math.clamp(cfg.wingsSpread, 0.35F, 1.65F) + (float) anim.spreadBoost();
        spread *= 1.0F - Math.clamp(cfg.wingsFold, 0.0F, 0.70F) * 0.58F;
        float thickness = Math.clamp(cfg.wingsDepth, 0.02F, 0.42F) * scale;
        int opacity = Math.clamp(cfg.wingsOpacity, 80, 255);

        double tilt = Math.toRadians(cfg.wingsTilt);
        Vec3 root = a.local(side * 0.07D * scale, 0.05D * scale + anim.sway(), 0.0D);
        Vec3 shoulder = tilted(a, side * 0.42D * spread * scale, 0.33D * scale + anim.flap() * 0.24D, thickness * 0.10D, tilt);
        Vec3 elbow = tilted(a, side * 0.88D * spread * scale, 0.62D * scale + anim.flap() * 0.55D + anim.lift(), thickness * 0.30D, tilt);
        Vec3 outer = tilted(a, side * 1.38D * spread * scale, 0.66D * scale + anim.flap() * 0.82D + anim.lift(), thickness * 0.58D, tilt);
        Vec3 low = tilted(a, side * 1.18D * spread * scale, -0.27D * scale + anim.flap() * 0.30D, thickness * 0.95D, tilt);
        Vec3 lowerRoot = tilted(a, side * 0.14D * scale, -0.54D * scale, thickness * 0.42D, tilt);

        int primary = color(cfg.wingsPrimaryColorArgb, cfg.wingsRainbow, hueOffset, opacity);
        int secondary = color(cfg.wingsSecondaryColorArgb, cfg.wingsRainbow, hueOffset + 0.14F, Math.max(80, opacity - 10));

        switch (style) {
            case 1 -> renderDemon(pose, vertices, a, root, shoulder, elbow, outer, low, lowerRoot, side, scale, thickness, primary, secondary, cfg);
            case 2 -> renderCrystal(pose, vertices, a, root, shoulder, elbow, outer, low, lowerRoot, side, scale, thickness, primary, secondary, cfg);
            case 3 -> renderDragon(pose, vertices, a, root, shoulder, elbow, outer, low, lowerRoot, side, scale, thickness, primary, secondary, cfg);
            case 4 -> renderTech(pose, vertices, a, root, shoulder, elbow, outer, low, lowerRoot, side, scale, thickness, primary, secondary, cfg);
            default -> renderAngel(pose, vertices, a, root, shoulder, elbow, outer, low, lowerRoot, side, scale, thickness, primary, secondary, cfg);
        }
    }

    private static void renderAngel(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a,
            Vec3 root, Vec3 shoulder, Vec3 elbow, Vec3 outer, Vec3 low, Vec3 lowerRoot,
            double side, float scale, float thickness, int primary, int secondary, TopkaConfig cfg
    ) {
        /*
         * 0.10.2 Angel is based on the uploaded Blockbench angel-wing model:
         * many small rotated cuboids arranged into several feather clusters.
         * There is deliberately NO continuous membrane or broad quad anywhere
         * in the Angel renderer.
         */
        int detail = Math.clamp(cfg.wingsDetail, 1, 5);
        double span = Math.max(0.55F, cfg.wingsSpread) * scale;

        // Compact three-piece shoulder/back mount.
        prismBetween(pose, v, root, shoulder, a.right(), a.back(),
                0.070D * scale, 0.050D * scale, shade(secondary, 0.80F), 0);
        Vec3 upperJoint = lerp(shoulder, elbow, 0.46D);
        prismBetween(pose, v, shoulder, upperJoint, a.right(), a.back(),
                0.060D * scale, 0.044D * scale, shade(secondary, 0.74F), 0);

        // UPPER CREST — short feathers that rise above the shoulder.
        int upperCount = 4 + detail;
        for (int i = 0; i < upperCount; i++) {
            double t = i / (double) Math.max(1, upperCount - 1);
            Vec3 base = lerp(root, shoulder, 0.22D + t * 0.70D)
                    .add(a.back().scale((-0.015D + t * 0.025D) * scale));

            Vec3 control = base
                    .add(a.right().scale(side * (0.24D + 0.16D * t) * span))
                    .add(a.up().scale((0.25D + 0.22D * (1.0D - t)) * scale))
                    .add(a.back().scale((0.02D + 0.05D * t) * scale));

            Vec3 tip = root
                    .add(a.right().scale(side * (0.58D + 0.54D * t) * span))
                    .add(a.up().scale((0.62D - 0.10D * t) * scale))
                    .add(a.back().scale((0.08D + 0.09D * t) * scale));

            int col = (i & 1) == 0 ? shade(primary, 1.04F) : primary;
            voxelFeather(pose, v, base, control, tip, a, side, scale,
                    5 + detail, 0.050D, 0.027D, col, 0);
        }

        // PRIMARY FLIGHT FEATHERS — the long lower/outward fan from the source
        // model, but preserved as individual cuboid chains with visible gaps.
        int primaryCount = 6 + detail;
        for (int i = 0; i < primaryCount; i++) {
            double t = i / (double) Math.max(1, primaryCount - 1);

            Vec3 base = lerp(shoulder, lowerRoot, 0.10D + 0.74D * t)
                    .add(a.back().scale((0.01D + 0.035D * t) * scale));

            double reach = (1.10D + 0.58D * (1.0D - t)) * span;
            Vec3 control = base
                    .add(a.right().scale(side * reach * 0.48D))
                    .add(a.up().scale((0.16D - 0.40D * t) * scale))
                    .add(a.back().scale((0.11D + 0.14D * t) * scale));

            Vec3 tip = root
                    .add(a.right().scale(side * reach))
                    .add(a.up().scale((0.49D - 1.28D * t) * scale))
                    .add(a.back().scale((0.20D + 0.31D * t) * scale));

            int col = (i & 1) == 0 ? primary : shade(primary, 0.91F);
            voxelFeather(pose, v, base, control, tip, a, side, scale,
                    6 + detail, 0.057D, 0.031D, col, 0);
        }

        // SECONDARY FEATHERS — a middle row offset toward the camera/back to
        // create the layered depth seen in the Blockbench base model.
        int secondaryCount = 5 + detail;
        for (int i = 0; i < secondaryCount; i++) {
            double t = i / (double) Math.max(1, secondaryCount - 1);

            Vec3 base = lerp(root, lowerRoot, 0.06D + 0.78D * t)
                    .add(a.back().scale(-0.035D * scale));

            double reach = (0.68D + 0.46D * (1.0D - t)) * span;
            Vec3 control = base
                    .add(a.right().scale(side * reach * 0.52D))
                    .add(a.up().scale((0.10D - 0.28D * t) * scale))
                    .add(a.back().scale((0.02D + 0.09D * t) * scale));

            Vec3 tip = root
                    .add(a.right().scale(side * reach))
                    .add(a.up().scale((0.33D - 0.88D * t) * scale))
                    .add(a.back().scale((0.04D + 0.18D * t) * scale));

            int col = (i & 1) == 0 ? secondary : shade(secondary, 0.90F);
            voxelFeather(pose, v, base, control, tip, a, side, scale,
                    5 + detail, 0.048D, 0.026D, col, 0);
        }

        // SHOULDER COVER FEATHERS — tiny block feathers close the root without
        // ever becoming a single rectangular surface.
        for (int i = 0; i < 5; i++) {
            double t = i / 4.0D;
            Vec3 base = root
                    .add(a.right().scale(side * (0.035D + t * 0.080D) * scale))
                    .add(a.up().scale((0.13D - 0.22D * t) * scale))
                    .add(a.back().scale(-0.045D * scale));
            Vec3 control = base
                    .add(a.right().scale(side * (0.18D + t * 0.04D) * scale))
                    .add(a.up().scale((0.08D - 0.09D * t) * scale));
            Vec3 tip = base
                    .add(a.right().scale(side * (0.38D + t * 0.10D) * scale))
                    .add(a.up().scale((0.12D - 0.22D * t) * scale))
                    .add(a.back().scale(0.025D * scale));

            voxelFeather(pose, v, base, control, tip, a, side, scale,
                    5, 0.039D, 0.022D, shade(secondary, 0.82F + 0.035F * i), 0);
        }
    }

    private static void renderDemon(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a,
            Vec3 root, Vec3 shoulder, Vec3 elbow, Vec3 outer, Vec3 low, Vec3 lowerRoot,
            double side, float scale, float thickness, int primary, int secondary, TopkaConfig cfg
    ) {
        // Three actual bone prisms.
        prismBetween(pose, v, root, shoulder, a.right(), a.back(), 0.095D * scale, 0.075D * scale, secondary, 1);
        prismBetween(pose, v, shoulder, elbow, a.right(), a.back(), 0.082D * scale, 0.065D * scale, secondary, 1);
        prismBetween(pose, v, elbow, outer, a.right(), a.back(), 0.060D * scale, 0.050D * scale, secondary, 1);

        int fingers = 3 + Math.clamp(cfg.wingsDetail, 1, 5);
        Vec3 prevBone = shoulder;
        for (int i = 0; i < fingers; i++) {
            double t = (i + 1.0D) / fingers;
            Vec3 boneEnd = lerp(outer, low, t)
                    .add(a.right().scale(side * 0.10D * scale * (1.0D - t)))
                    .add(a.back().scale(0.05D * scale * i));
            prismBetween(pose, v, elbow, boneEnd, a.right(), a.back(), 0.050D * scale, 0.045D * scale, secondary, 1);

            Vec3 membraneRoot = i == 0 ? shoulder : prevBone;
            Vec3 membraneLower = lerp(lowerRoot, low, t);
            extrudedPanel(pose, v, membraneRoot, boneEnd, membraneLower, lowerRoot, a.back(), thickness * 0.24D,
                    withAlpha(primary, Math.max(95, ((primary >>> 24) & 0xFF) - 22)), 1);
            prevBone = boneEnd;
        }
    }

    private static void renderCrystal(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a,
            Vec3 root, Vec3 shoulder, Vec3 elbow, Vec3 outer, Vec3 low, Vec3 lowerRoot,
            double side, float scale, float thickness, int primary, int secondary, TopkaConfig cfg
    ) {
        prismBetween(pose, v, root, shoulder, a.right(), a.back(), 0.085D * scale, 0.065D * scale, secondary, 2);

        int shards = 5 + Math.clamp(cfg.wingsDetail, 1, 5) * 2;
        for (int i = 0; i < shards; i++) {
            double t = i / (double) Math.max(1, shards - 1);
            Vec3 base = lerp(shoulder, lowerRoot, t * 0.92D);
            Vec3 shardTip = lerp(elbow, outer, Math.min(1.0D, 0.20D + t * 0.90D))
                    .add(a.up().scale((0.18D - t * 0.34D) * scale))
                    .add(a.right().scale(side * (0.10D + t * 0.10D) * scale));

            Vec3 tangent = a.right().scale(side);
            double half = (0.075D + (1.0D - t) * 0.045D) * scale;
            Vec3 b0 = base.add(tangent.scale(-half));
            Vec3 b1 = base.add(tangent.scale(half));
            Vec3 tipL = shardTip.add(tangent.scale(-half * 0.18D));
            Vec3 tipR = shardTip.add(tangent.scale(half * 0.18D));

            int col = (i & 1) == 0 ? primary : secondary;
            extrudedPanel(pose, v, b0, b1, tipR, tipL, a.back(), thickness * 0.55D, col, 2);
        }
    }

    private static void renderDragon(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a,
            Vec3 root, Vec3 shoulder, Vec3 elbow, Vec3 outer, Vec3 low, Vec3 lowerRoot,
            double side, float scale, float thickness, int primary, int secondary, TopkaConfig cfg
    ) {
        prismBetween(pose, v, root, shoulder, a.right(), a.back(), 0.11D * scale, 0.085D * scale, secondary, 3);
        prismBetween(pose, v, shoulder, elbow, a.right(), a.back(), 0.095D * scale, 0.075D * scale, secondary, 3);
        prismBetween(pose, v, elbow, outer, a.right(), a.back(), 0.075D * scale, 0.060D * scale, secondary, 3);

        int bands = 4 + Math.clamp(cfg.wingsDetail, 1, 5);
        for (int i = 0; i < bands; i++) {
            double t0 = i / (double) bands;
            double t1 = (i + 1.0D) / bands;
            Vec3 top0 = lerp(shoulder, outer, t0);
            Vec3 top1 = lerp(shoulder, outer, t1);
            Vec3 bot1 = lerp(lowerRoot, low, t1);
            Vec3 bot0 = lerp(lowerRoot, low, t0);
            int col = (i & 1) == 0 ? primary : secondary;
            extrudedPanel(pose, v, top0, top1, bot1, bot0, a.back(), thickness * 0.34D, col, 3);
        }

        // Small armor scales layered over the membrane.
        for (int i = 0; i < bands - 1; i++) {
            double t = (i + 0.5D) / bands;
            Vec3 c = lerp(shoulder, low, t);
            Vec3 r = a.right().scale(side * 0.09D * scale);
            Vec3 u = a.up().scale(0.12D * scale);
            extrudedPanel(pose, v, c.subtract(r), c.add(r), c.add(r).subtract(u), c.subtract(r).subtract(u),
                    a.back(), thickness * 0.52D, withAlpha(secondary, 230), 3);
        }
    }

    private static void renderTech(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a,
            Vec3 root, Vec3 shoulder, Vec3 elbow, Vec3 outer, Vec3 low, Vec3 lowerRoot,
            double side, float scale, float thickness, int primary, int secondary, TopkaConfig cfg
    ) {
        prismBetween(pose, v, root, shoulder, a.right(), a.back(), 0.09D * scale, 0.065D * scale, secondary, 2);

        int segments = 4 + Math.clamp(cfg.wingsDetail, 1, 5);
        for (int i = 0; i < segments; i++) {
            double t = i / (double) Math.max(1, segments - 1);
            Vec3 center = lerp(shoulder, low, t * 0.86D)
                    .add(a.right().scale(side * (0.18D + t * 0.16D) * scale));
            Vec3 dir = lerp(elbow, outer, 0.30D + t * 0.60D).subtract(center).normalize();
            Vec3 end = center.add(dir.scale((0.42D + (1.0D - t) * 0.22D) * scale));
            int col = (i & 1) == 0 ? primary : secondary;
            prismBetween(
                    pose, v,
                    center, end,
                    a.up(), a.back(),
                    (0.055D + (1.0D - t) * 0.025D) * scale,
                    Math.max(0.018D, thickness * 0.26D),
                    col, 2
            );
        }

        // Floating outer blade for a server-cosmetic silhouette.
        Vec3 bladeA = outer.add(a.right().scale(side * 0.10D * scale));
        Vec3 bladeB = bladeA.add(a.up().scale(-0.55D * scale)).add(a.back().scale(0.14D * scale));
        prismBetween(pose, v, bladeA, bladeB, a.right(), a.back(), 0.075D * scale, Math.max(0.02D, thickness * 0.30D), secondary, 2);
    }

    private static void renderBackMount(PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a, TopkaConfig cfg) {
        float scale = Math.clamp(cfg.wingsScale, 0.45F, 2.25F);
        int col = color(cfg.wingsSecondaryColorArgb, cfg.wingsRainbow, 0.25F, Math.max(150, cfg.wingsOpacity));
        Vec3 top = a.local(0.0D, 0.22D * scale, 0.01D);
        Vec3 bottom = a.local(0.0D, -0.28D * scale, 0.02D);
        prismBetween(pose, v, bottom, top, a.right(), a.back(), 0.11D * scale, 0.075D * scale, col, Math.floorMod(cfg.wingsStyle, 4));
    }

    private static void renderBones(
            PoseStack.Pose pose, VertexConsumer v, CosmeticAnchor a, CosmeticAnimation anim,
            TopkaConfig cfg, double side, float hueOffset
    ) {
        float scale = Math.clamp(cfg.wingsScale, 0.45F, 2.25F);
        float spread = Math.clamp(cfg.wingsSpread, 0.35F, 1.65F) + (float) anim.spreadBoost();
        Vec3 root = a.local(side * 0.07D * scale, 0.05D * scale + anim.sway(), 0.0D);
        Vec3 shoulder = a.local(side * 0.42D * spread * scale, 0.33D * scale + anim.flap() * 0.24D, 0.02D);
        Vec3 elbow = a.local(side * 0.88D * spread * scale, 0.62D * scale + anim.flap() * 0.55D + anim.lift(), 0.05D);

        int col = color(cfg.wingsSecondaryColorArgb, cfg.wingsRainbow, hueOffset + 0.12F, 190);
        float width = Math.max(1.0F, cfg.wingsBoneWidth * 0.68F);
        line(pose, v, root, shoulder, col, width);
        line(pose, v, shoulder, elbow, col, width);
    }

    private static void voxelFeather(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 start,
            Vec3 control,
            Vec3 end,
            CosmeticAnchor anchor,
            double side,
            float scale,
            int segments,
            double startHalfWidth,
            double startHalfDepth,
            int color,
            int tile
    ) {
        Vec3 previous = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double omt = 1.0D - t;

            Vec3 point = start.scale(omt * omt)
                    .add(control.scale(2.0D * omt * t))
                    .add(end.scale(t * t));

            // Source Blockbench model is built from many short rotated cubes.
            // Reproduce that structure: every feather is a chain of separate
            // tapered cuboids rather than a continuous face.
            double taper = 1.0D - 0.72D * Math.pow(t, 1.35D);
            double halfWidth = Math.max(0.012D * scale, startHalfWidth * scale * taper);
            double halfDepth = Math.max(0.010D * scale, startHalfDepth * scale * (0.92D - 0.42D * t));

            Vec3 segment = point.subtract(previous);
            if (segment.lengthSqr() > 1.0E-7D) {
                // Use an axis perpendicular to the feather direction and the
                // back axis so each little block follows the curve.
                Vec3 widthAxis = segment.cross(anchor.back());
                if (widthAxis.lengthSqr() < 1.0E-7D) {
                    widthAxis = anchor.right().scale(side);
                } else {
                    widthAxis = widthAxis.normalize();
                    if (widthAxis.dot(anchor.right()) * side < 0.0D) widthAxis = widthAxis.scale(-1.0D);
                }

                int segmentColor = (i & 1) == 0 ? color : shade(color, 0.94F);
                prismBetween(
                        pose,
                        vertices,
                        previous,
                        point,
                        widthAxis,
                        anchor.back(),
                        halfWidth,
                        halfDepth,
                        segmentColor,
                        tile
                );
            }
            previous = point;
        }
    }

    private static void taperedFeather(
            PoseStack.Pose pose,
            VertexConsumer v,
            Vec3 base,
            Vec3 tip,
            Vec3 widthAxis,
            Vec3 depthAxis,
            double baseHalfWidth,
            double midHalfWidth,
            double halfDepth,
            int color,
            int tile
    ) {
        Vec3 direction = tip.subtract(base);
        Vec3 mid = base.add(direction.scale(0.56D));

        Vec3 width = widthAxis.normalize();
        Vec3 depth = depthAxis.normalize();

        Vec3 baseL = base.subtract(width.scale(baseHalfWidth));
        Vec3 baseR = base.add(width.scale(baseHalfWidth));
        Vec3 midL = mid.subtract(width.scale(midHalfWidth));
        Vec3 midR = mid.add(width.scale(midHalfWidth));

        // Slightly bevel the pointed tip so the edge has real thickness.
        Vec3 tipBase = tip.subtract(direction.normalize().scale(Math.max(0.015D, base.distanceTo(tip) * 0.025D)));
        Vec3 tipL = tipBase.subtract(width.scale(baseHalfWidth * 0.14D));
        Vec3 tipR = tipBase.add(width.scale(baseHalfWidth * 0.14D));

        // Root -> widest part.
        extrudedPanel(
                pose, v,
                baseL, baseR, midR, midL,
                depth,
                halfDepth * 2.0D,
                color,
                tile
        );

        // Widest part -> pointed tip.
        extrudedPanel(
                pose, v,
                midL, midR, tipR, tipL,
                depth,
                halfDepth * 1.65D,
                shade(color, 0.96F),
                tile
        );

        // Tiny solid tip cap.
        prismBetween(
                pose, v,
                tipBase,
                tip,
                width,
                depth,
                Math.max(0.006D, baseHalfWidth * 0.10D),
                Math.max(0.006D, halfDepth * 0.55D),
                shade(color, 0.88F),
                tile
        );
    }

    private static void prismBetween(
            PoseStack.Pose pose, VertexConsumer v,
            Vec3 a, Vec3 b, Vec3 widthAxis, Vec3 depthAxis,
            double halfWidth, double halfDepth, int color, int tile
    ) {
        Vec3 w = widthAxis.normalize().scale(halfWidth);
        Vec3 d = depthAxis.normalize().scale(halfDepth);

        Vec3 a0 = a.subtract(w).subtract(d);
        Vec3 a1 = a.add(w).subtract(d);
        Vec3 a2 = a.add(w).add(d);
        Vec3 a3 = a.subtract(w).add(d);
        Vec3 b0 = b.subtract(w).subtract(d);
        Vec3 b1 = b.add(w).subtract(d);
        Vec3 b2 = b.add(w).add(d);
        Vec3 b3 = b.subtract(w).add(d);

        quad(pose, v, a0, a1, b1, b0, color, tile);
        quad(pose, v, a1, a2, b2, b1, shade(color, 0.88F), tile);
        quad(pose, v, a2, a3, b3, b2, shade(color, 0.72F), tile);
        quad(pose, v, a3, a0, b0, b3, shade(color, 0.82F), tile);
        quad(pose, v, a0, a3, a2, a1, shade(color, 0.78F), tile);
        quad(pose, v, b0, b1, b2, b3, color, tile);
    }

    private static void extrudedPanel(
            PoseStack.Pose pose, VertexConsumer v,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            Vec3 depthAxis, double thickness, int color, int tile
    ) {
        Vec3 off = depthAxis.normalize().scale(thickness * 0.5D);
        Vec3 af = a.add(off), bf = b.add(off), cf = c.add(off), df = d.add(off);
        Vec3 ab = a.subtract(off), bb = b.subtract(off), cb = c.subtract(off), db = d.subtract(off);

        quad(pose, v, af, bf, cf, df, color, tile);
        quad(pose, v, db, cb, bb, ab, shade(color, 0.72F), tile);

        int edge = shade(color, 0.58F);
        quad(pose, v, af, ab, bb, bf, edge, tile);
        quad(pose, v, bf, bb, cb, cf, shade(edge, 0.90F), tile);
        quad(pose, v, cf, cb, db, df, shade(edge, 0.76F), tile);
        quad(pose, v, df, db, ab, af, shade(edge, 0.84F), tile);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int tile) {
        float[] uv = tile == 0
                ? new float[]{0.0F, 0.0F, 1.0F, 1.0F}
                : UV_TILES[Math.floorMod(tile == 4 ? 2 : tile, UV_TILES.length)];
        vertex(pose, v, a, color, uv[0], uv[1]);
        vertex(pose, v, b, color, uv[2], uv[1]);
        vertex(pose, v, c, color, uv[2], uv[3]);
        vertex(pose, v, d, color, uv[0], uv[3]);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer v, Vec3 p, int color, float u, float vv) {
        v.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(color)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer v, Vec3 a, Vec3 b, int color, float width) {
        Vec3 delta = b.subtract(a);
        float nx = (float) delta.x;
        float ny = (float) delta.y;
        float nz = (float) delta.z;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-5F) return;
        nx /= len; ny /= len; nz /= len;
        v.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
        v.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
    }

    private static Vec3 tilted(CosmeticAnchor anchor, double x, double y, double z, double tilt) {
        double yy = y * Math.cos(tilt) - z * Math.sin(tilt);
        double zz = y * Math.sin(tilt) + z * Math.cos(tilt);
        return anchor.local(x, yy, zz);
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

    private static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }
}
