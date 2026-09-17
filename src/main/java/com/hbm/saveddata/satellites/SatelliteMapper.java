package com.hbm.saveddata.satellites;

import com.mojang.serialization.MapCodec;

import java.util.Set;

public class SatelliteMapper extends Satellite {

    public static final MapCodec<SatelliteMapper> CODEC = MapCodec.unit(SatelliteMapper::new);

    public SatelliteMapper() {
        super(Interfaces.SAT_PANEL, Set.of(InterfaceActions.HAS_MAP), Set.of());
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.MAPPER;
    }
}
