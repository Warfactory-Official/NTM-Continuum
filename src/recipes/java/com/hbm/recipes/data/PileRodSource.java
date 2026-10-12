package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.PileRod;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code ItemPileRodMK2.EnumPileRod}, with its two constructors. {@code turnsInto} names the next rod (1.7 metadata);
 * a rod that turns into itself states none.
 */
public final class PileRodSource extends DataMapSource<PileRod> {

    public PileRodSource() {
        super(ReactorDataMaps.PILE_ROD, false);
    }

    /** 1.7 {@code EnumPileRod(neutronSource)}. */
    private void source(String type, double neutronSource) {
        put("hbm:pile_rod_mk2_" + type, new PileRod(0, 0, 0, neutronSource, Optional.empty()));
    }

    /** 1.7 {@code EnumPileRod(reaction, life, heat, turnsInto)}. */
    private void rod(String type, double reaction, double life, double heat, String turnsInto) {
        put("hbm:pile_rod_mk2_" + type, new PileRod(reaction, life, heat, 0,
                turnsInto.equals(type) ? Optional.empty() : Optional.of("hbm:pile_rod_mk2_" + turnsInto)));
    }

    @Override
    protected void values() {
        source("ra226be", 1D);
        source("po210be", 1D);
        rod("zr", 0D, 0D, 0D, "zr");
        rod("nu", 1D, 25_000D, 0.25D, "pu239");
        rod("pu239", 1D, 500D, 0.5D, "rgp");
        rod("rgp", 1D, 1_000D, 0.5D, "waste");
        rod("waste", 1D, 0D, 1.5D, "waste");
        // port: the thorium rods are not in 1.7's EnumPileRod; port-only content, kept
        rod("thorium", 1D, 35_000D, 0.25D, "thorium_fuel");
        rod("thorium_fuel", 1D, 2_000D, 0.5D, "waste");
    }
}
