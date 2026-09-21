package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
        PackedMeshLibrary.Model model = PackedMeshLibrary.get(PackedMeshLibrary.Pack.WINGS, "set_wings");
        if (model == null) return;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player.isRemoved()) continue;
            if (player != client.player && !cfg.wingsShowOthers) continue;
            if (player == client.player && client.options.getCameraType().isFirstPerson()) continue;
            if (client.player.distanceToSqr(player) > 4096.0D) continue;

            AABB box = player.getBoundingBox();
            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());

            // Explicit player-space basis. This avoids the old issue where the
            // broad XY wing plane could end up nearly edge-on depending on yaw.
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 back = new Vec3(Math.sin(yaw), 0.0D, -Math.cos(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

            Vec3 origin = new Vec3(
                    (box.minX + box.maxX) * 0.5D,
                    box.minY + (player.isCrouching() ? 1.18D : 1.30D) + cfg.importedWingVerticalOffset,
                    (box.minZ + box.maxZ) * 0.5D
            );

            float sourceScale = 0.58F * cfg.importedWingScale * cfg.wingsScale;
            double backOffset = 0.13D + cfg.importedWingBackOffset;
            int tint = multiplyAlpha(cfg.wingsPrimaryColorArgb, cfg.wingsOpacity);

            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            for (PackedMeshLibrary.SubMesh subMesh : model.subMeshes()) {
                context.submitNodeCollector().submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucent(subMesh.texture()),
                        (pose, vertices) -> emitWingTriangles(
                                pose, vertices, subMesh,
                                origin, right, up, back,
                                sourceScale, backOffset, tint
                        )
                );
            }

            poseStack.popPose();
        }
    }

    private static void emitWingTriangles(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            PackedMeshLibrary.SubMesh mesh,
            Vec3 origin,
            Vec3 right,
            Vec3 up,
            Vec3 back,
            float scale,
            double backOffset,
            int color
    ) {
        var source = mesh.vertices();

        for (int i = 0; i + 2 < source.size(); i += 3) {
            PackedMeshLibrary.Vertex a = source.get(i);
            PackedMeshLibrary.Vertex b = source.get(i + 1);
            PackedMeshLibrary.Vertex c = source.get(i + 2);

            wingVertex(pose, vertices, a, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, b, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, c, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, c, origin, right, up, back, scale, backOffset, color);

            // Render the reverse winding as well. The uploaded OBJ contains
            // several thin plates, and this keeps the complete set visible
            // from both third-person front and third-person back cameras.
            wingVertex(pose, vertices, c, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, b, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, a, origin, right, up, back, scale, backOffset, color);
            wingVertex(pose, vertices, a, origin, right, up, back, scale, backOffset, color);
        }
    }

    private static void wingVertex(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            PackedMeshLibrary.Vertex source,
            Vec3 origin,
            Vec3 right,
            Vec3 up,
            Vec3 back,
            float scale,
            double backOffset,
            int color
    ) {
        // Source bounds are approximately:
        // X -2.25..2.24, Y 0..2.45, Z 0.32..0.84.
        // Re-center Y/Z around the supplied complete set before mapping it to
        // the player's animated body basis.
        double lx = source.x() * scale;
        double ly = (source.y() - 1.20D) * scale;
        double lz = (source.z() - 0.58D) * scale + backOffset;

        Vec3 p = origin
                .add(right.scale(lx))
                .add(up.scale(ly))
                .add(back.scale(lz));

        Vec3 normal = right.scale(source.nx())
                .add(up.scale(source.ny()))
                .add(back.scale(source.nz()));
        if (normal.lengthSqr() > 1.0E-8D) normal = normal.normalize();

        vertices.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(color)
                .setUv(source.u(), source.v())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
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
