package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** Bounded C2S flight input; the authenticated transport context supplies the player identity. */
public record JetpackInputPayload(float forward, float strafe, boolean jump, boolean sneak, boolean sprint)
        implements GolemPayload {

    public static final GolemCodec<JetpackInputPayload> CODEC = GolemCodec.of(JetpackInputPayload.class,
            Ids.of(Constants.MOD_ID, "jetpack_input"),
            (payload, buffer) -> {
                buffer.writeFloat(payload.forward);
                buffer.writeFloat(payload.strafe);
                buffer.writeBoolean(payload.jump);
                buffer.writeBoolean(payload.sneak);
                buffer.writeBoolean(payload.sprint);
            },
            buffer -> new JetpackInputPayload(buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean()));

    public JetpackInputPayload {
        forward = clamp(forward);
        strafe = clamp(strafe);
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0F;
        return Math.max(-1F, Math.min(1F, value));
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
