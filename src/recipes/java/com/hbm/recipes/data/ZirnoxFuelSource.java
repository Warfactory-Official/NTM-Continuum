package com.hbm.recipes.data;

import static com.hbm.hazard.HazardRegistry.*;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.ZirnoxFuel;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code ItemZirnoxRod.EnumZirnoxType} (life, heat, breeding) with the rod radiation of 1.7
 * {@code HazardRegistry.registerOtherFuel(rod_zirnox, type, base, target, false)}, what a used-up rod becomes and the
 * waste it reprocesses into.
 */
public final class ZirnoxFuelSource extends DataMapSource<ZirnoxFuel> {

    public ZirnoxFuelSource() {
        super(ReactorDataMaps.ZIRNOX_FUEL, false);
    }

    private void rod(String type, int life, int heat, boolean breeding, float base, float target, String depletesInto,
                     String waste) {
        put("hbm:rod_zirnox_" + type, new ZirnoxFuel(life, heat, breeding, base, target, "hbm:" + depletesInto,
                Optional.ofNullable(waste).map(w -> "hbm:" + w)));
    }

    /** A burning rod: it depletes into its own depleted rod. */
    private void fuel(String type, int life, int heat, float base, float target, String waste) {
        rod(type, life, heat, false, base, target, "rod_zirnox_depleted_" + type, waste);
    }

    @Override
    protected void values() {
        fuel("natural_uranium_fuel", 250_000, 30, u * rod_dual, wst * rod_dual * 11.5F, "waste_natural_uranium");
        fuel("uranium_fuel", 200_000, 50, uf * rod_dual, wst * rod_dual * 10F, "waste_uranium");
        rod("th232_fuel", 20_000, 0, true, th232 * rod_dual, thf * rod_dual, "rod_zirnox_thorium_fuel", null);
        fuel("thorium_fuel", 200_000, 40, thf * rod_dual, wst * rod_dual * 7.5F, "waste_thorium");
        fuel("mox_fuel", 165_000, 75, mox * rod_dual, wst * rod_dual * 10F, "waste_mox");
        fuel("plutonium_fuel", 175_000, 65, puf * rod_dual, wst * rod_dual * 12.5F, "waste_plutonium");
        fuel("u233_fuel", 150_000, 100, u233 * rod_dual, wst * rod_dual * 10F, "waste_u233");
        fuel("u235_fuel", 165_000, 85, u235 * rod_dual, wst * rod_dual * 11F, "waste_u235");
        fuel("les_fuel", 150_000, 150, saf * rod_dual, wst * rod_dual * 15F, "waste_schrabidium");
        rod("lithium_fuel", 20_000, 0, true, 0, 0.001F * rod_dual, "rod_zirnox_tritium", null);
        fuel("zfb_mox_fuel", 50_000, 35, mox * rod_dual, wst * rod_dual * 5F, "waste_zfb_mox");
    }
}
