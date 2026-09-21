package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.WaypointConfig;
import com.koqps.topka.cosmetic.WingCosmeticRenderer;
import com.koqps.topka.cosmetic.CapeCosmeticRenderer;
import com.koqps.topka.cosmetic.ImportedCosmeticRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

public final class WorldVisuals {
    private static final int CIRCLE_SEGMENTS = 64;
    private static final int HAT_SEGMENTS = 56;

    private WorldVisuals() { }

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(WorldVisuals::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.screen() != null) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;

        if (TopkaClient.MODULES.byId("hitbox").enabled()) renderHitboxes(context, camera, client);
        if (TopkaClient.MODULES.byId("targeting").enabled()) renderTarget(context, camera, client);
        if (TopkaClient.MODULES.byId("china_hat").enabled()) renderChinaHats(context, camera, client);
        if (TopkaClient.MODULES.byId("halo").enabled()) renderHalo(context, camera, client);
        if (TopkaClient.MODULES.byId("trails").enabled()) renderTrail(context, camera);
        if (TopkaClient.MODULES.byId("jump_circles").enabled()) renderJumpCircles(context, camera);
        if (TopkaClient.MODULES.byId("projectile_prediction").enabled()) renderProjectilePrediction(context, camera, client);
        if (TopkaClient.MODULES.byId("waypoints").enabled()) renderWaypointBeams(context, camera);
        if (TopkaClient.MODULES.byId("cape").enabled()) CapeCosmeticRenderer.render(context, camera, client);
        if (TopkaClient.MODULES.byId("wings").enabled()) {
            ImportedCosmeticRenderer.renderWingPack(context, camera, client);
        }
        if (TopkaClient.MODULES.byId("back_weapon").enabled()) renderBackWeapon(context, camera, client);
        if (TopkaClient.MODULES.byId("head_cosmetic").enabled()) renderHeadCosmetic(context, camera, client);
        if (TopkaClient.MODULES.byId("little_demon").enabled()) ImportedCosmeticRenderer.renderLittleDemon(context, camera, client);
    }

    private static void renderHitboxes(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();
        double expand = Math.clamp(cfg.hitboxExpand, 0.0F, 1.0F);
        float width = Math.clamp(cfg.hitboxLineWidth, 1.0F, 6.0F);

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.isRemoved() || entity == client.player || client.player.distanceToSqr(entity) > 4096.0D) continue;
            if (!shouldRenderHitbox(entity)) continue;

            AABB bounds = entity.getBoundingBox().inflate(expand);
            int color = hitboxColor(entity);

            context.poseStack().pushPose();
            context.poseStack().translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    context.poseStack(),
                    Shapes.create(bounds),
                    RenderTypes.linesTranslucent(),
                    color,
                    width,
                    true
            );
            context.poseStack().popPose();
        }
    }

    private static boolean shouldRenderHitbox(Entity entity) {
        var cfg = TopkaClient.CONFIG.get();
        if (entity instanceof Player) return cfg.hitboxPlayers;
        if (entity instanceof Monster) return cfg.hitboxHostile;
        if (entity instanceof LivingEntity) return cfg.hitboxPassive;
        return cfg.hitboxOther;
    }

    private static int hitboxColor(Entity entity) {
        var cfg = TopkaClient.CONFIG.get();
        if (entity instanceof Player) return cfg.hitboxPlayerColorArgb;
        if (entity instanceof Monster) return cfg.hitboxHostileColorArgb;
        if (entity instanceof LivingEntity) return cfg.hitboxPassiveColorArgb;
        return cfg.hitboxOtherColorArgb;
    }

    private static void renderTarget(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (!(client.hitResult instanceof EntityHitResult entityHit)) return;

        Entity entity = entityHit.getEntity();
        if (entity == client.player || entity.isRemoved()) return;

        var cfg = TopkaClient.CONFIG.get();
        AABB bounds = entity.getBoundingBox().inflate(0.055D);
        float pulse = cfg.targetPulse
                ? (float) (0.88D + (Math.sin(System.currentTimeMillis() / 120.0D) + 1.0D) * 0.12D)
                : 1.0F;
        float lineWidth = 2.6F * pulse;

        if (cfg.targetMode == 0 || cfg.targetMode == 2) {
            context.poseStack().pushPose();
            context.poseStack().translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    context.poseStack(),
                    Shapes.create(bounds),
                    RenderTypes.linesTranslucent(),
                    cfg.targetColorArgb,
                    lineWidth,
                    true
            );
            context.poseStack().popPose();
        }

        if (cfg.targetMode == 1 || cfg.targetMode == 2) {
            double centerX = (bounds.minX + bounds.maxX) * 0.5D;
            double centerZ = (bounds.minZ + bounds.maxZ) * 0.5D;
            double radius = Math.max(bounds.getXsize(), bounds.getZsize()) * 0.5D + cfg.targetPadding;
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(centerX - camera.x, bounds.minY + 0.025D - camera.y, centerZ - camera.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> emitCircle(
                            pose,
                            vertices,
                            (float) radius,
                            0.0D,
                            cfg.targetColorArgb,
                            lineWidth
                    )
            );
            poseStack.popPose();
        }
    }

    private static void renderChinaHats(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || entity.isRemoved()) continue;
            if (player != client.player && !cfg.chinaHatShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            double baseY = player.getBoundingBox().maxY + 0.08D;
            double x = player.getX();
            double z = player.getZ();
            float radius = Math.clamp(cfg.chinaHatRadius, 0.25F, 1.25F);
            float height = Math.clamp(cfg.chinaHatHeight, 0.12F, 0.8F);
            float lineWidth = Math.clamp(cfg.chinaHatLineWidth, 1.0F, 7.0F);
            int color = effectColor(cfg.chinaHatColorArgb, cfg.chinaHatRainbow, 0.0F, 245);

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(x - camera.x, baseY - camera.y, z - camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> {
                        int style = Math.floorMod(cfg.chinaHatStyle, 4);

                        if (style == 3) {
                            for (int ring = 0; ring < 5; ring++) {
                                float t = ring / 4.0F;
                                float ringRadius = radius * (1.0F - t * 0.82F);
                                double ringY = height * t;
                                int ringColor = effectColor(
                                        cfg.chinaHatColorArgb,
                                        cfg.chinaHatRainbow,
                                        t * 0.16F,
                                        220 - ring * 25
                                );
                                emitCircle(pose, vertices, ringRadius, ringY, ringColor, lineWidth);
                            }
                            return;
                        }

                        emitCircle(pose, vertices, radius, 0.0D, color, lineWidth);

                        if (style >= 1) {
                            Vec3 apex = new Vec3(0.0D, height, 0.0D);
                            int step = style == 2 ? 2 : 4;
                            for (int i = 0; i < HAT_SEGMENTS; i += step) {
                                double angle = Math.PI * 2.0D * i / HAT_SEGMENTS;
                                Vec3 edge = new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                                int spokeColor = effectColor(
                                        cfg.chinaHatColorArgb,
                                        cfg.chinaHatRainbow,
                                        i / (float) HAT_SEGMENTS,
                                        style == 2 ? 210 : 180
                                );
                                emitLine(pose, vertices, edge, apex, spokeColor, Math.max(1.0F, lineWidth - 0.35F));
                            }
                        }

                        if (style == 2) {
                            emitCircle(
                                    pose,
                                    vertices,
                                    radius * 0.82F,
                                    height * 0.20D,
                                    effectColor(cfg.chinaHatColorArgb, cfg.chinaHatRainbow, 0.12F, 175),
                                    Math.max(1.0F, lineWidth - 0.5F)
                            );
                            emitCircle(
                                    pose,
                                    vertices,
                                    radius * 0.52F,
                                    height * 0.52D,
                                    effectColor(cfg.chinaHatColorArgb, cfg.chinaHatRainbow, 0.24F, 145),
                                    Math.max(1.0F, lineWidth - 0.8F)
                            );
                        }
                    }
            );
            if (cfg.chinaHatStyle != 0) {
                context.submitNodeCollector().submitCustomGeometry(
                        poseStack,
                        RenderTypes.debugQuads(),
                        (pose, vertices) -> {
                            emitConeShell(
                                    pose,
                                    vertices,
                                    radius,
                                    height,
                                    cfg.chinaHatColorArgb,
                                    cfg.chinaHatRainbow,
                                    cfg.chinaHatStyle == 2 ? 118 : 82
                            );
                            emitAnnulus(
                                    pose,
                                    vertices,
                                    radius * 0.42F,
                                    radius * 1.10F,
                                    -0.006D,
                                    cfg.chinaHatColorArgb,
                                    cfg.chinaHatRainbow,
                                    cfg.chinaHatStyle == 2 ? 106 : 70
                            );
                        }
                );
            }
            poseStack.popPose();
        }
    }

    private static void renderHalo(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (client.options.getCameraType().isFirstPerson()) return;

        var cfg = TopkaClient.CONFIG.get();
        if (cfg.haloStyle == 4) {
            ImportedCosmeticRenderer.renderHaloPack(context, camera, client);
            return;
        }
        double bob = Math.sin(System.currentTimeMillis() / 350.0D) * 0.035D;
        double x = client.player.getX();
        double y = client.player.getBoundingBox().maxY + cfg.haloHeight + bob;
        double z = client.player.getZ();
        float radius = Math.clamp(cfg.haloRadius, 0.2F, 1.0F);
        float width = Math.clamp(cfg.haloLineWidth, 1.0F, 7.0F);
        int color = effectColor(cfg.haloColorArgb, cfg.haloRainbow, 0.0F, 240);

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);

        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> {
                    switch (Math.floorMod(cfg.haloStyle, 4)) {
                        case 0 -> emitCircle(pose, vertices, radius, 0.0D, color, width);
                        case 2 -> {
                            for (int ring = 0; ring < 3; ring++) {
                                float r = radius * (0.78F + ring * 0.16F);
                                double yy = (ring - 1) * 0.045D;
                                emitCircle(
                                        pose,
                                        vertices,
                                        r,
                                        yy,
                                        effectColor(cfg.haloColorArgb, cfg.haloRainbow, ring * 0.11F, 220 - ring * 35),
                                        Math.max(1.0F, width - ring * 0.25F)
                                );
                            }
                        }
                        case 3 -> {
                            double phase = System.currentTimeMillis() / 260.0D;
                            for (int ring = 0; ring < 3; ring++) {
                                float pulseRadius = radius * (0.86F + 0.10F * ring
                                        + (float) Math.sin(phase + ring * 1.7D) * 0.055F);
                                emitCircle(
                                        pose,
                                        vertices,
                                        pulseRadius,
                                        ring * 0.025D,
                                        effectColor(cfg.haloColorArgb, cfg.haloRainbow, ring * 0.13F, 220 - ring * 42),
                                        width
                                );
                            }
                        }
                        default -> {
                            emitCircle(pose, vertices, radius + 0.035F, 0.0D, withAlpha(color, 65), Math.min(10.0F, width * 2.2F));
                            emitCircle(pose, vertices, radius, 0.0D, color, width);
                            emitCircle(pose, vertices, Math.max(0.05F, radius - 0.035F), 0.0D, withAlpha(color, 115), Math.max(1.0F, width * 0.65F));
                        }
                    }
                }
        );
        if (cfg.haloStyle != 0) {
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, vertices) -> {
                        float band = Math.max(0.035F, radius * 0.16F);
                        emitAnnulus(
                                pose,
                                vertices,
                                Math.max(0.04F, radius - band),
                                radius + band,
                                0.0D,
                                cfg.haloColorArgb,
                                cfg.haloRainbow,
                                cfg.haloStyle == 1 ? 78 : 54
                        );
                    }
            );
        }
        poseStack.popPose();
    }

    private static void renderTrail(LevelRenderContext context, Vec3 camera) {
        List<VisualEffectsController.TrailPoint> points = VisualEffectsController.trailSnapshot();
        if (points.size() < 2) return;

        var cfg = TopkaClient.CONFIG.get();
        long now = System.currentTimeMillis();
        long lifetime = Math.max(250L, cfg.trailLifetimeMs);
        int layers = Math.clamp(cfg.trailLayers, 1, 6);
        float width = Math.clamp(cfg.trailWidth, 0.10F, 2.5F);
        float height = Math.clamp(cfg.trailHeight, 0.05F, 1.8F);
        float lineWidth = Math.clamp(cfg.trailLineWidth, 1.0F, 12.0F);

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> {
                    for (int i = 1; i < points.size(); i++) {
                        var previous = points.get(i - 1);
                        var current = points.get(i);
                        double age = Math.clamp((double) (now - current.createdAt()) / lifetime, 0.0D, 1.0D);
                        double life = 1.0D - age;
                        int alpha = (int) Math.round(225.0D * life);
                        int color = effectColor(cfg.trailColorArgb, cfg.trailRainbow, i * 0.035F, alpha);

                        Vec3 a = previous.position();
                        Vec3 b = current.position();
                        Vec3 delta = b.subtract(a);
                        Vec3 right = new Vec3(-delta.z, 0.0D, delta.x);
                        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
                        right = right.normalize();

                        if (cfg.trailGlow) {
                            emitLine(
                                    pose,
                                    vertices,
                                    a.add(0.0D, height * 0.45D, 0.0D),
                                    b.add(0.0D, height * 0.45D, 0.0D),
                                    withAlpha(color, Math.max(22, alpha / 4)),
                                    Math.min(14.0F, lineWidth * 2.2F)
                            );
                        }

                        switch (cfg.trailStyle) {
                            case 0 -> emitLine(pose, vertices, a, b, color, lineWidth);
                            case 2 -> {
                                double wave = Math.sin((i + now / 80.0D) * 0.48D) * height * 0.34D;
                                Vec3 leftA = a.add(right.scale(-width * 0.50D)).add(0.0D, wave, 0.0D);
                                Vec3 leftB = b.add(right.scale(-width * 0.50D)).add(0.0D, -wave, 0.0D);
                                Vec3 rightA = a.add(right.scale(width * 0.50D)).add(0.0D, -wave, 0.0D);
                                Vec3 rightB = b.add(right.scale(width * 0.50D)).add(0.0D, wave, 0.0D);
                                emitLine(pose, vertices, leftA, leftB, color, lineWidth);
                                emitLine(pose, vertices, rightA, rightB, color, lineWidth);
                                emitLine(pose, vertices, leftB, b.add(0.0D, height * 0.32D, 0.0D), withAlpha(color, alpha * 3 / 4), Math.max(1.0F, lineWidth - 0.5F));
                                emitLine(pose, vertices, rightB, b.add(0.0D, height * 0.32D, 0.0D), withAlpha(color, alpha * 3 / 4), Math.max(1.0F, lineWidth - 0.5F));
                            }
                            case 3 -> {
                                for (int layer = 0; layer < layers; layer++) {
                                    double t = layers == 1 ? 0.5D : (double) layer / (layers - 1);
                                    double y = (t - 0.5D) * height;
                                    float layerWidth = lineWidth + (float) ((1.0D - Math.abs(t - 0.5D) * 2.0D) * 2.0D);
                                    emitLine(
                                            pose,
                                            vertices,
                                            a.add(0.0D, y, 0.0D),
                                            b.add(0.0D, y, 0.0D),
                                            withAlpha(color, (int) (alpha * (0.55D + 0.45D * life))),
                                            layerWidth
                                    );
                                }
                            }
                            default -> {
                                // Wide layered ribbon inspired by the reference:
                                // several translucent rails form a tall, fat wake
                                // instead of a single hairline trail.
                                for (int layer = 0; layer < layers; layer++) {
                                    double t = layers == 1 ? 0.5D : (double) layer / (layers - 1);
                                    double side = (t - 0.5D) * width;
                                    double y = t * height;
                                    Vec3 offset = right.scale(side).add(0.0D, y, 0.0D);
                                    int layerAlpha = Math.max(20, (int) (alpha * (0.42D + 0.58D * (1.0D - Math.abs(t - 0.5D)))));
                                    emitLine(pose, vertices, a.add(offset), b.add(offset), withAlpha(color, layerAlpha), lineWidth);
                                }

                                Vec3 left = right.scale(-width * 0.5D);
                                Vec3 rightEdge = right.scale(width * 0.5D);
                                emitLine(pose, vertices, a.add(left), b.add(left), withAlpha(color, alpha), lineWidth + 0.6F);
                                emitLine(pose, vertices, a.add(rightEdge).add(0.0D, height, 0.0D), b.add(rightEdge).add(0.0D, height, 0.0D), withAlpha(color, alpha), lineWidth + 0.6F);
                            }
                        }
                    }
                }
        );

        if (cfg.trailStyle != 0) {
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, vertices) -> {
                        for (int i = 1; i < points.size(); i++) {
                            var previous = points.get(i - 1);
                            var current = points.get(i);
                            double age = Math.clamp((double) (now - current.createdAt()) / lifetime, 0.0D, 1.0D);
                            double life = 1.0D - age;
                            int alpha = (int) Math.round((cfg.trailStyle == 3 ? 120.0D : 92.0D) * life);
                            if (alpha <= 3) continue;

                            Vec3 a = previous.position();
                            Vec3 b = current.position();
                            Vec3 delta = b.subtract(a);
                            Vec3 right = new Vec3(-delta.z, 0.0D, delta.x);
                            if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
                            right = right.normalize();

                            int fill = effectColor(cfg.trailColorArgb, cfg.trailRainbow, i * 0.035F, alpha);

                            if (cfg.trailStyle == 2) {
                                double wave = Math.sin((i + now / 80.0D) * 0.48D) * height * 0.22D;
                                emitWingRibbon(pose, vertices, a, b, right, width, height, wave, fill);
                            } else {
                                emitRibbonPrism(pose, vertices, a, b, right, width, height, fill);
                            }
                        }
                    }
            );
        }
        poseStack.popPose();
    }

    private static void renderJumpCircles(LevelRenderContext context, Vec3 camera) {
        List<VisualEffectsController.JumpRing> rings = VisualEffectsController.ringSnapshot();
        if (rings.isEmpty()) return;

        var cfg = TopkaClient.CONFIG.get();
        long now = System.currentTimeMillis();
        long lifetime = Math.max(250L, cfg.jumpCircleLifetimeMs);
        int layers = Math.clamp(cfg.jumpCircleLayers, 1, 5);

        for (VisualEffectsController.JumpRing ring : rings) {
            double progress = Math.clamp((double) (now - ring.createdAt()) / lifetime, 0.0D, 1.0D);
            float radius = (float) (0.15D + progress * Math.max(0.2F, cfg.jumpCircleRadius));
            int alpha = (int) Math.round(235.0D * (1.0D - progress));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(
                    ring.position().x - camera.x,
                    ring.position().y - camera.y,
                    ring.position().z - camera.z
            );

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> {
                        switch (cfg.jumpCircleStyle) {
                            case 0 -> emitCircle(
                                    pose,
                                    vertices,
                                    radius,
                                    0.0D,
                                    effectColor(cfg.jumpCircleColorArgb, cfg.jumpCircleRainbow, 0.0F, alpha),
                                    cfg.jumpCircleLineWidth
                            );
                            case 2 -> {
                                for (int layer = 0; layer < layers; layer++) {
                                    float offset = layer * 0.055F;
                                    float ringRadius = radius * (1.0F - layer * 0.045F);
                                    int color = effectColor(cfg.jumpCircleColorArgb, cfg.jumpCircleRainbow, layer * 0.08F, Math.max(18, alpha - layer * 24));
                                    emitCircle(pose, vertices, ringRadius, offset, color, cfg.jumpCircleLineWidth);
                                }
                            }
                            case 3 -> {
                                for (int layer = 0; layer < layers; layer++) {
                                    float phase = layer / (float) Math.max(1, layers - 1);
                                    float ringRadius = radius * (0.62F + phase * 0.48F);
                                    double y = Math.sin((progress * Math.PI * 2.0D) + phase * Math.PI) * 0.08D;
                                    int color = effectColor(cfg.jumpCircleColorArgb, cfg.jumpCircleRainbow, phase * 0.22F, Math.max(18, alpha - layer * 18));
                                    emitCircle(pose, vertices, ringRadius, y, color, cfg.jumpCircleLineWidth + phase);
                                }
                            }
                            default -> {
                                // Thick luminous multi-ring style closest to the
                                // bright blue rings in the reference image.
                                for (int layer = 0; layer < layers; layer++) {
                                    float spread = layer * 0.045F;
                                    int glowAlpha = Math.max(16, alpha / (2 + layer));
                                    int coreAlpha = Math.max(30, alpha - layer * 20);
                                    int glowColor = effectColor(cfg.jumpCircleColorArgb, cfg.jumpCircleRainbow, layer * 0.07F, glowAlpha);
                                    int coreColor = effectColor(cfg.jumpCircleColorArgb, cfg.jumpCircleRainbow, layer * 0.07F, coreAlpha);
                                    emitCircle(pose, vertices, radius + spread, 0.002D * layer, glowColor, Math.min(12.0F, cfg.jumpCircleLineWidth * 2.25F));
                                    emitCircle(pose, vertices, radius + spread, 0.003D * layer, coreColor, cfg.jumpCircleLineWidth);
                                }
                            }
                        }
                    }
            );

            if (cfg.jumpCircleStyle != 0) {
                context.submitNodeCollector().submitCustomGeometry(
                        poseStack,
                        RenderTypes.debugQuads(),
                        (pose, vertices) -> {
                            float band = Math.max(0.055F, radius * 0.13F);
                            int fillAlpha = Math.max(10, alpha / 4);
                            if (cfg.jumpCircleStyle == 1) fillAlpha = Math.max(18, alpha / 3);
                            for (int layer = 0; layer < layers; layer++) {
                                float spread = layer * 0.045F;
                                float outer = radius + spread + band;
                                float inner = Math.max(0.02F, radius + spread - band);
                                emitAnnulus(
                                        pose,
                                        vertices,
                                        inner,
                                        outer,
                                        layer * 0.002D,
                                        cfg.jumpCircleColorArgb,
                                        cfg.jumpCircleRainbow,
                                        Math.max(8, fillAlpha - layer * 8)
                                );
                            }
                        }
                );
            }
            poseStack.popPose();
        }
    }

    private static void renderWaypointBeams(LevelRenderContext context, Vec3 camera) {
        List<WaypointConfig> waypoints = WaypointController.visibleWaypoints();
        if (waypoints.isEmpty()) return;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> {
                    for (WaypointConfig waypoint : waypoints) {
                        int color = withAlpha(waypoint.colorArgb, 210);
                        Vec3 base = new Vec3(waypoint.x + 0.5D, waypoint.y + 0.05D, waypoint.z + 0.5D);
                        Vec3 top = base.add(0.0D, 10.0D, 0.0D);
                        emitLine(pose, vertices, base, top, color, 2.0F);
                        emitCircleAt(pose, vertices, base, 0.55F, color, 2.0F);
                    }
                }
        );
        poseStack.popPose();
    }

    private static void renderCape(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (client.options.getCameraType().isFirstPerson() && !TopkaClient.CONFIG.get().capeShowOthers) return;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || entity.isRemoved()) continue;
            if (player != client.player && !TopkaClient.CONFIG.get().capeShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            var cfg = TopkaClient.CONFIG.get();
            float width = Math.clamp(cfg.capeWidth, 0.30F, 1.20F);
            float height = Math.clamp(cfg.capeHeight, 0.45F, 1.60F);
            float lineWidth = Math.clamp(cfg.capeLineWidth, 1.0F, 8.0F);

            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 back = forward.scale(-1.0D);
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));

            double speed = Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x
                    + player.getDeltaMovement().z * player.getDeltaMovement().z);
            double wave = Math.sin(System.currentTimeMillis() / 170.0D) * 0.035D
                    + Math.min(0.34D, speed * 0.38D);

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.maxY - 0.36D,
                    (box.minZ + box.maxZ) * 0.5D
            );

            Vec3 topCenter = center.add(back.scale(0.16D));
            Vec3 midCenter = topCenter.add(0.0D, -height * 0.50D, 0.0D).add(back.scale(0.08D + wave * 0.45D));
            Vec3 bottomCenter = topCenter.add(0.0D, -height, 0.0D).add(back.scale(0.15D + wave));

            Vec3 topLeft = topCenter.add(right.scale(-width / 2.0D));
            Vec3 topRight = topCenter.add(right.scale(width / 2.0D));
            Vec3 midLeft = midCenter.add(right.scale(-width * 0.49D));
            Vec3 midRight = midCenter.add(right.scale(width * 0.49D));
            Vec3 bottomLeft = bottomCenter.add(right.scale(-width * 0.44D));
            Vec3 bottomRight = bottomCenter.add(right.scale(width * 0.44D));

            int primary = effectColor(cfg.capeColorArgb, cfg.capeRainbow, 0.0F, cfg.capeOpacity);
            int secondary = effectColor(cfg.capeColorArgb, cfg.capeRainbow, 0.17F, Math.max(24, cfg.capeOpacity - 40));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, vertices) -> {
                        switch (Math.floorMod(cfg.capeStyle, 4)) {
                            case 1 -> {
                                // Split cloak with a narrow center gap.
                                Vec3 centerTopL = topCenter.add(right.scale(-0.035D));
                                Vec3 centerTopR = topCenter.add(right.scale(0.035D));
                                Vec3 centerBottomL = bottomCenter.add(right.scale(-0.065D));
                                Vec3 centerBottomR = bottomCenter.add(right.scale(0.065D));
                                emitQuad(pose, vertices, topLeft, centerTopL, centerBottomL, bottomLeft, primary);
                                emitQuad(pose, vertices, centerTopR, topRight, bottomRight, centerBottomR, secondary);
                            }
                            case 2 -> {
                                // Royal cloak: broad upper panel and tapered lower panel.
                                emitQuad(pose, vertices, topLeft, topRight, midRight, midLeft, primary);
                                emitQuad(pose, vertices, midLeft, midRight, bottomRight, bottomLeft, secondary);
                                Vec3 notch = bottomCenter.add(back.scale(0.045D)).add(0.0D, -0.06D, 0.0D);
                                emitQuad(pose, vertices, bottomLeft, bottomRight, notch, notch, withAlpha(primary, Math.max(18, cfg.capeOpacity - 55)));
                            }
                            case 3 -> {
                                // Energy mantle: narrower core over a wider translucent shell.
                                Vec3 shellLeft = topCenter.add(right.scale(-width * 0.62D));
                                Vec3 shellRight = topCenter.add(right.scale(width * 0.62D));
                                Vec3 shellBottomLeft = bottomCenter.add(right.scale(-width * 0.54D));
                                Vec3 shellBottomRight = bottomCenter.add(right.scale(width * 0.54D));
                                emitQuad(pose, vertices, shellLeft, shellRight, shellBottomRight, shellBottomLeft,
                                        withAlpha(primary, Math.max(18, cfg.capeOpacity / 2)));
                                emitQuad(pose, vertices, topLeft, topRight, bottomRight, bottomLeft, primary);
                            }
                            default -> {
                                // Fabric: two articulated panels so motion reads as cloth, not a flat rectangle.
                                emitQuad(pose, vertices, topLeft, topRight, midRight, midLeft, primary);
                                emitQuad(pose, vertices, midLeft, midRight, bottomRight, bottomLeft, secondary);
                            }
                        }
                    }
            );

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> {
                        int outline = effectColor(cfg.capeColorArgb, cfg.capeRainbow, 0.0F, 240);
                        float edge = cfg.capeGlow ? Math.min(10.0F, lineWidth * 1.65F) : lineWidth;
                        emitLine(pose, vertices, topLeft, topRight, outline, edge);
                        emitLine(pose, vertices, topRight, midRight, outline, edge);
                        emitLine(pose, vertices, midRight, bottomRight, outline, edge);
                        emitLine(pose, vertices, bottomRight, bottomLeft, outline, edge);
                        emitLine(pose, vertices, bottomLeft, midLeft, outline, edge);
                        emitLine(pose, vertices, midLeft, topLeft, outline, edge);
                        emitLine(pose, vertices, topLeft, midRight, withAlpha(outline, 125), Math.max(1.0F, lineWidth - 0.4F));
                        emitLine(pose, vertices, topRight, midLeft, withAlpha(outline, 125), Math.max(1.0F, lineWidth - 0.4F));
                    }
            );
            poseStack.popPose();
        }
    }

    private static void renderWings(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || entity.isRemoved()) continue;
            if (player != client.player && !cfg.wingsShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            // Use the rendered body yaw, not head/look direction. Head yaw was
            // the cause of the wings sliding sideways when the player looked around.
            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 back = forward.scale(-1.0D);
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

            float scale = Math.clamp(cfg.wingsScale, 0.45F, 2.25F);
            float spread = Math.clamp(cfg.wingsSpread, 0.35F, 1.65F);
            float flapAmount = Math.clamp(cfg.wingsFlapAmount, 0.0F, 0.55F);
            double phase = System.currentTimeMillis() / 1000.0D * Math.clamp(cfg.wingsFlapSpeed, 0.10F, 3.0F) * Math.PI * 2.0D;
            double flap = Math.sin(phase) * flapAmount;

            AABB playerBox = player.getBoundingBox();
            Vec3 anchor = new Vec3(
                    (playerBox.minX + playerBox.maxX) * 0.5D,
                    playerBox.maxY - 0.56D,
                    (playerBox.minZ + playerBox.maxZ) * 0.5D
            ).add(back.scale(0.115D));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(CosmeticTextures.WINGS),
                    (pose, vertices) -> {
                        emitWingSide(pose, vertices, anchor, right, back, up, -1.0D, scale, spread, flap, cfg, 0.0F);
                        emitWingSide(pose, vertices, anchor, right, back, up, 1.0D, scale, spread, flap, cfg, 0.5F);
                    }
            );

            // Only draw the internal structural bones when glow is enabled.
            // The old perimeter wireframe was what made the wings read as a mesh.
            if (cfg.wingsGlow) {
                context.submitNodeCollector().submitCustomGeometry(
                        poseStack,
                        RenderTypes.linesTranslucent(),
                        (pose, vertices) -> {
                            emitWingOutline(pose, vertices, anchor, right, back, up, -1.0D, scale, spread, flap, cfg, 0.0F);
                            emitWingOutline(pose, vertices, anchor, right, back, up, 1.0D, scale, spread, flap, cfg, 0.5F);
                        }
                );
            }

            poseStack.popPose();
        }
    }

    private static void emitWingSide(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 anchor,
            Vec3 right,
            Vec3 back,
            Vec3 up,
            double side,
            float scale,
            float spread,
            double flap,
            com.koqps.topka.config.TopkaConfig cfg,
            float colorOffset
    ) {
        int style = Math.floorMod(cfg.wingsStyle, 4);
        int detail = Math.clamp(cfg.wingsDetail, 1, 5);
        double s = side;
        double width = scale * spread;
        double lift = flap * scale;
        double depth = cfg.wingsDepth * scale;

        Vec3 root = anchor.add(right.scale(s * 0.06D * scale));
        Vec3 shoulder = root.add(right.scale(s * width * 0.43D)).add(up.scale((0.34D + lift * 0.30D) * scale));
        Vec3 elbow = root.add(right.scale(s * width * 0.92D)).add(up.scale((0.63D + lift * 0.62D) * scale)).add(back.scale(depth * 0.35D));
        Vec3 tip = root.add(right.scale(s * width * 1.58D)).add(up.scale((0.70D + lift) * scale)).add(back.scale(depth * 0.72D));
        Vec3 lowerTip = root.add(right.scale(s * width * 1.38D)).add(up.scale((-0.24D + lift * 0.42D) * scale)).add(back.scale(depth * 1.20D));
        Vec3 lowerRoot = root.add(up.scale(-0.52D * scale)).add(back.scale(depth * 0.58D));
        Vec3 tail = root.add(right.scale(s * width * 0.54D)).add(up.scale(-0.92D * scale)).add(back.scale(depth * 1.34D));

        int opacity = Math.clamp(cfg.wingsOpacity, 30, 255);
        int primary = effectColor(cfg.wingsPrimaryColorArgb, cfg.wingsRainbow, colorOffset, opacity);
        int secondary = effectColor(cfg.wingsSecondaryColorArgb, cfg.wingsRainbow, colorOffset + 0.15F, Math.max(40, opacity - 8));

        switch (style) {
            case 1 -> {
                // DEMON: large continuous membrane surfaces plus layered fingers.
                Vec3 claw = tip.add(right.scale(s * width * 0.22D)).add(up.scale(-0.10D * scale));
                emitWingPanel(pose, vertices, style, root, shoulder, elbow, lowerRoot, secondary);
                emitWingPanel(pose, vertices, style, shoulder, elbow, claw, lowerTip, primary);
                emitWingPanel(pose, vertices, style, lowerRoot, lowerTip, tail, root, withAlpha(primary, Math.max(55, opacity - 28)));

                for (int i = 0; i < detail; i++) {
                    double t = (i + 1.0D) / (detail + 1.0D);
                    Vec3 ridge = lerpVec(shoulder, elbow, t);
                    Vec3 edge = lerpVec(lowerRoot, lowerTip, t).add(back.scale(depth * 0.08D));
                    emitWingPanel(pose, vertices, style, ridge, edge, edge.add(right.scale(s * 0.08D * scale)), ridge.add(right.scale(s * 0.05D * scale)),
                            withAlpha(secondary, Math.max(48, opacity - 35)));
                }
            }
            case 2 -> {
                // CRYSTAL: overlapping faceted plates with texture detail.
                Vec3 high = tip.add(up.scale(0.20D * scale));
                Vec3 far = lowerTip.add(right.scale(s * width * 0.18D));
                emitWingPanel(pose, vertices, style, root, shoulder, high, elbow, primary);
                emitWingPanel(pose, vertices, style, root, elbow, far, lowerRoot, secondary);
                emitWingPanel(pose, vertices, style, lowerRoot, far, tail, root, withAlpha(primary, Math.max(50, opacity - 24)));

                for (int i = 0; i < detail; i++) {
                    double t = (i + 1.0D) / (detail + 1.0D);
                    Vec3 base = lerpVec(root, lowerRoot, t);
                    Vec3 shard = lerpVec(elbow, tip, Math.min(1.0D, 0.34D + t * 0.62D))
                            .add(right.scale(s * width * (0.05D + t * 0.09D)));
                    emitWingPanel(pose, vertices, style, base, shard, shard.add(up.scale(-0.14D * scale)), base.add(up.scale(-0.08D * scale)),
                            i % 2 == 0 ? primary : secondary);
                }
            }
            case 3 -> {
                // DRAGON: one broad leathery wing with overlapping scale-textured bands.
                Vec3 crown = elbow.add(up.scale(0.20D * scale));
                Vec3 rear = lowerTip.add(back.scale(depth * 0.62D));
                emitWingPanel(pose, vertices, style, root, shoulder, crown, lowerRoot, secondary);
                emitWingPanel(pose, vertices, style, shoulder, tip, lowerTip, crown, primary);
                emitWingPanel(pose, vertices, style, crown, lowerTip, rear, lowerRoot, withAlpha(primary, Math.max(55, opacity - 18)));
                emitWingPanel(pose, vertices, style, lowerRoot, rear, tail, root, withAlpha(secondary, Math.max(45, opacity - 30)));

                for (int i = 0; i < detail; i++) {
                    double t0 = i / (double) detail;
                    double t1 = (i + 1.0D) / detail;
                    Vec3 top0 = lerpVec(shoulder, tip, t0);
                    Vec3 top1 = lerpVec(shoulder, tip, t1);
                    Vec3 bot1 = lerpVec(lowerRoot, lowerTip, t1);
                    Vec3 bot0 = lerpVec(lowerRoot, lowerTip, t0);
                    emitWingPanel(pose, vertices, style, top0, top1, bot1, bot0,
                            withAlpha(i % 2 == 0 ? primary : secondary, Math.max(60, opacity - 22)));
                }
            }
            default -> {
                // ANGEL: many solid overlapping textured feathers.
                emitWingPanel(pose, vertices, style, root, shoulder, elbow, lowerRoot, secondary);
                int featherCount = 6 + detail * 3;
                for (int i = 0; i < featherCount; i++) {
                    double t = i / (double) Math.max(1, featherCount - 1);
                    Vec3 featherRoot = lerpVec(shoulder, lowerRoot, Math.min(0.92D, t * 0.96D));
                    double reach = width * (1.02D + 0.62D * (1.0D - t));
                    Vec3 featherTip = root
                            .add(right.scale(s * reach))
                            .add(up.scale((0.74D - t * 1.48D + lift * (0.82D - t * 0.38D)) * scale))
                            .add(back.scale(depth * (0.40D + t * 1.45D)));
                    double featherWidth = (0.13D + (1.0D - t) * 0.07D) * scale;
                    Vec3 base2 = featherRoot.add(right.scale(s * featherWidth));
                    Vec3 tip2 = featherTip.add(right.scale(-s * featherWidth * 0.58D)).add(up.scale(-0.12D * scale));
                    int feather = effectColor(
                            (i & 1) == 0 ? cfg.wingsPrimaryColorArgb : cfg.wingsSecondaryColorArgb,
                            cfg.wingsRainbow,
                            colorOffset + i * 0.021F,
                            Math.max(75, opacity - i * 2)
                    );
                    emitWingPanel(pose, vertices, style, featherRoot, base2, tip2, featherTip, feather);
                }
            }
        }
    }

    private static void emitWingPanel(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            int style,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            int color
    ) {
        float u0 = (style & 1) == 0 ? 0.0F : 0.5F;
        float v0 = style < 2 ? 0.0F : 0.5F;
        float u1 = u0 + 0.5F;
        float v1 = v0 + 0.5F;

        emitTexturedQuad(pose, vertices, a, b, c, d, color, u0, v0, u1, v1);
        // Reverse face so the cosmetic stays solid from front and rear camera angles.
        emitTexturedQuad(pose, vertices, d, c, b, a, color, u0, v0, u1, v1);
    }

    private static void emitWingOutline(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 anchor,
            Vec3 right,
            Vec3 back,
            Vec3 up,
            double side,
            float scale,
            float spread,
            double flap,
            com.koqps.topka.config.TopkaConfig cfg,
            float colorOffset
    ) {
        double s = side;
        double width = scale * spread;
        double lift = flap * scale;
        double depth = cfg.wingsDepth * scale;

        Vec3 root = anchor.add(right.scale(s * 0.06D * scale));
        Vec3 shoulder = root.add(right.scale(s * width * 0.43D)).add(up.scale((0.34D + lift * 0.30D) * scale));
        Vec3 elbow = root.add(right.scale(s * width * 0.92D)).add(up.scale((0.63D + lift * 0.62D) * scale)).add(back.scale(depth * 0.35D));
        Vec3 tip = root.add(right.scale(s * width * 1.58D)).add(up.scale((0.70D + lift) * scale)).add(back.scale(depth * 0.72D));
        Vec3 lowerRoot = root.add(up.scale(-0.52D * scale)).add(back.scale(depth * 0.58D));

        int bone = effectColor(cfg.wingsSecondaryColorArgb, cfg.wingsRainbow, colorOffset + 0.18F, 205);
        float boneWidth = Math.clamp(cfg.wingsBoneWidth, 1.0F, 8.0F);

        // Internal bones only — no perimeter wireframe.
        emitLine(pose, vertices, root, shoulder, bone, boneWidth);
        emitLine(pose, vertices, shoulder, elbow, bone, boneWidth);
        emitLine(pose, vertices, elbow, tip, bone, Math.max(1.0F, boneWidth - 0.3F));
        emitLine(pose, vertices, root, lowerRoot, withAlpha(bone, 150), Math.max(1.0F, boneWidth - 0.8F));
    }

    private static void renderBackWeapon(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || entity.isRemoved()) continue;
            if (player != client.player && !cfg.backWeaponShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 back = forward.scale(-1.0D);
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.maxY - 0.86D + cfg.backWeaponOffsetY,
                    (box.minZ + box.maxZ) * 0.5D
            ).add(back.scale(0.19D));

            float scale = Math.clamp(cfg.backWeaponScale, 0.45F, 2.25F);
            double angle = Math.toRadians(cfg.backWeaponAngle);
            Vec3 bladeUp = up.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
            Vec3 bladeSide = right.scale(Math.cos(angle)).add(up.scale(-Math.sin(angle)));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, vertices) -> emitBackWeaponModel(
                            pose, vertices, center, bladeUp, bladeSide, back, scale, cfg
                    )
            );
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> emitBackWeaponOutline(
                            pose, vertices, center, bladeUp, bladeSide, back, scale, cfg
                    )
            );
            poseStack.popPose();
        }
    }

    private static void emitBackWeaponModel(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 axis,
            Vec3 side,
            Vec3 back,
            float scale,
            com.koqps.topka.config.TopkaConfig cfg
    ) {
        int style = Math.floorMod(cfg.backWeaponStyle, 4);
        int alpha = Math.clamp(cfg.backWeaponOpacity, 40, 255);
        int primary = effectColor(cfg.backWeaponPrimaryArgb, cfg.backWeaponRainbow, 0.0F, alpha);
        int secondary = effectColor(cfg.backWeaponSecondaryArgb, cfg.backWeaponRainbow, 0.16F, Math.max(35, alpha - 18));

        double length = (style == 3 ? 1.52D : 1.38D) * scale;
        double bladeWidth = (style == 1 ? 0.095D : style == 2 ? 0.16D : 0.135D) * scale;
        Vec3 pommel = center.add(axis.scale(-0.60D * scale));
        Vec3 guard = center.add(axis.scale(-0.30D * scale));
        Vec3 bladeBase = center.add(axis.scale(-0.22D * scale));
        Vec3 bladeTip = center.add(axis.scale(length * 0.72D));

        if (style == 3) {
            // SCYTHE — shaft plus broad hooked blade.
            Vec3 shaftTop = center.add(axis.scale(0.72D * scale));
            Vec3 hookOut = shaftTop.add(side.scale(0.62D * scale)).add(axis.scale(-0.08D * scale));
            Vec3 hookTip = shaftTop.add(side.scale(0.82D * scale)).add(axis.scale(-0.42D * scale));
            emitBladeQuad(pose, vertices, pommel, shaftTop, side, 0.045D * scale, secondary);
            emitQuad(
                    pose, vertices,
                    shaftTop.add(side.scale(-0.06D * scale)),
                    hookOut.add(axis.scale(0.10D * scale)),
                    hookTip,
                    hookOut.add(axis.scale(-0.10D * scale)),
                    primary
            );
            return;
        }

        // Handle.
        emitBladeQuad(pose, vertices, pommel, guard, side, 0.055D * scale, secondary);

        // Guard.
        Vec3 guardLeft = guard.add(side.scale(-0.28D * scale));
        Vec3 guardRight = guard.add(side.scale(0.28D * scale));
        emitBladeQuad(pose, vertices, guardLeft, guardRight, axis, 0.045D * scale, secondary);

        if (style == 1) {
            // KATANA — narrow blade with slight lateral sweep.
            Vec3 mid = bladeBase.add(axis.scale(length * 0.46D)).add(side.scale(0.035D * scale));
            emitBladeQuad(pose, vertices, bladeBase, mid, side, bladeWidth, primary);
            emitBladeQuad(pose, vertices, mid, bladeTip.add(side.scale(0.09D * scale)), side, bladeWidth * 0.82D, primary);
        } else if (style == 2) {
            // CRYSTAL — faceted sword.
            Vec3 mid = bladeBase.add(axis.scale(length * 0.47D));
            Vec3 left = mid.add(side.scale(-bladeWidth * 1.25D));
            Vec3 rightP = mid.add(side.scale(bladeWidth * 1.25D));
            emitQuad(pose, vertices, bladeBase, left, bladeTip, rightP, primary);
            emitQuad(pose, vertices, bladeBase, rightP, bladeTip, left, withAlpha(secondary, Math.max(28, alpha - 35)));
        } else {
            // GREAT SWORD — wide two-stage blade.
            Vec3 mid = bladeBase.add(axis.scale(length * 0.46D));
            emitBladeQuad(pose, vertices, bladeBase, mid, side, bladeWidth, primary);
            emitBladeQuad(pose, vertices, mid, bladeTip, side, bladeWidth * 0.72D, secondary);
        }

        // Thin depth face so the blade does not read as a paper cutout from the side.
        Vec3 depth = back.scale(0.035D * scale);
        emitQuad(
                pose, vertices,
                bladeBase.add(depth),
                bladeTip.add(depth),
                bladeTip.subtract(depth),
                bladeBase.subtract(depth),
                withAlpha(primary, Math.max(24, alpha - 52))
        );
    }

    private static void emitBackWeaponOutline(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 axis,
            Vec3 side,
            Vec3 back,
            float scale,
            com.koqps.topka.config.TopkaConfig cfg
    ) {
        int style = Math.floorMod(cfg.backWeaponStyle, 4);
        int glow = effectColor(cfg.backWeaponSecondaryArgb, cfg.backWeaponRainbow, 0.18F, cfg.backWeaponGlow ? 230 : 190);
        float line = cfg.backWeaponGlow ? 4.0F : 2.0F;

        Vec3 pommel = center.add(axis.scale(-0.60D * scale));
        Vec3 guard = center.add(axis.scale(-0.30D * scale));
        Vec3 tip = center.add(axis.scale((style == 3 ? 1.10D : 0.99D) * scale));

        if (cfg.backWeaponGlow) {
            emitLine(pose, vertices, pommel, tip, withAlpha(glow, 65), 8.0F);
        }
        emitLine(pose, vertices, pommel, tip, glow, line);
        emitLine(pose, vertices, guard.add(side.scale(-0.30D * scale)), guard.add(side.scale(0.30D * scale)), glow, Math.max(1.0F, line - 0.5F));
    }

    private static void renderHeadCosmetic(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || entity.isRemoved()) continue;
            if (player != client.player && !cfg.headCosmeticShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.maxY + 0.08D,
                    (box.minZ + box.maxZ) * 0.5D
            );

            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            float scale = Math.clamp(cfg.headCosmeticScale, 0.45F, 2.0F);

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, vertices) -> emitHeadCosmeticModel(pose, vertices, center, right, forward, up, scale, cfg)
            );
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> emitHeadCosmeticOutline(pose, vertices, center, right, forward, up, scale, cfg)
            );
            poseStack.popPose();
        }
    }

    private static void emitHeadCosmeticModel(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 right,
            Vec3 forward,
            Vec3 up,
            float scale,
            com.koqps.topka.config.TopkaConfig cfg
    ) {
        int style = Math.floorMod(cfg.headCosmeticStyle, 4);
        int alpha = Math.clamp(cfg.headCosmeticOpacity, 40, 255);
        int primary = effectColor(cfg.headCosmeticPrimaryArgb, cfg.headCosmeticRainbow, 0.0F, alpha);
        int secondary = effectColor(cfg.headCosmeticSecondaryArgb, cfg.headCosmeticRainbow, 0.16F, Math.max(28, alpha - 18));

        if (style == 1) {
            // HORNS
            for (double s : new double[]{-1.0D, 1.0D}) {
                Vec3 base = center.add(right.scale(s * 0.22D * scale)).add(forward.scale(-0.03D * scale));
                Vec3 mid = base.add(right.scale(s * 0.14D * scale)).add(up.scale(0.30D * scale));
                Vec3 tip = mid.add(right.scale(s * 0.09D * scale)).add(up.scale(0.26D * scale)).add(forward.scale(-0.06D * scale));
                emitBladeQuad(pose, vertices, base, mid, forward, 0.07D * scale, primary);
                emitBladeQuad(pose, vertices, mid, tip, forward, 0.045D * scale, secondary);
            }
        } else if (style == 2) {
            // ANTLERS
            for (double s : new double[]{-1.0D, 1.0D}) {
                Vec3 root = center.add(right.scale(s * 0.19D * scale));
                Vec3 stem = root.add(right.scale(s * 0.12D * scale)).add(up.scale(0.48D * scale));
                Vec3 outer = stem.add(right.scale(s * 0.26D * scale)).add(up.scale(0.12D * scale));
                emitBladeQuad(pose, vertices, root, stem, forward, 0.045D * scale, primary);
                emitBladeQuad(pose, vertices, stem, outer, forward, 0.035D * scale, secondary);
                Vec3 tine = stem.add(up.scale(0.24D * scale)).add(right.scale(-s * 0.03D * scale));
                emitBladeQuad(pose, vertices, stem, tine, forward, 0.03D * scale, secondary);
            }
        } else if (style == 3) {
            // ARCANE CREST
            double bob = Math.sin(System.currentTimeMillis() / 280.0D) * 0.025D * scale;
            Vec3 c0 = center.add(up.scale((0.24D + bob) * scale));
            emitAnnulusWorld(pose, vertices, c0, right, forward, 0.28F * scale, 0.40F * scale, primary);
        } else {
            // CROWN
            Vec3 left = center.add(right.scale(-0.31D * scale));
            Vec3 rightP = center.add(right.scale(0.31D * scale));
            Vec3 front = forward.scale(0.18D * scale);
            Vec3 backV = forward.scale(-0.18D * scale);
            Vec3 lF = left.add(front);
            Vec3 rF = rightP.add(front);
            Vec3 lB = left.add(backV);
            Vec3 rB = rightP.add(backV);
            emitQuad(pose, vertices, lF, rF, rB, lB, withAlpha(primary, Math.max(30, alpha - 35)));
            Vec3 peakL = left.add(up.scale(0.30D * scale));
            Vec3 peakC = center.add(up.scale(0.42D * scale)).add(front.scale(0.20D));
            Vec3 peakR = rightP.add(up.scale(0.30D * scale));
            emitQuad(pose, vertices, lF, peakL, peakC, center.add(front), primary);
            emitQuad(pose, vertices, center.add(front), peakC, peakR, rF, secondary);
        }
    }

    private static void emitHeadCosmeticOutline(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 right,
            Vec3 forward,
            Vec3 up,
            float scale,
            com.koqps.topka.config.TopkaConfig cfg
    ) {
        int color = effectColor(cfg.headCosmeticPrimaryArgb, cfg.headCosmeticRainbow, 0.0F, 240);
        float width = cfg.headCosmeticGlow ? 3.6F : 2.0F;

        if (cfg.headCosmeticStyle == 0) {
            Vec3 l = center.add(right.scale(-0.31D * scale));
            Vec3 r = center.add(right.scale(0.31D * scale));
            emitLine(pose, vertices, l, center.add(up.scale(0.42D * scale)), color, width);
            emitLine(pose, vertices, center.add(up.scale(0.42D * scale)), r, color, width);
        } else if (cfg.headCosmeticStyle == 3) {
            Vec3 c0 = center.add(up.scale(0.24D * scale));
            emitCirclePlane(pose, vertices, c0, right, forward, 0.40F * scale, color, width);
        }
    }

    private static void emitBladeQuad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 a,
            Vec3 b,
            Vec3 widthAxis,
            double halfWidth,
            int color
    ) {
        Vec3 w = widthAxis.normalize().scale(halfWidth);
        emitQuad(pose, vertices, a.subtract(w), a.add(w), b.add(w), b.subtract(w), color);
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    private static void emitAnnulusWorld(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 axisA,
            Vec3 axisB,
            float innerRadius,
            float outerRadius,
            int color
    ) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0D * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS;
            Vec3 i0 = center.add(axisA.scale(Math.cos(a0) * innerRadius)).add(axisB.scale(Math.sin(a0) * innerRadius));
            Vec3 i1 = center.add(axisA.scale(Math.cos(a1) * innerRadius)).add(axisB.scale(Math.sin(a1) * innerRadius));
            Vec3 o1 = center.add(axisA.scale(Math.cos(a1) * outerRadius)).add(axisB.scale(Math.sin(a1) * outerRadius));
            Vec3 o0 = center.add(axisA.scale(Math.cos(a0) * outerRadius)).add(axisB.scale(Math.sin(a0) * outerRadius));
            emitQuad(pose, vertices, i0, i1, o1, o0, color);
        }
    }

    private static void emitCirclePlane(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 center,
            Vec3 axisA,
            Vec3 axisB,
            float radius,
            int color,
            float width
    ) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0D * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS;
            Vec3 p0 = center.add(axisA.scale(Math.cos(a0) * radius)).add(axisB.scale(Math.sin(a0) * radius));
            Vec3 p1 = center.add(axisA.scale(Math.cos(a1) * radius)).add(axisB.scale(Math.sin(a1) * radius));
            emitLine(pose, vertices, p0, p1, color, width);
        }
    }

    private static void renderProjectilePrediction(LevelRenderContext context, Vec3 camera, Minecraft client) {
        ProjectileSpec spec = projectileSpec(client.player.getMainHandItem(), client.player);
        if (spec == null) spec = projectileSpec(client.player.getOffhandItem(), client.player);
        if (spec == null) return;

        int steps = Math.clamp(TopkaClient.CONFIG.get().projectileSteps, 16, 160);
        List<Vec3> points = new ArrayList<>(steps + 1);

        Vec3 position = client.player.getEyePosition();
        Vec3 velocity = client.player.getLookAngle().normalize().scale(spec.speed());
        points.add(position);

        Vec3 impact = null;
        for (int i = 0; i < steps; i++) {
            Vec3 next = position.add(velocity);
            HitResult hit = client.level.clip(new ClipContext(
                    position,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    client.player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                impact = hit.getLocation();
                points.add(impact);
                break;
            }

            points.add(next);
            position = next;
            velocity = velocity.scale(spec.drag()).add(0.0D, -spec.gravity(), 0.0D);
            if (position.y < client.level.getMinY() - 8) break;
        }

        if (points.size() < 2) return;
        var cfg = TopkaClient.CONFIG.get();
        int color = cfg.projectileColorArgb;
        float width = Math.clamp(cfg.projectileLineWidth, 1.0F, 5.0F);

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> {
                    for (int i = 1; i < points.size(); i++) {
                        int alpha = Math.max(70, 235 - (int) (165.0D * i / points.size()));
                        emitLine(pose, vertices, points.get(i - 1), points.get(i), withAlpha(color, alpha), width);
                    }
                }
        );
        poseStack.popPose();

        if (impact != null) {
            AABB marker = new AABB(
                    impact.x - 0.09D, impact.y - 0.09D, impact.z - 0.09D,
                    impact.x + 0.09D, impact.y + 0.09D, impact.z + 0.09D
            );
            context.poseStack().pushPose();
            context.poseStack().translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    context.poseStack(),
                    Shapes.create(marker),
                    RenderTypes.linesTranslucent(),
                    color,
                    Math.max(2.0F, width),
                    true
            );
            context.poseStack().popPose();
        }
    }

    private static ProjectileSpec projectileSpec(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return null;

        if (stack.is(Items.BOW)) {
            if (!player.isUsingItem()) return null;
            int useTicks = Math.max(0, 72000 - player.getUseItemRemainingTicks());
            float draw = Math.clamp(useTicks / 20.0F, 0.0F, 1.0F);
            draw = (draw * draw + draw * 2.0F) / 3.0F;
            if (draw < 0.05F) return null;
            return new ProjectileSpec(Math.min(1.0F, draw) * 3.0D, 0.05D, 0.99D);
        }
        if (stack.is(Items.CROSSBOW)) return new ProjectileSpec(3.15D, 0.05D, 0.99D);
        if (stack.is(Items.TRIDENT)) return new ProjectileSpec(2.5D, 0.05D, 0.99D);
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)) {
            return new ProjectileSpec(1.5D, 0.03D, 0.99D);
        }
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.EXPERIENCE_BOTTLE)) {
            return new ProjectileSpec(0.9D, 0.05D, 0.99D);
        }
        return null;
    }

    private static void emitCircle(PoseStack.Pose pose, VertexConsumer vertices, float radius, double y, int color, float width) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0D * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS;
            Vec3 p0 = new Vec3(Math.cos(a0) * radius, y, Math.sin(a0) * radius);
            Vec3 p1 = new Vec3(Math.cos(a1) * radius, y, Math.sin(a1) * radius);
            emitLine(pose, vertices, p0, p1, color, width);
        }
    }

    private static void emitCircleAt(PoseStack.Pose pose, VertexConsumer vertices, Vec3 center, float radius, int color, float width) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0D * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS;
            Vec3 p0 = center.add(Math.cos(a0) * radius, 0.0D, Math.sin(a0) * radius);
            Vec3 p1 = center.add(Math.cos(a1) * radius, 0.0D, Math.sin(a1) * radius);
            emitLine(pose, vertices, p0, p1, color, width);
        }
    }

    private static void emitLine(PoseStack.Pose pose, VertexConsumer vertices, Vec3 start, Vec3 end, int color, float width) {
        float nx = (float) (end.x - start.x);
        float ny = (float) (end.y - start.y);
        float nz = (float) (end.z - start.z);

        vertices.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(width);
        vertices.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(width);
    }

    private static void emitTexturedQuad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            int color,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        vertices.addVertex(pose, (float) a.x, (float) a.y, (float) a.z)
                .setColor(color).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, (float) b.x, (float) b.y, (float) b.z)
                .setColor(color).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, (float) c.x, (float) c.y, (float) c.z)
                .setColor(color).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, (float) d.x, (float) d.y, (float) d.z)
                .setColor(color).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void emitQuad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            int color
    ) {
        vertices.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(color);
        vertices.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(color);
        vertices.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(color);
        vertices.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(color);
    }

    private static void emitRibbonPrism(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 a,
            Vec3 b,
            Vec3 right,
            float width,
            float height,
            int color
    ) {
        Vec3 leftOffset = right.scale(-width * 0.5D);
        Vec3 rightOffset = right.scale(width * 0.5D);
        Vec3 aLL = a.add(leftOffset);
        Vec3 bLL = b.add(leftOffset);
        Vec3 aRL = a.add(rightOffset);
        Vec3 bRL = b.add(rightOffset);
        Vec3 up = new Vec3(0.0D, height, 0.0D);
        Vec3 aLU = aLL.add(up);
        Vec3 bLU = bLL.add(up);
        Vec3 aRU = aRL.add(up);
        Vec3 bRU = bRL.add(up);

        emitQuad(pose, vertices, aLL, bLL, bLU, aLU, color);
        emitQuad(pose, vertices, bRL, aRL, aRU, bRU, color);
        emitQuad(pose, vertices, aLU, bLU, bRU, aRU, withAlpha(color, Math.max(12, ((color >>> 24) & 0xFF) * 3 / 4)));
    }

    private static void emitWingRibbon(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            Vec3 a,
            Vec3 b,
            Vec3 right,
            float width,
            float height,
            double wave,
            int color
    ) {
        Vec3 centerA = a.add(0.0D, height * 0.48D, 0.0D);
        Vec3 centerB = b.add(0.0D, height * 0.48D, 0.0D);
        Vec3 leftA = a.add(right.scale(-width * 0.22D)).add(0.0D, wave, 0.0D);
        Vec3 leftB = b.add(right.scale(-width)).add(0.0D, height + wave, 0.0D);
        Vec3 rightA = a.add(right.scale(width * 0.22D)).add(0.0D, -wave, 0.0D);
        Vec3 rightB = b.add(right.scale(width)).add(0.0D, height - wave, 0.0D);

        emitQuad(pose, vertices, centerA, centerB, leftB, leftA, color);
        emitQuad(pose, vertices, rightA, rightB, centerB, centerA, color);
    }

    private static void emitAnnulus(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float innerRadius,
            float outerRadius,
            double y,
            int baseColor,
            boolean rainbow,
            int alpha
    ) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0D * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / CIRCLE_SEGMENTS;
            Vec3 i0 = new Vec3(Math.cos(a0) * innerRadius, y, Math.sin(a0) * innerRadius);
            Vec3 i1 = new Vec3(Math.cos(a1) * innerRadius, y, Math.sin(a1) * innerRadius);
            Vec3 o1 = new Vec3(Math.cos(a1) * outerRadius, y, Math.sin(a1) * outerRadius);
            Vec3 o0 = new Vec3(Math.cos(a0) * outerRadius, y, Math.sin(a0) * outerRadius);
            int color = effectColor(baseColor, rainbow, i / (float) CIRCLE_SEGMENTS, alpha);
            emitQuad(pose, vertices, i0, i1, o1, o0, color);
        }
    }

    private static void emitConeShell(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float radius,
            float height,
            int baseColor,
            boolean rainbow,
            int alpha
    ) {
        int bands = 7;
        for (int band = 0; band < bands; band++) {
            float t0 = band / (float) bands;
            float t1 = (band + 1) / (float) bands;
            float r0 = radius * (1.0F - t0 * 0.96F);
            float r1 = radius * (1.0F - t1 * 0.96F);
            double y0 = height * t0;
            double y1 = height * t1;

            for (int i = 0; i < HAT_SEGMENTS; i++) {
                double a0 = Math.PI * 2.0D * i / HAT_SEGMENTS;
                double a1 = Math.PI * 2.0D * (i + 1) / HAT_SEGMENTS;
                Vec3 p00 = new Vec3(Math.cos(a0) * r0, y0, Math.sin(a0) * r0);
                Vec3 p01 = new Vec3(Math.cos(a1) * r0, y0, Math.sin(a1) * r0);
                Vec3 p11 = new Vec3(Math.cos(a1) * r1, y1, Math.sin(a1) * r1);
                Vec3 p10 = new Vec3(Math.cos(a0) * r1, y1, Math.sin(a0) * r1);
                int bandAlpha = Math.max(12, alpha - band * 7);
                int color = effectColor(baseColor, rainbow, i / (float) HAT_SEGMENTS + band * 0.035F, bandAlpha);
                emitQuad(pose, vertices, p00, p01, p11, p10, color);
            }
        }
    }

    private static int effectColor(int baseArgb, boolean rainbow, float offset, int alpha) {
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

    private record ProjectileSpec(double speed, double gravity, double drag) { }
}
