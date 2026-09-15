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
        if (!(level instanceof ServerLevel server)) return;
        PlasmaBlastPayload payload = new PlasmaBlastPayload(x, y, z, r, g, b, pitch, yaw, scale);
        double radiusSquared = 100D * 100D;
        for (var player : server.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSquared) {
                GolemNetwork.sendToPlayer(player, CODEC, payload);
            }
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
