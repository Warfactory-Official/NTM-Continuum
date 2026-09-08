package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretFriendly;

public class RenderTurretFriendly extends RenderTurretBase<TileEntityTurretFriendly> {

    public static final TurretState.Emitter<TileEntityTurretFriendly> EMITTER =
            (turret, state, frame) -> RenderTurretChekhov.emit(turret, state, frame,
                    TurretResources.BASE_FRIENDLY, TurretResources.CARRIAGE_FRIENDLY);

    @Override
    protected TurretState.Emitter<TileEntityTurretFriendly> emitter() {
        return EMITTER;
    }
}
