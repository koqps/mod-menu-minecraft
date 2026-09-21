package com.koqps.topka.item;

import java.util.List;

public record OniWeaponMesh(List<Layer> layers) {
    public record Layer(int textureSlot, List<Triangle> triangles) { }
    public record Triangle(Vertex a, Vertex b, Vertex c) { }
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) { }
}
