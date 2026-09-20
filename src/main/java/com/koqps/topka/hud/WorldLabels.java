package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.config.WaypointConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WorldLabels {
    private WorldLabels() { }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(WorldLabels::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.screen() != null) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        SubmitNodeCollector collector = context.submitNodeCollector();
        PoseStack poseStack = context.poseStack();
        CameraRenderState cameraState = context.levelState().cameraRenderState;

        if (TopkaClient.MODULES.byId("health_tags").enabled()) {
            submitHealthTags(client, poseStack, collector, cameraState, camera);
        }
        if (TopkaClient.MODULES.byId("waypoints").enabled()) {
            submitWaypoints(client, poseStack, collector, cameraState, camera);
        }
    }

    private static void submitHealthTags(
            Minecraft client,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            Vec3 camera
    ) {
        double maxDistance = Math.clamp(TopkaClient.CONFIG.get().healthTagMaxDistance, 8.0D, 128.0D);
        double maxDistanceSqr = maxDistance * maxDistance;

        List<Player> players = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != client.player && !player.isRemoved()
                    && client.player.distanceToSqr(player) <= maxDistanceSqr) {
                players.add(player);
            }
        }
        players.sort(Comparator.comparingDouble(player -> -player.distanceToSqr(camera)));

        for (Player player : players) {
            float health = Math.max(0.0F, player.getHealth());
            float max = Math.max(1.0F, player.getMaxHealth());
            float ratio = Math.clamp(health / max, 0.0F, 1.0F);
            int rgb = healthColor(ratio);

            String label = TopkaClient.CONFIG.get().healthTagHearts
                    ? "❤ " + Math.round(health)
                    : String.format("%.1f HP", health);
            Component text = Component.literal(label)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));

            poseStack.pushPose();
            poseStack.translate(player.getX() - camera.x, player.getY() - camera.y, player.getZ() - camera.z);
            collector.submitNameTag(
                    poseStack,
                    new Vec3(0.0D, player.getBbHeight() + 0.42D, 0.0D),
                    0,
                    text,
                    true,
                    LightCoordsUtil.FULL_BRIGHT,
                    cameraState
            );
            poseStack.popPose();
        }
    }

    private static void submitWaypoints(
            Minecraft client,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            Vec3 camera
    ) {
        for (WaypointConfig waypoint : WaypointController.visibleWaypoints()) {
            double dx = waypoint.x + 0.5D - client.player.getX();
            double dy = waypoint.y + 0.5D - client.player.getY();
            double dz = waypoint.z + 0.5D - client.player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));

            Component text = Component.literal(waypoint.name + "  " + distance + "m")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(waypoint.colorArgb & 0xFFFFFF)));

            poseStack.pushPose();
            poseStack.translate(
                    waypoint.x + 0.5D - camera.x,
                    waypoint.y + 1.4D - camera.y,
                    waypoint.z + 0.5D - camera.z
            );
            collector.submitNameTag(
                    poseStack,
                    Vec3.ZERO,
                    0,
                    text,
                    true,
                    LightCoordsUtil.FULL_BRIGHT,
                    cameraState
            );
            poseStack.popPose();
        }
    }

    private static int healthColor(float ratio) {
        int r = ratio < 0.5F ? 255 : Math.round(255F * (1F - ratio) * 2F);
        int g = ratio > 0.5F ? 255 : Math.round(255F * ratio * 2F);
        return (Math.clamp(r, 0, 255) << 16) | (Math.clamp(g, 0, 255) << 8) | 0x40;
    }
}
