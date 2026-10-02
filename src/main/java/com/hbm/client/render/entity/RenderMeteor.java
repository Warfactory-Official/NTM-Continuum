package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

/**
 * 1.7 RenderMeteor: a 5-block cube tumbling about (1,1,1); its texture is 1.7's orphan block_meteor_molten.png,
 * shipped as an entity texture.
 */
public final class RenderMeteor extends CompatEntityRenderer<EntityMeteor, RenderMeteor.State> {

    private static final int FULL_BRIGHT = 0xF000F0;

    // 1.7: six faces at +/-0.5, no-cull
    private static final float[][] CUBE = {
            {-0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, -0.5F, 0F, 0F, +0.5F, +0.5F, -0.5F, 0F, 1F, -0.5F, +0.5F, -0.5F, 1F, 1F},
            {-0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, -0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F, -0.5F, +0.5F, +0.5F, 1F, 1F},
            {+0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, +0.5F, 0F, 1F, +0.5F, +0.5F, +0.5F, 1F, 1F},
            {+0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, +0.5F, +0.5F, 0F, 1F, +0.5F, +0.5F, -0.5F, 1F, 1F},
            {-0.5F, -0.5F, +0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, -0.5F, -0.5F, 0F, 1F, -0.5F, -0.5F, -0.5F, 1F, 1F},
            {+0.5F, +0.5F, +0.5F, 1F, 0F, -0.5F, +0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F, +0.5F, +0.5F, -0.5F, 1F, 1F}};

    private final RenderType type = CompatRenderTypes.entityCutoutNoCull(ResourceManager.meteor_tex);

    public RenderMeteor(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    // 1.7: full-bright, UP normal - unshaded
    private static void draw(PoseStack.Pose pose, VertexConsumer buffer) {
        int color = 0xFFFFFFFF;
        for (float[] face : CUBE) {
            for (int v = 0; v < 4; v++) {
                int i = v * 5;
                Vertexes.vertex(buffer, pose, face[i], face[i + 1], face[i + 2], color, face[i + 3], face[i + 4],
                        OverlayTexture.NO_OVERLAY, FULL_BRIGHT, 0F, 1F, 0F);
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    // 1.7: ignoreFrustumCheck - 5-block cube
    @Override
    public boolean culled(EntityMeteor entity) {
        return false;
    }

    @Override
    public void extract(EntityMeteor entity, State state, float partialTicks) {
        state.spin = (entity.tickCount % 360 + partialTicks) * 10F;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        // the dispatcher pre-translates, so 1.7's glTranslated drops
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(state.spin), 1F, 1F, 1F));
        pose.scale(5F, 5F, 5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        sink.submitCustomGeometry(pose, type, RenderMeteor::draw);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float spin;
    }
}
