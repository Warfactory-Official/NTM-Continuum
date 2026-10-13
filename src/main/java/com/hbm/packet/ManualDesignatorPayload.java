package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMissileItems;
import com.hbm.items.tool.ItemDesignatorManual;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** C2S coordinate edit from the manual designator screen (0 add, 1 subtract, 2 set-to-player). */
public record ManualDesignatorPayload(int operator, int value, int reference) implements GolemPayload {
    public static final GolemCodec<ManualDesignatorPayload> CODEC = GolemCodec.of(ManualDesignatorPayload.class,
            Ids.of(Constants.MOD_ID, "manual_designator"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.operator());
                buffer.writeVarInt(payload.value());
                buffer.writeVarInt(payload.reference());
            }, buffer -> new ManualDesignatorPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Identifier id() { return CODEC.id(); }

    public static void handle(ServerPlayer player, ManualDesignatorPayload payload) {
        if (player == null || payload.operator < 0 || payload.operator > 2
                || payload.reference < 0 || payload.reference > 1
                || payload.value < 0 || payload.value > 100) return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ItemDesignatorManual)) return;

        int x = ModDataComponents.DESIGNATOR_X.getOr(stack, 0);
        int z = ModDataComponents.DESIGNATOR_Z.getOr(stack, 0);
        if (payload.operator == 2) {
            if (payload.reference == 0) x = (int) Math.round(player.getX());
            else z = (int) Math.round(player.getZ());
        } else {
            int delta = payload.operator == 0 ? payload.value : -payload.value;
            if (payload.reference == 0) x += delta;
            else z += delta;
        }
        ModDataComponents.DESIGNATOR_X.set(stack, x);
        ModDataComponents.DESIGNATOR_Z.set(stack, z);
        player.containerMenu.broadcastChanges();
    }
}
