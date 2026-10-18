package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 1.7 {@code SatelliteLunarMiner}: the asteroid miner with the lunar pool. */
public class SatelliteLunarMiner extends SatelliteMiner {

    public static final MapCodec<SatelliteLunarMiner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("progress", 0D).forGetter(sat -> sat.progress)
    ).apply(i, SatelliteLunarMiner::new));

    public SatelliteLunarMiner() {
        this(0D);
    }

    private SatelliteLunarMiner(double progress) {
        super(progress);
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.LUNAR_MINER;
    }

    @Override public String getType() { return "LUNAR_MINER"; }

    @Override
    protected EnumSatType chip() {
        return EnumSatType.MINER_LUNAR;
    }
}
