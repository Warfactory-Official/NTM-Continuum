package com.hbm.client.render.turret;

import com.hbm.client.render.BeamPronter;
import com.hbm.tileentity.turret.TileEntityTurretMaxwell;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** The CIWS mount carrying the microwave emitter, plus the eight interleaved spirals it fires. */
public class RenderTurretMaxwell extends RenderTurretBase<TileEntityTurretMaxwell> {

    public static final TurretState.Emitter<TileEntityTurretMaxwell> EMITTER = (turret, state, frame) -> {
        connectors(frame, state.connectorMask);
        Matrix4f microwave = mount(frame, state, TurretResources.BASE,
                TurretResources.HOWARD, "Carriage", TurretResources.CARRIAGE_CIWS, 1.5F,
                TurretResources.MAXWELL, "Microwave", TurretResources.MAXWELL_BODY);

        if (turret.beam <= 0) return;
        state.beam = true;
        state.beamLength = turret.lastDist - turret.getBarrelLength();
        state.beamSegments = (int) (turret.lastDist + 1);
        state.effect.set(microwave).translate((float) turret.getBarrelLength(), 2F, 0F);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretMaxwell> emitter() {
        return EMITTER;
    }

    /** 1.12.2 sums the whole animation term in float before the cast, which is what staggers the spirals. */
    @Override
    protected void submitEffects(TurretRenderState<TileEntityTurretMaxwell> state, PoseStack poseStack,
                                 SubmitSink sink) {
        TurretState<TileEntityTurretMaxwell> turret = state.turret;
        if (!turret.beam) return;

        Vec3 skeleton = new Vec3(turret.beamLength, 0D, 0D);
        poseStack.pushPose();
        Poses.mulPose(poseStack, turret.effect);
        for (int i = 0; i < 8; i++) {
            int start = (int) (turret.gameTime + turret.partialTicks * -50F + i * 45) % 360;
            BeamPronter.prontBeam(poseStack, sink, skeleton, BeamPronter.EnumWaveType.SPIRAL,
                    BeamPronter.EnumBeamType.SOLID, start, turret.beamSegments, 0.375F,
                    0x2020ff, 0x2020ff, 2, 0.05F);
        }
        poseStack.popPose();
    }
}
