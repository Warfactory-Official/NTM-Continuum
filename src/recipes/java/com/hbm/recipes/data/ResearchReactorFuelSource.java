package com.hbm.recipes.data;

import com.hbm.api.data.MachineDataMaps;
import com.hbm.api.data.MachineDataMaps.ResearchReactorWaste;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityReactorResearch.fuelMap}: each plate leaves its waste plate at metadata 1 (hot). */
public final class ResearchReactorFuelSource extends DataMapSource<ResearchReactorWaste> {

    public ResearchReactorFuelSource() {
        super(MachineDataMaps.RESEARCH_REACTOR_FUEL, false);
    }

    private void fuel(String plate, String waste) {
        put("hbm:" + plate, new ResearchReactorWaste("hbm:" + waste, true));
    }

    @Override
    protected void values() {
        fuel("plate_fuel_u233", "waste_plate_u233");
        fuel("plate_fuel_u235", "waste_plate_u235");
        fuel("plate_fuel_mox", "waste_plate_mox");
        fuel("plate_fuel_pu239", "waste_plate_pu239");
        fuel("plate_fuel_sa326", "waste_plate_sa326");
        fuel("plate_fuel_ra226be", "waste_plate_ra226be");
        fuel("plate_fuel_pu238be", "waste_plate_pu238be");
    }
}
