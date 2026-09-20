package com.hbm.uninos;

import com.hbm.uninos.graph.IGraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code PneumaticNetworkProvider.THE_PROVIDER}. */
public final class PneumaticGraphProvider implements IGraphProvider<PneumaticData> {

    public static final PneumaticGraphProvider INSTANCE = new PneumaticGraphProvider();

    private PneumaticGraphProvider() {
    }

    @Override
    public PneumaticData createData(BlockState state) {
        return PneumaticData.INSTANCE;
    }

    @Override
    public boolean dataCompatible(PneumaticData a, PneumaticData b) {
        return true;
    }

    @Override
    public Codec<PneumaticData> dataCodec() {
        return PneumaticData.CODEC;
    }
}
