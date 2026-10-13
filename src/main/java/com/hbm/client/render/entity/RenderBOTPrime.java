package com.hbm.client.render.entity;

import com.hbm.client.BossStatus;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBody;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.botprime.EntityWormBaseNT;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

/**
 * 1.7 {@code RenderWormHead} / {@code RenderWormBody}: the same routine twice over with a different mesh and
 * sheet, so it is one class here. The mesh points down its own heading, which the entity keeps in its yaw and
 * pitch rather than in its motion.
 */
public class RenderBOTPrime<T extends EntityWormBaseNT> extends CompatEntityRenderer<T, RenderBOTPrime.State> {

    private final WavefrontObj mesh;
    private final RenderType type;

    private RenderBOTPrime(EntityRendererProvider.Context context, WavefrontObj mesh, Identifier texture) {
        super(context);
        this.mesh = mesh;
        this.type = CompatRenderTypes.entityCutoutNoCull(texture);
        this.shadowStrength = 0F;
    }

    public static RenderBOTPrime<EntityBOTPrimeHead> head(EntityRendererProvider.Context context) {
        return new RenderBOTPrime<>(context, NtmEntityModels.BOT_PRIME_HEAD, ResourceManager.mark_zero_head_tex);
    }

    public static RenderBOTPrime<EntityBOTPrimeBody> body(EntityRendererProvider.Context context) {
        return new RenderBOTPrime<>(context, NtmEntityModels.BOT_PRIME_BODY, ResourceManager.mark_zero_body_tex);
    }

    @Override
    public boolean culled(T entity) {
        return false; // 1.7 ignoreFrustumCheck on the head, and a segment is never far from it
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        // 1.7 RenderWormHead pushed BossStatus; the body segments did not
        if (entity instanceof EntityBOTPrimeHead)
            BossStatus.set(entity, BossEvent.BossBarColor.PINK); // 1.7 render-pushed BossStatus
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch - 90F));
        sink.submitCustomGeometry(poseStack, this.type, (pose, buffer) ->
                this.mesh.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
    }
}
