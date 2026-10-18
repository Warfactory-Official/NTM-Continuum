package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderSuperComputer}: the cabinets, and the scanning lights, full-bright and black while idle. */
public class SuperComputerRenderer extends GemBER<TileEntityMachineSuperComputer, SuperComputerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/supercomputer.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/supercomputer.png");
    private final RenderType lights = CompatRenderTypes.flatCutout(Ids.of(Constants.MOD_ID, "textures/models/machines/supercomputer_scan.png"));

    @Override
    protected void setup(Gem.Model<SuperComputerRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(model.part("Computer"), surface);
    }

    @Override
    public SuperComputerRenderState createRenderState() {
        return new SuperComputerRenderState();
    }

    @Override
    public void extract(TileEntityMachineSuperComputer be, SuperComputerRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 3)) {
            case 2 -> 180F;
            case 4 -> 270F;
            case 5 -> 90F;
            default -> 0F;
        };
        state.running = be.didProcess;
        state.scroll = (be.getLevel().getGameTime() % 20 + partialTicks) / 20F;
    }

    /** The lights scroll their texture every frame, so they are drawn here rather than declared. */
    @Override
    protected void submitUndeclared(SuperComputerRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int color = state.running ? 0xFFFFFFFF : 0xFF000000;
        sink.submitCustomGeometry(poseStack, lights, (pose, buffer) ->
                model.renderPart("Lights", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, color, -state.scroll, 0F, false));
        poseStack.popPose();
    }
}
