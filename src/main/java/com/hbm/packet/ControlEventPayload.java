package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Cross-loader C2S payload carrying a {@link BlockPos} plus a control {@link CompoundTag} — the 26.1
 * equivalent of HBM's old {@code NBTControlPacket}. The server resolves the tile at {@code pos} and, if
 * it is an {@link com.hbm.interfaces.IControlReceiver}, feeds it the tag (see
 * {@link HbmPackets#handleControl}).
 *
 * <p>The type + codec are pure-vanilla and live in common; only registration (per loader) and the
 * client send entry point is supplied by {@link dev.golem.core.net.GolemNetwork}.
 */
public record ControlEventPayload(BlockPos pos, CompoundTag data) implements GolemPayload {

    public static final GolemCodec<ControlEventPayload> CODEC = GolemCodec.of(ControlEventPayload.class,
            Ids.of(Constants.MOD_ID, "control_event"),
            (payload, buffer) -> {
                buffer.writeBlockPos(payload.pos);
                buffer.writeNbt(payload.data);
            },
            buffer -> new ControlEventPayload(buffer.readBlockPos(), buffer.readNbt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
