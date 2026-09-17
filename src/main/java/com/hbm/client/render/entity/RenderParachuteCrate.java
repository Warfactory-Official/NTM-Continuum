package com.hbm.client.render.entity;

import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 render/entity/item/RenderParachuteCrate: the conserve crate swinging under the soyuz chute.
 */
public class RenderParachuteCrate extends CompatEntityRenderer<EntityParachuteCrate, RenderParachuteCrate.State> {

    // 1.7: swing pivot 7 blocks up the chute
    private static final int PIVOT_HEIGHT = 7;
    private final RenderType crate = CompatRenderTypes.entityCutoutCull(ResourceManager.supply_crate_tex);
    private final RenderType chute = CompatRenderTypes.entityCutoutCull(ResourceManager.soyuz_chute_tex);

    public RenderParachuteCrate(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityParachuteCrate entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityParachuteCrate entity, State state, float partialTicks) {
        double time = entity.level().getGameTime();
        state.swingZ = (float) (Math.sin(time * 0.05) * 5);
        state.swingX = (float) (Math.sin(time * 0.05 + Math.PI * 0.5) * 5);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();

        pose.translate(0, PIVOT_HEIGHT, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(state.swingZ));
        pose.mulPose(Axis.XP.rotationDegrees(state.swingX));
        pose.translate(0, -PIVOT_HEIGHT, 0);

        sink.submitCustomGeometry(pose, crate, (p, buf) ->
                NtmEntityModels.CONSERVE_CRATE.renderAll(p, buf, light, OverlayTexture.NO_OVERLAY));

        pose.translate(0, -1, 0);
        sink.submitCustomGeometry(pose, chute, (p, buf) ->
                NtmEntityModels.SOYUZ_LANDER.renderPart("Chute", p, buf, light, OverlayTexture.NO_OVERLAY));

        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float swingZ;
        public float swingX;
    }
}
