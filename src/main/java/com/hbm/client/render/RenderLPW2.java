package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineLPW2;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderLPW2}, part for part. Every motion is a function of world time, so the state is a bundle
 * of curves sampled once per frame and the parts read them. The screen is the one thing drawn outside the
 * declaration: 1.7 scrolls its {@code lpw2_term_error} texture through the texture matrix, and an offset
 * scroll is a {@code WavefrontObj.renderPart} argument rather than a Gem op.
 */
public final class RenderLPW2 extends GemBER<TileEntityMachineLPW2, LPW2RenderState> {

    private static final float MAGNITUDE = 0.125F;
    private static final float ROTATION = 5F;
    private static final float OFFSET = 10F;
    private static final float LENGTH = 0.6875F;
    private static final float SERVER_SWAY = 0.0625F * 0.25F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/lpw2.obj"));
    private final Surface surface = Surface.cutoutCull(
            Constants.MOD_ID + ":textures/models/machines/lpw2.png");
    private final RenderType screen = CompatRenderTypes.entityCutoutCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lpw2_term_error.png"));

    @Override
    protected void setup(Gem.Model<LPW2RenderState> declaration) {
        Gem.Part<LPW2RenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Frame"), surface);

        // --- renderMainAssembly ---
        Gem.Part<LPW2RenderState> assembly = machine.joint().slide(Gem.Axes.Z, state -> -state.sway * 0.125F);
        assembly.child(model.part("Center"), surface);
        assembly.child(model.part("Rotor"), surface).spin(Gem.Axes.Z, 0F, 3.5F, 0F, state -> -state.rotor * 360F);
        assembly.child(model.part("TurbineFront"), surface).spin(Gem.Axes.Z, 0F, 3.5F, 0F, state -> state.turbine * 360F);
        assembly.child(model.part("TurbineBack"), surface).spin(Gem.Axes.Z, 0F, 3.5F, 0F, state -> -state.turbine * 360F);
        assembly.child(model.part("Piston"), surface).slide(Gem.Axes.Z, state -> state.piston * 0.375F + 0.375F);
        // renderBell
        assembly.child(model.part("Engine"), surface)
                .translate(0F, 3.5F, 2.75F)
                .angle(Gem.Axes.Y, state -> state.v * 2F)
                .angle(Gem.Axes.X, state -> state.h * 2F)
                .translate(0F, -3.5F, -2.75F);

        // --- renderShroud (outside the sway) ---
        Gem.Part<LPW2RenderState> shroudH = machine.joint().slide(Gem.Axes.Y, state -> -state.h * MAGNITUDE);
        shroudH.child(model.part("ShroudH"), surface);
        flap(shroudH, 90F + 22.5F, state -> ROTATION * state.v + OFFSET);
        flap(shroudH, 90F - 22.5F, state -> ROTATION * state.v + OFFSET);
        flap(shroudH, 270F + 22.5F, state -> ROTATION * -state.v + OFFSET);
        flap(shroudH, 270F - 22.5F, state -> ROTATION * -state.v + OFFSET);

        Gem.Part<LPW2RenderState> shroudV = machine.joint().slide(Gem.Axes.X, state -> state.v * MAGNITUDE);
        shroudV.child(model.part("ShroudV"), surface);
        flap(shroudV, 22.5F, state -> ROTATION * state.h + OFFSET);
        flap(shroudV, -22.5F, state -> ROTATION * state.h + OFFSET);
        flap(shroudV, 180F + 22.5F, state -> ROTATION * -state.h + OFFSET);
        flap(shroudV, 180F - 22.5F, state -> ROTATION * -state.h + OFFSET);

        machine.child(model.part("SuspensionLeft"), surface)
                .translate(-2.625F, 0F, 0F)
                .stretch(Gem.Axes.X, state -> (LENGTH + state.v * MAGNITUDE) / LENGTH)
                .translate(2.625F, 0F, 0F);
        machine.child(model.part("SuspensionRight"), surface)
                .translate(2.625F, 0F, 0F)
                .stretch(Gem.Axes.X, state -> (LENGTH - state.v * MAGNITUDE) / LENGTH)
                .translate(-2.625F, 0F, 0F);
        machine.child(model.part("SuspensionTop"), surface)
                .translate(0F, 6.125F, 0F)
                .stretch(Gem.Axes.Y, state -> (LENGTH + state.h * MAGNITUDE) / LENGTH)
                .translate(0F, -6.125F, 0F);
        machine.child(model.part("SuspensionBottom"), surface)
                .translate(0F, 0.875F, 0F)
                .stretch(Gem.Axes.Y, state -> (LENGTH - state.h * MAGNITUDE) / LENGTH)
                .translate(0F, -0.875F, 0F);

        // --- wires ---
        machine.child(model.part("WireLeft"), surface)
                .translate(-2.9375F, 0F, 2.375F)
                .angle(Gem.Axes.Y, state -> state.sway * 10F)
                .translate(2.9375F, 0F, -2.375F);
        machine.child(model.part("WireRight"), surface)
                .translate(2.9375F, 0F, 2.375F)
                .angle(Gem.Axes.Y, state -> state.sway * -10F)
                .translate(-2.9375F, 0F, -2.375F);

        // --- covers and back suspension ---
        machine.child(model.part("Cover"), surface).slide(Gem.Axes.Z, state -> -state.cover * 0.125F);
        machine.child(model.part("SuspensionCoverFront"), surface)
                .translate(0F, 0F, 3.5F)
                .stretch(Gem.Axes.Z, state -> (3F + state.cover * 0.125F) / 3F)
                .translate(0F, 0F, -3.5F);
        machine.child(model.part("SuspensionCoverBack"), surface)
                .translate(0F, 0F, -5.5F)
                .stretch(Gem.Axes.Z, state -> (1.5F - state.cover * 0.125F) / 1.5F)
                .translate(0F, 0F, 5.5F);
        machine.child(model.part("SuspensionBackOuter"), surface)
                .translate(0F, 0F, -9F)
                .stretch(Gem.Axes.Z, state -> (1.25F - state.sway * 0.125F) / 1.25F)
                .translate(0F, 0F, 9F);
        machine.child(model.part("SuspensionBackCenter"), surface)
                .translate(0F, 0F, -9.5F)
                .stretch(Gem.Axes.Z, state -> (1.75F - state.sway * 0.125F) / 1.75F)
                .translate(0F, 0F, 9.5F);

        // --- servers ---
        machine.child(model.part("Server1"), surface)
                .slide(Gem.Axes.X, state -> state.sx * SERVER_SWAY).slide(Gem.Axes.Z, state -> state.sy * SERVER_SWAY);
        machine.child(model.part("Server2"), surface)
                .slide(Gem.Axes.X, state -> -state.sy * SERVER_SWAY).slide(Gem.Axes.Z, state -> state.sx * SERVER_SWAY);
        machine.child(model.part("Server3"), surface)
                .slide(Gem.Axes.X, state -> state.sy * SERVER_SWAY).slide(Gem.Axes.Z, state -> -state.sx * SERVER_SWAY);
        machine.child(model.part("Server4"), surface)
                .slide(Gem.Axes.X, state -> -state.sx * SERVER_SWAY).slide(Gem.Axes.Z, state -> -state.sy * SERVER_SWAY);
        machine.child(model.part("Monitor"), surface)
                .slide(Gem.Axes.X, state -> state.sy * SERVER_SWAY).slide(Gem.Axes.Z, state -> state.sx * SERVER_SWAY);
    }

