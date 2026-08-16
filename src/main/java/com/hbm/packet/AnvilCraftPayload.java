package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload for the anvil's construction crafting — the 26.1 port of {@code AnvilCraftPacket}. Carries
 * the index into {@link com.hbm.inventory.recipes.anvil.AnvilRecipes#getConstruction()} (the list is a
 * deterministic function of registered content, so client and server agree) and the mode (0 = craft one,
 * 1 = craft up to 64). Fully validated server-side in {@link HbmPackets#handleAnvilCraft}.
 */
public record AnvilCraftPayload(int recipeIndex, int mode) implements GolemPayload {

    public static final GolemCodec<AnvilCraftPayload> CODEC = GolemCodec.of(AnvilCraftPayload.class,
            Ids.of(Constants.MOD_ID, "anvil_craft"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.recipeIndex);
                buffer.writeVarInt(payload.mode);
            },
            buffer -> new AnvilCraftPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
