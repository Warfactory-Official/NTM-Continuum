package com.hbm.client.render.armor;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 1.7 {@code ModelGasMask} (Techne, 64x32): the plain gas mask, six boxes parented to a {@code mask} root that
 * copies the head's X/Y rotation point, yaw and pitch. The root sits at 0 when the children are converted, so
 * their rotation points stay as authored, each lifted by {@code 0.075 / 2}.
 *
 * <p>Upstream set {@code mirror = true} only after each {@code addBox}, which does not affect a box that already
 * exists; the boxes are therefore unmirrored.
 */
public final class ModelGasMask {

    private ModelGasMask() {}

    private static final float LIFT = 0.075F / 2F;
    private static final float TILT = -0.7853982F;

    public static final ModelPart MASK = mask();

    private static ModelPart mask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "shape1", 0, 0, 0F, 0F, 0F, 8, 8, 3, 0F - 4, 0F - 8 + LIFT, 0F - 4, 0F);
        box(root, "shape2", 22, 0, 0F, 0F, 0F, 2, 2, 1, 1F - 4, 3F - 8 + LIFT, -0.5333334F - 4, 0F);
        box(root, "shape3", 22, 0, 0F, 0F, 0F, 2, 2, 1, 5F - 4, 3F - 8 + LIFT, -0.5F - 4, 0F);
        box(root, "shape4", 0, 11, 0F, 0F, 0F, 2, 2, 2, 3F - 4, 5F - 8 + LIFT, 0F - 4, TILT);
        box(root, "shape5", 0, 15, 0F, 2F, -0.5F, 3, 4, 3, 2.5F - 4, 5F - 8 + LIFT, 0F - 4, TILT);
        box(root, "shape6", 0, 22, 0F, 0F, 0F, 8, 1, 5, 0F - 4, 3F - 8 + LIFT, 3F - 4, 0F);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z,
                            int w, int h, int d, float px, float py, float pz, float xRot) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, xRot, 0F, 0F));
    }
}
