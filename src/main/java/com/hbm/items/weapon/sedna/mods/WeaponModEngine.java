package com.hbm.items.weapon.sedna.mods;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import net.minecraft.world.item.ItemStack;

/** The drill engine choices from upstream, including their magazine and fire-delay replacements. */
public class WeaponModEngine extends WeaponModBase {
    public static final MagazineLiquidEngine ENGINE_DIESEL = new MagazineLiquidEngine(0, 4_000,
            () -> new FluidType[]{Fluids.DIESEL, Fluids.DIESEL_CRACK, Fluids.LIGHTOIL});
    public static final MagazineLiquidEngine ENGINE_AVIATION = new MagazineLiquidEngine(0, 4_000,
            () -> new FluidType[]{Fluids.KEROSENE, Fluids.LPG});
    public static final MagazineElectricEngine ENGINE_ELECTRIC = new MagazineElectricEngine(0, 1_000_000);
    public static final MagazineLiquidEngine ENGINE_TURBO = new MagazineLiquidEngine(0, 4_000,
            () -> new FluidType[]{Fluids.KEROSENE_REFORM, Fluids.REFORMATE});
    protected IMagazine<?> magazine;
    protected int delay;
    public WeaponModEngine(int id) { super(id, "ENGINE"); setPriority(PRIORITY_SET); }
    public WeaponModEngine mag(IMagazine<?> magazine) { this.magazine = magazine; return this; }
    public WeaponModEngine delay(int delay) { this.delay = delay; return this; }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.O_MAGAZINE.equals(key) && magazine != null) return cast(magazine, base);
        return Receiver.I_DELAYAFTERFIRE.equals(key) ? cast(delay, base) : base;
    }
    @Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
    @Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
