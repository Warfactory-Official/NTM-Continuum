package com.hbm.client.render.weapon;

import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import java.util.List;

/**
 * Source-compatible form of original-hbm's Techne {@code ModelB92}.
 *
 * <p>The old model is a tree of twenty textured boxes, not an OBJ. Keeping the boxes as GEM quads
 * preserves the authored pivots and lets the same model run through vanilla and the optional instanced
 * backend without a version-specific {@code ModelRenderer} dependency.
 */
final class B92BoxGeometry {

    private record Box(String name, float x, float y, float z, float width, float height, float depth,
                       float u, float v) {}

    private static final List<Box> BOXES = List.of(
            new Box("Muzzle1", 0F, 0F, 0F, 2F, 3F, 2F, 22F, 36F),
            new Box("Barrel1", 0F, 0F, 0F, 24F, 2F, 3F, 0F, 0F),
            new Box("Barrel2", 0F, 0F, 0F, 22F, 1F, 2F, 0F, 5F),
            new Box("Grip", 0F, 0F, 0F, 20F, 3F, 4F, 0F, 8F),
            new Box("Front1", 0F, 0F, 0F, 2F, 4F, 4F, 10F, 36F),
            new Box("Front2", 0F, 0F, 0F, 2F, 6F, 3F, 0F, 36F),
            new Box("Body", 0F, 0F, 0F, 15F, 7F, 4F, 0F, 15F),
            new Box("Top", 0F, 0F, 0F, 15F, 1F, 3F, 28F, 60F),
            new Box("GripBottom", 0F, 0F, 0F, 18F, 1F, 2F, 24F, 43F),
            new Box("Handle", 0F, 0F, 0F, 6F, 15F, 4F, 0F, 45F),
            new Box("HandleBack", 5.5F, 0F, 0F, 1F, 15F, 3F, 20F, 46F),
            new Box("Frame1", 0F, 0F, 0F, 7F, 1F, 2F, 28F, 57F),
            new Box("Frame2", 0F, 0F, 0F, 2F, 4F, 2F, 28F, 51F),
            new Box("Frame3", 0F, -1F, 0F, 3F, 1F, 2F, 46F, 57F),
            new Box("Trigger", 0F, 0F, 0F, 2F, 3F, 1F, 36F, 53F),
            new Box("BackPlate1", -1F, 0F, 0F, 1F, 4F, 3F, 56F, 53F),
            new Box("Back", 0F, 0F, 0F, 2F, 4F, 4F, 42F, 49F),
            new Box("BackPlate2", -2F, 0F, 0F, 2F, 4F, 4F, 48F, 5F),
            new Box("Pump1", 0F, 0F, 0F, 7F, 2F, 2F, 46F, 29F),
            new Box("Pump2", 0F, 0F, 0F, 3F, 3F, 7F, 44F, 33F),
            new Box("BodyPlate", 0F, 0F, 0F, 14F, 5F, 5F, 0F, 26F));

    private B92BoxGeometry() {}

    static Geometry part(String name) {
        Box box = BOXES.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown B92 box " + name));
        return box(box, "hbm:b92/" + name);
    }

    private static Geometry box(Box box, String name) {
        Quads.Builder builder = Quads.named(name);
        float x0 = box.x;
        float y0 = box.y;
        float z0 = box.z;
        float x1 = x0 + box.width;
        float y1 = y0 + box.height;
        float z1 = z0 + box.depth;
        float u0 = box.u / 64F;
        float v0 = box.v / 64F;
        float u1 = (box.u + box.width) / 64F;
        float v1 = (box.v + box.height) / 64F;

        face(builder, 0F, 0F, 1F, x0, y0, z1, x1, y1, z1, u0, v0, u1, v1);
        face(builder, 0F, 0F, -1F, x1, y0, z0, x0, y1, z0, u0, v0, u1, v1);
        face(builder, 1F, 0F, 0F, x1, y0, z1, x1, y1, z0, u0, v0, u1, v1);
        face(builder, -1F, 0F, 0F, x0, y0, z0, x0, y1, z1, u0, v0, u1, v1);
        face(builder, 0F, 1F, 0F, x0, y1, z1, x1, y1, z0, u0, v0, u1, v1);
        face(builder, 0F, -1F, 0F, x0, y0, z0, x1, y0, z1, u0, v0, u1, v1);
        return builder.build();
    }

    private static void face(Quads.Builder builder, float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float u0, float v0, float u1, float v1) {
        builder.normal(nx, ny, nz)
                .vertex(x0, y0, z0, u0, v1)
                .vertex(x1, y0, z1, u1, v1)
                .vertex(x1, y1, z1, u1, v0)
                .vertex(x0, y1, z0, u0, v0);
    }
}
