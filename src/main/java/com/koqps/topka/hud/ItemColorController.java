package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import com.koqps.topka.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemColorController {
    private ItemColorController() { }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenEvents.afterForeground(screen).register((current, graphics, mouseX, mouseY, tickProgress) -> {
                if (!TopkaClient.MODULES.byId("item_color").enabled()) return;

                ContainerScreenAccessor accessor = (ContainerScreenAccessor) container;
                int left = accessor.modmenu$getLeftPos();
                int top = accessor.modmenu$getTopPos();
                int color = TopkaClient.CONFIG.get().itemHighlightColorArgb;

                for (Slot slot : container.getMenu().slots) {
                    ItemStack stack = slot.getItem();
                    if (stack.isEmpty()) continue;
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (!TopkaClient.CONFIG.get().highlightedItems.contains(id)) continue;

                    int x = left + slot.x - 1;
                    int y = top + slot.y - 1;
                    graphics.fill(x, y, x + 18, y + 2, color);
                    graphics.fill(x, y + 16, x + 18, y + 18, color);
                    graphics.fill(x, y, x + 2, y + 18, color);
                    graphics.fill(x + 16, y, x + 18, y + 18, color);
                }
            });
        });
    }
}
