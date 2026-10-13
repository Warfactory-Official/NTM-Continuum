package com.hbm.saveddata.satellites;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code SatelliteMiner}, the asteroid mining ship: a cargo landing pad tuned to it receives a miner rocket
 * every ten minutes carrying a draw from its item pool.
 */
public class SatelliteMiner extends Satellite {

    public static final MapCodec<SatelliteMiner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastOp", 0L).forGetter(sat -> sat.lastOp)
    ).apply(i, SatelliteMiner::new));

    /**
     * Wall-clock milliseconds of the last delivery, 1.7 {@code System.currentTimeMillis()} as the cargo pad
     * stamps it: 0 on a fresh satellite, so its first rocket comes at once, then one every ten minutes of real
     * time.
     */
    public long lastOp;

    public SatelliteMiner() {
        this(0L);
    }

    protected SatelliteMiner(long lastOp) {
        this.lastOp = lastOp;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.MINER;
    }

    @Override public String getType() { return "ASTEROID_MINER"; }

    /** 1.7 writeToNBT/readFromNBT store only {@code lastOp}; the base target is not saved. */
    @Override
    public boolean savesBaseData() {
        return false;
    }

    /** 1.7 {@code SatelliteMiner.getCargoForItem}; null where the item launches no mining satellite. */
    public static @Nullable String getCargoForItem(ItemStack stack) {
        SatelliteType type = XSatelliteRegistry.typeFor(stack);
        return type == null ? null : type.cargo();
    }

    public @Nullable String getCargo() {
        return type().cargo();
    }
}
