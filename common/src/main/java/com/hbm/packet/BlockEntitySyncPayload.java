package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlockEntitySyncPayload(BlockPos pos, byte[] data) implements CustomPacketPayload {

    public static final Type<BlockEntitySyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "be_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockEntitySyncPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockEntitySyncPayload::pos,
            ByteBufCodecs.BYTE_ARRAY, BlockEntitySyncPayload::data,
            BlockEntitySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
