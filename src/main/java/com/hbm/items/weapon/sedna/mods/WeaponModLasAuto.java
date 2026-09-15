package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModLasAuto extends WeaponModBase {
    public WeaponModLasAuto(int id) { super(id, "RECEIVER"); setPriority(PRIORITY_SET); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base * 0.66F, base);
        if (Receiver.B_REFIREONHOLD.equals(key)) return cast(true, base);
        if (Receiver.I_DELAYAFTERFIRE.equals(key)) return cast(5, base);
        return GunConfig.O_SCOPETEXTURE.equals(key) ? cast(null, base) : base;
    }
}
