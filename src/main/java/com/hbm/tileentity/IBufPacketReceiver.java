package com.hbm.tileentity;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * A {@link net.minecraft.world.level.block.entity.BlockEntity} that syncs to the client by cramming
 * arbitrary data into a network buffer (the 1.12.2 "BufPacket" mechanism), rather than via vanilla's
 * NBT update tag. Ported from 1.12.2 with {@code ByteBuf} → {@link RegistryFriendlyByteBuf} so item
 * stacks and other registry-aware values can be streamed.
 *
 * <p>The send/receive plumbing is cross-loader: {@link TileEntityLoadedBase#networkPackNT()} compiles
 * the payload server-side and {@code GolemNetwork.sendToTracking} ships it; the registered
 * client receiver resolves the tile and calls {@link #deserialize(RegistryFriendlyByteBuf)}.
 */
public interface IBufPacketReceiver {

    void serialize(RegistryFriendlyByteBuf buf);

    void deserialize(RegistryFriendlyByteBuf buf);
}
