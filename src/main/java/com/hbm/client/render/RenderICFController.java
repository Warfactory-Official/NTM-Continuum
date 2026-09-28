package com.hbm.client.render;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderICFController}: while the laser fires, a dark red spiral beam from the controller's centre out
 * to where the beam stopped. The controller block itself is an ordinary block model.
 */
public final class RenderICFController extends GemBER<TileEntityICFController, RenderICFController.State> {

    public static final class State extends CompatRenderState {
        public int laserLength;
        public float yaw;
    }

    /** 1.7 {@code getRenderBoundingBox}: 50 blocks either way, the beam's reach. */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityICFController controller, State state, float partialTicks, Vec3 cameraPosition) {
        state.laserLength = controller.laserLength;
        state.yaw = switch (ForgeDirection.getOrientation(MachineCore.facingOrdinal(controller.getBlockState(), 2))) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F; // NORTH
        };
    }

    /** The beam's length is how far it got, so it is built, not declared. */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.laserLength <= 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        // unlit, full-bright, untextured: BeamPronter's own render type
        BeamPronter.prontBeam(poseStack, sink, new Vec3(state.laserLength, 0, 0), BeamPronter.EnumWaveType.SPIRAL,
                BeamPronter.EnumBeamType.SOLID, 0, 1, 0F, 0x202020, 0x100000, 10, 0.125F);
        poseStack.popPose();
    }
}
