package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cross-loader C2S payload carrying a {@link BlockPos} plus a control {@link CompoundTag} — the 26.1
 * equivalent of HBM's old {@code NBTControlPacket}. The server resolves the tile at {@code pos} and, if
 * it is an {@link com.hbm.interfaces.IControlReceiver}, feeds it the tag (see
 * {@link HbmPackets#handleControl}).
 *
 * <p>The type + codec are pure-vanilla and live in common; only registration (per loader) and the
 * client send entry point differ between NeoForge and Fabric (see {@link HbmClientPackets}).
 */
public record ControlEventPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<ControlEventPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "control_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControlEventPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ControlEventPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, ControlEventPayload::data,
            ControlEventPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
