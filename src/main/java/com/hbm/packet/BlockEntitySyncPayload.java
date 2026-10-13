package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Cross-loader S2C payload that carries a {@link BlockPos} plus a pre-serialized blob of bytes, the
 * 26.1 equivalent of HBM's old {@code BufPacket}. The blob is whatever the target tile's
 * {@link com.hbm.tileentity.IBufPacketReceiver#serialize} wrote; the client receiver wraps it back in
 * a {@link RegistryFriendlyByteBuf} and feeds it to {@code deserialize}.
 *
 * <p>The payload type + codec are pure-vanilla and live in common; only registration (per loader) and
 * the send/receive entry points differ between NeoForge and Fabric.
 */
public record BlockEntitySyncPayload(BlockPos pos, byte[] data) implements GolemPayload {

    public static final GolemCodec<BlockEntitySyncPayload> CODEC = GolemCodec.of(BlockEntitySyncPayload.class,
            Ids.of(Constants.MOD_ID, "be_sync"),
            (payload, buffer) -> {
                buffer.writeBlockPos(payload.pos);
                buffer.writeByteArray(payload.data);
            },
            buffer -> new BlockEntitySyncPayload(buffer.readBlockPos(), buffer.readByteArray()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
