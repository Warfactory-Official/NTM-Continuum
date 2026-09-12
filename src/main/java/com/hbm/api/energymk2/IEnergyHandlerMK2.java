package com.hbm.api.energymk2;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.uninos.IBufferedEndpoint;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The common ancestor of providers and receivers, carrying everything both roles need — which is
 * everything except the subscription helpers.
 *
 * <p>The buffer, the speed limits, the priority and the transfer arithmetic used to be split between
 * {@link IEnergyReceiverMK2} and {@link IEnergyProviderMK2}, so a machine that did both inherited half
 * its contract from each. The node graph arbitrates over one handler type rather than two roles — a
 * buffer is a provider and a receiver in the same round — so the members sit here and the two role
 * interfaces are markers that add only their own helpers.
 *
 * <p>{@code ConnectionPriority} moved up with them. Nested types are inherited, so a qualified use
 * through either role interface still resolves; an <i>import</i> does not, because Java demands the
 * canonical name there.
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

    /** Uses up available power. No sanity checking — ensure the requested power is &lt;= the current power. */
    default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    /** Max speed at which this handler can send energy. Defaults to its capacity. */
    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    /** Max speed at which this handler can take energy. Defaults to its capacity. */
    default long getReceiverSpeed() {
        return this.getMaxPower();
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
