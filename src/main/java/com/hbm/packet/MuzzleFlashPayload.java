package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** S2C: a tracked gun holder fired; the client records the observer-side flash timestamp. */
public record MuzzleFlashPayload(int entityId) implements GolemPayload {

    public static final GolemCodec<MuzzleFlashPayload> CODEC = GolemCodec.of(MuzzleFlashPayload.class,
            Ids.of(Constants.MOD_ID, "muzzle_flash"),
            (payload, buffer) -> buffer.writeVarInt(payload.entityId),
            buffer -> new MuzzleFlashPayload(buffer.readVarInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
