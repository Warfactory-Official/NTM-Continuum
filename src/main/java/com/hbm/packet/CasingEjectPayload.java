package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** Immutable casing spawn snapshot; the particle owner consumes this on the client main thread. */
public record CasingEjectPayload(double x, double y, double z, double mX, double mY, double mZ,
        float yaw, float pitch, float mPitch, float mYaw, String casing, boolean smoking, int smokeLife,
        double smokeLift, int nodeLife) implements GolemPayload {

    public static final GolemCodec<CasingEjectPayload> CODEC = GolemCodec.of(CasingEjectPayload.class,
            Ids.of(Constants.MOD_ID, "casing_eject"),
            (payload, buffer) -> {
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeDouble(payload.mX);
                buffer.writeDouble(payload.mY);
                buffer.writeDouble(payload.mZ);
                buffer.writeFloat(payload.yaw);
                buffer.writeFloat(payload.pitch);
                buffer.writeFloat(payload.mPitch);
                buffer.writeFloat(payload.mYaw);
                buffer.writeUtf(payload.casing);
                buffer.writeBoolean(payload.smoking);
                buffer.writeInt(payload.smokeLife);
                buffer.writeDouble(payload.smokeLift);
                buffer.writeInt(payload.nodeLife);
            },
            buffer -> new CasingEjectPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readUtf(256),
                    buffer.readBoolean(), buffer.readInt(), buffer.readDouble(), buffer.readInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
