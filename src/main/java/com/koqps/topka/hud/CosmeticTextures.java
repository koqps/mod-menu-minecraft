package com.koqps.topka.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Runtime-generated cosmetic texture atlas.
 *
 * Keeping this texture inside the mod avoids inheriting resource-pack textures
 * and gives the wing renderer proper feather/membrane/crystal surface detail
 * instead of debug-style flat colored polygons.
 */
public final class CosmeticTextures {
    public static final Identifier WINGS = Identifier.fromNamespaceAndPath("topka", "dynamic/wings_atlas");
    public static final Identifier ARMOR = Identifier.fromNamespaceAndPath("topka", "dynamic/armor_atlas");
    public static final Identifier ANGEL_BASE = Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/angel_wings.png");

    private static DynamicTexture wingsTexture;
    private static DynamicTexture armorTexture;
    private static boolean registered;

    private CosmeticTextures() { }

    public static void register(Minecraft client) {
        if (registered) return;

        NativeImage image = new NativeImage(128, 128, false);
        paintAtlas(image);

        wingsTexture = new DynamicTexture(() -> "Mod Menu wing atlas", image);
        client.getTextureManager().register(WINGS, wingsTexture);

        NativeImage armor = new NativeImage(128, 64, false);
        paintArmorAtlas(armor);
        armorTexture = new DynamicTexture(() -> "Mod Menu armor atlas", armor);
        client.getTextureManager().register(ARMOR, armorTexture);

        registered = true;
    }

    public static void close() {
        if (wingsTexture != null) {
            wingsTexture.close();
            wingsTexture = null;
        }
        if (armorTexture != null) {
            armorTexture.close();
            armorTexture = null;
        }
        registered = false;
    }

    private static void paintAtlas(NativeImage image) {
        clear(image);

        // Tile 0: Angel feathers
        fillTile(image, 0, 0, 0xFFEDEFF7, 0xFFC6CBD8, 225);
        for (int i = 0; i < 7; i++) {
            int y = 5 + i * 8;
            drawLine(image, 4, y, 59, y + 11, 0xD9FFFFFF, 2);
            drawLine(image, 8, y + 3, 55, y + 13, 0x707A8296, 1);
        }
        drawLine(image, 7, 58, 58, 7, 0xA8FFFFFF, 2);

        // Tile 1: Demon membrane with dark structural veins.
        fillTile(image, 64, 0, 0xFFE0E1E7, 0xFF979AA7, 220);
        int cx = 67, cy = 59;
        int[][] tips = {{124, 6}, {124, 21}, {124, 39}, {119, 59}};
        for (int[] tip : tips) {
            drawLine(image, cx, cy, tip[0], tip[1], 0xC94A4D59, 2);
        }
        drawLine(image, 68, 58, 124, 58, 0xA85C606C, 2);

        // Tile 2: Crystal facets.
        fillTile(image, 0, 64, 0xFFF3F5FF, 0xFFADB6D0, 232);
        drawLine(image, 0, 64, 32, 96, 0xBFFFFFFF, 2);
        drawLine(image, 64, 64, 32, 96, 0xBFFFFFFF, 2);
        drawLine(image, 0, 127, 32, 96, 0xA88C98B7, 2);
        drawLine(image, 64, 127, 32, 96, 0xA88C98B7, 2);
        drawLine(image, 32, 64, 32, 127, 0x80FFFFFF, 1);

        // Tile 3: Dragon scales.
        fillTile(image, 64, 64, 0xFFE4E6EE, 0xFF9EA3B1, 230);
        for (int row = 0; row < 7; row++) {
            int y = 66 + row * 9;
            int offset = (row & 1) == 0 ? 0 : 5;
            for (int col = 0; col < 6; col++) {
                int x = 66 + offset + col * 10;
                drawScale(image, x, y);
            }
        }
    }

    private static void paintArmorAtlas(NativeImage image) {
        clear(image);

        // Left half: Valkyrie — cool steel with warm gold trim.
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean trim = x < 4 || x > 59 || y < 4 || y > 59 || ((x + y) % 19 == 0);
                image.setPixel(x, y, trim ? 0xFFE5C45C : 0xFFD6D9E3);
            }
        }
        for (int y = 8; y < 56; y += 12) {
            drawLine(image, 4, y, 59, y + 5, 0xFF8E96AA, 2);
        }

        // Right half: Demonic — blackened metal with crimson/purple trim.
        for (int y = 0; y < 64; y++) {
            for (int x = 64; x < 128; x++) {
                int lx = x - 64;
                boolean trim = lx < 4 || lx > 59 || y < 4 || y > 59 || ((lx * 3 + y * 2) % 23 == 0);
                image.setPixel(x, y, trim ? 0xFF9B2135 : 0xFF252233);
            }
        }
        for (int y = 10; y < 56; y += 13) {
            drawLine(image, 68, y, 123, 54 - (y / 2), 0xFF6B3C9C, 2);
        }
    }

    private static void clear(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixel(x, y, 0x00000000);
            }
        }
    }

    private static void fillTile(NativeImage image, int ox, int oy, int top, int bottom, int alpha) {
        for (int y = 0; y < 64; y++) {
            float t = y / 63.0F;
            int tr = (top >>> 16) & 0xFF;
            int tg = (top >>> 8) & 0xFF;
            int tb = top & 0xFF;
            int br = (bottom >>> 16) & 0xFF;
            int bg = (bottom >>> 8) & 0xFF;
            int bb = bottom & 0xFF;
            int r = Math.round(tr + (br - tr) * t);
            int g = Math.round(tg + (bg - tg) * t);
            int b = Math.round(tb + (bb - tb) * t);

            for (int x = 0; x < 64; x++) {
                float edge = Math.min(Math.min(x, 63 - x), Math.min(y, 63 - y));
                int a = edge <= 1.0F ? Math.max(50, alpha / 3) : alpha;
                image.setPixel(ox + x, oy + y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    private static void drawScale(NativeImage image, int x, int y) {
        for (int dx = 0; dx <= 9; dx++) {
            int dy = Math.round((float) (Math.sin(dx / 9.0D * Math.PI) * 4.0D));
            put(image, x + dx, y + dy, 0xA85D6372);
            if (dy > 0) put(image, x + dx, y + dy - 1, 0x66FFFFFF);
        }
    }

    private static void drawLine(NativeImage image, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            for (int ox = -thickness / 2; ox <= thickness / 2; ox++) {
                for (int oy = -thickness / 2; oy <= thickness / 2; oy++) {
                    put(image, x0 + ox, y0 + oy, color);
                }
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void put(NativeImage image, int x, int y, int color) {
        if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
            image.setPixel(x, y, color);
        }
    }
}
