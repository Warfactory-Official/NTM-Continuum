package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 1.7 render/entity/projectile/RenderSawblade: the sawmill's blade, spinning until it has landed. */
public class RenderSawblade extends CompatEntityRenderer<EntitySawblade, RenderSawblade.State> {

    /** 1.7: {@code millis % (360 * 5) / 3}, so the angle runs to 600 and snaps back. */
    private static final long SPIN_PERIOD = 360L * 5L;

    public RenderSawblade(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySawblade entity, State state, float partialTicks) {
        state.orientation = entity.getOrientation();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (state.orientation % 6) {
            case 5 -> pose.mulPose(Axis.YP.rotationDegrees(90F));
            case 2 -> pose.mulPose(Axis.YP.rotationDegrees(180F));
            case 4 -> pose.mulPose(Axis.YP.rotationDegrees(270F));
            default -> {
            }
        }

        pose.translate(0D, 0D, -1D);

        if (state.orientation < EntitySawblade.LANDED) {
            pose.mulPose(Axis.ZN.rotationDegrees((GameTime.now() % SPIN_PERIOD) / 3F));
        }

        pose.translate(0D, -1.375D, 0D);

        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(ResourceManager.sawmill_tex), (p, buffer) ->
                NtmEntityModels.SAWMILL.renderPart("Blade", p, buffer, light, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public int orientation;
    }
}
