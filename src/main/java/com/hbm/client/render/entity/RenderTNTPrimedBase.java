package com.hbm.client.render.entity;

import com.hbm.entity.item.EntityTntNtm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * hbm-space's {@code RenderTNTPrimedBase}: a primed charge drawn as vanilla's {@code TntRenderer} draws it (swell over
 * the last ten ticks, white flash every fifth tick), tumbling by the entity's yaw and pitch after the quarter turn, as
 * hbm-space's {@code glRotatef(rotationYaw, 0, 1, 0)} and {@code glRotatef(rotationPitch, 0, 0, 1)} did. Used only while
 * {@link EntityTntNtm#spin} is on (ADDON-API-INVENTORY V-181); plain HBM keeps vanilla's renderer.
 *
 * <p>As 1.7 it reads the raw rotation, not one interpolated between ticks: the tumble steps once a tick.
 */
public class RenderTNTPrimedBase extends CompatEntityRenderer<EntityTntNtm, RenderTNTPrimedBase.State> {

    public RenderTNTPrimedBase(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityTntNtm entity, State state, float partialTicks) {
        state.fuse = entity.getFuse() - partialTicks + 1.0F;
        state.yaw = entity.spinYaw(1.0F);
        state.pitch = entity.spinPitch(1.0F);
        state.block.extract(entity.getBlockState());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.0F, 0.5F, 0.0F);
        // hbm-space turns before the swell (vanilla has no turn at all)
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (state.fuse < 10.0F) {
            float swell = Mth.clamp(1.0F - state.fuse / 10.0F, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            float scale = 1.0F + swell * 0.3F;
            pose.scale(scale, scale, scale);
        }
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.translate(-0.5F, -0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        boolean flash = (int) state.fuse / 5 % 2 == 0;
        int overlay = flash ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10) : OverlayTexture.NO_OVERLAY;
        sink.submitBlock(pose, state.block, state.lightCoords, overlay);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        final CompatBlockRenderState block = new CompatBlockRenderState();
        float fuse;
        float yaw;
        float pitch;
    }
}
