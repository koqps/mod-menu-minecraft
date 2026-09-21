package com.koqps.topka.item;

import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class WeaponMeshSpecialRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath("topka", "weapon_mesh");

    private static final Identifier VAL_BATTERY =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/battery.png");
    private static final Identifier VAL_BOWFILL =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/bowfill.png");
    private static final Identifier VAL_CAMERA =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/cameralens1.png");
    private static final Identifier VAL_CLOUD_HEART =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/cloudheart.png");
    private static final Identifier VAL_GLASSES =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/glassese.png");
    private static final Identifier VAL_HEART_NITRO =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/heartnitro.png");
    private static final Identifier VAL_HEART_ROTATE =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/heartrotate.png");
    private static final Identifier VAL_HEART_ROTATE_1 =
            Identifier.fromNamespaceAndPath("topka", "textures/valentine_items/heartrotate1.png");

    private final PackedMeshLibrary.Pack pack;
    private final String model;
    private final float scale;
    private final float x;
    private final float y;
    private final float z;
    private final float rx;
    private final float ry;
    private final float rz;

    public WeaponMeshSpecialRenderer(
            PackedMeshLibrary.Pack pack,
            String model,
            float scale,
            float x,
            float y,
            float z,
            float rx,
            float ry,
            float rz
    ) {
        this.pack = pack;
        this.model = model;
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    public static void register() {
        SpecialModelRenderers.ID_MAPPER.put(TYPE_ID, Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outlineColor
    ) {
        PackedMeshLibrary.Model mesh = PackedMeshLibrary.get(pack, model);
        if (mesh == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5D + x, 0.5D + y, 0.5D + z);
        poseStack.rotateDegrees(Axis.XP, rx);
        poseStack.rotateDegrees(Axis.YP, ry);
        poseStack.rotateDegrees(Axis.ZP, rz);
        poseStack.scale(scale, scale, scale);

        if (pack == PackedMeshLibrary.Pack.VALENTINE_ITEMS) {
            submitValentine(mesh, poseStack, collector, light, overlay);
        } else {
            PackedMeshSubmitter.submit(mesh, poseStack, collector, light, overlay, 0xFFFFFFFF);
        }

        poseStack.popPose();
    }

    private static void submitValentine(
            PackedMeshLibrary.Model model,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int overlay
    ) {
        for (PackedMeshLibrary.SubMesh subMesh : model.subMeshes()) {
            AnimationFrame animation = animationFor(subMesh.texture());
            int renderLight = animation.animated() ? LightCoordsUtil.FULL_BRIGHT : light;

            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(subMesh.texture()),
                    (pose, consumer) -> emitValentineTriangles(
                            subMesh, pose, consumer, renderLight, overlay, animation
                    )
            );
        }
    }

    private static void emitValentineTriangles(
            PackedMeshLibrary.SubMesh mesh,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            AnimationFrame animation
    ) {
        var vertices = mesh.vertices();
        for (int i = 0; i + 2 < vertices.size(); i += 3) {
            valentineVertex(consumer, pose, vertices.get(i), light, overlay, animation);
            valentineVertex(consumer, pose, vertices.get(i + 1), light, overlay, animation);
            valentineVertex(consumer, pose, vertices.get(i + 2), light, overlay, animation);
            valentineVertex(consumer, pose, vertices.get(i + 2), light, overlay, animation);
        }
    }

    private static void valentineVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            PackedMeshLibrary.Vertex vertex,
            int light,
            int overlay,
            AnimationFrame animation
    ) {
        float v = animation.frames() <= 1
                ? vertex.v()
                : (animation.frame() + vertex.v()) / animation.frames();

        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(0xFFFFFFFF)
                .setUv(vertex.u(), v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
    }

    private static AnimationFrame animationFor(Identifier texture) {
        long now = System.currentTimeMillis();

        if (texture.equals(VAL_BATTERY)) {
            return new AnimationFrame(pingPongFrame(14, 100L, now), 14);
        }
        if (texture.equals(VAL_BOWFILL)) {
            return new AnimationFrame(pingPongFrame(32, 100L, now), 32);
        }
        if (texture.equals(VAL_CAMERA)) {
            return new AnimationFrame(pingPongFrame(26, 100L, now), 26);
        }
        if (texture.equals(VAL_CLOUD_HEART)) {
            return new AnimationFrame(loopFrame(26, 100L, now), 26);
        }
        if (texture.equals(VAL_GLASSES)) {
            return new AnimationFrame(pingPongFrame(32, 100L, now), 32);
        }
        if (texture.equals(VAL_HEART_NITRO)) {
            return new AnimationFrame(loopFrame(18, 100L, now), 18);
        }
        if (texture.equals(VAL_HEART_ROTATE)) {
            return new AnimationFrame(loopFrame(24, 100L, now), 24);
        }
        if (texture.equals(VAL_HEART_ROTATE_1)) {
            return new AnimationFrame(loopFrame(24, 50L, now), 24);
        }

        return new AnimationFrame(0, 1);
    }

    private static int loopFrame(int frames, long frameMillis, long now) {
        return (int) ((now / Math.max(1L, frameMillis)) % Math.max(1, frames));
    }

    private static int pingPongFrame(int frames, long frameMillis, long now) {
        if (frames <= 1) return 0;
        int period = frames * 2 - 2;
        int step = (int) ((now / Math.max(1L, frameMillis)) % period);
        return step < frames ? step : period - step;
    }

    private record AnimationFrame(int frame, int frames) {
        boolean animated() {
            return frames > 1;
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float r = Math.max(0.6F, Math.abs(scale) * 1.25F);
        output.accept(new Vector3f(-r, -r, -r));
        output.accept(new Vector3f(r, r, r));
    }

    public record Unbaked(
            String pack,
            String model,
            float scale,
            float x,
            float y,
            float z,
            float rx,
            float ry,
            float rz
    ) implements SpecialModelRenderer.Unbaked<Void> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("pack").forGetter(Unbaked::pack),
                        Codec.STRING.fieldOf("model").forGetter(Unbaked::model),
                        Codec.FLOAT.optionalFieldOf("scale", 0.72F).forGetter(Unbaked::scale),
                        Codec.FLOAT.optionalFieldOf("x", 0.0F).forGetter(Unbaked::x),
                        Codec.FLOAT.optionalFieldOf("y", 0.0F).forGetter(Unbaked::y),
                        Codec.FLOAT.optionalFieldOf("z", 0.0F).forGetter(Unbaked::z),
                        Codec.FLOAT.optionalFieldOf("rx", 0.0F).forGetter(Unbaked::rx),
                        Codec.FLOAT.optionalFieldOf("ry", 0.0F).forGetter(Unbaked::ry),
                        Codec.FLOAT.optionalFieldOf("rz", 0.0F).forGetter(Unbaked::rz)
                ).apply(instance, Unbaked::new)
        );

        @Override
        public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            PackedMeshLibrary.Pack selected = switch (pack.toLowerCase()) {
                case "ender" -> PackedMeshLibrary.Pack.ENDER;
                case "valentine" -> PackedMeshLibrary.Pack.VALENTINE_ITEMS;
                default -> PackedMeshLibrary.Pack.ONI;
            };
            return new WeaponMeshSpecialRenderer(selected, model, scale, x, y, z, rx, ry, rz);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
