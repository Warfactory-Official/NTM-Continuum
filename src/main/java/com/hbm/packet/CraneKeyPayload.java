package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** C2S edge update for one RBMK crane control key. */
public record CraneKeyPayload(int key, boolean pressed) implements GolemPayload {
    public static final GolemCodec<CraneKeyPayload> CODEC = GolemCodec.of(CraneKeyPayload.class,
            Ids.of(Constants.MOD_ID, "crane_key"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.key);
                buffer.writeBoolean(payload.pressed);
            },
            buffer -> new CraneKeyPayload(buffer.readVarInt(), buffer.readBoolean()));

    @Override public Identifier id() { return CODEC.id(); }
}
