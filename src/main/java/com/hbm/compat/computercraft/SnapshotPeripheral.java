package com.hbm.compat.computercraft;

//? if computercraft {
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A peripheral over one block entity whose computer-thread reads never touch that block entity (ntm-next's design).
 *
 * <p>1.7's OpenComputers callbacks ran on the server thread ({@code @Callback(direct = true)} still read live
 * fields). CC: Tweaked runs a {@code @LuaFunction} on a computer thread unless it says {@code mainThread = true},
 * so a read-only callback reads a copy instead: the server thread copies the readout into {@code longs}/{@code refs}
 * when {@link Peripherals} hands the peripheral out and after every server tick, bracketed by {@code seq} (odd while
 * writing, released even after). {@link #read} validates a copy against two acquires of {@code seq} and retries a
 * torn one. A method that writes, or reads more than {@link #capture} copies, runs {@code mainThread = true} and
 * reaches the block entity through {@link #machine()}.
 *
 * <p>The copy is one tick old at most, which is what an OpenComputers caller saw too: 1.7 values only change
 * inside the machine's own tick.
 */
public abstract class SnapshotPeripheral<BE extends BlockEntity> implements IPeripheral {

    private static final VarHandle SEQ;
    private static final VarHandle LONGS = MethodHandles.arrayElementVarHandle(long[].class);
    private static final VarHandle REFS = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final int SPINS = 64;

    static {
        try {
            SEQ = MethodHandles.lookup().findVarHandle(SnapshotPeripheral.class, "seq", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final BE machine;
    private final String type;
    private final long[] longs;
    private final Object[] refs;
    @SuppressWarnings("unused") // reached through SEQ
    private int seq;

    /**
     * @param type  the 1.7 OpenComputers component name ({@code getComponentName}), which CC shows as the
     *              peripheral type ({@code peripheral.find("ntm_geiger")})
     * @param longs the number of primitive snapshot slots {@link #capture} fills
     * @param refs  the number of object snapshot slots (strings, arrays of immutable values)
     */
    protected SnapshotPeripheral(BE machine, String type, int longs, int refs) {
        this.machine = machine;
        this.type = type;
        this.longs = new long[longs];
        this.refs = new Object[refs];
    }

    @FunctionalInterface
    protected interface Readout {
        Object[] read(SnapshotPeripheral<?> snapshot);
    }

    /** Server thread only; stores through {@link #put} and {@link #putRef}. */
    protected abstract void capture(BE machine);

    final void refresh() {
        int s = (int) SEQ.getOpaque(this);
        SEQ.setOpaque(this, s + 1);
        VarHandle.storeStoreFence();
        try {
            capture(machine);
        } finally {
            // a throwing capture must not leave every reader spinning on an odd sequence
            SEQ.setRelease(this, s + 2);
        }
    }

    final BE target() {
        return machine;
    }

    protected final void put(int slot, long value) {
        LONGS.setOpaque(longs, slot, value);
    }

    protected final void put(int slot, double value) {
        LONGS.setOpaque(longs, slot, Double.doubleToRawLongBits(value));
    }

    protected final void put(int slot, boolean value) {
        LONGS.setOpaque(longs, slot, value ? 1L : 0L);
    }

    /** Store only immutable values (strings, boxed numbers, arrays never written again). */
    protected final void putRef(int slot, @Nullable Object value) {
        REFS.setOpaque(refs, slot, value);
    }

    public final long longAt(int slot) {
        return (long) LONGS.getOpaque(longs, slot);
    }

    public final int intAt(int slot) {
        return (int) (long) LONGS.getOpaque(longs, slot);
    }

    public final double doubleAt(int slot) {
        return Double.longBitsToDouble((long) LONGS.getOpaque(longs, slot));
    }

    public final boolean booleanAt(int slot) {
        return (long) LONGS.getOpaque(longs, slot) != 0L;
    }

    public final @Nullable Object refAt(int slot) {
        return REFS.getOpaque(refs, slot);
    }

    /** Runs {@code readout} against a consistent snapshot, retrying a torn one. Any thread. */
    protected final Object[] read(Readout readout) {
        for (int spins = 0; ; spins++) {
            int s = (int) SEQ.getAcquire(this);
            if ((s & 1) == 0) {
                Object[] out = readout.read(this);
                VarHandle.loadLoadFence();
                if ((int) SEQ.getAcquire(this) == s) return out;
            }
            if (spins < SPINS) {
                Thread.onSpinWait();
            } else {
                Thread.yield();
            }
        }
    }

    /** The live block entity: only from {@code @LuaFunction(mainThread = true)} methods and {@link #capture}. */
    protected final BE machine() {
        assert machine.getLevel() != null && machine.getLevel().getServer() != null
                && machine.getLevel().getServer().isSameThread() : type + " touched its block entity off-thread";
        return machine;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final Object getTarget() {
        return machine;
    }

    @Override
    public final boolean equals(@Nullable IPeripheral other) {
        return other instanceof SnapshotPeripheral<?> peripheral && peripheral.machine == machine
                && peripheral.getClass() == getClass();
    }
}
//?}
