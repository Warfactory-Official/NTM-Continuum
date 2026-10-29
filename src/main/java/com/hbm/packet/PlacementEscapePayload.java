package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent after the placed blocks and initial block-entity geometry.
 */
public record PlacementEscapePayload(String dimension, long core) implements GolemPayload {
    public static final GolemCodec<PlacementEscapePayload> CODEC = GolemCodec.of(PlacementEscapePayload.class,
            Ids.of(Constants.MOD_ID, "placement_escape"),
            (payload, buffer) -> {
                buffer.writeUtf(payload.dimension());
                buffer.writeLong(payload.core());
            }, buffer -> new PlacementEscapePayload(buffer.readUtf(), buffer.readLong()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
