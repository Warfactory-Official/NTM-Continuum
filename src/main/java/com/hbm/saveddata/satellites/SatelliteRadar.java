package com.hbm.saveddata.satellites;

import com.mojang.serialization.MapCodec;

import java.util.Set;

public class SatelliteRadar extends Satellite {

    public static final MapCodec<SatelliteRadar> CODEC = MapCodec.unit(SatelliteRadar::new);

    public SatelliteRadar() {
        super(Interfaces.SAT_PANEL, Set.of(InterfaceActions.HAS_MAP, InterfaceActions.HAS_RADAR), Set.of());
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.RADAR;
    }
}
