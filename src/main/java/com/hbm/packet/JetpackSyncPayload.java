package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** S2C authoritative advanced-jetpack state, replacing 1.12's {@code JetpackSyncPacket}. */
public record JetpackSyncPayload(int playerId, boolean opening, boolean hover, float thrust,
                                 int jetpackFlyTime, boolean useCompactHUD, int failureTicks,
                                 boolean backpackEnabled, boolean hudEnabled,
                                 boolean jetpackKeyDown) implements GolemPayload {

    public static final GolemCodec<JetpackSyncPayload> CODEC = GolemCodec.of(JetpackSyncPayload.class,
            Ids.of(Constants.MOD_ID, "jetpack_sync"),
            (payload, buffer) -> {
                buffer.writeInt(payload.playerId);
                buffer.writeBoolean(payload.opening);
                buffer.writeBoolean(payload.hover);
                buffer.writeFloat(payload.thrust);
                buffer.writeInt(payload.jetpackFlyTime);
                buffer.writeBoolean(payload.useCompactHUD);
                buffer.writeInt(payload.failureTicks);
                buffer.writeBoolean(payload.backpackEnabled);
                buffer.writeBoolean(payload.hudEnabled);
                buffer.writeBoolean(payload.jetpackKeyDown);
            },
            buffer -> new JetpackSyncPayload(buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readFloat(), buffer.readInt(), buffer.readBoolean(), buffer.readInt(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
