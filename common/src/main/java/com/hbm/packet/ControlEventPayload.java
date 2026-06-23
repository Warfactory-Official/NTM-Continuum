package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
