package com.hbm.client.render.turret;

import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.turret.TileEntityTurretFritz;

public class RenderTurretFritz extends RenderTurretBase<TileEntityTurretFritz> {

    public static final TurretState.Emitter<TileEntityTurretFritz> EMITTER = (turret, state, frame) -> {
        int mask = state.connectorMask;
        if (turret.getLevel() != null) {
            DirPos[] ports = turret.getConPos();
            for (int i = 0; i < 8 && i < ports.length; i++) {
                DirPos port = ports[i];
                if (Library.canConnectFluid(turret.getLevel(), port.getPos(), port.getDir(), turret.tank.getTankType())) {
                    mask |= 1 << i;
                }
            }
        }
        connectors(frame, mask);
        mount(frame, state, TurretResources.BASE,
                TurretResources.CHEKHOV, "Carriage", TurretResources.CARRIAGE, 1.5F,
                TurretResources.FRITZ, "Gun", TurretResources.FRITZ_BODY);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretFritz> emitter() {
        return EMITTER;
    }
}
