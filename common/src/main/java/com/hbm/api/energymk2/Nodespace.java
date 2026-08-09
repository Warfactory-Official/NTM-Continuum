package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Compatibility husk over UNINOS for the power network (formerly Nodespace MK1).
 *
 * @author hbm
 */
public class Nodespace {

    public static final INetworkProvider<PowerNetMK2> THE_POWER_PROVIDER = PowerNetMK2::new;

    @Deprecated
    public static PowerNode getNode(Level world, BlockPos pos) {
        return (PowerNode) UniNodespace.getNode(world, pos, THE_POWER_PROVIDER);
    }

    @Deprecated
    public static void createNode(Level world, PowerNode node) {
        UniNodespace.createNode(world, node);
    }

    @Deprecated
    public static void destroyNode(Level world, BlockPos pos) {
        UniNodespace.destroyNode(world, pos, THE_POWER_PROVIDER);
    }

    public static class PowerNode extends GenNode<PowerNetMK2> {

        public PowerNode(BlockPos... positions) {
            super(THE_POWER_PROVIDER, positions);
            this.positions = positions;
        }

        @Override
        public PowerNode setConnections(DirPos... connections) {
            super.setConnections(connections);
            return this;
        }
    }
}
