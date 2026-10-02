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

    /**
     * What "the held item changed" compares. The item, as in 1.7; a receiver ported from old-hbm (1.12
     * {@code ItemStack.areItemsEqual}: item and damage) answers with its damage included.
     */
    default Object equipKey(ItemStack stack) {
        return stack.getItem();
    }

    /**
     * old-hbm (1.12) also told a receiver when it entered the <em>off</em> hand ({@code onEquip(player, OFF_HAND)}); 1.7
     * had no off hand. Receivers ported from old-hbm override this; the default ignores the off hand.
     */
    default void onEquipOffHand(Player player, ItemStack stack) {}

}
