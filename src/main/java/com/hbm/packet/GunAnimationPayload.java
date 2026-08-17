package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: the gun in the player's hand just did something worth showing.
 *
 * <p>Ported from 1.12.2 {@code GunAnimationPacketSedna}. The animation system it drives is not ported
 * yet, and the handler no-ops on an unknown cue exactly as upstream's does — but the packet is carried
 * regardless, because it is also what tells the client <i>when</i> a shot happened, which the muzzle
 * flash and recoil need. Without it those would have to guess from synced item state, which arrives a
 * tick or more late.
 */
public record GunAnimationPayload(int animation, int receiver, int configIndex) implements GolemPayload {

    public static final GolemCodec<GunAnimationPayload> CODEC = GolemCodec.of(GunAnimationPayload.class,
            Ids.of(Constants.MOD_ID, "gun_animation"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.animation);
                buffer.writeVarInt(payload.receiver);
                buffer.writeVarInt(payload.configIndex);
            },
            buffer -> new GunAnimationPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
