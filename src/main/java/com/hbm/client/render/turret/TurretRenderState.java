package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.render.CompatRenderState;

public final class TurretRenderState<T extends TileEntityTurretBaseNT> extends CompatRenderState {

    public final TurretState<T> turret;

    public TurretRenderState(TurretState.Emitter<T> emitter) {
        this.turret = new TurretState<>(emitter);
    }
}
