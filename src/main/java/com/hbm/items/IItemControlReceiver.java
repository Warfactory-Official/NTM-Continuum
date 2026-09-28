package com.hbm.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code IItemControlReceiver}: a held item that takes a control tag from its own client screen
 * ({@code NBTItemControlPacket}), the way a block entity takes one through {@code IControlReceiver}.
 */
public interface IItemControlReceiver {

    void receiveControl(Player player, ItemStack stack, CompoundTag data);
}
