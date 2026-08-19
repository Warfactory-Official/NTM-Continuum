package com.hbm.uninos.networkproviders;

import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.NodeNet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The microwave link from a fusion klystron to a torus. It carries no payload of its own: the klystron
 * registers as a provider, the torus as a receiver, and the torus reads the klystron energy it was
 * handed directly each tick — so {@link #update()} has nothing to do.
 */
public class KlystronNetwork extends NodeNet<BlockEntity, BlockEntity, KlystronNetwork.KlystronNode, KlystronNetwork> {

    public static final INetworkProvider<KlystronNetwork> THE_PROVIDER = KlystronNetwork::new;

    @Override
    public void update() { }

    public static class KlystronNode extends GenNode<KlystronNetwork> {

        public KlystronNode(INetworkProvider<KlystronNetwork> provider, BlockPos... positions) {
            super(provider, positions);
        }
    }
}
