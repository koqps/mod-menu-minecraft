package com.koqps.topka.cosmetic;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.hud.CosmeticTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
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
                humanoidRenderState.outlineColor,
                null
        );
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
