package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import java.util.function.Consumer;
import org.joml.Vector3fc;

/**
 * The inventory preview of a non-fuel RBMK column, ported from the old baked models' inventory quads:
 * four stacked casing sections ({@code RBMKColumnBakedModel}), optionally the four pipe stubs at the
 * top ({@code RBMKControlBakedModel.addPipes}), and optionally the sliding rod's Lid OBJ part parked at
 * y=3. Framing is identity here — {@code hbm:item/rbmk_column_base} carries the old
 * {@code BakedModelTransforms.rbmkColumn()} display verbatim, exactly as a baked model received it.
 */
public final class RBMKBlockColumnItemModel {
    private static final WavefrontObj RODS = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_rods.obj"));
    private static final int SECTIONS = 4;
    private static final float PIPE_Y = 4F, LID_Y = 3F;

    private final Identifier side, top, bottom, pipeSide, pipeTop, lidTexture;
    private final float scale, yaw, lift;

    public RBMKBlockColumnItemModel(Identifier side, Identifier top, Identifier bottom,
                                    Identifier pipeSide, Identifier pipeTop, Identifier lidTexture,
                                    float scale, float yaw, float lift) {
        this.side = RBMKColumnItemModel.normalize(side);
        this.top = RBMKColumnItemModel.normalize(top);
        this.bottom = RBMKColumnItemModel.normalize(bottom != null ? bottom : top);
        this.pipeSide = RBMKColumnItemModel.normalize(pipeSide);
        this.pipeTop = RBMKColumnItemModel.normalize(pipeTop);
        this.lidTexture = RBMKColumnItemModel.normalize(lidTexture);
        this.scale = scale; this.yaw = yaw; this.lift = lift;
    }

    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        var sideType = CompatRenderTypes.entityCutout(side);
        var topType = CompatRenderTypes.entityCutout(top);
        for (int i = 0; i < SECTIONS; i++) {
            final int y = i;
            sink.submitCustomGeometry(pose, sideType,
                    (last, buffer) -> RBMKColumnGeometry.box(last, buffer, 0, y, 0, 1, y + 1, 1, light, false));
            sink.submitCustomGeometry(pose, topType,
                    (last, buffer) -> RBMKColumnGeometry.box(last, buffer, 0, y, 0, 1, y + 1, 1, light, light, true));
        }
        if (pipeSide != null && pipeTop != null) {
            var pipeSideType = CompatRenderTypes.entityCutout(pipeSide);
            var pipeTopType = CompatRenderTypes.entityCutout(pipeTop);
            sink.submitCustomGeometry(pose, pipeSideType, (last, buffer) -> {
                for (float x : new float[]{.0625F, .5625F}) for (float z : new float[]{.0625F, .5625F})
                    RBMKColumnGeometry.box(last, buffer, x, PIPE_Y, z, x + .375F, PIPE_Y + .125F, z + .375F, light, false);
            });
            sink.submitCustomGeometry(pose, pipeTopType, (last, buffer) -> {
                for (float x : new float[]{.0625F, .5625F}) for (float z : new float[]{.0625F, .5625F})
                    RBMKColumnGeometry.box(last, buffer, x, PIPE_Y, z, x + .375F, PIPE_Y + .125F, z + .375F, light, light, true);
            });
        }
        if (lidTexture != null) {
            // The Lid OBJ is authored centred on the origin, like every old HBM part; the casing here
            // is in block space, so it needs the same +0.5/+0.5 shift the world renderers bake in.
            pose.pushPose(); pose.translate(.5F, LID_Y, .5F);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(lidTexture),
                    (last, buffer) -> RODS.renderPart("Lid", last, buffer, light, overlay));
            pose.popPose();
        }
        pose.popPose();
    }

    public void extents(Consumer<Vector3fc> output) {
        PoseStack pose = new PoseStack();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        RBMKItemModelBounds.emitBox(pose, 0F, 0F, 0F, 1F, 4F, 1F, output);
        if (pipeSide != null && pipeTop != null) {
            for (float x : new float[]{.0625F, .5625F}) for (float z : new float[]{.0625F, .5625F}) {
                RBMKItemModelBounds.emitBox(pose, x, PIPE_Y, z,
                        x + .375F, PIPE_Y + .125F, z + .375F, output);
            }
        }
        if (lidTexture != null) {
            pose.pushPose();
            pose.translate(.5F, LID_Y, .5F);
            RBMKItemModelBounds.emitPart(pose, RODS, "Lid", output);
            pose.popPose();
        }
    }
}
