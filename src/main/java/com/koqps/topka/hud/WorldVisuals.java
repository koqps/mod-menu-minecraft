package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.WaypointConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
        if (TopkaClient.MODULES.byId("cape").enabled()) renderCape(context, camera, client);
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
            float lineWidth = Math.clamp(cfg.chinaHatLineWidth, 1.0F, 5.0F);
            int color = Theme.accent();

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(x - camera.x, baseY - camera.y, z - camera.z);

            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> {
                        for (int i = 0; i < HAT_SEGMENTS; i++) {
                            double a0 = Math.PI * 2.0D * i / HAT_SEGMENTS;
                            double a1 = Math.PI * 2.0D * (i + 1) / HAT_SEGMENTS;
                            Vec3 p0 = new Vec3(Math.cos(a0) * radius, 0.0D, Math.sin(a0) * radius);
                            Vec3 p1 = new Vec3(Math.cos(a1) * radius, 0.0D, Math.sin(a1) * radius);
                            emitLine(pose, vertices, p0, p1, color, lineWidth);
                        }

                        Vec3 apex = new Vec3(0.0D, height, 0.0D);
                        for (int i = 0; i < HAT_SEGMENTS; i += 4) {
                            double angle = Math.PI * 2.0D * i / HAT_SEGMENTS;
                            Vec3 edge = new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                            emitLine(pose, vertices, edge, apex, color, lineWidth);
                        }
                    }
            );
            poseStack.popPose();
        }
    }

    private static void renderHalo(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (client.options.getCameraType().isFirstPerson()) return;

        var cfg = TopkaClient.CONFIG.get();
        double bob = Math.sin(System.currentTimeMillis() / 350.0D) * 0.035D;
        double x = client.player.getX();
        double y = client.player.getBoundingBox().maxY + cfg.haloHeight + bob;
        double z = client.player.getZ();
        float radius = Math.clamp(cfg.haloRadius, 0.2F, 1.0F);
        float width = Math.clamp(cfg.haloLineWidth, 1.0F, 5.0F);
        int color = Theme.secondary();

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);

        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> emitCircle(pose, vertices, radius, 0.0D, color, width)
        );
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
            float lineWidth = Math.clamp(cfg.capeLineWidth, 1.0F, 5.0F);
            int color = cfg.capeColorArgb;

            Vec3 look = player.getLookAngle();
            Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
            if (back.lengthSqr() < 1.0E-5D) back = new Vec3(0.0D, 0.0D, -1.0D);
            back = back.normalize();
            Vec3 right = new Vec3(-back.z, 0.0D, back.x);

            double speed = Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x
                    + player.getDeltaMovement().z * player.getDeltaMovement().z);
            double wave = Math.sin(System.currentTimeMillis() / 170.0D) * 0.04D + Math.min(0.30D, speed * 0.35D);

            Vec3 topCenter = new Vec3(player.getX(), player.getBoundingBox().maxY - 0.36D, player.getZ())
                    .add(back.scale(0.20D));
            Vec3 bottomCenter = topCenter.add(0.0D, -height, 0.0D).add(back.scale(0.14D + wave));
            Vec3 topLeft = topCenter.add(right.scale(-width / 2.0D));
            Vec3 topRight = topCenter.add(right.scale(width / 2.0D));
            Vec3 bottomLeft = bottomCenter.add(right.scale(-width * 0.46D));
            Vec3 bottomRight = bottomCenter.add(right.scale(width * 0.46D));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.linesTranslucent(),
                    (pose, vertices) -> {
                        emitLine(pose, vertices, topLeft, topRight, color, lineWidth);
                        emitLine(pose, vertices, topRight, bottomRight, color, lineWidth);
                        emitLine(pose, vertices, bottomRight, bottomLeft, color, lineWidth);
                        emitLine(pose, vertices, bottomLeft, topLeft, color, lineWidth);
                        emitLine(pose, vertices, topCenter, bottomCenter, withAlpha(color, 170), Math.max(1.0F, lineWidth - 0.5F));
                    }
            );
            poseStack.popPose();
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
