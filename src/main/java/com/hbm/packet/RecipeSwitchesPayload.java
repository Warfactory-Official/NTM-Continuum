package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** S2C, with every datapack sync: the server's {@code GeneralConfig.recipeSwitches()}, so tables list its rows. */
public record RecipeSwitchesPayload(int bits) implements GolemPayload {

    public static final GolemCodec<RecipeSwitchesPayload> CODEC = GolemCodec.of(RecipeSwitchesPayload.class,
            Ids.of(Constants.MOD_ID, "recipe_switches"),
            (payload, buffer) -> buffer.writeVarInt(payload.bits),
            buffer -> new RecipeSwitchesPayload(buffer.readVarInt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
