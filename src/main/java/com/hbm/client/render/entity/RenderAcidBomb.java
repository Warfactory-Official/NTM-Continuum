package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityAcidBomb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The glyphid bombardier's acid glob: a slime ball drawn at half size facing the camera. */
public final class RenderAcidBomb extends CompatEntityRenderer<EntityAcidBomb, RenderAcidBomb.State> {

    public RenderAcidBomb(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityAcidBomb entity, State state, float partialTicks) {
        state.item.extract(entity.level(), new ItemStack(Items.SLIME_BALL), ItemDisplayContext.GROUND);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose(camera.orientation);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        sink.submitItem(pose, state.item, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        final CompatItemRenderState item = new CompatItemRenderState();
    }
}
