package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;

/** The four-section, non-piped fuel-channel inventory model from {@code RBMKColumnModel.Kind.FUEL}. */
public final class RBMKColumnItemModel {
    private static final WavefrontObj ELEMENT = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element.obj"));
    private final Identifier side, top, inner;
    private final float scale, yaw, lift;
    public RBMKColumnItemModel(Identifier side, Identifier top, Identifier inner, float scale, float yaw, float lift) {
        this.side = normalize(side); this.top = normalize(top); this.inner = normalize(inner);
        this.scale = scale; this.yaw = yaw; this.lift = lift;
    }
    /**
     * The item definitions name atlas sprites ({@code hbm:blocks/rbmk/rbmk_element_side}) because that is
     * how every other model JSON spells textures, but these ids are bound directly against the resource
     * pack, which needs the full {@code textures/….png} path. Accept both spellings.
     */
    public static Identifier normalize(Identifier id) {
        if (id == null) return null;
        String path = id.getPath();
        return path.startsWith("textures/") && path.endsWith(".png") ? id
                : Ids.of(id.getNamespace(), "textures/" + path + ".png");
    }
    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        // RenderRBMKRod:36-37: fuel columns are non-piped and sit 0.675 below the item origin.
        pose.translate(.5F, .5F + lift, .5F); pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale); pose.translate(-.5F, 0F, -.5F);
        for (int segment = 0; segment < 4; segment++) {
            final int y = segment;
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(side),
                    (last, buffer) -> RBMKColumnGeometry.box(last, buffer, 0, y, 0, 1, y + 1, 1, light, false));
            // The OBJ parts are authored centred on the origin (x/z in [-0.5, 0.5]) while the casing
            // boxes span [0, 1]; shift them into block space or they ride half a block off the shell.
            pose.pushPose(); pose.translate(.5F, y, .5F);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(inner),
                    (last, buffer) -> ELEMENT.renderPart("Inner", last, buffer, light, overlay));
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(top),
                    (last, buffer) -> ELEMENT.renderPart("Cap", last, buffer, light, overlay));
            pose.popPose();
        }
        pose.popPose();
    }
}
