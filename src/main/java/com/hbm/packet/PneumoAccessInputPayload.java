package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;

/**
 * Client to server: a click on one of the access terminal's virtual slots, naming the type by the id the menu
 * handed out. 1.7 {@code ContainerPneumoStorageAccess.sendClickToServer}.
 */
public record PneumoAccessInputPayload(int containerId, int op, int entryId) implements GolemPayload {

    public static final GolemCodec<PneumoAccessInputPayload> CODEC = GolemCodec.of(PneumoAccessInputPayload.class,
            Ids.of(Constants.MOD_ID, "pneumo_access_input"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.containerId);
                buffer.writeByte(payload.op);
                buffer.writeVarInt(payload.entryId);
            },
            buffer -> new PneumoAccessInputPayload(buffer.readVarInt(), buffer.readByte(), buffer.readVarInt()));

    public static void handleServer(PneumoAccessInputPayload payload, ServerPlayer player) {
        // vanilla refuses a spectator's container clicks the same way
        if (player.isSpectator()) return;
        if (!(player.containerMenu instanceof ContainerPneumoStorageAccess menu)) return;
        if (menu.containerId != payload.containerId || !menu.stillValid(player)) return;
        ContainerPneumoStorageAccess.ClickOp[] ops = ContainerPneumoStorageAccess.ClickOp.values();
        if (payload.op < 0 || payload.op >= ops.length) return;
        menu.handleClick(ops[payload.op], payload.entryId);
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
