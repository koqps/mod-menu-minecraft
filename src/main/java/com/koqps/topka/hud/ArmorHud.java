package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TopkaClient.MOD_ID, "armor_hud");
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorHud() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, ArmorHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!TopkaClient.MODULES.byId("armor_display").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;

        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.armorHudX;
        int y = cfg.armorHudY;
        int w = 170;
        int h = 72;
        float scale = Math.clamp(cfg.armorHudScale, 0.55F, 2.0F);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (cfg.armorHudBackground) {
            graphics.fill(0, 0, w, h, cfg.hudBackgroundArgb);
            graphics.fill(0, 0, 3, h, Theme.accent());
        }
        graphics.text(client.font, "ARMOR", 10, 6, cfg.mutedTextArgb, false);

        int slotX = 10;
        boolean any = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                graphics.fill(slotX, 22, slotX + 18, 40, 0x5533333B);
                graphics.fill(slotX + 2, 24, slotX + 16, 38, 0x33202026);
                slotX += 38;
                continue;
            }

            any = true;
            graphics.item(stack, slotX + 1, 21);
            graphics.itemDecorations(client.font, stack, slotX + 1, 21);

            int pct = 100;
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                pct = Math.clamp(
                        Math.round((stack.getMaxDamage() - stack.getDamageValue()) * 100F / stack.getMaxDamage()),
                        0,
                        100
                );
            }
            int durabilityColor = pct > 60 ? 0xFF58E38C : pct > 30 ? 0xFFFFD166 : 0xFFFF5C77;
            graphics.text(client.font, pct + "%", slotX - 1, 45, durabilityColor, true);
            slotX += 38;
        }

        if (!any) {
            graphics.text(client.font, "No armor equipped", 10, 48, 0xFF707080, false);
        }

        graphics.pose().popMatrix();
    }
}
