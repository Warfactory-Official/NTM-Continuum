package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModShredderSpeedup extends WeaponModBase {
    public WeaponModShredderSpeedup(int id) { super(id, "SPEED"); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.I_DELAYAFTERFIRE.equals(key) || Receiver.I_DELAYAFTERDRYFIRE.equals(key)) return cast((Integer) base / 2, base);
        return base;
    }
}