    /** 1.7 {@code renderFlap}: turned about Z at y=3.5 into position, then hinged about X at (6.96875, 8.5). */
    private void flap(Gem.Part<LPW2RenderState> parent, float position, Gem.FloatOf<LPW2RenderState> hinge) {
        parent.child(model.part("Flap"), surface)
                .translate(0F, 3.5F, 0F)
                .rotate(Gem.Axes.Z, position)
                .translate(0F, -3.5F, 0F)
                .translate(0F, 6.96875F, 8.5F)
                .angle(Gem.Axes.X, hinge)
                .translate(0F, -6.96875F, -8.5F);
    }

    @Override
    public LPW2RenderState createRenderState() {
        return new LPW2RenderState();
    }

    @Override
    public void extract(TileEntityMachineLPW2 be, LPW2RenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };

        long time = be.getLevel() == null ? 0L : be.getLevel().getGameTime() % 1000000;
        // world time + partialTicks for HBM's own tile; an addon's engine may run its own clock (hbm-space's spin-up)
        double t = be.animationTime(partialTicks);

        double swayTimer = (t / 3D) % (Math.PI * 4);
        state.sway = (float) ((Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5);

        double bellTimer = (t / 5D) % (Math.PI * 4);
        state.h = (float) ((Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D);
        state.v = (float) ((Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D);

        double pistonTimer = (t / 5D) % (Math.PI * 2);
        state.piston = (float) sps(pistonTimer);
        double rotorTimer = (t / 5D) % (Math.PI * 16);
        state.rotor = (float) ((sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D);
        state.turbine = (float) ((t % 100) / 100D);

        double coverTimer = (t / 5D) % (Math.PI * 4);
        state.cover = (float) ((Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5);

        double serverTimer = (t / 2D) % (Math.PI * 4);
        state.sx = (float) ((Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D);
        state.sy = (float) ((Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D);

        double errorTimer = (time + partialTicks) / 3D;
        state.errorScroll = (float) ((sps(errorTimer) + errorTimer / 2D) % 1);
    }

    /** The monitor's screen: the error texture scrolled by the texture matrix, riding the monitor's sway. */
    @Override
    protected void submitUndeclared(LPW2RenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(state.sy * SERVER_SWAY, 0F, state.sx * SERVER_SWAY);
        int light = state.lightCoords;
        float scroll = state.errorScroll;
        sink.submitCustomGeometry(poseStack, screen, (pose, buffer) ->
                model.renderPart("Screen", pose, buffer, light, OverlayTexture.NO_OVERLAY, -1, 0F, scroll, false));
        poseStack.popPose();
    }

    /** 1.7 {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }
}
