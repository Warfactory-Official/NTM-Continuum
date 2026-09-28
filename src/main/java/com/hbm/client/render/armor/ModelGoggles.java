package com.hbm.client.render.armor;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 1.7 {@code ModelGoggles} (Techne, 64x32): five boxes parented to a {@code google} root that copies the head's
 * X/Y rotation point, yaw and pitch. The {@code google} root sits at 0 when the children are converted, so their
 * rotation points stay as authored. {@code mirror = true} after {@code addBox} does not affect an existing box.
 */
public final class ModelGoggles {

    private ModelGoggles() {}

    public static final ModelPart GOOGLE = build();

    private static ModelPart build() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "shape1", 0, 0, 9, 3, 1, -4.5F, -3F - 2, -4.5F);
        box(root, "shape2", 0, 4, 9, 2, 5, -4.5F, -3F - 2, -3.5F);
        box(root, "shape5", 26, 0, 2, 2, 1, 1F, -2.5F - 2, -5F);
        box(root, "shape6", 20, 0, 2, 2, 1, -3F, -2.5F - 2, -5F);
        box(root, "shape7", 0, 11, 9, 1, 4, -4.5F, -3F - 2, 0.5F);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    private static void box(PartDefinition root, String name, int u, int v, int w, int h, int d,
                            float px, float py, float pz) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(0F, 0F, 0F, w, h, d),
                PartPose.offset(px, py, pz));
    }
}
