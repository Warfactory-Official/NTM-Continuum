package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;
public class WeaponModOverride extends WeaponModBase {
    private final float damage;
    public WeaponModOverride(int id, float damage, String... slots) { super(id, slots); this.damage = damage; setPriority(PRIORITY_SET); }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        return Receiver.F_BASEDAMAGE.equals(key) ? cast(damage, base) : base;
    }
}
