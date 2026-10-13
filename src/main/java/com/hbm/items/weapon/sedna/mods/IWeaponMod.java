package com.hbm.items.weapon.sedna.mods;

import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

/** A stack-installed weapon modifier. Lower-level stat evaluation is generic. */
public interface IWeaponMod {
    int getModPriority();
    String[] getSlots();
    <T> T eval(T base, ItemStack gun, String key, Object parent);
    default void onInstall(ItemStack gun, ItemStack mod, int index) {}
    default void onUninstall(ItemStack gun, ItemStack mod, int index) {}

    default void onInstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        onInstall(gun, mod, index);
    }

    default void onUninstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) {
        onUninstall(gun, mod, index);
    }
}
