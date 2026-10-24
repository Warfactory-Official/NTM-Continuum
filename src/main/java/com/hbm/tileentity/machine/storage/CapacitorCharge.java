package com.hbm.tileentity.machine.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Stored HE and its ceiling travel together when a capacitor is carried. */
public record CapacitorCharge(long power, long maxPower) {
    public static final Codec<CapacitorCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("power").forGetter(CapacitorCharge::power),
            Codec.LONG.fieldOf("max_power").forGetter(CapacitorCharge::maxPower)
    ).apply(instance, CapacitorCharge::new));

    /** Exact percent thresholds avoid overflowing power * 100 for custom ceilings. */
    public static int fillPercent(long power, long ceiling) {
        var hundreds = ceiling / 100;
        var remainder = ceiling % 100;
        var low = 0;
        var high = 100;
        while (low < high) {
            var middle = (low + high + 1) >>> 1;
            var threshold = hundreds * middle + (remainder * middle + 99) / 100;
            if (power >= threshold) low = middle;
            else high = middle - 1;
        }
        return low;
    }
}
