package com.hbm.client.render;

import com.hbm.blocks.generic.BlockEmitter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.tileentity.deco.TileEntityEmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * 1.7 {@code RenderEmitter}: from the emitting face, a beam {@code beam - 1} blocks long in the dyed colour (a
 * slowly cycling pastel when undyed) - a spiral core in 85% of the colour inside a 10% halo, with {@code
 * sqrt(girth * 50)} segments - and the hand-drill effects: two jittering arcs (1), a double (2) or triple (3)
 * helix turning at 10 degrees a tick. Effect 4's rings are particles sent by the block entity.
 */
public final class RenderEmitter extends GemBER<TileEntityEmitter, RenderEmitter.State> {

    public static final class State extends CompatRenderState {
        public Direction facing = Direction.NORTH;
        public int range;
        public int color;
        public float girth;
        public int effect;
        public long time;
        public float interp;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityEmitter emitter, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = emitter.getBlockState().getValue(BlockEmitter.FACING);
        state.range = emitter.beam - 1;
        state.time = emitter.getLevel() == null ? 0L : emitter.getLevel().getGameTime();
        state.color = emitter.color == 0 ? Color.HSBtoRGB(state.time / 50.0F, 0.5F, 0.25F) & 16777215 : emitter.color;
        state.girth = emitter.girth;
        state.effect = emitter.effect;
        state.interp = partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int range = state.range;
        if (range <= 0) return;

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        switch (state.facing) {
            case DOWN -> { pose.translate(0D, 0.5D, -0.5D); pose.mulPose(Axis.XP.rotationDegrees(90F)); }
            case UP -> { pose.translate(0D, 0.5D, 0.5D); pose.mulPose(Axis.XP.rotationDegrees(-90F)); }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270F));
            default -> { }
        }
        pose.translate(0D, 0.5D, 0.5D);

        int originalColor = state.color;
        float girth = state.girth;
        int r = (originalColor & 0xff0000) >> 16;
        int g = (originalColor & 0x00ff00) >> 8;
        int b = (originalColor & 0x0000ff);
        float innerMult = 0.85F;
        float outerMult = 0.1F;
        int colorInner = ((int) (r * innerMult) << 16) | ((int) (g * innerMult) << 8) | ((int) (b * innerMult));
        int colorOuter = ((int) (r * outerMult) << 16) | ((int) (g * outerMult) << 8) | ((int) (b * outerMult));
        Vec3 beam = new Vec3(0, 0, range);
        long time = state.time;
        float f = state.interp;

        int segments = (int) Math.max(Math.sqrt(girth * 50), 2);
        BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0, 1, 0F,
                colorOuter, colorInner, segments, girth);

        if (state.effect == 1) {
            BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.RANDOM, EnumBeamType.SOLID, (int) time / 2,
                    (int) Math.max(range / girth / 2, 1), girth * 2, colorOuter, colorInner, 4, girth * 0.1F);
            BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.RANDOM, EnumBeamType.SOLID, (int) time / 2 + 15,
                    (int) Math.max(range / girth / 4, 1), girth * 2, colorOuter, colorInner, 4, girth * 0.1F);
        }
        if (state.effect == 2 || state.effect == 3) {
            int helices = state.effect == 2 ? 2 : 3;
            int spin = (int) (time + f) * -10 % 360;
            for (int i = 0; i < helices; i++) {
                BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.SPIRAL, EnumBeamType.SOLID, spin + i * 360 / helices,
                        (int) Math.max(range / girth / 2, 1), girth * 2, colorOuter, colorInner, 4, girth * 0.1F);
            }
        }
        pose.popPose();
    }
}
