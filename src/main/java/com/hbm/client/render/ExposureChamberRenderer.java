package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Animated renderer for the legacy Exposure Chamber OBJ and its energized particle beams. */
public final class ExposureChamberRenderer
        extends GemBER<TileEntityMachineExposureChamber, ExposureChamberRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/exposure_chamber.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/exposure_chamber.png");

    @Override
    protected void setup(Gem.Model<ExposureChamberRenderState> declaration) {
        Gem.Part<ExposureChamberRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Chamber"), surface);
        machine.child(model.part("Magnets"), surface)
                .angle(Gem.Axes.Y, state -> state.rotation);
        machine.child(model.part("Core"), surface)
                .visibleIf(state -> state.isOn)
                .light(state -> FULLBRIGHT)
                .angle(Gem.Axes.Y, state -> state.rotation / 2F)
                .slide(Gem.Axes.Y, ExposureChamberRenderer::bob);
    }

    /** The core's idle float, one sine of the world clock, as upstream wrote it. */
    private static float bob(ExposureChamberRenderState state) {
        return (float) (Math.sin((state.gameTime % (Math.PI * 16D) + state.partialTick) * 0.125D) * 0.0625D);
    }

    @Override
    public ExposureChamberRenderState createRenderState() {
        return new ExposureChamberRenderState();
    }

    @Override
    public void extract(TileEntityMachineExposureChamber be, ExposureChamberRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 4 -> 180F;
            case 3 -> 270F;
            case 5 -> 0F;
            default -> 90F;
        };
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        state.isOn = be.isOn;
        state.gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.gameMillis = GameTime.now();
        state.partialTick = partialTicks;
    }

    /**
     * The beams are {@link BeamPronter} geometry rebuilt every frame from a random walk, so there is
     * nothing resident for either backend to hold; they stay drawn rather than declared.
     */
    @Override
    protected void submitUndeclared(ExposureChamberRenderState state, PoseStack poseStack, SubmitSink collector,
                                    CameraInfo camera) {
        if (!state.isOn) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        Random random = new Random(state.gameTime / 8L);
        int color = state.gameTime % 8L >= 4L ? 0x80D0FF : 0xFFFFFF;
        int animation = (int) (state.gameMillis % 1_000L) / 50;
        // Preserve the upstream precedence quirk: only the literal 5 is reduced modulo 1000.
        int delayedAnimation = (int) (state.gameMillis + 5L % 1_000L) / 50;
        int rotation = (int) (state.gameMillis % 360L);
        random.nextInt(2);
        beamIf(random.nextInt(2) == 0, poseStack, collector, 0F, 3.675F, -7.5F, color, animation);
        beamIf(random.nextInt(2) == 0, poseStack, collector, 1.1875F, 2.5F, -7.5F, color, animation);
        beamIf(random.nextInt(2) == 0, poseStack, collector, -1.1875F, 2.5F, -7.5F, color, animation);

        poseStack.pushPose();
        poseStack.translate(0F, 1.75F, 0F);
        BeamPronter.prontBeam(poseStack, collector, new Vec3(0, 1.5, 0),
                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                animation, 10, 0.125F, 0x80D0FF, 0xFFFFFF, 1, 0F);
        BeamPronter.prontBeam(poseStack, collector, new Vec3(0, 1.5, 0),
                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                delayedAnimation, 10, 0.125F, 0x8080FF, 0xFFFFFF, 1, 0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, 2.5F, 0F);
        BeamPronter.prontBeam(poseStack, collector, new Vec3(0, 0, -1),
                BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                rotation, 15, 0.125F, 0xFFFF80, 0xFFFFFF, 1, 0F);
        BeamPronter.prontBeam(poseStack, collector, new Vec3(0, 0, -1),
                BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                rotation + 180, 15, 0.125F, 0xFF8080, 0xFFFFFF, 1, 0F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void beamIf(boolean draw, PoseStack poseStack, SubmitSink collector, float x, float y, float z,
                        int color, int animation) {
        if (!draw) return;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        BeamPronter.prontBeam(poseStack, collector, new Vec3(0, 0, 5),
                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                animation, 15, 0.125F, color, 0xFFFFFF, 1, 0F);
        poseStack.popPose();
    }

}
