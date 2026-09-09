package com.hbm.client.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.item.ItemDisplayContext;

/** Shared item-space submitter used by the modern special renderer and legacy BEWLR bridge. */
public final class JetpackGliderItemModel {

    private final JetpackColladaModel model;
    private final org.joml.Vector3f center;
    private final float scale;

    public JetpackGliderItemModel() {
        this.model = JetpackColladaModel.load();
        org.joml.Vector3f[] bounds = model.openBounds();
        center = new org.joml.Vector3f(bounds[0]).add(bounds[1]).mul(0.5F);
        org.joml.Vector3f size = new org.joml.Vector3f(bounds[1]).sub(bounds[0]);
        scale = 1.15F / Math.max(size.x, Math.max(size.y, size.z));
    }

    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay,
            ItemDisplayContext context) {
        submitRaw(pose, sink, light, overlay);
    }

    public void submitRaw(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(155F));
        pose.scale(scale, scale, scale);
        pose.translate(-center.x, -center.y, -center.z);
        model.submit(pose, sink, light, 1000L, false);
        pose.popPose();
    }
}
