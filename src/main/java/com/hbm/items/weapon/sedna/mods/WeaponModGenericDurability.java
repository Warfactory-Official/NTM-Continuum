package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.GunConfig;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
public class WeaponModGenericDurability extends WeaponModBase {
    public WeaponModGenericDurability(int id) { super(id, "GENERIC_DURABILITY"); setPriority(PRIORITY_MULTIPLICATIVE); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return parent instanceof GunConfig && Objects.equals(key, GunConfig.F_DURABILITY) && base instanceof Float
                ? cast((Float) base * 2F, base) : base;
    }
}
