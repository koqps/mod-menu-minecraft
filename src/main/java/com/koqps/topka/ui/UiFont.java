package com.koqps.topka.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Dedicated Mod Menu UI font style. It points at the mod's own font resource
 * instead of the active minecraft:default font, so texture packs that replace
 * the default UI font do not automatically restyle this client.
 */
public final class UiFont {
    public static final Style STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("topka", "ui")));

    private UiFont() { }

    public static Component text(String value) {
        return Component.literal(value).setStyle(STYLE);
    }

    public static Component trim(Font font, String value, int maxWidth) {
        FormattedText fitted = font.substrByWidth(
                FormattedText.of(value, STYLE),
                Math.max(0, maxWidth)
        );
        return text(fitted.getString());
    }
}
