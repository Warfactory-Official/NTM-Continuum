package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.IntakeFanDirections;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ renderer for the two-by-two legacy air intake. */
public final class IntakeRenderer implements CompatBER<TileEntityMachineIntake, IntakeRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/intake.obj"));
    private final RenderType renderType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/intake.png"));

    @Override public IntakeRenderState createRenderState() { return new IntakeRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineIntake be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineIntake be, IntakeRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.yaw = IntakeFanDirections.intakeYaw(be.getFacing());
        state.rotation = be.prevFan + (be.fan - be.prevFan) * partialTicks;
    }

    @Override
    public void submit(IntakeRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(-0.5F, 0F, 0.5F);
        part(sink, poseStack, "Base", state.lightCoords);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        part(sink, poseStack, "Fan", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
