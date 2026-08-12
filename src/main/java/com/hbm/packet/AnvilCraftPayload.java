package com.hbm.packet;

import com.hbm.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload for the anvil's construction crafting — the 26.1 port of {@code AnvilCraftPacket}. Carries
 * the index into {@link com.hbm.inventory.recipes.anvil.AnvilRecipes#getConstruction()} (the list is a
 * deterministic function of registered content, so client and server agree) and the mode (0 = craft one,
 * 1 = craft up to 64). Fully validated server-side in {@link HbmPackets#handleAnvilCraft}.
 */
public record AnvilCraftPayload(int recipeIndex, int mode) implements CustomPacketPayload {

    public static final Type<AnvilCraftPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "anvil_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilCraftPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnvilCraftPayload::recipeIndex,
            ByteBufCodecs.VAR_INT, AnvilCraftPayload::mode,
            AnvilCraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
