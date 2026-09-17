package com.hbm.saveddata.satellites;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SatelliteLunarMiner extends SatelliteMiner {

    public static final MapCodec<SatelliteLunarMiner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastOp", 0L).forGetter(sat -> sat.lastOp)
    ).apply(i, SatelliteLunarMiner::new));

    public SatelliteLunarMiner() {
        this(0L);
    }

    private SatelliteLunarMiner(long lastOp) {
        super(lastOp);
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.LUNAR_MINER;
    }
}
