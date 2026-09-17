package com.hbm.items.weapon.sedna.mods;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import dev.golem.core.world.CompatEnchantments;

/** Drill magnet/sifter enchantment mutation, resolved through the dynamic server registry. */
public final class WeaponModDrillFortune extends WeaponModBase {
    private final int addFortune;

    public WeaponModDrillFortune(int id, String slot, int fortune) {
        super(id, slot);
        setPriority(PRIORITY_ADDITIVE);
        addFortune = fortune;
    }

    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) { return base; }

    private void adjust(ServerLevel level, ItemStack gun, int delta) { CompatEnchantments.adjustFortune(level, gun, delta); }

    @Override public void onInstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) { adjust(level, gun, addFortune); }
    @Override public void onUninstall(ServerLevel level, ItemStack gun, ItemStack mod, int index) { adjust(level, gun, -addFortune); }
    @Override public void onInstall(ItemStack gun, ItemStack mod, int index) { throw new IllegalStateException("Drill fortune upgrades require a ServerLevel"); }
    @Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { throw new IllegalStateException("Drill fortune upgrades require a ServerLevel"); }
}
