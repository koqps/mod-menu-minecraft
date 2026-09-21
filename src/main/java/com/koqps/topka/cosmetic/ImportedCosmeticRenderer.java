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
import net.minecraft.util.Mth;
import net.minecraft.Util;
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

            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double renderX = Mth.lerp(partialTick, player.xo, player.getX());
            double renderY = Mth.lerp(partialTick, player.yo, player.getY());
            double renderZ = Mth.lerp(partialTick, player.zo, player.getZ());
            double yaw = Math.toRadians(player.getVisualRotationYInDegrees());

            // Player-local basis. Position is interpolated with the same frame
            // partial tick used by Minecraft entity rendering, so the cosmetic
            // stays glued to the body instead of trailing one game tick behind.
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3 back = new Vec3(Math.sin(yaw), 0.0D, -Math.cos(yaw));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

            Vec3 origin = new Vec3(
                    renderX,
                    renderY + (player.isCrouching() ? 1.18D : 1.30D) + cfg.importedWingVerticalOffset,
                    renderZ
            );

            float sourceScale = 0.50F * cfg.importedWingScale * cfg.wingsScale;
            // Keep only a small body clearance. User-configured back offset is
            // intentionally damped so old configs cannot push the rig far away.
            double backOffset = 0.075D + cfg.importedWingBackOffset * 0.20D;
            float flap = (float) Math.sin(Util.getMillis() * 0.001D * Math.max(0.05F, cfg.wingsFlapSpeed) * 3.0D)
                    * Math.clamp(cfg.wingsFlapAmount, 0.0F, 1.0F) * 0.075F;
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
                                sourceScale, backOffset, flap, tint
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
            float flap,
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
            float flap,
            int color
    ) {
        double lx = source.x() * scale;
        double ly = (source.y() - 1.20D) * scale;
        double lz = (source.z() - 0.58D) * scale + backOffset;

        // Mechanical in-plane flex around the two center mounts. This changes
        // shape without translating the complete rig away from the player.
        double side = Math.signum(lx);
        if (side != 0.0D) {
            double pivotX = side * 0.18D;
            double pivotY = 0.08D;
            double angle = -side * flap;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double dx = lx - pivotX;
            double dy = ly - pivotY;
            lx = pivotX + dx * cos - dy * sin;
            ly = pivotY + dx * sin + dy * cos;
        }

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
