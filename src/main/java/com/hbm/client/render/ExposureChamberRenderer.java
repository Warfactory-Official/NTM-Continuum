package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Animated renderer for the legacy Exposure Chamber OBJ and its energized particle beams. */
public final class ExposureChamberRenderer
        implements CompatBER<TileEntityMachineExposureChamber, ExposureChamberRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public ExposureChamberRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/exposure_chamber.obj");
        Identifier texture = dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/exposure_chamber.png");
        model = WavefrontObj.load(obj);
        renderType = CompatRenderTypes.entityCutout(texture);
    }

    @Override
    public ExposureChamberRenderState createRenderState() {
        return new ExposureChamberRenderState();
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineExposureChamber be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineExposureChamber be, ExposureChamberRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        // This is the old TESR's explicit map, not the generic machine renderer's map.
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 180F;
            case 3 -> 270F;
            case 5 -> 0F;
            default -> 90F;
        };
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        state.isOn = be.isOn;
        state.gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.wallClockMillis = System.currentTimeMillis();
        state.partialTick = partialTicks;
    }

    @Override
    public void submit(ExposureChamberRenderState state, PoseStack poseStack, SubmitSink collector,
                       CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(collector, poseStack, "Chamber", light);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation));
        part(collector, poseStack, "Magnets", light);
        poseStack.popPose();

        if (state.isOn) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation / 2F));
            poseStack.translate(0F, (float) (Math.sin((state.gameTime % (Math.PI * 16D)
                    + state.partialTick) * 0.125D) * 0.0625D), 0F);
            part(collector, poseStack, "Core", 0xF000F0);
            poseStack.popPose();

            Random random = new Random(state.gameTime / 8L);
            int color = state.gameTime % 8L >= 4L ? 0x80D0FF : 0xFFFFFF;
            int animation = (int) (state.wallClockMillis % 1_000L) / 50;
            // Preserve the upstream precedence quirk: only the literal 5 is reduced modulo 1000.
            int delayedAnimation = (int) (state.wallClockMillis + 5L % 1_000L) / 50;
            int rotation = (int) (state.wallClockMillis % 360L);
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
        }

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

    private void part(SubmitSink collector, PoseStack poseStack, String name, int light) {
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
