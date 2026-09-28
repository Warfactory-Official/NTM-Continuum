package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.items.tool.ItemToolAbility;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code NBTItemControlPacket} as the tool-ability screen used it: the edited preset configuration
 * of the held tool, handed to its {@code receiveControl}, which restricts it to what the tool offers.
 */
public record ToolAbilityControlPayload(CompoundTag data) implements GolemPayload {

    public static final GolemCodec<ToolAbilityControlPayload> CODEC = GolemCodec.of(ToolAbilityControlPayload.class,
            Ids.of(Constants.MOD_ID, "tool_ability_control"),
            (payload, buffer) -> buffer.writeNbt(payload.data),
            buffer -> new ToolAbilityControlPayload(buffer.readNbt()));

    public static void handleServer(ToolAbilityControlPayload payload, ServerPlayer player) {
        if (payload.data == null) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemToolAbility tool) tool.receiveControl(held, payload.data);
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
