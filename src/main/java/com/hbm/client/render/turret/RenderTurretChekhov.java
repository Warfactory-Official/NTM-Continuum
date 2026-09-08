package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderTurretChekhov extends RenderTurretBase<TileEntityTurretChekhov> {

    public static final TurretState.Emitter<TileEntityTurretChekhov> EMITTER =
            (turret, state, frame) -> emit(turret, state, frame,
                    TurretResources.BASE, TurretResources.CARRIAGE);

    static void emit(TileEntityTurretChekhov turret, TurretState<?> state, TurretFrame frame,
                     Identifier baseTexture, Identifier carriageTexture) {
        connectors(frame, state.connectorMask);
        Matrix4f body = mount(frame, state, baseTexture,
                TurretResources.CHEKHOV, "Carriage", carriageTexture, 1.5F,
                TurretResources.CHEKHOV, "Body", TurretResources.CHEKHOV_BODY);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        TurretFrame.Part barrels = frame.push()
                                        .obj(TurretResources.CHEKHOV, "Barrels")
                                        .texture(TurretResources.CHEKHOV_BARRELS);
        barrels.pose.set(body)
                    .translate(0F, 1.5F, 0F)
                    .rotateX(-spin * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
    }

    @Override
    protected TurretState.Emitter<TileEntityTurretChekhov> emitter() {
        return EMITTER;
    }
}
