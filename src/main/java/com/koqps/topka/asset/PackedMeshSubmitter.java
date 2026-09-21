package com.koqps.topka.asset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class PackedMeshSubmitter {
    private PackedMeshSubmitter() { }

    public static void submit(
            PackedMeshLibrary.Model model,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            int tintArgb
    ) {
        if (model == null) return;

        for (PackedMeshLibrary.SubMesh subMesh : model.subMeshes()) {
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(subMesh.texture()),
                    (pose, consumer) -> emitTriangles(subMesh, pose, consumer, light, overlay, tintArgb)
            );
        }
    }

    private static void emitTriangles(
            PackedMeshLibrary.SubMesh mesh,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            int tintArgb
    ) {
        var vertices = mesh.vertices();
        for (int i = 0; i + 2 < vertices.size(); i += 3) {
            PackedMeshLibrary.Vertex a = vertices.get(i);
            PackedMeshLibrary.Vertex b = vertices.get(i + 1);
            PackedMeshLibrary.Vertex c = vertices.get(i + 2);
            vertex(consumer, pose, a, light, overlay, tintArgb);
            vertex(consumer, pose, b, light, overlay, tintArgb);
            vertex(consumer, pose, c, light, overlay, tintArgb);
            vertex(consumer, pose, c, light, overlay, tintArgb);
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            PackedMeshLibrary.Vertex vertex,
            int light,
            int overlay,
            int tintArgb
    ) {
        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(tintArgb)
                .setUv(vertex.u(), vertex.v())
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
    }
}
