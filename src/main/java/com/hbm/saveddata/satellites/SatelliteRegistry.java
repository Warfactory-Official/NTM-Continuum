package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The satellite id table. A satellite persists under its slot here, so the order is a save-format contract
 * and must never change; a slot whose satellite is not ported yet stays empty rather than being closed up.
 *
 * <p>Upstream keeps a {@code List<Class<? extends Satellite>>} and reflects on it to construct one. Slots
 * hold a factory here instead, which is what lets an unported slot be an explicit hole rather than a
 * reflection failure at load time.
 *
 * <p>Six slots are empty, each waiting on something other than the satellite itself: 0 the mapper (the
 * pollution subsystem), 2 the radar (the radar machine), 3 and 10 the two lasers (their orbital strike
 * entities), 8 the Horizons probe (its payload entity and an advancement), 12 the science package (tape
 * drives).
 *
 * <p>Upstream also maps a family of per-kind {@code sat_*} chip items onto these slots, left over from
 * before the one-item-with-variants form existed. Only the variant item is ported, so a satellite is
 * identified by its variant alone.
 */
public final class SatelliteRegistry {

    private SatelliteRegistry() {}

    /** Upstream's registration order. Every slot is fixed; empty ones are not yet ported. */
    private static final Supplier<Satellite>[] SLOTS = slots();
    private static final Map<Class<? extends Satellite>, Integer> IDS = new HashMap<>();
    private static final Map<Integer, Integer> TYPE_TO_ID = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static Supplier<Satellite>[] slots() {
        Supplier<Satellite>[] slots = new Supplier[13];
        slots[1] = SatelliteScanner::new;
        slots[4] = SatelliteResonator::new;
        slots[5] = SatelliteRelay::new;
        slots[6] = SatelliteMiner::new;
        slots[7] = SatelliteLunarMiner::new;
        slots[9] = SatelliteDetector::new;
        slots[11] = SatelliteRayScan::new;
        return slots;
    }

    static {
        for (int id = 0; id < SLOTS.length; id++) {
            if (SLOTS[id] == null) continue;
            IDS.put(SLOTS[id].get().getClass(), id);
        }
    }

    static {
        bind(EnumSatType.SPY, 0);
        bind(EnumSatType.SCANNER, 1);
        bind(EnumSatType.RADAR, 2);
        bind(EnumSatType.DEATH_RAY, 3);
        bind(EnumSatType.XENIUM_RESONATOR, 4);
        bind(EnumSatType.RELAY, 5);
        bind(EnumSatType.MINER_ASTRO, 6);
        bind(EnumSatType.MINER_LUNAR, 7);
        bind(EnumSatType.DETECTOR, 9);
        bind(EnumSatType.PRECISION_LASER, 10);
        bind(EnumSatType.RAY_SCAN, 11);
        bind(EnumSatType.SCIENCE, 12);
    }

    private static void bind(EnumSatType type, int id) {
        TYPE_TO_ID.put(type.ordinal(), id);
    }

    public static @Nullable Satellite create(int id) {
        if (id < 0 || id >= SLOTS.length || SLOTS[id] == null) return null;
        return SLOTS[id].get();
    }

    public static int idOf(Class<? extends Satellite> type) {
        return IDS.getOrDefault(type, -1);
    }

    /** The slot a launchable item carries, or −1 when it is not a satellite. */
    public static int idFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof com.hbm.items.machine.ItemSatellite satellite)) {
            return -1;
        }
        EnumSatType type = satellite.typeOf(stack);
        return type == null ? -1 : TYPE_TO_ID.getOrDefault(type.ordinal(), -1);
    }
}
