package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the supplied OBJ cosmetic packs without rebuilding their geometry.
 * The packed mesh data comes directly from the uploaded ZIPs.
 */
public final class ImportedCosmeticRenderer {
    private ImportedCosmeticRenderer() { }

    public static void renderWingPack(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.wingsShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.minY + 0.84D + cfg.importedWingVerticalOffset,
                    (box.minZ + box.maxZ) * 0.5D
            );

            double yaw = player.getVisualRotationYInDegrees();
            double radians = Math.toRadians(yaw);
            Vec3 back = new Vec3(Math.sin(radians), 0.0D, -Math.cos(radians));
            center = center.add(back.scale(0.11D + cfg.importedWingBackOffset));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            poseStack.rotateDegrees(Axis.YP, (float) (180.0D - yaw));

            // The supplied OBJ set spans roughly 4.5 units wide x 2.45 high.
            // A fixed source-to-player conversion keeps the exact geometry but
            // fits it to Minecraft's player scale.
            float scale = 0.42F * cfg.importedWingScale * cfg.wingsScale;
            poseStack.scale(scale, scale, scale);

            PackedMeshSubmitter.submit(
                    PackedMeshLibrary.get(PackedMeshLibrary.Pack.WINGS, "set_wings"),
                    poseStack,
                    context.submitNodeCollector(),
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    multiplyAlpha(cfg.wingsPrimaryColorArgb, cfg.wingsOpacity)
            );
            poseStack.popPose();
        }
    }

    public static void renderHaloPack(LevelRenderContext context, Vec3 camera, Minecraft client) {
        if (client.options.getCameraType().isFirstPerson()) return;
        var cfg = TopkaClient.CONFIG.get();

        AABB box = client.player.getBoundingBox();
        Vec3 center = new Vec3(
                (box.minX + box.maxX) * 0.5D,
                box.maxY + cfg.importedHaloHeight,
                (box.minZ + box.maxZ) * 0.5D
        );

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        poseStack.scale(cfg.importedHaloScale, cfg.importedHaloScale, cfg.importedHaloScale);

        PackedMeshSubmitter.submit(
                PackedMeshLibrary.get(PackedMeshLibrary.Pack.WINGS, "model_4"),
                poseStack,
                context.submitNodeCollector(),
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                cfg.importedHaloTintArgb
        );
        poseStack.popPose();
    }

    public static void renderLittleDemon(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.littleDemonShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    (box.minY + box.maxY) * 0.5D + cfg.littleDemonVerticalOffset,
                    (box.minZ + box.maxZ) * 0.5D
            );

            double yaw = player.getVisualRotationYInDegrees();
            double radians = Math.toRadians(yaw);
            Vec3 back = new Vec3(Math.sin(radians), 0.0D, -Math.cos(radians));
            center = center.add(back.scale(cfg.littleDemonBackOffset));

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            poseStack.rotateDegrees(Axis.YP, (float) (180.0D - yaw));
            poseStack.scale(cfg.littleDemonScale, cfg.littleDemonScale, cfg.littleDemonScale);

            PackedMeshSubmitter.submit(
                    PackedMeshLibrary.get(PackedMeshLibrary.Pack.LITTLE_DEMON, "full_set"),
                    poseStack,
                    context.submitNodeCollector(),
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    cfg.littleDemonTintArgb
            );
            poseStack.popPose();
        }
    }

    public static void renderArmorPack(LevelRenderContext context, Vec3 camera, Minecraft client) {
        var cfg = TopkaClient.CONFIG.get();
        PackedMeshLibrary.Pack pack = cfg.armorCosmeticStyle == 2
                ? PackedMeshLibrary.Pack.DEMONIC_ARMOR
                : PackedMeshLibrary.Pack.VALKYRIE_ARMOR;
        String modelName = cfg.armorCosmeticStyle == 2 ? "demonic" : "valkyrie";

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.armorCosmeticShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.minY + cfg.armorCosmeticVerticalOffset,
                    (box.minZ + box.maxZ) * 0.5D
            );

            double yaw = player.getVisualRotationYInDegrees();

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            poseStack.rotateDegrees(Axis.YP, (float) (180.0D - yaw));
            float scale = cfg.armorCosmeticScale;
            poseStack.scale(scale, scale, scale);

            PackedMeshSubmitter.submit(
                    PackedMeshLibrary.get(pack, modelName),
                    poseStack,
                    context.submitNodeCollector(),
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    cfg.armorCosmeticTintArgb
            );
            poseStack.popPose();
        }
    }

    private static int multiplyAlpha(int argb, int alpha) {
        int sourceAlpha = (argb >>> 24) & 0xFF;
        int combined = Math.clamp(Math.round(sourceAlpha * (alpha / 255.0F)), 0, 255);
        return (combined << 24) | (argb & 0x00FFFFFF);
    }
}
