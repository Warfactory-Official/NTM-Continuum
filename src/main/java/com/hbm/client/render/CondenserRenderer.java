package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The animated condenser OBJ renderer, ported from the old twin-fan TESR. */
public final class CondenserRenderer implements CompatBER<TileEntityCondenserPowered, CondenserRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public CondenserRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/condenser.obj"));
        renderType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/condenser.png"));
    }

    @Override public CondenserRenderState createRenderState() { return new CondenserRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityCondenserPowered be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityCondenserPowered be, CondenserRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        ForgeDirection facing = ForgeDirection.getOrientation(meta - BlockDummyable.offset);
        state.yaw = 90F + facing.getRotationDegrees();
        state.spin = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
    }

    @Override
    public void submit(CondenserRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(sink, poseStack, "Condenser", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.spin));
        poseStack.translate(0F, -1.5F, 0F);
        part(sink, poseStack, "Fan1", state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.spin));
        poseStack.translate(0F, -1.5F, 0F);
        part(sink, poseStack, "Fan2", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
