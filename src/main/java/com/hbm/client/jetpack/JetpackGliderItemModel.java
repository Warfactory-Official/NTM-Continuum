package com.hbm.client.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/** Shared item-space submitter used by the modern special renderer and legacy BEWLR bridge. */
public final class JetpackGliderItemModel {

    private static JetpackGliderItemModel shared;

    private final JetpackColladaModel model;
    private final Vector3f center;
    private final float scale;

    /**
     * The one glider item body. Building one parses the animation scene and loads its meshes, which is
     * a model-load cost: 26.1 pays it once when it bakes the special model, and the older versions have
     * no bake step to pay it in. Their renderer is called per draw and must find this already built.
     */
    public static JetpackGliderItemModel shared() {
        JetpackGliderItemModel built = shared;
        if (built == null) {
            built = new JetpackGliderItemModel();
            shared = built;
        }
        return built;
    }

    /** A resource reload replaces the meshes the declaration holds onto. */
    public static void clear() {
        shared = null;
    }

    public JetpackGliderItemModel() {
        this.model = JetpackColladaModel.load();
        Vector3f[] bounds = model.openBounds();
        center = new Vector3f(bounds[0]).add(bounds[1]).mul(0.5F);
        Vector3f size = new Vector3f(bounds[1]).sub(bounds[0]);
        scale = 1.15F / Math.max(size.x, Math.max(size.y, size.z));
    }

    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay,
            ItemDisplayContext context) {
        submitRaw(pose, sink, light, overlay);
    }

    public void submitRaw(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(155F));
        pose.scale(scale, scale, scale);
        pose.translate(-center.x, -center.y, -center.z);
        model.submit(pose, sink, light, 1000L, false);
        pose.popPose();
    }
}
