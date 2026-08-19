package com.hbm.uninos.networkproviders;

import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.NodeNet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The plasma link from a fusion torus to its consumers (boiler, MHD turbine, collector, breeder). Like
 * {@link KlystronNetwork} it is a pure membership graph: the torus registers as a provider and pushes
 * energy into each {@link com.hbm.tileentity.machine.fusion.IFusionPowerReceiver} it finds on the net,
 * so {@link #update()} does nothing.
 */
public class PlasmaNetwork extends NodeNet<BlockEntity, BlockEntity, PlasmaNetwork.PlasmaNode, PlasmaNetwork> {

    public static final INetworkProvider<PlasmaNetwork> THE_PROVIDER = PlasmaNetwork::new;

    @Override
    public void update() { }

    public static class PlasmaNode extends GenNode<PlasmaNetwork> {

        public PlasmaNode(INetworkProvider<PlasmaNetwork> provider, BlockPos... positions) {
            super(provider, positions);
        }
    }
}
