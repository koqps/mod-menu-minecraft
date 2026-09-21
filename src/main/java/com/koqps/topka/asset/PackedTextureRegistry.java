package com.koqps.topka.asset;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class PackedTextureRegistry {
    public static final Identifier ONI = Identifier.fromNamespaceAndPath("topka", "textures/weapons/oni_atlas.png");
    public static final Identifier LITTLE_DEMON = Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/little_demon.png");
    public static final Identifier PLAIN_WHITE = Identifier.fromNamespaceAndPath("topka", "textures/cosmetic/plain_white.png");

    private record TextureSpec(Identifier target, String folder, int chunks) { }

    private static final List<TextureSpec> SPECS = List.of(
            new TextureSpec(ONI, "oni_tex", 6),
            new TextureSpec(LITTLE_DEMON, "demon_tex", 1),
            new TextureSpec(PLAIN_WHITE, "white_tex", 1)
    );

    private static final List<DynamicTexture> OPEN_TEXTURES = new ArrayList<>();
    private static boolean registered;

    private PackedTextureRegistry() { }

    public static void register(Minecraft client) {
        if (registered) return;

        try {
            for (TextureSpec spec : SPECS) {
                byte[] png = readBase64Resource(spec.folder, spec.chunks);
                NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                DynamicTexture texture = new DynamicTexture(() -> "Mod Menu uploaded asset " + spec.target, image);
                client.getTextureManager().register(spec.target, texture);
                OPEN_TEXTURES.add(texture);
            }
            registered = true;
        } catch (IOException exception) {
            close();
            throw new IllegalStateException("Unable to register uploaded cosmetic textures", exception);
        }
    }

    public static void close() {
        for (DynamicTexture texture : OPEN_TEXTURES) {
            texture.close();
        }
        OPEN_TEXTURES.clear();
        registered = false;
    }

    private static byte[] readBase64Resource(String folder, int chunks) throws IOException {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < chunks; i++) {
            Identifier resource = Identifier.fromNamespaceAndPath(
                    "topka",
                    "packed/" + folder + "/" + String.format("%02d.b64", i)
            );
            try (InputStream input = Minecraft.getInstance().getResourceManager().open(resource)) {
                encoded.append(new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim());
            }
        }
        return Base64.getDecoder().decode(encoded.toString());
    }
}
