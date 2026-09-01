package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exact modern BER equivalent of the old RenderAutosaw part order and transforms. */
public final class AutosawRenderer implements CompatBER<TileEntityMachineAutosaw, AutosawRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/autosaw.obj"));
    private final RenderType renderType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/autosaw.png"));

    @Override
    public AutosawRenderState createRenderState() {
        return new AutosawRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityMachineAutosaw blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityMachineAutosaw blockEntity, AutosawRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        state.yaw = blockEntity.prevRotationYaw
                + (blockEntity.rotationYaw - blockEntity.prevRotationYaw) * partialTicks;
        state.angle = 80F - (blockEntity.prevRotationPitch
                + (blockEntity.rotationPitch - blockEntity.prevRotationPitch) * partialTicks);
        state.spin = blockEntity.lastSpin
                + (blockEntity.spin - blockEntity.lastSpin) * partialTicks;
        Level level = blockEntity.getLevel();
        state.engine = blockEntity.isOn && level != null
                ? Math.sin(level.getGameTime() * 2D % (Math.PI * 2D) + partialTicks) : 0D;
    }

    @Override
    public void submit(AutosawRenderState state, PoseStack poseStack, SubmitSink collector,
                       CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        part(poseStack, collector, "Base", state.lightCoords);

        // GlStateManager.rotate(turn, 0, -1, 0) is a negative Y rotation.
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        part(poseStack, collector, "Main", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0D, state.engine * 0.01D, 0D);
        part(poseStack, collector, "Engine", state.lightCoords);
        poseStack.popPose();

        poseStack.translate(0D, 1.75D, 0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.angle));
        poseStack.translate(0D, -1.75D, 0D);
        part(poseStack, collector, "ArmUpper", state.lightCoords);

        poseStack.translate(0D, 1.75D, -4D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.angle * 2F));
        poseStack.translate(0D, -1.75D, 4D);
        poseStack.translate(-0.01D, 0D, 0D);
        part(poseStack, collector, "ArmLower", state.lightCoords);
        poseStack.translate(0.01D, 0D, 0D);

        poseStack.translate(0D, 1.75D, -8D);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.angle));
        poseStack.translate(0D, -1.75D, 8D);
        part(poseStack, collector, "ArmTip", state.lightCoords);

        poseStack.translate(0D, 1.75D, -10D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.spin));
        poseStack.translate(0D, -1.75D, 10D);
        part(poseStack, collector, "Sawblade", state.lightCoords);
        poseStack.popPose();
    }

    private void part(PoseStack poseStack, SubmitSink collector, String name, int light) {
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
