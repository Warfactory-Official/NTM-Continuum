package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.items.weapon.ItemCrucible;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S: 1.12.2 {@code AuxButtonPacket(0, 0, 0, value, 1000)} from {@code ItemCrucible.updateClient}: whether the holder
 * is drawing a cut (or swinging one), which the server keeps as {@code doSpecialClick}.
 */
public record CrucibleClickPayload(boolean clicked) implements GolemPayload {

    public static final GolemCodec<CrucibleClickPayload> CODEC = GolemCodec.of(CrucibleClickPayload.class,
            Ids.of(Constants.MOD_ID, "crucible_click"),
            (payload, buffer) -> buffer.writeBoolean(payload.clicked),
            buffer -> new CrucibleClickPayload(buffer.readBoolean()));

    public static void handleServer(CrucibleClickPayload payload, ServerPlayer player) {
        ItemCrucible.setSpecialClick(player, payload.clicked);
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
