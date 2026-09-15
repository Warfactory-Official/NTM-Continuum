package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModSlowdown extends WeaponModBase {
    public WeaponModSlowdown(int id) { super(id, "SPEED"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.I_DELAYAFTERFIRE.equals(key)) return cast((Integer) base * 2, base);
        if (Receiver.F_SPREADINNATE.equals(key)) return cast(0F, base);
        return base;
    }
}
