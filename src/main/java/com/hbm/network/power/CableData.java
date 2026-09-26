package com.hbm.network.power;

import com.mojang.serialization.Codec;

/**
 * The conductor datum for the power graph, and it carries NOTHING - a tier field was inert until it silently partitioned a world's power grid; rate limiting is the diode's job.
 */
public record CableData() {

    public static final CableData INSTANCE = new CableData();

    /**
     * Numerically lenient on purpose: pre-v3 saves wrote the tier int at this position and any of them decodes to the singleton.
     */
    public static final Codec<CableData> CODEC = Codec.BYTE.xmap(b -> INSTANCE, d -> (byte) 0);
}
