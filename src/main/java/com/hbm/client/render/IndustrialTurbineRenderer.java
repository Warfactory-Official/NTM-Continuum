package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
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

/** Animated renderer for the industrial turbine, matching the old three-part TESR. */
public final class IndustrialTurbineRenderer
        implements CompatBER<TileEntityMachineIndustrialTurbine, IndustrialTurbineRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public IndustrialTurbineRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID,
                "models/machines/industrial_turbine.obj"));
        renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/industrial_turbine.png"));
    }

    @Override public IndustrialTurbineRenderState createRenderState() { return new IndustrialTurbineRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineIndustrialTurbine be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineIndustrialTurbine be, IndustrialTurbineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = 180F + switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.gauge = 135F - (be.tanks[0].getTankType().getID() - Fluids.STEAM.getID()) * 90F;
        state.rotor = be.lastRotor + (be.rotor - be.lastRotor) * partialTicks;
    }

    @Override
    public void submit(IndustrialTurbineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(sink, poseStack, "Turbine", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.gauge));
        poseStack.translate(0F, -1.5F, 0F);
        part(sink, poseStack, "Gauge", state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.rotor));
        poseStack.translate(0F, -1.5F, 0F);
        part(sink, poseStack, "Flywheel", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
