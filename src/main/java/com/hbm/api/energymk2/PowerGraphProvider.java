package com.hbm.api.energymk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/** Cables carry no data of their own, and every conductor interconnects, exactly as 1.7 had it. */
public final class PowerGraphProvider implements IGraphProvider<CableData> {

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
