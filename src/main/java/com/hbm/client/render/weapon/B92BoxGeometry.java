package com.hbm.client.render.weapon;

import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import java.util.List;

/**
 * 1.7's Techne {@code ModelB92} as GEM quads: twenty-one mirrored boxes on a 64 by 64 sheet, each in its
 * part's own space (the box offset from {@code addBox}, before the rotation point).
 */
final class B92BoxGeometry {

    private record Box(String name, float x, float y, float z, int width, int height, int depth, int u, int v) {}

    private static final List<Box> BOXES = List.of(
            new Box("Muzzle1", 0F, 0F, 0F, 2, 3, 2, 22, 36),
            new Box("Barrel1", 0F, 0F, 0F, 24, 2, 3, 0, 0),
            new Box("Barrel2", 0F, 0F, 0F, 22, 1, 2, 0, 5),
            new Box("Grip", 0F, 0F, 0F, 20, 3, 4, 0, 8),
            new Box("Front1", 0F, 0F, 0F, 2, 4, 4, 10, 36),
            new Box("Front2", 0F, 0F, 0F, 2, 6, 3, 0, 36),
            new Box("Body", 0F, 0F, 0F, 15, 7, 4, 0, 15),
            new Box("Top", 0F, 0F, 0F, 15, 1, 3, 28, 60),
            new Box("GripBottom", 0F, 0F, 0F, 18, 1, 2, 24, 43),
            new Box("Handle", 0F, 0F, 0F, 6, 15, 4, 0, 45),
            new Box("HandleBack", 5.5F, 0F, 0F, 1, 15, 3, 20, 46),
            new Box("Frame1", 0F, 0F, 0F, 7, 1, 2, 28, 57),
            new Box("Frame2", 0F, 0F, 0F, 2, 4, 2, 28, 51),
            new Box("Frame3", 0F, -1F, 0F, 3, 1, 2, 46, 57),
            new Box("Trigger", 0F, 0F, 0F, 2, 3, 1, 36, 53),
            new Box("BackPlate1", -1F, 0F, 0F, 1, 4, 3, 56, 53),
            new Box("Back", 0F, 0F, 0F, 2, 4, 4, 42, 49),
            new Box("BackPlate2", -2F, 0F, 0F, 2, 4, 4, 48, 5),
            new Box("Pump1", 0F, 0F, 0F, 7, 2, 2, 46, 29),
            new Box("Pump2", 0F, 0F, 0F, 3, 3, 7, 44, 33),
            new Box("BodyPlate", 0F, 0F, 0F, 14, 5, 5, 0, 26));

    private B92BoxGeometry() {}

    static Geometry part(String name) {
        Box box = BOXES.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown B92 box " + name));
        return box(box, "hbm:b92/" + name);
    }

    /** 1.7 {@code ModelBox} with {@code mirror}: x swapped, the usual six-face unwrap, every face flipped. */
    private static Geometry box(Box box, String name) {
        float x = box.x + box.width, x2 = box.x;
        float y = box.y, y2 = box.y + box.height;
        float z = box.z, z2 = box.z + box.depth;
        float[][] p = {
                {x, y, z}, {x2, y, z}, {x2, y2, z}, {x, y2, z},
                {x, y, z2}, {x2, y, z2}, {x2, y2, z2}, {x, y2, z2}};
        int u = box.u, v = box.v, dx = box.width, dy = box.height, dz = box.depth;

        Quads.Builder builder = Quads.named(name);
        face(builder, p, new int[] {5, 1, 2, 6}, u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy);
        face(builder, p, new int[] {0, 4, 7, 3}, u, v + dz, u + dz, v + dz + dy);
        face(builder, p, new int[] {5, 4, 0, 1}, u + dz, v, u + dz + dx, v + dz);
        face(builder, p, new int[] {2, 3, 7, 6}, u + dz + dx, v + dz, u + dz + dx + dx, v);
        face(builder, p, new int[] {1, 0, 3, 2}, u + dz, v + dz, u + dz + dx, v + dz + dy);
        face(builder, p, new int[] {4, 5, 6, 7}, u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy);
        return builder.build();
    }

    /** 1.7 {@code TexturedQuad} then {@code flipFace}: corners take (u2,v1) (u1,v1) (u1,v2) (u2,v2), drawn in reverse. */
    private static void face(Quads.Builder builder, float[][] p, int[] corners, int u1, int v1, int u2, int v2) {
        float[][] uv = {{u2, v1}, {u1, v1}, {u1, v2}, {u2, v2}};
        float[] a = p[corners[3]], b = p[corners[2]], c = p[corners[1]];
        float ax = b[0] - a[0], ay = b[1] - a[1], az = b[2] - a[2];
        float bx = b[0] - c[0], by = b[1] - c[1], bz = b[2] - c[2];
        float nx = by * az - bz * ay, ny = bz * ax - bx * az, nz = bx * ay - by * ax;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        builder.normal(nx / length, ny / length, nz / length);
        for (int i = 3; i >= 0; i--) {
            float[] corner = p[corners[i]];
            builder.vertex(corner[0], corner[1], corner[2], uv[i][0] / 64F, uv[i][1] / 64F);
        }
    }
}
