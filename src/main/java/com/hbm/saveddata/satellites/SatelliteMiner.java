package com.hbm.saveddata.satellites;

import com.hbm.util.GameTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class SatelliteMiner extends Satellite {

    public static final MapCodec<SatelliteMiner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastOp", 0L).forGetter(sat -> sat.lastOp)
    ).apply(i, SatelliteMiner::new));

    /**
     * 1.7 stamped this off {@code System.currentTimeMillis()}; here it is {@link GameTime#millis(net.minecraft.world.level.Level)},
     * so the window survives {@code /tick freeze} and both ends share one clock domain.
     */
    public long lastOp;

    public SatelliteMiner() {
        this(0L);
    }

    protected SatelliteMiner(long lastOp) {
        super(Interfaces.NONE, Set.of(), Set.of());
        this.lastOp = lastOp;
    }

    /**
     * 1.7 {@code SatelliteMiner.getCargoForItem}; null where the item launches no mining satellite.
     */
    public static @Nullable String getCargoForStack(net.minecraft.world.item.ItemStack stack) {
        SatelliteType type = SatelliteType.fromStack(stack);
        return type == null ? null : type.cargo();
    }

    public @Nullable String getCargo() {
        return type().cargo();
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.MINER;
    }
}
