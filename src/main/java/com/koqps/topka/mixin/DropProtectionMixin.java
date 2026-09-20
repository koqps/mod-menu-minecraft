package com.koqps.topka.mixin;

import com.koqps.topka.TopkaClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(MultiPlayerGameMode.class)
public final class DropProtectionMixin {
    @Unique private long modmenu$confirmationExpiresAt;
    @Unique private String modmenu$confirmationItem = "";

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void modmenu$protectDrop(LocalPlayer player, boolean entireStack, CallbackInfo ci) {
        if (!TopkaClient.MODULES.byId("drop_protection").enabled()) return;

        ItemStack stack = player.getInventory().getSelectedItem();
        if (stack.isEmpty() || !modmenu$isProtected(stack)) return;

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        long now = System.currentTimeMillis();
        boolean confirmed = itemId.equals(modmenu$confirmationItem) && now <= modmenu$confirmationExpiresAt;

        if (confirmed) {
            modmenu$confirmationExpiresAt = 0L;
            modmenu$confirmationItem = "";
            return;
        }

        modmenu$confirmationItem = itemId;
        modmenu$confirmationExpiresAt = now + Math.max(700L, TopkaClient.CONFIG.get().dropProtectionWindowMs);
        player.sendOverlayMessage(Component.literal("Protected item — press Drop again to confirm"));
        ci.cancel();
    }

    @Unique
    private static boolean modmenu$isProtected(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);

        if (TopkaClient.CONFIG.get().protectTotems && path.contains("totem_of_undying")) return true;
        if (TopkaClient.CONFIG.get().protectArmor && (
                path.endsWith("_helmet") || path.endsWith("_chestplate")
                        || path.endsWith("_leggings") || path.endsWith("_boots")
                        || path.equals("elytra"))) return true;

        if (TopkaClient.CONFIG.get().protectTools && (
                path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
                        || path.endsWith("_shovel") || path.endsWith("_hoe")
                        || path.equals("bow") || path.equals("crossbow")
                        || path.equals("trident") || path.equals("mace"))) return true;

        return TopkaClient.CONFIG.get().protectNamedItems && stack.has(DataComponents.CUSTOM_NAME);
    }
}
