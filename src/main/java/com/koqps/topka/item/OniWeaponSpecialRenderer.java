package com.koqps.topka.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class OniWeaponSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath("topka", "oni_weapon");

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath("topka", "textures/oni/oni_tex_0.png"),
            Identifier.fromNamespaceAndPath("topka", "textures/oni/oni_tex_1.png"),
            Identifier.fromNamespaceAndPath("topka", "textures/oni/oni_tex_2.png"),
            Identifier.fromNamespaceAndPath("topka", "textures/oni/oni_tex_3.png"),
            Identifier.fromNamespaceAndPath("topka", "textures/oni/oni_tex_4.png")
    };

    private final String modelId;

    public OniWeaponSpecialRenderer(String modelId) {
        this.modelId = modelId;
    }

    public static void init() {
        SpecialModelRenderers.ID_MAPPER.put(TYPE_ID, Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean foil,
            int outlineColor
    ) {
        OniWeaponMesh mesh = OniWeaponModels.get(modelId);
        if (mesh == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);

        // Source pack geometry is already normalized while preserving its exact
        // proportions. This scale keeps even the large Oni sword inside the
        // normal item-render volume while letting display transforms handle
        // first/third person positioning.
        poseStack.scale(0.70F, 0.70F, 0.70F);

        for (OniWeaponMesh.Layer layer : mesh.layers()) {
            int slot = Math.floorMod(layer.textureSlot(), TEXTURES.length);
            Identifier texture = TEXTURES[slot];

            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(texture),
                    (pose, vertices) -> {
                        for (OniWeaponMesh.Triangle triangle : layer.triangles()) {
                            emitTriangleAsQuad(vertices, pose, triangle, lightCoords, overlayCoords);
                        }
                    }
            );
        }

        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        // Conservative normalized bounds for inventory/GUI culling.
        consumer.accept(new Vector3f(-0.55F, -0.70F, -0.55F));
        consumer.accept(new Vector3f(0.55F, 0.70F, 0.55F));
    }

    private static void emitTriangleAsQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            OniWeaponMesh.Triangle triangle,
            int light,
            int overlay
    ) {
        vertex(consumer, pose, triangle.a(), light, overlay);
        vertex(consumer, pose, triangle.b(), light, overlay);
        vertex(consumer, pose, triangle.c(), light, overlay);

        // Entity render types use quad topology. Repeating the final vertex
        // preserves the source triangle without fabricating extra geometry.
        vertex(consumer, pose, triangle.c(), light, overlay);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            OniWeaponMesh.Vertex vertex,
            int light,
            int overlay
    ) {
        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(-1)
                .setUv(vertex.u(), vertex.v())
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
    }

    public record Unbaked(String modelId) implements SpecialModelRenderer.Unbaked<Void> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        com.mojang.serialization.Codec.STRING
                                .fieldOf("model_id")
                                .forGetter(Unbaked::modelId)
                ).apply(instance, Unbaked::new)
        );

        @Override
        public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new OniWeaponSpecialRenderer(modelId);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
