package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: the player's level's Tom impact state (fire, dust, impact), 1.7's "TOM IMPACT DATA" block of
 * {@code PermaSyncHandler}. 1.7 wrote it to every player every tick; the port sends it when it changed for that
 * player (or the player changed level), which is the same information. The client stores it in
 * {@link com.hbm.handler.ImpactWorldHandler} against its current level.
 */
public record ImpactSyncPayload(float fire, float dust, boolean impact) implements GolemPayload {

    public static final GolemCodec<ImpactSyncPayload> CODEC = GolemCodec.of(ImpactSyncPayload.class,
            Ids.of(Constants.MOD_ID, "impact_sync"),
            (payload, buffer) -> {
                buffer.writeFloat(payload.fire);
                buffer.writeFloat(payload.dust);
                buffer.writeBoolean(payload.impact);
            },
            buffer -> new ImpactSyncPayload(buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
