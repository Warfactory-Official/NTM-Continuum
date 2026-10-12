package com.hbm.uninos.graph;

import com.hbm.lib.ForgeDirection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/**
 * The per-port roles of 1.7's subscriptions, for the graph networks that find their machines by probing faces.
 *
 * <p>In 1.7 a machine joined a net only through its own per-port calls, every tick: {@code trySubscribe(type, world,
 * x, y, z, dir)} made it a receiver of the net whose duct sits at {@code x, y, z} ({@code IFluidReceiverMK2},
 * {@code IEnergyReceiverMK2}), {@code tryProvide(...)} a provider ({@code IFluidStandardSenderMK2},
 * {@code IEnergyProviderMK2}), and the net ({@code NodeNet.addReceiver/addProvider}) dropped an entry its owner had
 * not renewed for three seconds ({@code FluidNetMK2.timeout}, {@code PowerNetMK2.timeout}). A machine's output
 * therefore only left through the ports it was sent from, and an input port took only what it subscribed to.
 *
 * <p>The port's nets find a machine by standing next to it, so those calls record a claim here instead: the
 * {@link com.hbm.lib.DirPos} (the duct's position and the machine's side facing it), the user object, the fluid (or
 * {@link #POWER}) and the role. A net that reaches a user through the face of cell {@code pos} on the user's side
 * {@code dir} gives it a role only with a claim of that role at exactly that {@code DirPos}, renewed within
 * {@link #TIMEOUT_MS}; a loader fluid capability on a machine face asks the same question for the position a foreign
 * pipe stands at.
 *
 * <p>A user class that never claims a role for a fluid (or for power) keeps the face rule for it: every face its
 * {@code canConnect} opens. That is the port's own content that never had per-port calls, and anything still
 * missing its 1.7 calls; it is what every machine did before this table existed, never less. Which classes those are
 * is learnt, per class, role and fluid, and never assumed: the first time a net (or a foreign pipe) has something to
 * move for such a triple and no claim of it was ever made, the triple waits {@link #GRACE_TICKS} without a role, as a
 * 1.7 machine had none before its first {@code trySubscribe}; a claim in that time makes it per port for good,
 * otherwise it falls back to the face rule (and still becomes per port at its first claim).
 */
public final class PortClaims {

    /** The 1.7 roles, as the two calls that make them. */
    public static final int PROVIDE = 1;
    public static final int SUBSCRIBE = 2;

    /** The "fluid" of an energy claim: power nets carry one thing. */
    public static final Object POWER = new Object() {
        @Override public String toString() { return "power"; }
    };

    /** 1.7 {@code FluidNetMK2.timeout} / {@code PowerNetMK2.timeout}: wall-clock milliseconds, as there. */
    public static final long TIMEOUT_MS = 3_000L;

    private static final long SWEEP_MS = 10_000L;

    /**
     * How long an unlearnt (class, role, fluid) waits for its first claim before it falls back to the face rule, in
     * server ticks. The slowest 1.7 caller renews every 40 ticks (the electric furnace's {@code updateConnections}).
     */
    public static final long GRACE_TICKS = 41L;

    private static final Reference2ObjectOpenHashMap<Level, PortClaims> LEVELS = new Reference2ObjectOpenHashMap<>();

    /** Per user class, per role: the fluids (or {@link #POWER}) it has ever claimed, so the face rule stops there. */
    private static final Reference2ObjectOpenHashMap<Class<?>, ReferenceOpenHashSet<Object>> PROVIDES = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Class<?>, ReferenceOpenHashSet<Object>> SUBSCRIBES = new Reference2ObjectOpenHashMap<>();

    /** Per user class, per role: when a net first had something to move for an unlearnt fluid, in server ticks. */
    private static final Reference2ObjectOpenHashMap<Class<?>, it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap<Object>> PROVIDES_SEEN = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Class<?>, it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap<Object>> SUBSCRIBES_SEEN = new Reference2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<ArrayList<Claim>> byCell = new Long2ObjectOpenHashMap<>();
    private long lastSweep;

