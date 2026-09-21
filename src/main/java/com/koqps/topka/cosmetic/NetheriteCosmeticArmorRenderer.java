package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
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
        }
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
            default -> leggings ? VANILLA_LEGGINGS : VANILLA_OUTER;
        };
    }
}
