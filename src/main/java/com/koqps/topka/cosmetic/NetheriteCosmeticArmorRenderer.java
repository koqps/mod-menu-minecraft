package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.asset.PackedMeshLibrary;
import com.koqps.topka.asset.PackedMeshSubmitter;
import com.koqps.topka.asset.PackedTextureRegistry;
import com.koqps.topka.hud.CosmeticTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Cosmetic armor renderer built directly on Minecraft's vanilla player armor
 * models. This replaces the old floating/world-space armor shells.
 *
 * The player must actually be wearing netherite armor in a slot. The cosmetics
 * menu then selects Default / Valkyrie / Demonic for that slot.
 */
public final class NetheriteCosmeticArmorRenderer implements ArmorRenderer {
    private static final Identifier VANILLA_OUTER =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/humanoid/netherite.png");
    private static final Identifier VANILLA_LEGGINGS =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/humanoid_leggings/netherite.png");
    private static final Identifier VALENTINE_OUTER =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentines_armor_layer_1.png");
    private static final Identifier VALENTINE_LEGGINGS =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentines_armor_layer_2.png");

    private static final Identifier VALENTINE_TEX1 =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/valentine_tex1.png");
    private static final Identifier VALENTINE_TEX2 =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/valentine_tex2.png");
    private static final Identifier VALENTINE_TEX3 =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/valentine_tex3.png");
    private static final Identifier VALENTINE_TEX8 =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/valentine_tex8.png");
    private static final Identifier VALENTINE_GLASSES =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/glassese.png");
    private static final Identifier VALENTINE_CLOUD_HEART =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/cloudheart.png");
    private static final Identifier VALENTINE_HEART_NITRO =
            Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/valentine/heartnitro.png");

    private final ArmorModelSet<HumanoidModel<HumanoidRenderState>> armorModels;

    private NetheriteCosmeticArmorRenderer(EntityRendererProvider.Context context) {
        this.armorModels = ArmorModelSet.bake(
                ModelLayers.PLAYER_ARMOR,
                context.getModelSet(),
                HumanoidModel::new
        );
    }

    public static void register() {
        ArmorRenderer.register(
                NetheriteCosmeticArmorRenderer::new,
                Items.NETHERITE_HELMET,
                Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS
        );
    }

    @Override
    public void render(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            ItemStack stack,
            HumanoidRenderState humanoidRenderState,
            EquipmentSlot slot,
            int light,
            HumanoidModel<HumanoidRenderState> contextModel
    ) {
        int style = styleFor(slot);

        Identifier texture = textureFor(style, slot);
        HumanoidModel<HumanoidRenderState> armorModel = armorModels.get(slot);

        ArmorRenderer.submitTransformCopyingModel(
                contextModel,
                humanoidRenderState,
                armorModel,
                humanoidRenderState,
                false,
                submitNodeCollector,
                poseStack,
                RenderTypes.armorCutoutNoCull(texture),
                light,
                OverlayTexture.NO_OVERLAY,
                humanoidRenderState.outlineColor
        );

        // The reference Demonic set has a much stronger silhouette than a
        // texture-only reskin. Keep Minecraft's real armor rig, then attach
        // horns/spikes/gems directly to the animated player bones.
        if (style == 2) {
            submitDemonicDetails(slot, contextModel, poseStack, submitNodeCollector, light);
        } else if (style == 3) {
            submitValentineDetails(slot, contextModel, poseStack, submitNodeCollector, light);
        }
    }

    private static void submitValentineDetails(
            EquipmentSlot slot,
            HumanoidModel<HumanoidRenderState> model,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        if (slot == EquipmentSlot.HEAD) {
            submitValentineHelmet(model.head, poseStack, collector, light);
        } else if (slot == EquipmentSlot.CHEST) {
            // The pack's actual armor_layer_1/2 files are static. Its real
            // animated assets live in separate cosmetic textures, so add one
            // of those source animations back as the chest's luminous heart.
            submitAnimatedHeartPanel(model.body, poseStack, collector);
        }
    }

