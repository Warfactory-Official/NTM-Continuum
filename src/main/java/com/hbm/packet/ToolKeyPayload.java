package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** C2S edge update for the copy tool's two modifier keys (1.7 {@code TOOL_ALT} / {@code TOOL_CTRL}). */
public record ToolKeyPayload(int key, boolean pressed) implements GolemPayload {
    public static final GolemCodec<ToolKeyPayload> CODEC = GolemCodec.of(ToolKeyPayload.class,
            Ids.of(Constants.MOD_ID, "tool_key"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.key);
                buffer.writeBoolean(payload.pressed);
            },
            buffer -> new ToolKeyPayload(buffer.readVarInt(), buffer.readBoolean()));

    @Override public Identifier id() { return CODEC.id(); }
}
