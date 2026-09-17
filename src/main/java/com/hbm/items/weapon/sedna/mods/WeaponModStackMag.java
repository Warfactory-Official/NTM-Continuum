package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import net.minecraft.world.item.ItemStack;

/** Increases detachable/tube magazine capacity by 50%, preserving its accepted rounds and index. */
public class WeaponModStackMag extends WeaponModBase {
    protected static final MagazineSingleReload DUMMY_SINGLE = new MagazineSingleReload(0, 0);
    protected static final MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);
    public WeaponModStackMag(int id) { super(id, "MAG"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (!Receiver.O_MAGAZINE.equals(key)) return base;
        if (base instanceof MagazineSingleReload original) {
            DUMMY_SINGLE.acceptedBullets = original.acceptedBullets;
            DUMMY_SINGLE.capacity = original.capacity * 3 / 2;
            DUMMY_SINGLE.index = original.index;
            return cast(DUMMY_SINGLE, base);
        }
        if (base instanceof MagazineFullReload original) {
            DUMMY_FULL.acceptedBullets = original.acceptedBullets;
            DUMMY_FULL.capacity = original.capacity * 3 / 2;
            DUMMY_FULL.index = original.index;
            return cast(DUMMY_FULL, base);
        }
        return base;
    }
    @Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
    @Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
