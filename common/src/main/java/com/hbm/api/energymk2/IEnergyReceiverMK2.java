package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** If it receives energy, use this. */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

    /**
     * Transfers a specified amount of energy to this receiver. Absorbs as much as it can and returns
     * the excess.
     */
    default long transferPower(long power, boolean simulate) {
        if (power + this.getPower() <= this.getMaxPower()) {
            if (!simulate) this.setPower(power + this.getPower());
            return 0;
        }
        long capacity = this.getMaxPower() - this.getPower();
        long overshoot = power - capacity;
        if (!simulate) this.setPower(this.getMaxPower());
        return overshoot;
    }

    default long getReceiverSpeed() {
        return this.getMaxPower();
    }

    /** Whether a provider can provide power by touching the block (via proxies), bypassing the network. */
    default boolean allowDirectProvision() {
        return true;
    }

    default void trySubscribe(Level world, DirPos pos) {
        trySubscribe(world, pos.getPos(), pos.getDir());
    }

    default void trySubscribe(Level world, BlockPos pos, ForgeDirection dir) {
        trySubscribe(world, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void trySubscribe(Level world, int x, int y, int z, ForgeDirection dir) {
        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof IEnergyConductorMK2 con) {
            if (!con.canConnect(dir.getOpposite())) return;

            Nodespace.PowerNode node = Nodespace.getNode(world, new BlockPos(x, y, z));

            if (node != null && node.net != null) {
                node.net.addReceiver(this);
            }
        }
    }

    default void tryUnsubscribe(Level world, int x, int y, int z) {
        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof IEnergyConductorMK2 con) {
            Nodespace.PowerNode node = con.createNode();

            if (node != null && node.net != null) {
                node.net.removeReceiver(this);
            }
        }
    }

    default ConnectionPriority getPriority() {
        return ConnectionPriority.NORMAL;
    }

    /** More is better-er. */
    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = new ConnectionPriority[]{LOWEST, LOW, NORMAL, HIGH, HIGHEST};
    }
}
