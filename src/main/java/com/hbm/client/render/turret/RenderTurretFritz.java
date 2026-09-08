package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretFritz;

public class RenderTurretFritz extends RenderTurretBase<TileEntityTurretFritz> {

    public static final TurretState.Emitter<TileEntityTurretFritz> EMITTER = (turret, state, frame) -> {
        connectors(frame, state.connectorMask);
        mount(frame, state, TurretResources.BASE,
                TurretResources.CHEKHOV, "Carriage", TurretResources.CARRIAGE, 1.5F,
                TurretResources.FRITZ, "Gun", TurretResources.FRITZ_BODY);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretFritz> emitter() {
        return EMITTER;
    }
}
