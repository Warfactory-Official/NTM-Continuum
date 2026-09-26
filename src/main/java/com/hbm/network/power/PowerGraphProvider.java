package com.hbm.network.power;

import dev.golem.core.graph.GraphProvider;
import dev.golem.core.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/** Cables carry no data of their own, and every conductor interconnects, exactly as 1.7 had it. */
public final class PowerGraphProvider implements GraphProvider<CableData> {

    public static final PowerGraphProvider INSTANCE = new PowerGraphProvider();

    private PowerGraphProvider() {}

    @Override
    public CableData createData(BlockState state) {
        return CableData.INSTANCE;
    }

    @Override
    public boolean dataCompatible(CableData a, CableData b) {
        return true;
    }

    @Override
    public Codec<CableData> dataCodec() {
        return CableData.CODEC;
    }

    @Override
    public void onNetsMerged(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        PowerNetwork.mergeBridges(keep, drain);
    }
}
