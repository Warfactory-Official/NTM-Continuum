package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Six-directional OBJ renderer for the redstone fan. */
public final class FanRenderer implements CompatBER<MachineFan.TileEntityFan, FanRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/fan.obj"));
    private final RenderType renderType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/fan.png"));

    @Override public FanRenderState createRenderState() { return new FanRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(MachineFan.TileEntityFan be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(MachineFan.TileEntityFan be, FanRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        state.facing = blockState.hasProperty(MachineFan.FACING)
                ? blockState.getValue(MachineFan.FACING) : Direction.NORTH;
        state.rotation = be.prevSpin + (be.spin - be.prevSpin) * partialTicks;
    }

    @Override
    public void submit(FanRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.translate(0F, 0.5F, 0F);
        switch (state.facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90F));
            case UP -> { }
        }
        poseStack.translate(0F, -0.5F, 0F);
        part(sink, poseStack, "Frame", state.lightCoords);

        poseStack.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        part(sink, poseStack, "Blades", state.lightCoords);
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
