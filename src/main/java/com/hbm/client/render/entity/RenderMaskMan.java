package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityMaskMan;
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
import net.minecraft.util.Mth;

/**
 * 1.7 {@code RenderMaskMan} plus its {@code ModelMaskMan}: seven named OBJ parts posed by hand off the limb swing.
 * 1.7 split them across a Render and a ModelBase only because the vanilla mob renderer demanded a model; there is
 * nothing in the model half but the same transform chain, so it lives here.
 *
 * <p>Below half health the head is swapped for the skull, and the IOU note is drawn over it on its own sheet.
 */
public class RenderMaskMan extends CompatEntityRenderer<EntityMaskMan, RenderMaskMan.State> {

    private static final RenderType BODY = CompatRenderTypes.entityCutoutNoCull(ResourceManager.maskman_tex);
    private static final RenderType IOU = CompatRenderTypes.entityCutoutNoCull(ResourceManager.iou_tex);

    public RenderMaskMan(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityMaskMan entity, State state, float partialTicks) {
        // 1.7 read limbSwing/limbSwingAmount straight off the entity inside the model
        float swing = entity.walkAnimation.position(partialTicks);
        float amount = entity.walkAnimation.speed(partialTicks) * 0.5F;
        state.swing = (float) Math.toDegrees(Mth.cos(swing / 2F + (float) Math.PI) * 1.4F * amount);

        state.headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.getYHeadRot())
                - Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.wounded = entity.getHealth() < entity.getMaxHealth() / 2F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;

        poseStack.pushPose();
        // ModelMaskMan opened with rotate(180,1,0,0) + translate(0,-1.5,0), but that is not a transform: it is the
        // model undoing the scale(-1,-1,1) + translate(0,-1.5,0) RendererLivingEntity had already applied. The pair
        // cancels to a 180 turn, so the whole inherited chain nets out to yaw plus the model's own quarter turn.
        poseStack.mulPose(Axis.YP.rotationDegrees(270F - state.bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.swing * -0.1F));

        part(poseStack, sink, BODY, "Torso", light);

        limb(poseStack, sink, "LLeg", light, -0.5F, 1.75F, -0.5F, state.swing);
        limb(poseStack, sink, "RLeg", light, -0.5F, 1.75F, 0.5F, -state.swing);
        limb(poseStack, sink, "LArm", light, -0.5F, 3.75F, -1.5F, state.swing * 0.25F);
        limb(poseStack, sink, "RArm", light, -0.5F, 3.75F, 1.5F, state.swing * -0.25F);

        poseStack.pushPose();
        poseStack.translate(0.5F, 4F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.headYaw));

        if (state.wounded) {
            part(poseStack, sink, BODY, "Skull", light);
            part(poseStack, sink, IOU, "IOU", light);
        } else {
            part(poseStack, sink, BODY, "Head", light);
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void limb(PoseStack poseStack, SubmitSink sink, String part, int light,
                             float x, float y, float z, float roll) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        part(poseStack, sink, BODY, part, light);
        poseStack.popPose();
    }

    private static void part(PoseStack poseStack, SubmitSink sink, RenderType type, String part, int light) {
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                NtmEntityModels.MASK_MAN.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    public static final class State extends CompatEntityRenderState {
        float swing;
        float headYaw;
        float bodyYaw;
        boolean wounded;
    }
}
