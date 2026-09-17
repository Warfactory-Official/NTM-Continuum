package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Replaces a gun's accepted calibre, magazine size and base damage. */
public class WeaponModCaliber extends WeaponModBase {
    protected static final MagazineSingleReload DUMMY_SINGLE = new MagazineSingleReload(0, 0);
    protected static final MagazineFullReload DUMMY_FULL = new MagazineFullReload(0, 0);
    protected static final MagazineBelt DUMMY_BELT = new MagazineBelt();
    private final List<BulletConfig> configs = new ArrayList<>();
    private final int count;
    private final float baseDamage;

    public WeaponModCaliber(int id, int count, float baseDamage, BulletConfig... configs) {
        super(id, "CALIBER");
        setPriority(PRIORITY_SET);
        this.configs.addAll(Arrays.asList(configs));
        this.count = count;
        this.baseDamage = baseDamage;
    }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.O_MAGAZINE.equals(key)) {
            if (base instanceof MagazineSingleReload original) {
                DUMMY_SINGLE.acceptedBullets = configs; DUMMY_SINGLE.capacity = count; DUMMY_SINGLE.index = original.index;
                return cast(DUMMY_SINGLE, base);
            }
            if (base instanceof MagazineFullReload original) {
                DUMMY_FULL.acceptedBullets = configs; DUMMY_FULL.capacity = count; DUMMY_FULL.index = original.index;
                return cast(DUMMY_FULL, base);
            }
            if (base instanceof MagazineBelt) { DUMMY_BELT.acceptedBullets.clear(); DUMMY_BELT.acceptedBullets.addAll(configs); return cast(DUMMY_BELT, base); }
        }
        return Receiver.F_BASEDAMAGE.equals(key) ? cast(baseDamage, base) : base;
    }
    @Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
    @Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
