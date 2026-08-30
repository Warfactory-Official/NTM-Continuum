package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
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

/** Static OBJ renderer for the legacy gas turbine. */
public final class GasTurbineRenderer implements CompatBER<TileEntityMachineTurbineGas, GasTurbineRenderState> {
    private final WavefrontObj model;
    private final RenderType renderType;

    public GasTurbineRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/turbinegas.obj"));
        renderType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbinegas.png"));
    }

    @Override public GasTurbineRenderState createRenderState() { return new GasTurbineRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineTurbineGas be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineTurbineGas be, GasTurbineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }

    @Override
    public void submit(GasTurbineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, true));
        poseStack.popPose();
    }
}
