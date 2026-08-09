package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload syncing the local player's {@link com.hbm.entity.EntityHbmProps} — the 26.1 replacement
 * for the old {@code HbmPlayerSyncPacket}. The blob is the props' versioned ByteBuf serialization
 * ({@code EntityHbmProps.serialize}); the client applies it to the local player's duck props. Sent
 * only to the owning player (every 5 ticks from {@code EntityEffectHandler}), so no entity id is
 * carried.
 */
public record EntityPropsSyncPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<EntityPropsSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "props_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPropsSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY, EntityPropsSyncPayload::data,
            EntityPropsSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
