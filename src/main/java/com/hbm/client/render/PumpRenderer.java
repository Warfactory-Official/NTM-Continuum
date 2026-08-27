package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
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

/** Faithful split-part renderer for the legacy electric pump OBJ. */
public final class PumpRenderer implements CompatBER<TileEntityMachinePumpElectric, PumpRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/pump.obj"));
    private final RenderType renderType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/pump_electric.png"));

    @Override public PumpRenderState createRenderState() { return new PumpRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachinePumpElectric be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachinePumpElectric be, PumpRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case SOUTH -> 90F;
            case EAST -> 180F;
            case NORTH -> 270F;
            default -> 0F;
        };
        state.rotation = be.lastRotor + (be.rotor - be.lastRotor) * partialTicks;
    }

    @Override
    public void submit(PumpRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(sink, poseStack, "Base", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 2.25F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation - 90F));
        poseStack.translate(0F, -2.25F, 0F);
        part(sink, poseStack, "Rotor", state.lightCoords);
        poseStack.popPose();

        double sin = Math.sin(state.rotation * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(state.rotation * Math.PI / 180D) * 0.5D;
        double angle = Math.acos(cos / 2D) * 180D / Math.PI - 90D;
        double cath = Math.sqrt(1D + (cos * cos) / 2D);

        poseStack.pushPose();
        poseStack.translate(0D, 1D - cath + sin, 0D);
        poseStack.translate(0D, 4.75D, 0D);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) angle));
        poseStack.translate(0D, -4.75D, 0D);
        part(sink, poseStack, "Arms", state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, 1D - cath + sin, 0D);
        part(sink, poseStack, "Piston", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
