package com.koqps.topka.asset;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Loads the compact mesh packs generated directly from the user supplied
 * OBJ/FBX packages. The source geometry, UVs, normals and per-submesh texture
 * assignments are preserved in the MMP2 container.
 */
public final class PackedMeshLibrary {
    public enum Pack {
        ONI("oni_pack", 5),
        ENDER("ender_pack", 4),
        WINGS("wing_pack", 1),
        LITTLE_DEMON("demon_pack", 1),
        VALKYRIE_ARMOR("valkyrie_armor", 1),
        DEMONIC_ARMOR("demonic_armor", 1);

        private final String folder;
        private final int chunks;

        Pack(String folder, int chunks) {
            this.folder = folder;
            this.chunks = chunks;
        }
    }

    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) { }
    public record SubMesh(Identifier texture, List<Vertex> vertices) { }
    public record Model(String name, List<SubMesh> subMeshes) { }

    private static final Map<Pack, Map<String, Model>> CACHE = new HashMap<>();

    private PackedMeshLibrary() { }

    public static Model get(Pack pack, String name) {
        return loadPack(pack).get(name);
    }

    public static List<String> names(Pack pack) {
        return List.copyOf(loadPack(pack).keySet());
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Map<String, Model> loadPack(Pack pack) {
        return CACHE.computeIfAbsent(pack, PackedMeshLibrary::readPackUnchecked);
    }

    private static Map<String, Model> readPackUnchecked(Pack pack) {
        try {
            return readPack(pack);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load cosmetic mesh pack " + pack, exception);
        }
    }

    private static Map<String, Model> readPack(Pack pack) throws IOException {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < pack.chunks; i++) {
            Identifier resource = Identifier.fromNamespaceAndPath(
                    "topka",
                    "packed/" + pack.folder + "/" + String.format("%02d.b64", i)
            );
            try (InputStream input = Minecraft.getInstance().getResourceManager().open(resource)) {
                encoded.append(new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim());
            }
        }

        byte[] compressed = Base64.getDecoder().decode(encoded.toString());
        byte[] raw;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            gzip.transferTo(output);
            raw = output.toByteArray();
        }

        ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.remaining() < 8
                || buffer.get() != 'M'
                || buffer.get() != 'M'
                || buffer.get() != 'P'
                || buffer.get() != '2') {
            throw new IOException("Invalid MMP2 header for " + pack);
        }

        int modelCount = buffer.getInt();
        Map<String, Model> models = new HashMap<>(Math.max(4, modelCount * 2));

        for (int modelIndex = 0; modelIndex < modelCount; modelIndex++) {
            String name = readString(buffer);
            int subMeshCount = buffer.getInt();
            List<SubMesh> subMeshes = new ArrayList<>(subMeshCount);

            for (int sub = 0; sub < subMeshCount; sub++) {
                Identifier texture = Identifier.parse(readString(buffer));
                int vertexCount = buffer.getInt();
                List<Vertex> vertices = new ArrayList<>(vertexCount);

                for (int vertex = 0; vertex < vertexCount; vertex++) {
                    vertices.add(new Vertex(
                            buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                            buffer.getFloat(), buffer.getFloat(),
                            buffer.getFloat(), buffer.getFloat(), buffer.getFloat()
                    ));
                }

                subMeshes.add(new SubMesh(texture, List.copyOf(vertices)));
            }

            models.put(name, new Model(name, List.copyOf(subMeshes)));
        }

        return Map.copyOf(models);
    }

    private static String readString(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < 2) throw new IOException("Unexpected end of mesh pack");
        int length = Short.toUnsignedInt(buffer.getShort());
        if (length < 0 || length > buffer.remaining()) throw new IOException("Invalid string length " + length);

        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
