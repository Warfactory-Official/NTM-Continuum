package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: 1.7 {@code HbmAnimationPacket} for an {@code IAnimatedItem} tool (receiver and item index are always 0 there).
 * The item id travels with it so a late packet cannot start an animation on an item selected in the meantime.
 */
public record ToolAnimationPayload(int animation, String itemId) implements GolemPayload {

    public static final GolemCodec<ToolAnimationPayload> CODEC = GolemCodec.of(ToolAnimationPayload.class,
            Ids.of(Constants.MOD_ID, "tool_animation"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.animation);
                buffer.writeUtf(payload.itemId);
            },
            buffer -> new ToolAnimationPayload(buffer.readVarInt(), buffer.readUtf(256)));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
