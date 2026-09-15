package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
public class WeaponModTestFirerate extends WeaponModBase {
    public WeaponModTestFirerate(int id, String... slots) { super(id, slots); setPriority(PRIORITY_MULT_FINAL); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver && Objects.equals(key, Receiver.I_DELAYAFTERFIRE) && base instanceof Integer
                ? cast(Math.max((Integer) base / 2, 1), base) : base;
    }
}
