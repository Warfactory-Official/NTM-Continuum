package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: the gun in the player's hand just did something worth showing.
 *
 * <p>Ported from 1.12.2 {@code GunAnimationPacketSedna}. The item id is carried alongside the old
 * animation/config fields so a delayed packet cannot apply an akimbo rail to a different gun selected
 * in the meantime.
 */
public record GunAnimationPayload(int animation, int receiver, int configIndex, String itemId) implements GolemPayload {

    public static final GolemCodec<GunAnimationPayload> CODEC = GolemCodec.of(GunAnimationPayload.class,
            Ids.of(Constants.MOD_ID, "gun_animation"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.animation);
                buffer.writeVarInt(payload.receiver);
                buffer.writeVarInt(payload.configIndex);
                buffer.writeUtf(payload.itemId);
            },
            buffer -> new GunAnimationPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(256)));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
