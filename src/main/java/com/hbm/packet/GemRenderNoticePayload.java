package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C, on a player's first join of a world: the client says in chat that GemRender is missing, if it is. Only the
 * client knows its own mods, so the server cannot decide this itself.
 */
public record GemRenderNoticePayload() implements GolemPayload {

    public static final GolemCodec<GemRenderNoticePayload> CODEC = GolemCodec.of(GemRenderNoticePayload.class,
            Ids.of(Constants.MOD_ID, "gemrender_notice"), (payload, buffer) -> {}, buffer -> new GemRenderNoticePayload());

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
