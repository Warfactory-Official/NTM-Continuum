package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.PortClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * If it receives energy, use this. The whole contract is on {@link IEnergyHandlerMK2}; this says which
 * of the two roles the machine means to play.
 *
 * <p>The graph walks the faces of its own cells and asks what is on the other side, so a machine is found
 * by standing next to a cable. {@link #trySubscribe} is 1.7's per-port call, made every tick from each port:
 * it says at which ports the machine takes power (see {@link PortClaims}), and a net that reaches it through
 * another face gives it nothing. A face that wants nothing this round says so by answering zero to
 * {@link IEnergyHandlerMK2#getReceiverSpeed()}.
 */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

    /** 1.7 {@code IEnergyReceiverMK2.getReceiverSpeed}: a receiver takes up to its capacity. */
    @Override
    default long getReceiverSpeed() {
        return this.getMaxPower();
    }

    default void trySubscribe(Level world, DirPos pos) { trySubscribe(world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir()); }
    default void trySubscribe(Level world, BlockPos pos, ForgeDirection dir) { trySubscribe(world, pos.getX(), pos.getY(), pos.getZ(), dir); }

    /**
     * 1.7 {@code IEnergyReceiverMK2.trySubscribe}: this machine receives from the net of the cable at
     * {@code x, y, z}, which it touches on its side {@code dir}, for the next three seconds.
     */
    default void trySubscribe(Level world, int x, int y, int z, ForgeDirection dir) {
        PortClaims.claim(world, x, y, z, dir, this, PortClaims.POWER, PortClaims.SUBSCRIBE);
    }

    /** 1.7 {@code IEnergyReceiverMK2.tryUnsubscribe}: stop receiving from the cable at {@code x, y, z} now. */
    default void tryUnsubscribe(Level world, int x, int y, int z) {
        PortClaims.withdraw(world, x, y, z, this, PortClaims.POWER, PortClaims.SUBSCRIBE);
    }
}
