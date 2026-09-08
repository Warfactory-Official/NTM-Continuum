package com.hbm.client.render.turret;

import com.hbm.client.render.BeamPronter;
import com.hbm.tileentity.turret.TileEntityTurretTauon;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RenderTurretTauon extends RenderTurretBase<TileEntityTurretTauon> {

    public static final TurretState.Emitter<TileEntityTurretTauon> EMITTER = (turret, state, frame) -> {
        connectors(frame, state.connectorMask);
        Matrix4f cannon = mount(frame, state, TurretResources.BASE,
                TurretResources.CHEKHOV, "Carriage", TurretResources.CARRIAGE, 1.5F,
                TurretResources.TAUON, "Cannon", TurretResources.TAUON_BODY);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        TurretFrame.Part rotor = frame.push().obj(TurretResources.TAUON, "Rotor")
                                      .texture(TurretResources.TAUON_BODY);
        rotor.pose.set(cannon)
                  .translate(0F, 1.375F, 0F)
                  .rotateX(-spin * Mth.DEG_TO_RAD)
                  .translate(0F, -1.375F, 0F);

        if (turret.beam <= 0) return;
        state.beam = true;
        state.beamLength = turret.lastDist;
        state.beamSegments = (int) turret.lastDist + 1;
        state.effect.set(cannon).translate(0F, 1.5F, 0F);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretTauon> emitter() {
        return EMITTER;
    }

    /** 1.12.2 quantises the wave to {@code totalWorldTime / 5}, with no interpolation. */
    @Override
    protected void submitEffects(TurretRenderState<TileEntityTurretTauon> state, PoseStack poseStack,
                                 SubmitSink sink) {
        TurretState<TileEntityTurretTauon> turret = state.turret;
        if (!turret.beam) return;

        poseStack.pushPose();
        Poses.mulPose(poseStack, turret.effect);
        int start = (int) (turret.gameTime / 5 % 360);
        BeamPronter.prontBeam(poseStack, sink, new Vec3(turret.beamLength, 0D, 0D),
                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE, start, turret.beamSegments,
                0.1F, 0xffa200, 0xffd000, 0, 0);
        poseStack.popPose();
    }
}
