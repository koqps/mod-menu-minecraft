package com.koqps.topka.item;

import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class WeaponMeshSpecialRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath("topka", "weapon_mesh");

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

        PackedMeshSubmitter.submit(mesh, poseStack, collector, light, overlay, 0xFFFFFFFF);

        poseStack.popPose();
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
