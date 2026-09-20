package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

public final class ArmorHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(TopkaClient.MOD_ID, "armor_hud");
    private ArmorHud() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, ArmorHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!TopkaClient.MODULES.byId("armor_display").enabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;
        var cfg = TopkaClient.CONFIG.get();
        int x = cfg.armorHudX, y = cfg.armorHudY;
        int w = 164, h = 58;
        graphics.fill(x, y, x + w, y + h, 0xD9121219);
        graphics.fill(x, y, x + 3, y + h, Theme.accent());
        graphics.text(client.font, "ARMOR", x + 10, y + 6, 0xFF9C9CAE, false);

        int lineY = y + 19;
        int count = 0;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            if (name.length() > 16) name = name.substring(0, 16);
            String durability = "";
            if (stack.isDamageableItem()) {
                int left = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                int pct = stack.getMaxDamage() <= 0 ? 100 : Math.round(left * 100F / stack.getMaxDamage());
                durability = "  " + pct + "%";
            }
            graphics.text(client.font, name + durability, x + 10, lineY, 0xFFF0F0F5, false);
            lineY += 10;
            if (++count >= 4) break;
        }
        if (count == 0) graphics.text(client.font, "No armor equipped", x + 10, y + 23, 0xFF707080, false);
    }
}