    private static void submitValentineHelmet(
            ModelPart head,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        PackedMeshLibrary.Model mesh =
                PackedMeshLibrary.get(PackedMeshLibrary.Pack.VALENTINE_HELMET, "helmet");
        if (mesh == null) return;

        poseStack.pushPose();
        head.translateAndRotate(poseStack);

        // The source helmet is authored as an oversized head cosmetic. Fit it
        // more tightly around Minecraft's 8x8 head while keeping all of the
        // original high-detail geometry and side ornaments.
        poseStack.scale(0.78F, 0.78F, 0.78F);
        poseStack.translate(0.0F, 0.035F, 0.015F);

        for (PackedMeshLibrary.SubMesh subMesh : mesh.subMeshes()) {
            Identifier texture = subMesh.texture();
            int frames = 1;
            int frame = 0;

            if (texture.equals(VALENTINE_GLASSES)) {
                frames = 32;
                frame = pingPongFrame(32, 100L);
            } else if (texture.equals(VALENTINE_CLOUD_HEART)) {
                frames = 26;
                frame = loopFrame(26, 100L);
            }

            final int frameIndex = frame;
            final int frameCount = frames;
            final boolean visor = texture.equals(VALENTINE_GLASSES);

            if (visor) {
                // The two visor plates sit almost coplanar with the helmet
                // front in the source model. Move only this animated submesh a
                // little toward the camera so it cannot disappear into the
                // face shell.
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, -0.045F);
            }

            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(texture),
                    (pose, vertices) -> emitAnimatedMesh(
                            subMesh,
                            pose,
                            vertices,
                            visor ? LightCoordsUtil.FULL_BRIGHT : light,
                            frameIndex,
                            frameCount
                    )
            );

            if (visor) {
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void emitAnimatedMesh(
            PackedMeshLibrary.SubMesh mesh,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int frame,
            int frames
    ) {
        var vertices = mesh.vertices();
        for (int i = 0; i + 2 < vertices.size(); i += 3) {
            animatedVertex(consumer, pose, vertices.get(i), light, frame, frames);
            animatedVertex(consumer, pose, vertices.get(i + 1), light, frame, frames);
            animatedVertex(consumer, pose, vertices.get(i + 2), light, frame, frames);
            animatedVertex(consumer, pose, vertices.get(i + 2), light, frame, frames);
        }
    }

    private static void animatedVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            PackedMeshLibrary.Vertex vertex,
            int light,
            int frame,
            int frames
    ) {
        float v = frames <= 1
                ? vertex.v()
                : (frame + vertex.v()) / frames;

        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(0xFFFFFFFF)
                .setUv(vertex.u(), v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
    }

    private static void submitAnimatedHeartPanel(
            ModelPart body,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        final int frames = 18;
        final int frame = loopFrame(frames, 100L);
        final float v0 = frame / (float) frames;
        final float v1 = (frame + 1) / (float) frames;

        poseStack.pushPose();
        body.translateAndRotate(poseStack);

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucent(VALENTINE_HEART_NITRO),
                (pose, vertices) -> {
                    float z = -0.218F;
                    float x0 = -0.105F;
                    float x1 = 0.105F;
                    float y0 = 0.18F;
                    float y1 = 0.39F;

                    panelVertex(vertices, pose, x0, y1, z, 0.0F, v1);
                    panelVertex(vertices, pose, x1, y1, z, 1.0F, v1);
                    panelVertex(vertices, pose, x1, y0, z, 1.0F, v0);
                    panelVertex(vertices, pose, x0, y0, z, 0.0F, v0);
                }
        );

        poseStack.popPose();
    }

    private static void panelVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    private static int loopFrame(int frames, long frameMillis) {
        long step = Math.max(0L, System.currentTimeMillis() / Math.max(1L, frameMillis));
        return (int) (step % Math.max(1, frames));
    }

    private static int pingPongFrame(int frames, long frameMillis) {
        if (frames <= 1) return 0;
        int period = frames * 2 - 2;
        int step = (int) ((System.currentTimeMillis() / Math.max(1L, frameMillis)) % period);
        return step < frames ? step : period - step;
    }

    private static void renderPaladin(
            EquipmentSlot slot,
            HumanoidModel<HumanoidRenderState> model,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        switch (slot) {
            case HEAD -> submitPaladinPart("helmet", model.head, poseStack, collector, light);
            case CHEST -> {
                submitPaladinPart("body", model.body, poseStack, collector, light);
                submitPaladinPart("right_arm", model.rightArm, poseStack, collector, light);
                submitPaladinPart("left_arm", model.leftArm, poseStack, collector, light);
            }
            case LEGS -> {
                // Hip/waist geometry follows the torso pivot while each leg
                // follows Minecraft's normal animated leg bone.
                submitPaladinPart("waist", model.body, poseStack, collector, light);
                submitPaladinPart("right_leg", model.rightLeg, poseStack, collector, light);
                submitPaladinPart("left_leg", model.leftLeg, poseStack, collector, light);
            }
            case FEET -> {
                submitPaladinPart("right_boot", model.rightLeg, poseStack, collector, light);
                submitPaladinPart("left_boot", model.leftLeg, poseStack, collector, light);
            }
            default -> { }
        }
    }

