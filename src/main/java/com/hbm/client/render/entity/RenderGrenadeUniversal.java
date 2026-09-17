package com.hbm.client.render.entity;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 1.7 RenderGrenadeUniversal: the thrown grenade tumbling about its heading, tipped over once it has bounced.
 * 1.7 drew the grenade's assembled shell mesh; this port's grenade items carry no mesh renderer yet, so the
 * projectile draws the grenade item itself under the same pose chain.
 */
public final class RenderGrenadeUniversal extends CompatEntityRenderer<EntityGrenadeUniversal, RenderGrenadeUniversal.State> {

    public RenderGrenadeUniversal(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityGrenadeUniversal entity, State state, float partialTicks) {
        state.item.extract(entity.level(), entity.getGrenadeItem(), ItemDisplayContext.GROUND);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.spin = (float) Mth.lerp(partialTicks, entity.prevSpin, entity.spin);
        state.bounces = entity.getBounces();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.spin));
        if (state.bounces > 0) pose.mulPose(Axis.ZP.rotationDegrees(-80F));
        sink.submitItem(pose, state.item, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        final CompatItemRenderState item = new CompatItemRenderState();
        float yaw;
        float spin;
        int bounces;
    }
}
