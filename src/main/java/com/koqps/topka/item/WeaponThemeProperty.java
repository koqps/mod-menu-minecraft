package com.koqps.topka.item;

import com.koqps.topka.TopkaClient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record WeaponThemeProperty() implements SelectItemModelProperty<WeaponThemeProperty.Theme> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("topka", "weapon_theme");
    public static final Codec<Theme> VALUE_CODEC = StringRepresentable.fromEnum(Theme::values);
    public static final Type<WeaponThemeProperty, Theme> TYPE =
            Type.create(MapCodec.unit(new WeaponThemeProperty()), VALUE_CODEC);

    public static void register() {
        SelectItemModelProperties.ID_MAPPER.put(ID, TYPE);
    }

    @Override
    public Theme get(
            ItemStack stack,
            ClientLevel level,
            LivingEntity entity,
            int seed,
            ItemDisplayContext displayContext
    ) {
        if (!TopkaClient.MODULES.byId("weapon_models").enabled()) return Theme.VANILLA;
        var cfg = TopkaClient.CONFIG.get();

        if (isDiamond(stack)) return Theme.fromConfig(cfg.diamondWeaponTheme);
        if (isNetherite(stack)) return Theme.fromConfig(cfg.netheriteWeaponTheme);
        return Theme.fromConfig(cfg.utilityWeaponTheme);
    }

    @Override
    public Codec<Theme> valueCodec() {
        return VALUE_CODEC;
    }

    @Override
    public Type<? extends SelectItemModelProperty<Theme>, Theme> type() {
        return TYPE;
    }

    private static boolean isDiamond(ItemStack stack) {
        return stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.DIAMOND_PICKAXE)
                || stack.is(Items.DIAMOND_SHOVEL)
                || stack.is(Items.DIAMOND_HOE);
    }

    private static boolean isNetherite(ItemStack stack) {
        return stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.NETHERITE_AXE)
                || stack.is(Items.NETHERITE_PICKAXE)
                || stack.is(Items.NETHERITE_SHOVEL)
                || stack.is(Items.NETHERITE_HOE);
    }

    public enum Theme implements StringRepresentable {
        VANILLA("vanilla"),
        ONI("oni"),
        ENDER("ender");

        private final String serializedName;

        Theme(String serializedName) {
            this.serializedName = serializedName;
        }

        public static Theme fromConfig(int value) {
            return switch (Math.floorMod(value, 3)) {
                case 1 -> ONI;
                case 2 -> ENDER;
                default -> VANILLA;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