    private static void submitPaladinPart(
            String name,
            ModelPart part,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        PackedMeshLibrary.Model mesh = PackedMeshLibrary.get(PackedMeshLibrary.Pack.PALADIN_ARMOR, name);
        if (mesh == null) return;

        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        PackedMeshSubmitter.submit(
                mesh,
                poseStack,
                collector,
                light,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
        poseStack.popPose();
    }

    private static void submitDemonicDetails(
            EquipmentSlot slot,
            HumanoidModel<HumanoidRenderState> model,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light
    ) {
        final int red = 0xFF6F2023;
        final int redDark = 0xFF3D1115;
        final int copper = 0xFFD97845;
        final int steel = 0xFF2B2C33;

        switch (slot) {
            case HEAD -> {
                // Segmented side horns + top copper plate.
                boxOnPart(model.head, poseStack, collector, light, red,
                        -0.28F, -0.66F, -0.08F, -0.18F, -0.34F, 0.08F);
                boxOnPart(model.head, poseStack, collector, light, redDark,
                        -0.34F, -0.78F, -0.04F, -0.25F, -0.58F, 0.06F);
                boxOnPart(model.head, poseStack, collector, light, red,
                         0.18F, -0.66F, -0.08F,  0.28F, -0.34F, 0.08F);
                boxOnPart(model.head, poseStack, collector, light, redDark,
                         0.25F, -0.78F, -0.04F,  0.34F, -0.58F, 0.06F);
                boxOnPart(model.head, poseStack, collector, light, copper,
                        -0.08F, -0.56F, -0.30F, 0.08F, -0.42F, -0.23F);
            }
            case CHEST -> {
                // Layered chest ornament + shoulder spikes.
                boxOnPart(model.body, poseStack, collector, light, steel,
                        -0.19F, 0.15F, -0.16F, 0.19F, 0.42F, -0.10F);
                boxOnPart(model.body, poseStack, collector, light, copper,
                        -0.06F, 0.25F, -0.205F, 0.06F, 0.37F, -0.145F);

                boxOnPart(model.leftArm, poseStack, collector, light, red,
                        -0.28F, -0.13F, -0.11F, -0.10F, 0.08F, 0.11F);
                boxOnPart(model.leftArm, poseStack, collector, light, redDark,
                        -0.40F, -0.10F, -0.07F, -0.25F, 0.04F, 0.07F);

                boxOnPart(model.rightArm, poseStack, collector, light, red,
                         0.10F, -0.13F, -0.11F, 0.28F, 0.08F, 0.11F);
                boxOnPart(model.rightArm, poseStack, collector, light, redDark,
                         0.25F, -0.10F, -0.07F, 0.40F, 0.04F, 0.07F);

                // Heavy gauntlet blocks with red side fins.
                boxOnPart(model.leftArm, poseStack, collector, light, steel,
                        -0.17F, 0.34F, -0.18F, 0.17F, 0.66F, 0.18F);
                boxOnPart(model.leftArm, poseStack, collector, light, red,
                        -0.27F, 0.40F, -0.10F, -0.14F, 0.61F, 0.10F);

                boxOnPart(model.rightArm, poseStack, collector, light, steel,
                        -0.17F, 0.34F, -0.18F, 0.17F, 0.66F, 0.18F);
                boxOnPart(model.rightArm, poseStack, collector, light, red,
                         0.14F, 0.40F, -0.10F, 0.27F, 0.61F, 0.10F);
            }
            case LEGS -> {
                // Raised knee housings + copper gems.
                boxOnPart(model.leftLeg, poseStack, collector, light, steel,
                        -0.16F, 0.30F, -0.18F, 0.16F, 0.53F, 0.14F);
                boxOnPart(model.leftLeg, poseStack, collector, light, copper,
                        -0.07F, 0.34F, -0.225F, 0.07F, 0.46F, -0.165F);

                boxOnPart(model.rightLeg, poseStack, collector, light, steel,
                        -0.16F, 0.30F, -0.18F, 0.16F, 0.53F, 0.14F);
                boxOnPart(model.rightLeg, poseStack, collector, light, copper,
                        -0.07F, 0.34F, -0.225F, 0.07F, 0.46F, -0.165F);
            }
            case FEET -> {
                // Dark shin covers with the red lower rim visible in the ref.
                boxOnPart(model.leftLeg, poseStack, collector, light, redDark,
                        -0.18F, 0.60F, -0.19F, 0.18F, 0.76F, 0.19F);
                boxOnPart(model.leftLeg, poseStack, collector, light, red,
                        -0.19F, 0.70F, -0.20F, 0.19F, 0.78F, 0.20F);

                boxOnPart(model.rightLeg, poseStack, collector, light, redDark,
                        -0.18F, 0.60F, -0.19F, 0.18F, 0.76F, 0.19F);
                boxOnPart(model.rightLeg, poseStack, collector, light, red,
                        -0.19F, 0.70F, -0.20F, 0.19F, 0.78F, 0.20F);
            }
            default -> { }
        }
    }

    private static void boxOnPart(
            ModelPart part,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int color,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucent(PackedTextureRegistry.PLAIN_WHITE),
                (pose, vertices) -> emitBox(
                        pose, vertices, light, color,
                        minX, minY, minZ, maxX, maxY, maxZ
                )
        );

        poseStack.popPose();
    }

