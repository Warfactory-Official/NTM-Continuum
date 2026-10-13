package com.hbm.client.render.entity;

import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.entity.train.EntityRailCarCargo;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.7 {@code RenderTrainCargoTram}: the mesh sits at the midpoint between the two bogies, which is not where
 * the entity is on a curve, so the renderer subtracts the difference between the interpolated entity position
 * and the interpolated bogie midpoint.
 */
public class RenderTrainCargoTram<T extends EntityRailCarBase> extends CompatEntityRenderer<T, RenderTrainCargoTram.State> {

    private final WavefrontObj mesh;
    private final RenderType type;

    public RenderTrainCargoTram(EntityRendererProvider.Context context) {
        this(context, NtmEntityModels.TRAIN_CARGO_TRAM, ResourceManager.train_tram_tex);
    }

    protected RenderTrainCargoTram(EntityRendererProvider.Context context, WavefrontObj mesh, Identifier texture) {
        super(context);
        this.mesh = mesh;
        this.type = CompatRenderTypes.entityCutoutNoCull(texture);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        state.offsetX = Mth.lerp(partialTicks, entity.xo, entity.getX())
                - Mth.lerp(partialTicks, entity.lastRenderX, entity.renderX);
        state.offsetY = Mth.lerp(partialTicks, entity.yo, entity.getY())
                - Mth.lerp(partialTicks, entity.lastRenderY, entity.renderY);
        state.offsetZ = Mth.lerp(partialTicks, entity.zo, entity.getZ())
                - Mth.lerp(partialTicks, entity.lastRenderZ, entity.renderZ);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.occupiedSlots = entity instanceof EntityRailCarCargo cargo ? cargo.getOccupiedSlots() : 0;
        extractCargo(entity, state, partialTicks);
    }

    /** The tram itself carries nothing; the trailer resolves its crate stack. */
    protected void extractCargo(T entity, State state, float partialTicks) {}

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(-state.offsetX, -state.offsetY, -state.offsetZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));

        int light = state.lightCoords;
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                mesh.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

        submitCargo(state, poseStack, sink);

        poseStack.popPose();
    }

    /** The tram itself draws nothing on top; the trailer stacks crates. */
    protected void submitCargo(State state, PoseStack poseStack, SubmitSink sink) { }

    public static class State extends CompatEntityRenderState {
        public double offsetX, offsetY, offsetZ;
        public float yaw, pitch;
        public int occupiedSlots;
        public final CompatItemRenderState crate = new CompatItemRenderState();
    }
}
