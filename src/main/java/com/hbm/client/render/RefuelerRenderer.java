package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockRefueler;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Refueler: the static body, and the fuel level glowing inside it. */
public class RefuelerRenderer extends GemBER<TileEntityRefueler, RefuelerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/refueler.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/refueler.png");
    /** The old pass disabled texturing and used SRC_ALPHA/ONE additive blending. */
    private final RenderType fluidType = CompatRenderTypes.lightning();

    @Override
    protected void setup(Gem.Model<RefuelerRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.part("Fueler"), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public RefuelerRenderState createRenderState() {
        return new RefuelerRenderState();
    }

    /** Old: a base rotate(90) then the meta map W(4)=90, S(3)=180, E(5)=270, N(2)=0. */
    @Override
    public void extract(TileEntityRefueler be, RefuelerRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(BlockRefueler.FACING) ? bs.getValue(BlockRefueler.FACING) : Direction.NORTH;
        state.yaw = 90F + switch (facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.fillLevel = be.prevFillLevel + (be.fillLevel - be.prevFillLevel) * partialTicks;
        state.fluidColor = be.tank.getTankType().getColor();
    }

    /**
     * The fuel column is drawn with position and colour only, with no texture and no normal, so it is not a
     * surface either backend could bucket, and it keeps the vanilla path.
     */
    @Override
    protected void submitUndeclared(RefuelerRenderState state, PoseStack poseStack, SubmitSink collector,
                                    CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0F, (float) ((1D - state.fillLevel) * -0.625D), 0F);
        int argb = 0xBF000000 | (state.fluidColor & 0xFFFFFF);
        collector.submitCustomGeometry(poseStack, fluidType,
                (pose, buffer) -> model.renderPartColor("Fluid", pose, buffer, argb));
        poseStack.popPose();
    }
}
