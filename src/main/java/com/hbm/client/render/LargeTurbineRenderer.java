package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
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

/** Animated renderer for the legacy four-long steam turbine. */
public final class LargeTurbineRenderer
        implements CompatBER<TileEntityMachineLargeTurbine, LargeTurbineRenderState> {

    private final WavefrontObj model;
    private final RenderType bodyType;
    private final RenderType bladesType;

    public LargeTurbineRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/turbine.obj"));
        bodyType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbine.png"));
        bladesType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbofan_blades.png"));
    }

    @Override public LargeTurbineRenderState createRenderState() { return new LargeTurbineRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineLargeTurbine be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineLargeTurbine be, LargeTurbineRenderState state,
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
        state.rotor = be.getRotor(partialTicks);
    }

    @Override
    public void submit(LargeTurbineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0F, 0F, -1F);
        part(sink, poseStack, bodyType, "Body", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 1F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotor));
        poseStack.translate(0F, -1F, 0F);
        part(sink, poseStack, bladesType, "Blades", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, RenderType type, String name, int light) {
        sink.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light,
                        OverlayTexture.NO_OVERLAY, -1, 0F, 0F, true));
    }
}
