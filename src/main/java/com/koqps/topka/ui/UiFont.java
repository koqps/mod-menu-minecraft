package com.koqps.topka.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * UI text intentionally uses Minecraft's normal default font.
 */
public final class UiFont {
    public static final Style STYLE = Style.EMPTY;

    private UiFont() { }

    public static Component text(String value) {
        return Component.literal(value);
    }

    public static Component trim(Font font, String value, int maxWidth) {
        FormattedText fitted = font.substrByWidth(
                FormattedText.of(value),
                Math.max(0, maxWidth)
        );
        return Component.literal(fitted.getString());
    }
}
