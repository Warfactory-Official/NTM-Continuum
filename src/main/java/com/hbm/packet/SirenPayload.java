package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.items.machine.ItemCassette;

/**
 * 1.7 {@code TESirenPacket}: the siren at {@code pos} says which track ({@link ItemCassette.TrackType} ordinal)
 * should be playing and whether it is active. Sent every server tick to every player within 1500 blocks, as
 * upstream did through {@code sendToAllAround}; the client handler is idempotent, so the repetition is what
 * lets a player who walks into range start hearing a siren that was already on.
 */
public record SirenPayload(BlockPos pos, int track, boolean active) implements GolemPayload {

    public static final GolemCodec<SirenPayload> CODEC = GolemCodec.of(SirenPayload.class,
            Ids.of(Constants.MOD_ID, "siren"),
            (payload, buffer) -> {
                buffer.writeBlockPos(payload.pos);
                buffer.writeVarInt(payload.track);
                buffer.writeBoolean(payload.active);
            },
            buffer -> new SirenPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean()));

    public static final double RANGE = 1500D;

    public static void send(ServerLevel level, BlockPos pos, int track, boolean active) {
        SirenPayload payload = new SirenPayload(pos.immutable(), track, active);
        double radiusSquared = RANGE * RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > radiusSquared) continue;
            // A sweep this wide reaches every player in the level, including one whose connection never
            // negotiated our channel (a bare in-level test mock, a client without the mod); the loader
            // throws on such a send rather than dropping it, so ask first.
            if (!GolemNetwork.canSendToPlayer(player, CODEC)) continue;
            GolemNetwork.sendToPlayer(player, CODEC, payload);
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
