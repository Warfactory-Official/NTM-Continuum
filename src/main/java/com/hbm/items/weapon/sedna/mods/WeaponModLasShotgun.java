package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModLasShotgun extends WeaponModBase {
    public WeaponModLasShotgun(int id) { super(id, "BARREL"); setPriority(PRIORITY_MULTIPLICATIVE); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base * 0.35F, base);
        if (Receiver.F_SPLITPROJECTILES.equals(key)) return cast((Float) base * 3F, base);
        if (Receiver.F_SPREADINNATE.equals(key)) return cast((Float) base + 3F, base);
        if (Receiver.F_SPREADHIPFIRE.equals(key)) return cast(0F, base);
        return GunConfig.O_CROSSHAIR.equals(key) ? cast(Crosshair.L_CIRCLE, base) : base;
    }
}
