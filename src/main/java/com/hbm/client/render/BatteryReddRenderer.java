package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Faithful current FEnSU renderer: Base/Wheel/Lights plus the additive plasma and legacy zaps. */
public final class BatteryReddRenderer extends GemBER<TileEntityBatteryREDD, BatteryReddRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final float PLASMA_R = 1F;
    private static final float PLASMA_G = 0.25F;
    private static final float PLASMA_B = 0.75F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/fensu2.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/fensu2.png");
    private final RenderType plasma = CompatRenderTypes.plasmaGlow(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma.png"));
    private final RenderType sparkle = CompatRenderTypes.plasmaGlow(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_sparkle.png"));

    @Override
    protected void setup(Gem.Model<BatteryReddRenderState> declaration) {
        Gem.Part<BatteryReddRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Base"), body);

        Gem.Part<BatteryReddRenderState> wheel = machine.joint()
                .translate(0F, 5.5F, 0F)
                .angle(Gem.Axes.X, state -> state.rotation)
                .translate(0F, -5.5F, 0F);
        wheel.child(model.part("Wheel"), body);
        wheel.child(model.part("Lights"), body)
                .light(state -> FULLBRIGHT);
    }

    @Override
    public BatteryReddRenderState createRenderState() {
        return new BatteryReddRenderState();
    }

    @Override
    public void extract(TileEntityBatteryREDD blockEntity, BatteryReddRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = blockEntity.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 2 -> 270F;
            case 4 -> 0F;
            case 3 -> 90F;
            case 5 -> 180F;
            default -> 0F;
        };
        state.rotation = blockEntity.prevRotation
                + (blockEntity.rotation - blockEntity.prevRotation) * partialTicks;
        state.speed = blockEntity.getSpeed();
        state.gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();
        state.gameMillis = GameTime.now();
        state.sparkleNearby = cameraPosition.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5D,
                blockEntity.getBlockPos().getY() + 2.5D, blockEntity.getBlockPos().getZ() + 0.5D) < 100D * 100D;
    }

    /**
     * The motion trails, the scrolling plasma sheet and the arcing zaps are all built from the clock
     * every frame; none of them is a mesh a backend could hold.
     */
    @Override
    protected void submitUndeclared(BatteryReddRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        poseStack.pushPose();
        poseStack.translate(0F, 5.5F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotation));
        poseStack.translate(0F, -5.5F, 0F);
        renderTrails(state, poseStack, collector);
        renderPlasma(state, poseStack, collector);
        poseStack.popPose();
        if (state.speed > 0F) renderZaps(state, poseStack, collector);
        poseStack.popPose();
    }

    private void renderPlasma(BatteryReddRenderState state, PoseStack poseStack, SubmitSink collector) {
        float speed = state.speed / 15F;
        int color = argb((0.45F + (float) Math.sin(state.gameMillis / 1000D) * 0.15F) * speed,
                PLASMA_R, PLASMA_G, PLASMA_B);
        float mainOsc = (float) (sps(state.gameMillis / 1000D) % 1D);
        collector.submitCustomGeometry(poseStack, plasma,
                (pose, buffer) -> model.renderPart("Plasma", pose, buffer, FULLBRIGHT,
                        OverlayTexture.NO_OVERLAY, color, 0F, mainOsc, false));

        if (state.sparkleNearby) {
            long time = state.gameMillis;
            float spin = (float) ((time / 250D * -1D) % 1D);
            float oscillation = (float) (Math.sin(time / 1000D) * 0.5D % 1D);
            int sparkleColor = argb(0.75F * speed, PLASMA_R * 2F, PLASMA_G * 2F, PLASMA_B * 2F);
            collector.submitCustomGeometry(poseStack, sparkle,
                    (pose, buffer) -> model.renderPart("Plasma", pose, buffer, FULLBRIGHT,
                            OverlayTexture.NO_OVERLAY, sparkleColor, spin, oscillation, false));
        }
    }

    private void renderTrails(BatteryReddRenderState state, PoseStack poseStack, SubmitSink collector) {
        double span = state.speed * 0.75D;
        if (span <= 0D) return;
        poseStack.pushPose();
        poseStack.translate(0D, 5.5D, 0D);
        collector.submitCustomGeometry(poseStack, CompatRenderTypes.lightning(), (pose, buffer) -> {
            double length = 4.25D;
            double width = 0.125D;
            float[] alpha = {0.75F, 0.5F, 0.25F, 0F};
            for (int side = -1; side <= 1; side += 2) {
                float x = 0.8125F * side;
                for (int sector = 0; sector < 8; sector++) {
                    double angle = sector * 45D;
                    for (int segment = 0; segment < 3; segment++) {
                        trailQuad(pose, buffer, x, length, width,
                                angle + span * segment, angle + span * (segment + 1),
                                alpha[segment], alpha[segment + 1]);
                    }
                }
            }
        });
        poseStack.popPose();
    }

    private static void trailQuad(PoseStack.Pose pose, VertexConsumer buffer, float x,
                                  double length, double width, double fromDegrees, double toDegrees,
                                  float fromAlpha, float toAlpha) {
        double from = Math.toRadians(fromDegrees);
        double to = Math.toRadians(toDegrees);
        float iy0 = (float) (Math.cos(from) * (length - width));
        float iz0 = (float) (-Math.sin(from) * (length - width));
        float oy0 = (float) (Math.cos(from) * (length + width));
        float oz0 = (float) (-Math.sin(from) * (length + width));
        float iy1 = (float) (Math.cos(to) * (length - width));
        float iz1 = (float) (-Math.sin(to) * (length - width));
        float oy1 = (float) (Math.cos(to) * (length + width));
        float oz1 = (float) (-Math.sin(to) * (length + width));
        int fromColor = argb(fromAlpha, 1F, 1F, 0F);
        int toColor = argb(toAlpha, 1F, 1F, 0F);
        trailVertex(pose, buffer, x, iy0, iz0, fromColor);
        trailVertex(pose, buffer, x, oy0, oz0, fromColor);
        trailVertex(pose, buffer, x, oy1, oz1, toColor);
        trailVertex(pose, buffer, x, iy1, iz1, toColor);
    }

    private static void trailVertex(PoseStack.Pose pose, VertexConsumer buffer,
                                    float x, float y, float z, int color) {
        Vertexes.color(buffer, pose, x, y, z, color);
    }

    private static double sps(double value) {
        return Math.sin(Math.PI / 2D * Math.cos(value));
    }

    private void renderZaps(BatteryReddRenderState state, PoseStack poseStack, SubmitSink collector) {
        Random random = new Random(state.gameTime / 5L);
        random.nextBoolean();
        int animation = (int) (state.gameMillis % 1000L) / 50;
        zap(random.nextBoolean(), state, poseStack, collector, 3.125F, 0F,
                new Vec3(-1.375D, -2.625D, 3.75D), animation);
        zap(random.nextBoolean(), state, poseStack, collector, -3.125F, 0F,
                new Vec3(1.375D, -2.625D, 3.75D), animation);
        zap(random.nextBoolean(), state, poseStack, collector, 3.125F, 0F,
                new Vec3(-1.375D, -2.625D, -3.75D), animation);
        zap(random.nextBoolean(), state, poseStack, collector, -3.125F, 0F,
                new Vec3(1.375D, -2.625D, -3.75D), animation);
    }

    private void zap(boolean draw, BatteryReddRenderState state, PoseStack poseStack, SubmitSink collector,
                     float x, float z, Vec3 end, int animation) {
        if (!draw) return;
        poseStack.pushPose();
        poseStack.translate(x, 5.5F, z);
        BeamPronter.prontBeam(poseStack, collector, end, BeamPronter.EnumWaveType.RANDOM,
                BeamPronter.EnumBeamType.SOLID, animation, 15, 0.25F, 0x404040, 0x002040, 3, 0.0625F);
        BeamPronter.prontBeam(poseStack, collector, end, BeamPronter.EnumWaveType.RANDOM,
                BeamPronter.EnumBeamType.SOLID, animation, 1, 0F, 0x404040, 0x002040, 3, 0.0625F);
        poseStack.popPose();
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255F)));
    }

}
