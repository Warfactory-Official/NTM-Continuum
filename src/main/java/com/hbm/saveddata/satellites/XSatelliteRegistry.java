package com.hbm.saveddata.satellites;

import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code XSatelliteRegistry}: which item launches which satellite, and the orbit entry point.
 *
 * <p>1.7 registers the eleven {@code satellite} variants, then {@code sat_gerald} for Horizons, then "all the
 * legacy crap" ({@code sat_mapper}, {@code sat_scanner}, {@code sat_radar}, {@code sat_laser},
 * {@code sat_resonator}, {@code sat_miner}, {@code sat_lunar_miner}). {@code registerSatellite} refuses any class
 * that is already a value in the map, and every legacy chip names a class a variant already claimed, so none of
 * those seven calls registers anything: in 1.7 a legacy chip carries a frequency but launches nothing. That is
 * ported as it behaves, not as it reads. {@code sat_foeq}, {@code sat_relay} and {@code sat_chip} were never
 * registered at all.
 */
public final class XSatelliteRegistry {

    private XSatelliteRegistry() {}

    /** The satellite class an item launches, or null where it launches nothing. */
    public static @Nullable SatelliteType typeFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemSatellite satellite) {
            EnumSatType chip = satellite.typeOf(stack);
            return chip == null ? null : forChip(chip);
        }
        if (stack.getItem() == ModSatelliteItems.sat_gerald.get()) return SatelliteType.HORIZONS;
        return null;
    }

    /** 1.7 {@code registerSatellite(..., new ComparableStack(ModItems.satellite, 1, EnumSatType.X))}. */
    public static SatelliteType forChip(EnumSatType chip) {
        return switch (chip) {
            case SPY -> SatelliteType.MAPPER;
            case SCANNER -> SatelliteType.SCANNER;
            case RADAR -> SatelliteType.RADAR;
            case DEATH_RAY -> SatelliteType.DEATH_RAY;
            case XENIUM_RESONATOR -> SatelliteType.RESONATOR;
            case RELAY -> SatelliteType.RELAY;
            case MINER_ASTRO -> SatelliteType.MINER;
            case MINER_LUNAR -> SatelliteType.LUNAR_MINER;
            case PRECISION_LASER -> SatelliteType.PRECISION_LASER;
            case DETECTOR -> SatelliteType.DETECTOR;
            case RAY_SCAN -> SatelliteType.RAY_SCAN;
        };
    }

    /** The item that launches a satellite class (1.7 NEI's reverse lookup); empty for none. */
    public static ItemStack stackFor(SatelliteType type) {
        if (type == SatelliteType.HORIZONS) return new ItemStack(ModSatelliteItems.sat_gerald.get());
        for (EnumSatType chip : EnumSatType.VALUES) {
            if (forChip(chip) == type) return ItemSatellite.make(chip);
        }
        return ItemStack.EMPTY;
    }

    /** 1.7 {@code createFromItem}. */
    public static @Nullable Satellite createFromItem(ItemStack stack) {
        SatelliteType type = typeFor(stack);
        return type == null ? null : type.create();
    }

    /** 1.7 {@code createFromId}. */
    public static @Nullable Satellite createFromId(int id) {
        SatelliteType type = SatelliteType.fromId(id);
        return type == null ? null : type.create();
    }

    /**
     * 1.7 {@code orbit}: a frequency already in orbit is not replaced; the payload is handed to the satellite
     * that is already there ({@code onPartDelivered}). Otherwise the item's satellite, if it has one, is created,
     * stored and told it reached orbit.
     */
    public static void orbit(ServerLevel level, ItemStack stack, int freq, double x, double y, double z) {
        SatelliteSavedData data = SatelliteSavedData.get(level);
        Satellite existing = data.sats.get(freq);

        if (existing != null) {
            existing.onPartDelivered(level, stack);
        } else {
            Satellite sat = createFromItem(stack);

            if (sat != null) {
                data.sats.put(freq, sat);
                sat.onOrbit(level, x, y, z);
                data.setDirty();
            }
        }
    }
}
