package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import net.minecraft.world.item.ItemStack;

/** Triples the capacity of fluid/electric drill canisters and preserves their type constraints. */
public class WeaponModCanisters extends WeaponModBase {
    public WeaponModCanisters(int id) { super(id, "CANISTERS"); setPriority(PRIORITY_MULT_FINAL); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (!Receiver.O_MAGAZINE.equals(key)) return base;
        if (base instanceof MagazineLiquidEngine original) {
            MagazineLiquidEngine replacement = new MagazineLiquidEngine(original.index, original.capacity * 3,
                    original::accepted);
            return cast(replacement, base);
        }
        if (base instanceof MagazineElectricEngine original) {
            MagazineElectricEngine replacement = new MagazineElectricEngine(original.index, original.capacity * 3);
            return cast(replacement, base);
        }
        return base;
    }
    @Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
    @Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
