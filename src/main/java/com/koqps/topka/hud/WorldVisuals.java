package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class WorldVisuals {
    private WorldVisuals() { }

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(WorldVisuals::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.screen() != null) return;

        boolean hitboxes = TopkaClient.MODULES.byId("hitbox").enabled();
        boolean hat = TopkaClient.MODULES.byId("china_hat").enabled();
        if (!hitboxes && !hat) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (hitboxes) renderHitboxes(context, camera, client);
        if (hat && !client.options.getCameraType().isFirstPerson()) renderChinaHat(context, camera, client);
    }

    private static void renderHitboxes(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();
        double expand = Math.max(0.0F, cfg.hitboxExpand);
        int color = cfg.hitboxColorArgb;
        float width = 2.0F;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.isRemoved() || entity == client.player || client.player.distanceToSqr(entity) > 4096.0) continue;
            AABB bounds = entity.getBoundingBox().inflate(expand);
            context.poseStack().pushPose();
            context.poseStack().translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    context.poseStack(), Shapes.create(bounds), RenderTypes.linesTranslucent(), color, width, true);
            context.poseStack().popPose();
        }
    }

    private static void renderChinaHat(LevelRenderContext context, Vec3 camera, Minecraft client) {
        double x = client.player.getX();
        double z = client.player.getZ();
        double y = client.player.getBoundingBox().maxY + 0.10;
        int color = Theme.accent();

        for (int i = 0; i < 4; i++) {
            double radius = 0.62 - i * 0.13;
            double yy = y + i * 0.08;
            AABB slice = new AABB(x - radius, yy, z - radius, x + radius, yy + 0.012, z + radius);
            context.poseStack().pushPose();
            context.poseStack().translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    context.poseStack(), Shapes.create(slice), RenderTypes.linesTranslucent(), color, 2.0F, true);
            context.poseStack().popPose();
        }
    }
}
