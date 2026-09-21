package com.koqps.topka.item;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class OniWeaponModels {
    private static final Map<String, OniWeaponMesh> CACHE = new HashMap<>();

    private OniWeaponModels() { }

    public static OniWeaponMesh get(String modelId) {
        return CACHE.computeIfAbsent(modelId, OniWeaponModels::loadUnchecked);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static OniWeaponMesh loadUnchecked(String modelId) {
        try {
            return load(modelId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Oni weapon model: " + modelId, e);
        }
    }

    private static OniWeaponMesh load(String modelId) throws IOException {
        Identifier resourceId = Identifier.fromNamespaceAndPath(
                "topka",
                "models/oni/" + modelId + ".oni.gz"
        );

        InputStream raw = Minecraft.getInstance().getResourceManager().open(resourceId);
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'O' || magic[1] != 'N' || magic[2] != 'I' || magic[3] != '1') {
                throw new IOException("Invalid Oni model magic for " + modelId);
            }

            int layerCount = in.readInt();
            List<OniWeaponMesh.Layer> layers = new ArrayList<>(layerCount);

            for (int layer = 0; layer < layerCount; layer++) {
                int textureSlot = in.readInt();
                int triangleCount = in.readInt();
                List<OniWeaponMesh.Triangle> triangles = new ArrayList<>(triangleCount);

                for (int triangle = 0; triangle < triangleCount; triangle++) {
                    OniWeaponMesh.Vertex a = readVertex(in);
                    OniWeaponMesh.Vertex b = readVertex(in);
                    OniWeaponMesh.Vertex c = readVertex(in);
                    triangles.add(new OniWeaponMesh.Triangle(a, b, c));
                }

                layers.add(new OniWeaponMesh.Layer(textureSlot, List.copyOf(triangles)));
            }

            return new OniWeaponMesh(List.copyOf(layers));
        }
    }

    private static OniWeaponMesh.Vertex readVertex(DataInputStream in) throws IOException {
        return new OniWeaponMesh.Vertex(
                in.readFloat(), in.readFloat(), in.readFloat(),
                in.readFloat(), in.readFloat(),
                in.readFloat(), in.readFloat(), in.readFloat()
        );
    }
}
