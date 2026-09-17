package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
public class WeaponModGenericDamage extends WeaponModBase {
    public WeaponModGenericDamage(int id) { super(id, "GENERIC_DAMAGE"); setPriority(PRIORITY_MULTIPLICATIVE); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof Receiver && Objects.equals(key, Receiver.F_BASEDAMAGE) && base instanceof Float
                ? cast((Float) base * 1.15F, base) : base;
    }
}
