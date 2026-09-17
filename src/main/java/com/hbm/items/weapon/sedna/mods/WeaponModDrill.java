package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import net.minecraft.world.item.ItemStack;
/** Drill modifier stat bridge. The drill action itself remains with its factory owner. */
public class WeaponModDrill extends WeaponModBase {
    private float damage = 1F;
    private double reach = 1D;
    private float threshold = -1F;
    private float piercing = -1F;
    private int aoe = -1;
    private int harvest = -1;
    public WeaponModDrill(int id) { super(id, "DRILL"); setPriority(PRIORITY_SET); }
    public WeaponModDrill damage(float v) { damage = v; return this; }
    public WeaponModDrill reach(double v) { reach = v; return this; }
    public WeaponModDrill dt(float v) { threshold = v; return this; }
    public WeaponModDrill pierce(float v) { piercing = v; return this; }
    public WeaponModDrill aoe(int v) { aoe = v; return this; }
    public WeaponModDrill harvest(int v) { harvest = v; return this; }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Receiver.F_BASEDAMAGE.equals(key)) return cast((Float) base * damage, base);
        if (XFactoryDrill.D_REACH.equals(key)) return cast((Double) base * reach, base);
        if (XFactoryDrill.F_DTNEG.equals(key) && threshold >= 0) return cast(threshold, base);
        if (XFactoryDrill.F_PIERCE.equals(key) && piercing >= 0) return cast(piercing, base);
        if (XFactoryDrill.I_AOE.equals(key) && aoe >= 0) return cast(aoe, base);
        if (XFactoryDrill.I_HARVEST.equals(key) && harvest >= 0) return cast(harvest, base);
        return base;
    }
}
