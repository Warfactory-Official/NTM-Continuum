package com.hbm.client.render.armor;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * 1.7 {@code ModelM65}: the M65 gas mask box model on a 32x32 sheet, worn by the Liquidator helmet and
 * the two gas-mask attachments. Baked once into two bare roots - the mask and its filter canister - so
 * the filter can be left off without mutating a part a deferred submission still holds.
 *
 * <p>Upstream set {@code mirror = true} only after each {@code addBox}, which does not affect a box that
 * already exists; the boxes are therefore unmirrored. The {@code yOffset} of 0.5 is folded into every
 * rotation point as upstream did.
 */
public final class ModelM65 {

    private ModelM65() {}

    private static final float Y = 0.5F;
    private static final float SLOPE = 0.6108652F;

    public static final ModelPart MASK = mask();
    public static final ModelPart FILTER = filter();

    private static ModelPart mask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "head", 0, 0, 0F, 0F, 0F, 8, 8, 8, -4F, -8F + Y, -4F, 0F);
        box(root, "nose", 0, 16, 0F, 0F, 0F, 3, 3, 1, -1.5F, -3.5F + Y, -5F, 0F);
        box(root, "outlet", 0, 20, 0F, -2F, 0F, 2, 2, 1, -1F, -3.5F + Y, -5F, -0.4799655F);
        box(root, "nose_slope", 8, 16, 0F, 0F, -2F, 3, 2, 2, -1.5F, -2F + Y, -4F, SLOPE);
        box(root, "eye1", 0, 23, 0F, 0F, 0F, 3, 3, 0, -3.5F, -6F + Y, -4.2F, 0F);
        box(root, "eye2", 0, 26, 0F, 0F, 0F, 3, 3, 0, 0.5F, -6F + Y, -4.2F, 0F);
        box(root, "i_forgot", 6, 20, 0F, 0F, 0F, 2, 2, 1, -1F, -3.2F + Y, -6F, 0F);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static ModelPart filter() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "connector", 6, 23, 0F, 0F, -3F, 2, 2, 1, -1F, -2F + Y, -4F, SLOPE);
        box(root, "filter1", 18, 21, 0F, -1F, -5F, 3, 4, 2, -1.5F, -2F + Y, -4F, SLOPE);
        box(root, "filter2", 18, 16, 0F, -0.5F, -5F, 4, 3, 2, -2F, -2F + Y, -4F, SLOPE);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z,
                            int w, int h, int d, float px, float py, float pz, float xRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, xRot, 0F, 0F));
    }
}
