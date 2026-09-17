package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModMinigunSpeedup extends WeaponModBase {
    public WeaponModMinigunSpeedup(int id) { super(id, "SPEED"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.I_ROUNDSPERCYCLE.equals(key)) return cast((Integer) base * 3, base);
        if (Receiver.F_SPREADINNATE.equals(key)) return cast((Float) base * 1.5F, base);
        return base;
    }
}
