package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload syncing the local player's {@link com.hbm.entity.EntityHbmProps} — the 26.1 replacement
 * for the old {@code HbmPlayerSyncPacket}. The blob is the props' versioned ByteBuf serialization
 * ({@code EntityHbmProps.serialize}); the client applies it to the local player's duck props. Sent
 * only to the owning player (every 5 ticks from {@code EntityEffectHandler}), so no entity id is
 * carried.
 */
public record EntityPropsSyncPayload(byte[] data) implements GolemPayload {

    public static final GolemCodec<EntityPropsSyncPayload> CODEC = GolemCodec.of(EntityPropsSyncPayload.class,
            Ids.of(Constants.MOD_ID, "props_sync"),
            (payload, buffer) -> buffer.writeByteArray(payload.data),
            buffer -> new EntityPropsSyncPayload(buffer.readByteArray()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
