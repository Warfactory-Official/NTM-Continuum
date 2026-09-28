package com.hbm.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code IEquipReceiver}: told on the server when a player switches to this item — the held item's
 * <em>item</em> changed since the last tick (1.7 {@code ModEventHandler.onLivingUpdate} against
 * {@code previousEquipment[0]}). Driven by {@code com.hbm.handler.EquipReceiverHandler}.
 */
public interface IEquipReceiver {

    void onEquip(Player player, ItemStack stack);
}
