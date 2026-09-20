package com.hbm.uninos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * 1.7's pneumatic nodes carry no per-node data (every {@code PneumaticNode} is interchangeable), so the
 * kernel's slot holds one shared value; a record for the codec's value-based dedup.
 */
public record PneumaticData() {

    public static final PneumaticData INSTANCE = new PneumaticData();
    public static final Codec<PneumaticData> CODEC = MapCodec.unit(INSTANCE).codec();
}
