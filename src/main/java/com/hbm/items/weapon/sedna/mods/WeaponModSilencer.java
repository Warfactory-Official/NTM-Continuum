package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.sound.ModGunSounds;
import net.minecraft.world.item.ItemStack;

/** Source silencer sound selection, including the AMAT-specific report. */
public final class WeaponModSilencer extends WeaponModBase {
    public WeaponModSilencer(int id) { super(id, "SILENCER"); }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key == Receiver.S_FIRESOUND) {
            return cast(gun.is(ModGunItems.gun_amat.get()) ? ModGunSounds.FIRE_AMAT_SILENCER.get() : ModGunSounds.FIRE_SILENCED.get(), base);
        }
        return base;
    }
}
