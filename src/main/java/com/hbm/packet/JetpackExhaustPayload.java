package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** S2C exhaust cue for the old remote jetpack plume effects. */
public record JetpackExhaustPayload(int playerId, int mode) implements GolemPayload {

    public static final GolemCodec<JetpackExhaustPayload> CODEC = GolemCodec.of(JetpackExhaustPayload.class,
            Ids.of(Constants.MOD_ID, "jetpack_exhaust"),
            (payload, buffer) -> {
                buffer.writeInt(payload.playerId);
                buffer.writeByte(payload.mode);
            },
            buffer -> new JetpackExhaustPayload(buffer.readInt(), buffer.readByte()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
