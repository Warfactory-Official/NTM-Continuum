package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityChungus;
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

/** Animated renderer for the Leviathan turbine, matching the old three-part TESR. */
public final class ChungusRenderer implements CompatBER<TileEntityChungus, ChungusRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public ChungusRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/chungus.obj"));
        renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/chungus.png"));
    }

    @Override public ChungusRenderState createRenderState() { return new ChungusRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityChungus be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityChungus be, ChungusRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
                state.yaw = 180F + switch (MachineCore.facing(blockState)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.lever = 15F - grade(be.tanks[0].getTankType()) * 10F;
        state.rotor = be.lastRotor + (be.rotor - be.lastRotor) * partialTicks;
    }

    /** Which notch of the compressor lever this input steam corresponds to, 0 (steam) to 3 (ultra dense). */
    private static int grade(FluidType type) {
        if (type == Fluids.HOTSTEAM) return 1;
        if (type == Fluids.SUPERHOTSTEAM) return 2;
        if (type == Fluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    @Override
    public void submit(ChungusRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0F, 0F, -3F);
        part(sink, poseStack, "Body", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 0F, 4.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.lever));
        poseStack.translate(0F, 0F, -4.5F);
        part(sink, poseStack, "Lever", state.lightCoords);
        poseStack.popPose();

        poseStack.translate(0F, 2.5F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.rotor));
        poseStack.translate(0F, -2.5F, 0F);
        part(sink, poseStack, "Blades", state.lightCoords);
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
