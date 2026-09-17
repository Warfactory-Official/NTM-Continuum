package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** Gun casing snapshot with the client-selected legacy animation-mode discriminator. */
public record GunCasingEjectPayload(double x, double y, double z, double mX, double mY, double mZ,
        float yaw, float pitch, float mPitch, float mYaw, String casing, boolean smoking, int smokeLife,
        double smokeLift, int nodeLife, boolean legacy) implements GolemPayload {

    public static final GolemCodec<GunCasingEjectPayload> CODEC = GolemCodec.of(GunCasingEjectPayload.class,
            Ids.of(Constants.MOD_ID, "gun_casing_eject"),
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
                buffer.writeBoolean(payload.legacy);
            },
            buffer -> new GunCasingEjectPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readUtf(256),
                    buffer.readBoolean(), buffer.readInt(), buffer.readDouble(), buffer.readInt(),
                    buffer.readBoolean()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
