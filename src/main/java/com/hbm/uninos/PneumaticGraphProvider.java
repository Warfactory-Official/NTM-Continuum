package com.hbm.uninos;

import dev.golem.core.graph.GraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code PneumaticNetworkProvider.THE_PROVIDER}. */
public final class PneumaticGraphProvider implements GraphProvider<PneumaticData> {

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
