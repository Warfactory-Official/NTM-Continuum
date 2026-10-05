package com.hbm.compat.happyghast.client;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.compat.happyghast.EntityRiggedFatMan;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;

/**
 * The rigged Fat Man: the block's own OBJ and texture ({@code bombs/fatman}, as {@code BombObjRenderer} draws them), at
 * the block's origin and yaw, so lifting the bomb off its block does not move it by a pixel. A plain vanilla renderer,
 * not a {@code CompatEntityRenderer}: vanilla's {@code submit} is what draws the leash ropes, and the compat bridge
 * replaces it.
 */
public class RenderRiggedFatMan extends EntityRenderer<EntityRiggedFatMan, RenderRiggedFatMan.State> {

    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/bombs/fatman.obj"));
    private static final RenderType TYPE =
            CompatRenderTypes.entityCutout(Ids.of(Constants.MOD_ID, "textures/models/bombs/fatman.png"));

    /** The point it noses over about when dropped: the middle of the body (model y 0..1.59). */
    private static final float TIP_PIVOT_Y = 0.75F;

    public RenderRiggedFatMan(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRiggedFatMan entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot(partialTicks);
        state.tip = entity.getXRot(partialTicks);
    }

    /** The model reaches 2 blocks past the centre (the fins); the hitbox stops at 0.8. */
    @Override
    protected AABB getBoundingBoxForCulling(EntityRiggedFatMan entity) {
        return entity.getBoundingBox().inflate(1.4, 0.2, 1.4);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        // -yRot is the turn vanilla gives the leash attachment points (Vec3.yRot(-yRot)), so the ropes stay on the body
        // as it swings round; with MODEL_YAW after it, a freshly rigged bomb sits at its block's yaw (BombObjRenderer)
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        // the model's own quarter turn; the rope ends are turned by the same amount (EntityRiggedFatMan.MODEL_YAW)
        poseStack.mulPose(Axis.YP.rotationDegrees(EntityRiggedFatMan.MODEL_YAW));
        if (state.tip != 0F) {
            // nose (-X) down: +Z turns +X towards +Y
            poseStack.translate(0F, TIP_PIVOT_Y, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.tip));
            poseStack.translate(0F, -TIP_PIVOT_Y, 0F);
        }
        collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) ->
                MODEL.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera); // the leash ropes
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float tip;
    }
}
