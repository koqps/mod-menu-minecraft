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
        DEMONIC_ARMOR("demonic_armor", 1),
        PALADIN_ARMOR("paladin_armor", 1);

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

        if (pack == Pack.VALKYRIE_ARMOR) {
            addArmorParts(models, "valkyrie", false);
        } else if (pack == Pack.DEMONIC_ARMOR) {
            addArmorParts(models, "demonic", false);
        } else if (pack == Pack.PALADIN_ARMOR) {
            // Paladin is stored as the untouched source OBJ. Split/re-center
            // it here so every generated section is local to the Minecraft
            // body bone it will animate with.
            addArmorParts(models, "paladin", true);
        }

        return Map.copyOf(models);
    }

    private static void addArmorParts(Map<String, Model> models, String fullName, boolean paladin) {
        Model full = models.get(fullName);
        if (full == null) return;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        double sumX = 0.0D;
        double sumZ = 0.0D;
        long sampledVertices = 0L;

        for (SubMesh subMesh : full.subMeshes()) {
            for (Vertex vertex : subMesh.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
                maxZ = Math.max(maxZ, vertex.z());
                sumX += vertex.x();
                sumZ += vertex.z();
                sampledVertices++;
            }
        }

        float sourceHeight = Math.max(0.001F, maxY - minY);

        // The Paladin mesh has long asymmetric protrusions, so using its
        // min/max bounding-box midpoint shifts the whole suit off the player.
        // Use the mesh-average X/Z center instead, and fit it slightly under
        // two blocks tall so the helmet/shoulders don't engulf the player.
        float targetHeight = paladin ? 1.82F : 2.0F;
        float fitScale = targetHeight / sourceHeight;
        float centerX = paladin && sampledVertices > 0
                ? (float) (sumX / sampledVertices)
                : (minX + maxX) * 0.5F;
        float centerZ = paladin && sampledVertices > 0
                ? (float) (sumZ / sampledVertices)
                : (minZ + maxZ) * 0.5F;

        String[] parts = {
                "helmet", "body", "left_arm", "right_arm",
                "waist", "left_leg", "right_leg", "left_boot", "right_boot"
        };

        Map<String, List<SubMesh>> built = new HashMap<>();
        for (String part : parts) built.put(part, new ArrayList<>());

        for (SubMesh subMesh : full.subMeshes()) {
            Map<String, List<Vertex>> byPart = new HashMap<>();
            for (String part : parts) byPart.put(part, new ArrayList<>());

            List<Vertex> vertices = subMesh.vertices();
            for (int i = 0; i + 2 < vertices.size(); i += 3) {
                Vertex a = vertices.get(i);
                Vertex b = vertices.get(i + 1);
                Vertex c = vertices.get(i + 2);

                float cx = ((a.x() + b.x() + c.x()) / 3.0F - centerX) * fitScale;
                float cy = ((a.y() + b.y() + c.y()) / 3.0F - minY) * fitScale;
                String part = paladin
                        ? classifyPaladinPart(cx, cy)
                        : classifyArmorPart(cx, cy);

                List<Vertex> target = byPart.get(part);
                target.add(toArmorLocal(a, part, fitScale, centerX, minY, centerZ));
                target.add(toArmorLocal(b, part, fitScale, centerX, minY, centerZ));
                target.add(toArmorLocal(c, part, fitScale, centerX, minY, centerZ));
            }

            for (String part : parts) {
                List<Vertex> partVertices = byPart.get(part);
                if (!partVertices.isEmpty()) {
                    built.get(part).add(new SubMesh(subMesh.texture(), List.copyOf(partVertices)));
                }
            }
        }

        for (String part : parts) {
            List<SubMesh> subMeshes = built.get(part);
            if (!subMeshes.isEmpty()) {
                models.put(part, new Model(part, List.copyOf(subMeshes)));
            }
        }
    }

    private static String classifyPaladinPart(float x, float y) {
        // Tighter boundaries than the older generic OBJ slicer. The Paladin
        // source has broad decorative shoulder/helmet geometry, so keeping the
        // central torso narrower prevents those triangles from being assigned
        // to the wrong animated bone.
        if (y >= 1.42F && Math.abs(x) <= 0.39F) return "helmet";

        if (y >= 0.80F) {
            if (x > 0.285F) return "right_arm";
            if (x < -0.285F) return "left_arm";
            return "body";
        }

        if (y >= 0.34F) {
            if (Math.abs(x) < 0.19F && y >= 0.66F) return "waist";
            return x >= 0.0F ? "right_leg" : "left_leg";
        }

        return x >= 0.0F ? "right_boot" : "left_boot";
    }

    private static String classifyArmorPart(float x, float y) {
        if (y >= 1.50F && Math.abs(x) <= 0.43F) return "helmet";

        if (y >= 0.78F) {
            if (x > 0.30F) return "right_arm";
            if (x < -0.30F) return "left_arm";
            return "body";
        }

        if (y >= 0.38F) {
            if (Math.abs(x) < 0.20F && y >= 0.68F) return "waist";
            return x >= 0.0F ? "right_leg" : "left_leg";
        }

        return x >= 0.0F ? "right_boot" : "left_boot";
    }

    private static Vertex toArmorLocal(
            Vertex vertex,
            String part,
            float fitScale,
            float centerX,
            float minY,
            float centerZ
    ) {
        float worldX = (vertex.x() - centerX) * fitScale;
        float worldY = (vertex.y() - minY) * fitScale;
        float worldZ = (vertex.z() - centerZ) * fitScale;

        float anchorX;
        float anchorY;
        switch (part) {
            case "right_arm" -> {
                anchorX = 0.3125F;
                anchorY = 1.375F;
            }
            case "left_arm" -> {
                anchorX = -0.3125F;
                anchorY = 1.375F;
            }
            case "right_leg", "right_boot" -> {
                anchorX = 0.11875F;
                anchorY = 0.75F;
            }
            case "left_leg", "left_boot" -> {
                anchorX = -0.11875F;
                anchorY = 0.75F;
            }
            default -> {
                anchorX = 0.0F;
                anchorY = 1.5F;
            }
        }

        return new Vertex(
                -(worldX - anchorX),
                -(worldY - anchorY),
                worldZ,
                vertex.u(),
                vertex.v(),
                -vertex.nx(),
                -vertex.ny(),
                vertex.nz()
        );
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
