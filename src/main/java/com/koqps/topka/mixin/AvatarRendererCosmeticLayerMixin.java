package com.koqps.topka.mixin;

import com.koqps.topka.cosmetic.CosmeticArmorLayer;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererCosmeticLayerMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void modmenu$installCosmeticArmorLayer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            boolean slim,
            CallbackInfo ci
    ) {
        @SuppressWarnings("unchecked")
        RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;

        ((LivingEntityRendererAccessor) (Object) this)
                .modmenu$addLayer(new CosmeticArmorLayer(parent));
    }
}
