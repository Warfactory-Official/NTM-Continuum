package com.hbm.recipes.data;

import com.hbm.api.data.MachinePartDataMaps;
import dev.golem.core.data.DataMapSource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 1.7 {@code ItemPistons.EnumPistonType}: efficiency for the LOW, MEDIUM, HIGH, AERO and GAS fuel grades. */
public final class PistonSource extends DataMapSource<Map<String, Double>> {

    public PistonSource() {
        super(MachinePartDataMaps.PISTON, false);
    }

    private void piston(String id, double low, double medium, double high, double aero, double gas) {
        Map<String, Double> eff = new LinkedHashMap<>();
        eff.put("LOW", low);
        eff.put("MEDIUM", medium);
        eff.put("HIGH", high);
        eff.put("AERO", aero);
        eff.put("GAS", gas);
        put("hbm:" + id, eff);
    }

    @Override
    protected void values() {
        piston("piston_set_steel", 1.00, 0.75, 0.25, 0.00, 0.00);
        piston("piston_set_dura", 0.50, 1.00, 0.90, 0.50, 0.00);
        piston("piston_set_desh", 0.00, 0.50, 1.00, 0.75, 0.00);
        piston("piston_set_starmetal", 0.50, 0.75, 1.00, 0.90, 0.50);
    }
}
