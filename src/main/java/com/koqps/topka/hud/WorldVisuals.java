package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

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

        if (TopkaClient.MODULES.byId("hitbox").enabled()) {
            renderHitboxes(context, camera, client);
        }
        if (TopkaClient.MODULES.byId("targeting").enabled()) {
            renderTarget(context, camera, client);
        }
        if (TopkaClient.MODULES.byId("china_hat").enabled()) {
            renderChinaHats(context, camera, client);
        }
        if (TopkaClient.MODULES.byId("halo").enabled()) {
            renderHalo(context, camera, client);
        }
        if (TopkaClient.MODULES.byId("trails").enabled()) {
            renderTrail(context, camera);
        }
        if (TopkaClient.MODULES.byId("jump_circles").enabled()) {
            renderJumpCircles(context, camera);
        }
        if (TopkaClient.MODULES.byId("projectile_trajectory").enabled()) {
            renderProjectileTrajectory(context, camera, client);
        }
    }

    private static void renderHitboxes(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();
        double expand = Math.clamp(cfg.hitboxExpand, 0.0F, 1.0F);
        float width = Math.clamp(cfg.hitboxLineWidth, 1.0F, 6.0F);

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.isRemoved() || entity == client.player || client.player.distanceToSqr(entity) > 4096.0D) continue;

            AABB bounds = entity.getBoundingBox().inflate(expand);
            int color = entity instanceof Player ? cfg.hitboxColorArgb : withAlpha(cfg.hitboxColorArgb, 185);

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

    private static void renderTarget(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (!(client.hitResult instanceof EntityHitResult entityHit)) return;

        Entity entity = entityHit.getEntity();
        if (entity == client.player || entity.isRemoved()) return;

        var cfg = TopkaClient.CONFIG.get();
        AABB bounds = entity.getBoundingBox().inflate(0.055D);

        context.poseStack().pushPose();
        context.poseStack().translate(-camera.x, -camera.y, -camera.z);
        context.submitNodeCollector().submitShapeOutline(
                context.poseStack(),
                Shapes.create(bounds),
                RenderTypes.linesTranslucent(),
                cfg.targetColorArgb,
                3.0F,
                true
        );
        context.poseStack().popPose();
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
        int color = cfg.secondaryAccentArgb;

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
                        int alpha = (int) Math.round(210.0D * (1.0D - age));
                        int color = withAlpha(cfg.trailColorArgb, alpha);
                        emitLine(pose, vertices, previous.position(), current.position(), color, cfg.trailLineWidth);
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

        for (VisualEffectsController.JumpRing ring : rings) {
            double progress = Math.clamp((double) (now - ring.createdAt()) / lifetime, 0.0D, 1.0D);
            float radius = (float) (0.15D + progress * Math.max(0.2F, cfg.jumpCircleRadius));
            int alpha = (int) Math.round(230.0D * (1.0D - progress));
            int color = withAlpha(cfg.jumpCircleColorArgb, alpha);

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
                    (pose, vertices) -> emitCircle(
                            pose,
                            vertices,
                            radius,
                            0.0D,
                            color,
                            cfg.jumpCircleLineWidth
                    )
            );
            poseStack.popPose();
        }
    }


    private static void renderProjectileTrajectory(LevelRenderContext context, Vec3 camera, Minecraft client) {
        ItemStack stack = client.player.getMainHandItem();
        if (!isSupportedProjectile(stack)) {
            stack = client.player.getOffhandItem();
        }
        if (!isSupportedProjectile(stack)) return;

        var cfg = TopkaClient.CONFIG.get();
        int steps = Math.clamp(cfg.trajectorySteps, 12, 120);
        float width = Math.clamp(cfg.trajectoryLineWidth, 1.0F, 6.0F);
        int color = cfg.trajectoryColorArgb;

        double speed = projectileSpeed(stack);
        double gravity = projectileGravity(stack);
        Vec3 position = client.player.getEyePosition().add(client.player.getLookAngle().scale(0.20D));
        Vec3 velocity = client.player.getLookAngle().normalize().scale(speed);
        List<Vec3> points = new java.util.ArrayList<>(steps + 1);
        points.add(position);

        for (int i = 0; i < steps; i++) {
            position = position.add(velocity.scale(0.10D));
            points.add(position);
            velocity = new Vec3(velocity.x * 0.99D, velocity.y * 0.99D - gravity * 0.10D, velocity.z * 0.99D);
            if (position.y < client.level.getMinY() - 4) break;
        }

        if (points.size() < 2) return;
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> {
                    for (int i = 1; i < points.size(); i++) {
                        int alpha = 230 - (int) (150.0D * i / Math.max(1, points.size() - 1));
                        emitLine(pose, vertices, points.get(i - 1), points.get(i), withAlpha(color, alpha), width);
                    }
                }
        );
        poseStack.popPose();

        Vec3 end = points.get(points.size() - 1);
        poseStack.pushPose();
        poseStack.translate(end.x - camera.x, end.y - camera.y, end.z - camera.z);
        context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.linesTranslucent(),
                (pose, vertices) -> emitCircle(pose, vertices, 0.14F, 0.0D, color, width)
        );
        poseStack.popPose();
    }

    private static boolean isSupportedProjectile(ItemStack stack) {
        return !stack.isEmpty() && (
                stack.is(Items.BOW)
                        || stack.is(Items.CROSSBOW)
                        || stack.is(Items.TRIDENT)
                        || stack.is(Items.SNOWBALL)
                        || stack.is(Items.EGG)
                        || stack.is(Items.ENDER_PEARL)
        );
    }

    private static double projectileSpeed(ItemStack stack) {
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)) return 1.55D;
        if (stack.is(Items.TRIDENT)) return 2.50D;
        return 3.00D;
    }

    private static double projectileGravity(ItemStack stack) {
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return 0.05D;
        if (stack.is(Items.TRIDENT)) return 0.05D;
        return 0.03D;
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

    private static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }
}
