package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import java.util.function.Supplier;

/** 1.7 {@code SpotlightModular}: the fluorescent tube, which joins up with its neighbours into a strip. */
public class SpotlightModular extends Spotlight {

    public SpotlightModular(Properties properties, int beamLength, LightType type, boolean isOn,
                            Supplier<net.minecraft.world.level.block.Block> off, Supplier<net.minecraft.world.level.block.Block> on) {
        super(properties, beamLength, type, isOn, off, on);
    }

    @Override
    public String getPartName(int connectionCount) {
        if (connectionCount == 0) return "FluoroSingle";
        if (connectionCount == 1) return "FluoroCap";
        return "FluoroMid";
    }

    public boolean canConnectTo(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == this;
    }
}
