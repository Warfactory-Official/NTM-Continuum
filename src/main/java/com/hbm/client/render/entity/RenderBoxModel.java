package com.hbm.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The 1.7 {@code ModelBase} box models that carry no animation - one cube, one skin. Modern versions have no
 * equivalent of building a {@code ModelRenderer} inline, so each is baked once into a bare {@link ModelPart} here;
 * nothing about them varies per entity, so none of them need a registered model layer.
 */
public final class RenderBoxModel {

    /** 1.7 ModelChopperMine: one 8x8x8 cube on a 32x16 sheet. */
    public static final ModelPart CHOPPER_MINE = cube(32, 16, -4F, -4F, -4F, 8, 8, 8);
    /** 1.7 ModelShrapnel: one 4x4x4 cube on a 16x8 sheet. */
    public static final ModelPart SHRAPNEL = cube(16, 8, 1F, -0.5F, -0.5F, 4, 4, 4);
    /** 1.7 ModelBullet: one 2x1x1 cube on an 8x4 sheet. */
    public static final ModelPart BULLET = cube(8, 4, 1F, -0.5F, -0.5F, 2, 1, 1);

    private RenderBoxModel() {
    }

    private static ModelPart cube(int texWidth, int texHeight, float x, float y, float z, int w, int h, int d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("box",
                CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, w, h, d),
                PartPose.offset(x, y, z));
        return LayerDefinition.create(mesh, texWidth, texHeight).bakeRoot();
    }

    public static void submit(PoseStack poseStack, SubmitSink sink, RenderType type, ModelPart part, int light) {
        ModelParts.submit(sink, poseStack, type, part, light, OverlayTexture.NO_OVERLAY);
    }
}
