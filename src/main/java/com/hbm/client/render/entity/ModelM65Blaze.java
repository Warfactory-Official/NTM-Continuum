package com.hbm.client.render.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 1.7 {@code ModelM65Blaze} (Techne, 32x32): the M65 gas mask the radiation blaze ({@code RenderRADBeast}) wears,
 * {@code ModelM65} with a {@code yOffset} of 4 instead of 0.5 and the filter canister always present. All ten
 * shapes are children of one {@code mask} root at 0, so their rotation points stay as authored.
 *
 * <p>Upstream set {@code mirror = true} only after each {@code addBox}, which does not affect a box that already
 * exists; the boxes are therefore unmirrored.
 */
public final class ModelM65Blaze {

    private ModelM65Blaze() {}

    private static final float Y = 4F;
    private static final float SLOPE = 0.6108652F;

    public static final ModelPart MASK = mask();

    private static ModelPart mask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "shape1", 0, 0, 0F, 0F, 0F, 8, 8, 8, -4F, -8F + Y, -4F, 0F);
        box(root, "shape2", 0, 16, 0F, 0F, 0F, 3, 3, 1, -1.5F, -3.5F + Y, -5F, 0F);
        box(root, "shape3", 0, 20, 0F, -2F, 0F, 2, 2, 1, -1F, -3.5F + Y, -5F, -0.4799655F);
        box(root, "shape4", 8, 16, 0F, 0F, -2F, 3, 2, 2, -1.5F, -2F + Y, -4F, SLOPE);
        box(root, "shape5", 0, 23, 0F, 0F, 0F, 3, 3, 0, -3.5F, -6F + Y, -4.2F, 0F);
        box(root, "shape6", 0, 26, 0F, 0F, 0F, 3, 3, 0, 0.5F, -6F + Y, -4.2F, 0F);
        box(root, "shape7", 6, 20, 0F, 0F, 0F, 2, 2, 1, -1F, -3.2F + Y, -6F, 0F);
        box(root, "shape8", 6, 23, 0F, 0F, -3F, 2, 2, 1, -1F, -2F + Y, -4F, SLOPE);
        box(root, "shape9", 18, 21, 0F, -1F, -5F, 3, 4, 2, -1.5F, -2F + Y, -4F, SLOPE);
        box(root, "shape10", 18, 16, 0F, -0.5F, -5F, 4, 3, 2, -2F, -2F + Y, -4F, SLOPE);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z,
                            int w, int h, int d, float px, float py, float pz, float xRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, xRot, 0F, 0F));
    }
}