    private PortClaims() {}

    /** Drops a level's table on unload (from {@code PowerNetwork.init}, beside the endpoint registry's). */
    public static void clearLevel(Level level) {
        LEVELS.remove(level);
    }

    private static @Nullable PortClaims of(Level level, boolean create) {
        if (!(level instanceof ServerLevel)) return null;
        PortClaims claims = LEVELS.get(level);
        if (claims == null && create) LEVELS.put(level, claims = new PortClaims());
        return claims;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The calls (1.7 trySubscribe / tryProvide / tryUnsubscribe)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Records that {@code user} plays {@code role} for {@code type} at the port whose duct stands at {@code x, y, z}
     * on the user's side {@code dir}. A sideless call ({@link ForgeDirection#UNKNOWN}, 1.7's battery in buffer mode
     * providing to its own node) claims nothing: the cell's own machine is never gated.
     */
    public static void claim(Level level, int x, int y, int z, ForgeDirection dir, Object user, Object type, int role) {
        if (dir == null || dir == ForgeDirection.UNKNOWN || type == null) return;
        PortClaims claims = of(level, true);
        if (claims == null) return;
        long now = System.currentTimeMillis();
        user = owner(user);
        learn(role == PROVIDE ? PROVIDES : SUBSCRIBES, user.getClass(), type);
        long cell = BlockPos.asLong(x, y, z);
        ArrayList<Claim> list = claims.byCell.get(cell);
        if (list == null) claims.byCell.put(cell, list = new ArrayList<>(2));
        int side = dir.ordinal();
        for (int i = 0, n = list.size(); i < n; i++) {
            Claim c = list.get(i);
            if (c.user == user && c.type == type && c.side == side && c.role == role) {
                c.time = now;
                claims.sweep(now);
                return;
            }
        }
        list.add(new Claim(user, type, side, role, now));
        claims.sweep(now);
    }

    /**
     * 1.7 {@code tryUnsubscribe} / {@code net.removeReceiver} / {@code removeProvider} at a duct position: the user's
     * claims of {@code role} there end now rather than after the timeout. {@code type} null: any fluid.
     */
    public static void withdraw(Level level, int x, int y, int z, Object user, @Nullable Object type, int role) {
        PortClaims claims = of(level, false);
        if (claims == null) return;
        ArrayList<Claim> list = claims.byCell.get(BlockPos.asLong(x, y, z));
        if (list == null) return;
        Object owner = owner(user);
        list.removeIf(c -> c.user == owner && c.role == role && (type == null || c.type == type));
    }

