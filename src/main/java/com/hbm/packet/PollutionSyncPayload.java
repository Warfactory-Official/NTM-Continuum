package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: the pollution of the region the player stands in (soot, poison, heavy metal, fallout), 1.7's "POLLUTION" block of
 * {@code PermaSyncHandler}. 1.7 wrote it to every player every tick; the port sends it when it changed for that
 * player, which is the same information. The client keeps it in {@link com.hbm.client.SmogFog} for the smog.
 */
public record PollutionSyncPayload(float soot, float poison, float heavyMetal, float fallout) implements GolemPayload {

    public static final GolemCodec<PollutionSyncPayload> CODEC = GolemCodec.of(PollutionSyncPayload.class,
            Ids.of(Constants.MOD_ID, "pollution_sync"),
            (payload, buffer) -> {
                buffer.writeFloat(payload.soot);
                buffer.writeFloat(payload.poison);
                buffer.writeFloat(payload.heavyMetal);
                buffer.writeFloat(payload.fallout);
            },
            buffer -> new PollutionSyncPayload(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
