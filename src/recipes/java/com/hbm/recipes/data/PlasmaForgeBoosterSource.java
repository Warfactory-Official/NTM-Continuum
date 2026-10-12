package com.hbm.recipes.data;

import com.hbm.api.data.MachinePartDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityFusionPlasmaForge.boosters}, in its order. */
public final class PlasmaForgeBoosterSource extends DataMapSource<Integer> {

    public PlasmaForgeBoosterSource() {
        super(MachinePartDataMaps.PLASMA_FORGE_BOOSTER, false);
    }

    @Override
    protected void values() {
        // 1.7 new OreDictStack(CO60.nugget()), ...: the ore-dictionary names are the c: tags (nuggetCobalt60 is
        // c:nuggets/co60, dustTiny is dusts_tiny), each holding exactly 1.7's HBM item
        put("#c:nuggets/co60", 20);
        put("#c:billets/co60", 120);
        put("#c:ingots/co60", 200);
        put("#c:dusts/co60", 200);
        put("#c:nuggets/sr90", 40);
        put("#c:dusts_tiny/sr90", 40);
        put("#c:billets/sr90", 240);
        put("#c:ingots/sr90", 400);
        put("#c:dusts/sr90", 400);
        put("#c:nuggets/au198", 60);
        put("#c:billets/au198", 360);
        put("#c:ingots/au198", 600);
        put("#c:dusts/au198", 600);
        put("#c:dusts_tiny/i131", 60);
        put("#c:dusts/i131", 600);
        put("#c:dusts_tiny/xe135", 60);
        put("#c:dusts/xe135", 600);
        put("#c:dusts_tiny/cs137", 50);
        put("#c:dusts/cs137", 500);
        put("#c:dusts/at209", 1_200);
    }
}
