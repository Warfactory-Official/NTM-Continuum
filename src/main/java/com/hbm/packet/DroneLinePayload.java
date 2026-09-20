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

/**
 * 1.7 {@code ParticleUtil.spawnDroneLine}: the {@code debugdrone} aux particle, a coloured line from a drone node
 * towards its link, shown only to a player holding one of the drone family's items. 1.7 sent it 150 blocks around.
 */
public record DroneLinePayload(double x, double y, double z, double dx, double dy, double dz, int color)
        implements GolemPayload {

    public static final GolemCodec<DroneLinePayload> CODEC = GolemCodec.of(DroneLinePayload.class,
            Ids.of(Constants.MOD_ID, "drone_line"),
            (payload, buffer) -> {
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeFloat((float) payload.dx);
                buffer.writeFloat((float) payload.dy);
                buffer.writeFloat((float) payload.dz);
                buffer.writeInt(payload.color);
            },
            buffer -> new DroneLinePayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt()));

    public static final double RANGE = 150;

    public static void send(ServerLevel level, double x, double y, double z, double dx, double dy, double dz, int color) {
        DroneLinePayload payload = new DroneLinePayload(x, y, z, dx, dy, dz, color);
        double radiusSquared = RANGE * RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSquared) {
                GolemNetwork.sendToPlayer(player, CODEC, payload);
            }
        }
    }

    public static void send(ServerLevel level, BlockPos from, BlockPos to, double lengthFraction, int color) {
        send(level, from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5,
                (to.getX() - from.getX()) * lengthFraction, (to.getY() - from.getY()) * lengthFraction,
                (to.getZ() - from.getZ()) * lengthFraction, color);
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
