package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** S2C: the source Sedna plasma-blast shockwave, not an explosion substitute. */
public record PlasmaBlastPayload(double x, double y, double z, float r, float g, float b,
        float pitch, float yaw, float scale) implements GolemPayload {

    public static final GolemCodec<PlasmaBlastPayload> CODEC = GolemCodec.of(PlasmaBlastPayload.class,
            Ids.of(Constants.MOD_ID, "plasma_blast"),
            (payload, buffer) -> {
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeFloat(payload.r);
                buffer.writeFloat(payload.g);
                buffer.writeFloat(payload.b);
                buffer.writeFloat(payload.pitch);
                buffer.writeFloat(payload.yaw);
                buffer.writeFloat(payload.scale);
            },
            buffer -> new PlasmaBlastPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat()));

    /** Source TargetPoint radius, retained through the existing loader-neutral send-to-player seam. */
    public static void send(Level level, double x, double y, double z, float r, float g, float b,
            float pitch, float yaw, float scale) {
        send(level, x, y, z, r, g, b, pitch, yaw, scale, 100D);
    }

    public static void send(Level level, double x, double y, double z, float r, float g, float b,
            float pitch, float yaw, float scale, double range) {
        sendAround(level, x, y, z, range, new PlasmaBlastPayload(x, y, z, r, g, b, pitch, yaw, scale));
    }

    /** 1.7's {@code TargetPoint}: every player within {@code range} of the centre, wherever the blast itself is. */
    public static void sendAround(Level level, double centerX, double centerY, double centerZ, double range,
            PlasmaBlastPayload payload) {
        if (!(level instanceof ServerLevel server)) return;
        double radiusSquared = range * range;
        for (var player : server.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) <= radiusSquared) {
                GolemNetwork.sendToPlayer(player, CODEC, payload);
            }
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
