package com.hbm.api.energymk2;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.uninos.IBufferedEndpoint;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The common ancestor of providers and receivers, carrying everything both roles need, which is
 * everything except the subscription helpers.
 *
 * <p>The buffer, the speed limits, the priority and the transfer arithmetic live here, not in
 * {@link IEnergyReceiverMK2} and {@link IEnergyProviderMK2}, so a machine that does both inherits the
 * whole contract once. The node graph arbitrates over one handler type, since a buffer is a provider and a
 * receiver in the same round. The two role interfaces are markers that add only their own helpers.
 *
 * <p>{@code ConnectionPriority} is nested here. A qualified use through either role interface resolves because
 * nested types are inherited; an <i>import</i> must use the canonical name.
 */
public interface IEnergyHandlerMK2 extends IEnergyConnectorMK2, ILoadedTile, IBufferedEndpoint {

    long getPower();

    void setPower(long power);

    long getMaxPower();

    boolean particleDebug = false;

    default Vec3 getDebugParticlePosMK2() {
        BlockEntity te = (BlockEntity) this;
        return new Vec3(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 1, te.getBlockPos().getZ() + 0.5);
    }

    /** Uses up available power. No sanity checking: ensure the requested power is &lt;= the current power. */
    default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    /**
     * Max speed at which this handler can send energy. Zero unless it is an {@link IEnergyProviderMK2}, which
     * defaults it to its capacity: 1.7 declared this on the provider interface only, so a consumer's buffer
     * was never offered to a net, so a battery on the same cable could drain a machine.
     */
    default long getProviderSpeed() {
        return 0L;
    }

    /**
     * Max speed at which this handler can take energy. Zero unless it is an {@link IEnergyReceiverMK2}, which
     * defaults it to its capacity: 1.7 declared this on the receiver interface only, so a generator never took
     * power from a net.
     */
    default long getReceiverSpeed() {
        return 0L;
    }

    /** Absorbs as much of {@code power} as it can and returns the excess. */
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

    /** Whether getPower/setPower describe a finite owned buffer rather than a virtual transfer action. */
    default boolean ownsEnergyBuffer() { return !passThrough(); }

    /** A relay supplies its separate pending stock rather than the intake counter returned by getPower(). */
    default boolean passThrough() { return false; }

    /** The supply available to this round; stored-energy handlers retain their existing speed bound. */
    default long getPowerAvailable() {
        return passThrough() ? getProviderSpeed() : Math.min(getPower(), getProviderSpeed());
    }

    /** Stored energy used by the round's conservation tracker. */
    default long getTrackedPower() { return getPower(); }

    /** Free room in a two-stage relay: at most one limit arriving and two limits held. */
    static long transitRoom(long limit, long stock, long arriving) {
        return Math.max(0L, Math.min(limit - arriving, Math.multiplyExact(limit, 2L) - stock - arriving));
    }

    default ConnectionPriority getPriority() {
        return ConnectionPriority.NORMAL;
    }

    /** Whether a provider may hand power over by touching the block, bypassing the network. */
    default boolean allowDirectProvision() {
        return true;
    }

    /** More is better-er. */
    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = new ConnectionPriority[]{LOWEST, LOW, NORMAL, HIGH, HIGHEST};

        /** Clamped to the band a storage machine may sit in; its GUI button cycles inside this. */
        public static ConnectionPriority storage(int ordinal) {
            if (ordinal < LOW.ordinal()) return LOW;
            return ordinal > HIGH.ordinal() ? HIGH : VALUES[ordinal];
        }

        public static ConnectionPriority nextStorage(ConnectionPriority current) {
            int next = current.ordinal() + 1;
            return next > HIGH.ordinal() ? LOW : storage(next);
        }
    }
}
