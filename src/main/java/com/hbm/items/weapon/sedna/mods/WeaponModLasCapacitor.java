package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import net.minecraft.world.item.ItemStack;
public class WeaponModLasCapacitor extends WeaponModBase {
    public WeaponModLasCapacitor(int id) { super(id, "UNDERBARREL"); setPriority(PRIORITY_MULTIPLICATIVE); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base * 1.05F, base);
        if (Receiver.O_MAGAZINE.equals(key) && base instanceof MagazineFullReload original) {
            WeaponModStackMag.DUMMY_FULL.acceptedBullets = original.acceptedBullets;
            WeaponModStackMag.DUMMY_FULL.capacity = original.capacity * 3 / 2;
            WeaponModStackMag.DUMMY_FULL.index = original.index;
            return cast(WeaponModStackMag.DUMMY_FULL, base);
        }
        return base;
    }
}
