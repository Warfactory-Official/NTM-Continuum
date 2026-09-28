package com.hbm.tileentity.network;

import dev.golem.core.world.EnergySource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * The RF face of the two 1.7 converters, published through Core's {@code EnergyExposure}: the HE to RF converter
 * only gives RF up (CoFH {@code extractEnergy}; its {@code receiveEnergy} returned 0) and the RF to HE converter
 * only takes it ({@code receiveEnergy}; its {@code extractEnergy} returned 0). Both report their 1 MRF store.
 */
public final class ConverterEnergySource implements EnergySource {

    public static final ConverterEnergySource INSTANCE = new ConverterEnergySource();
    private static final Integer NOTHING = 0;

    private ConverterEnergySource() {}

    @Override
    public long amountFe(ServerLevel level, BlockPos pos) {
        Object be = level.getBlockEntity(pos);
        if (be instanceof TileEntityConverterHeRf he) return he.stored;
        if (be instanceof TileEntityConverterRfHe rf) return rf.stored;
        return 0;
    }

    @Override
    public long capacityFe(ServerLevel level, BlockPos pos) {
        Object be = level.getBlockEntity(pos);
        if (be instanceof TileEntityConverterHeRf) return TileEntityConverterHeRf.RF_CAPACITY;
        if (be instanceof TileEntityConverterRfHe) return TileEntityConverterRfHe.RF_CAPACITY;
        return 0;
    }

    @Override
    public long insertableFe(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityConverterRfHe rf ? rf.insertableRf() : 0;
    }

    @Override
    public long extractableFe(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityConverterHeRf he
                ? Math.min(he.stored, TileEntityConverterHeRf.RF_MAX_EXTRACT) : 0;
    }

    @Override
    public long insertFe(ServerLevel level, BlockPos pos, @Nullable BlockPos from, long fe) {
        return level.getBlockEntity(pos) instanceof TileEntityConverterRfHe rf ? rf.receiveRf(fe) : 0;
    }

    @Override
    public long extractFe(ServerLevel level, BlockPos pos, long fe) {
        return level.getBlockEntity(pos) instanceof TileEntityConverterHeRf he ? he.extractRf(fe) : 0;
    }

    @Override
    public Object snapshot(ServerLevel level, BlockPos pos) {
        Object be = level.getBlockEntity(pos);
        if (be instanceof TileEntityConverterHeRf he) return he.stored;
        if (be instanceof TileEntityConverterRfHe rf) return rf.stored;
        return NOTHING;
    }

    @Override
    public void restore(ServerLevel level, BlockPos pos, Object state) {
        if (!(state instanceof Integer stored)) return;
        Object be = level.getBlockEntity(pos);
        if (be instanceof TileEntityConverterHeRf he) he.stored = stored;
        if (be instanceof TileEntityConverterRfHe rf) rf.stored = stored;
    }
}
