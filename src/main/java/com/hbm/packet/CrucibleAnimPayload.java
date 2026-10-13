package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: 1.12.2's {@code AuxParticlePacketNT(HbmEffectNT.Anim, {hand, mode, name})} as the sword cutters sent it to
 * their holder: {@link #EQUIP} ({@code "equip"}, the Collada deploy) and {@link #CSWING} ({@code "cSwing"}, the
 * crucible's random swing). Always the main hand; the client keys the animation to the item it then holds.
 */
public record CrucibleAnimPayload(byte mode) implements GolemPayload {

    public static final byte EQUIP = 0;
    public static final byte CSWING = 1;

    public static final GolemCodec<CrucibleAnimPayload> CODEC = GolemCodec.of(CrucibleAnimPayload.class,
            Ids.of(Constants.MOD_ID, "crucible_anim"),
            (payload, buffer) -> buffer.writeByte(payload.mode),
            buffer -> new CrucibleAnimPayload(buffer.readByte()));

    /** To the holder only. */
    public static void send(ServerPlayer player, byte mode) {
        GolemNetwork.sendToPlayer(player, CODEC, new CrucibleAnimPayload(mode));
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
