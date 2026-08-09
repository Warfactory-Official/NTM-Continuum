package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cross-loader S2C payload that carries a {@link BlockPos} plus a pre-serialized blob of bytes — the
 * 26.1 equivalent of HBM's old {@code BufPacket}. The blob is whatever the target tile's
 * {@link com.hbm.tileentity.IBufPacketReceiver#serialize} wrote; the client receiver wraps it back in
 * a {@link RegistryFriendlyByteBuf} and feeds it to {@code deserialize}.
 *
 * <p>The payload type + codec are pure-vanilla and live in common; only registration (per loader) and
 * the send/receive entry points differ between NeoForge and Fabric.
 */
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
