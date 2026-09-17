package com.hbm.saveddata.satellites;

import com.mojang.serialization.MapCodec;

import java.util.Set;

public class SatelliteScanner extends Satellite {

    public static final MapCodec<SatelliteScanner> CODEC = MapCodec.unit(SatelliteScanner::new);

    public SatelliteScanner() {
        super(Interfaces.SAT_PANEL, Set.of(InterfaceActions.HAS_ORES), Set.of());
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.SCANNER;
    }
}
