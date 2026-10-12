package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload for the anvil's construction crafting — the 26.1 port of {@code AnvilCraftPacket}. Carries
 * the recipe id of the construction row (1.7 sent its index in the recipe list; the rows are datapack recipes here, and
 * a reload on the server can reorder the list before the client has the new one, so an index could craft another
 * recipe) and the mode (0 = craft one, 1 = craft up to 64). Fully validated server-side in {@link HbmPackets#handleAnvilCraft}.
 */
public record AnvilCraftPayload(String recipeId, int mode) implements GolemPayload {

    public static final GolemCodec<AnvilCraftPayload> CODEC = GolemCodec.of(AnvilCraftPayload.class,
            Ids.of(Constants.MOD_ID, "anvil_craft"),
            (payload, buffer) -> {
                buffer.writeUtf(payload.recipeId);
                buffer.writeVarInt(payload.mode);
            },
            buffer -> new AnvilCraftPayload(buffer.readUtf(256), buffer.readVarInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