    private static void learn(Reference2ObjectOpenHashMap<Class<?>, ReferenceOpenHashSet<Object>> table, Class<?> cls, Object type) {
        ReferenceOpenHashSet<Object> types = table.get(cls);
        if (types == null) table.put(cls, types = new ReferenceOpenHashSet<>(4));
        types.add(type);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The question a net asks
    // ------------------------------------------------------------------------------------------------------------

    /**
     * May the net give {@code user} the role {@code role} for {@code type} through the duct cell {@code cell}, reached
     * on the user's side {@code side}? Ask only with something to move (supply or demand above zero). Yes with a live
     * claim at exactly that port; for a user class that never claimed this role for this fluid, no during its
     * {@link #GRACE_TICKS} and the face rule after; no otherwise. {@code side} UNKNOWN (the cell's own machine): always
     * yes.
     */
    public static boolean allows(Level level, long cell, ForgeDirection side, Object user, Object type, int role) {
        if (side == null || side == ForgeDirection.UNKNOWN) return true;
        user = owner(user);
        Class<?> cls = user.getClass();
        ReferenceOpenHashSet<Object> learnt = (role == PROVIDE ? PROVIDES : SUBSCRIBES).get(cls);
        if (learnt != null && learnt.contains(type)) return claimed(level, cell, side, user, type, role);
        if (!(level instanceof ServerLevel server)) return false;
        long now = server.getServer().getTickCount();
        var seen = (role == PROVIDE ? PROVIDES_SEEN : SUBSCRIBES_SEEN).get(cls);
        if (seen == null) (role == PROVIDE ? PROVIDES_SEEN : SUBSCRIBES_SEEN).put(cls, seen = new it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap<>(4));
        if (!seen.containsKey(type)) {
            seen.put(type, now);
            return false;
        }
        return now - seen.getLong(type) >= GRACE_TICKS;
    }

    /** The object whose claims count for {@code user}: a {@link Delegate}'s machine, else the user itself. */
    public static Object owner(Object user) {
        if (user instanceof Delegate delegate) {
            Object owner = delegate.portClaimOwner();
            if (owner != null) return owner;
        }
        return user;
    }

    /**
     * A user that only fronts another machine: a block entity at its own block (1.7's {@code TileEntityProxyCombo}:
     * the custom machine's {@code cm_port}, the PWR's port block, the ICF port cell), or a second user object of one
     * machine (1.7's factory coolant {@code delegate}). Claims made by it or asked of it are its machine's, so a port
     * claimed through either is the same port.
     */
    public interface Delegate {
        @Nullable Object portClaimOwner();
    }

    /** Whether a live claim exists at exactly this port, ignoring the face rule. For tests and the audit. */
    public static boolean claimed(Level level, long cell, ForgeDirection side, Object user, Object type, int role) {
        user = owner(user);
        PortClaims claims = of(level, false);
        if (claims == null) return false;
        ArrayList<Claim> list = claims.byCell.get(cell);
        if (list == null) return false;
        long now = System.currentTimeMillis();
        int s = side.ordinal();
        for (int i = 0, n = list.size(); i < n; i++) {
            Claim c = list.get(i);
            if (c.user == user && c.type == type && c.side == s && c.role == role) return now - c.time <= TIMEOUT_MS;
        }
        return false;
    }

    /** Whether {@code cls} has ever claimed {@code role} for {@code type}: from then on, that role is per port. */
    public static boolean perPort(Class<?> cls, Object type, int role) {
        ReferenceOpenHashSet<Object> learnt = (role == PROVIDE ? PROVIDES : SUBSCRIBES).get(cls);
        return learnt != null && learnt.contains(type);
    }

    /** What this table knows about {@code user}'s roles for {@code type}, for a failure message. */
    public static String describe(Object user, Object type) {
        Class<?> cls = owner(user).getClass();
        StringBuilder out = new StringBuilder(cls.getName()).append(' ').append(type).append(':');
        for (int role : new int[]{PROVIDE, SUBSCRIBE}) {
            var learnt = (role == PROVIDE ? PROVIDES : SUBSCRIBES).get(cls);
            var seen = (role == PROVIDE ? PROVIDES_SEEN : SUBSCRIBES_SEEN).get(cls);
            out.append(role == PROVIDE ? " provide=" : " subscribe=")
                    .append(learnt != null && learnt.contains(type) ? "per-port"
                            : seen != null && seen.containsKey(type) ? "first asked at tick " + seen.getLong(type) : "never asked");
        }
        return out.toString();
    }

    /** Drops expired claims now and then, so a machine that went away does not leave its ports behind forever. */
    private void sweep(long now) {
        if (now - lastSweep < SWEEP_MS) return;
        lastSweep = now;
        for (ObjectIterator<Long2ObjectMap.Entry<ArrayList<Claim>>> it = byCell.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            ArrayList<Claim> list = it.next().getValue();
            list.removeIf(c -> now - c.time > TIMEOUT_MS);
            if (list.isEmpty()) it.remove();
        }
    }

    private static final class Claim {
        final Object user;
        final Object type;
        final int side;
        final int role;
        long time;

        Claim(Object user, Object type, int side, int role, long time) {
            this.user = user;
            this.type = type;
            this.side = side;
            this.role = role;
            this.time = time;
        }
    }
}
