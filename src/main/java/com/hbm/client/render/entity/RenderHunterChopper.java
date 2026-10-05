package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderHunterChopper: a bare {@code Render}, not a {@code RenderLiving}, so the chopper gets none of the living
 * frame - no body yaw, no hurt flash, no death tilt. The 2.75 block lift, the 4x scale and the X-180 flip are 1.7's
 * chain verbatim, with the interpolated yaw applied about Y and the pitch about Z after it.
 *
 * <p>1.7's boss bar came from {@code BossStatus.setBossStatus} here; the ported entity drives a {@code ServerBossEvent}
 * itself. 1.7's {@code ignoreFrustumCheck} is {@link #culled}, which the rotor disc needs - it spans some fifteen
 * blocks against an 8.25 block hitbox.
 */
public class RenderHunterChopper extends CompatEntityRenderer<EntityHunterChopper, RenderHunterChopper.State> {

    private static final RenderType TYPE = CompatRenderTypes.entityCutoutCull(ResourceManager.chopper_tex);

    private final ModelHunterChopper model;

    public RenderHunterChopper(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ModelHunterChopper(ModelHunterChopper.createBodyLayer().bakeRoot());
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityHunterChopper entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityHunterChopper entity, State state, float partialTicks) {
        com.hbm.client.BossStatus.set(entity, net.minecraft.world.BossEvent.BossBarColor.PINK); // 1.7 render-pushed BossStatus
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0F, 2F, 0F);
        poseStack.translate(0F, 0.75F, 0F);
        poseStack.scale(4F, 4F, 4F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        int light = state.lightCoords;
        float age = state.age;
        // The rotors are posed inside the draw callback: the part tree is shared by every chopper this
        // renderer draws, and the callback is the one moment the pose and the draw are adjacent.
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> {
            model.pose(age);
            ModelParts.render(pose, model.root(), buffer, light, OverlayTexture.NO_OVERLAY);
        });
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
        float age;
    }
}