    private static void emitBox(
            PoseStack.Pose pose,
            VertexConsumer v,
            int light,
            int color,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        face(pose, v, light, color,
                minX,minY,minZ, maxX,minY,minZ, maxX,maxY,minZ, minX,maxY,minZ,
                0F,0F,-1F);
        face(pose, v, light, color,
                maxX,minY,maxZ, minX,minY,maxZ, minX,maxY,maxZ, maxX,maxY,maxZ,
                0F,0F,1F);
        face(pose, v, light, color,
                minX,minY,maxZ, minX,minY,minZ, minX,maxY,minZ, minX,maxY,maxZ,
                -1F,0F,0F);
        face(pose, v, light, color,
                maxX,minY,minZ, maxX,minY,maxZ, maxX,maxY,maxZ, maxX,maxY,minZ,
                1F,0F,0F);
        face(pose, v, light, color,
                minX,minY,maxZ, maxX,minY,maxZ, maxX,minY,minZ, minX,minY,minZ,
                0F,-1F,0F);
        face(pose, v, light, color,
                minX,maxY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, minX,maxY,maxZ,
                0F,1F,0F);
    }

    private static void face(
            PoseStack.Pose pose,
            VertexConsumer v,
            int light,
            int color,
            float ax,float ay,float az,
            float bx,float by,float bz,
            float cx,float cy,float cz,
            float dx,float dy,float dz,
            float nx,float ny,float nz
    ) {
        detailVertex(v, pose, ax,ay,az, color, 0F,0F, light, nx,ny,nz);
        detailVertex(v, pose, bx,by,bz, color, 1F,0F, light, nx,ny,nz);
        detailVertex(v, pose, cx,cy,cz, color, 1F,1F, light, nx,ny,nz);
        detailVertex(v, pose, dx,dy,dz, color, 0F,1F, light, nx,ny,nz);
    }

    private static void detailVertex(
            VertexConsumer v,
            PoseStack.Pose pose,
            float x,float y,float z,
            int color,
            float u,float texV,
            int light,
            float nx,float ny,float nz
    ) {
        v.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, texV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static int styleFor(EquipmentSlot slot) {
        if (!TopkaClient.MODULES.byId("armor_cosmetic").enabled()) return 0;

        var cfg = TopkaClient.CONFIG.get();
        return switch (slot) {
            case HEAD -> cfg.armorHelmetStyle;
            case CHEST -> cfg.armorChestStyle;
            case LEGS -> cfg.armorLeggingsStyle;
            case FEET -> cfg.armorBootsStyle;
            default -> 0;
        };
    }

    private static Identifier textureFor(int style, EquipmentSlot slot) {
        boolean leggings = slot == EquipmentSlot.LEGS;

        return switch (style) {
            case 1 -> leggings
                    ? CosmeticTextures.VALKYRIE_LEGGINGS
                    : CosmeticTextures.VALKYRIE_ARMOR;
            case 2 -> leggings
                    ? CosmeticTextures.DEMONIC_LEGGINGS
                    : CosmeticTextures.DEMONIC_ARMOR;
            case 3 -> leggings ? VALENTINE_LEGGINGS : VALENTINE_OUTER;
            default -> leggings ? VANILLA_LEGGINGS : VANILLA_OUTER;
        };
    }
}
