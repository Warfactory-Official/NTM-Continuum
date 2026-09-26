package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/** {@code e.plasmaForge.add(name, r => r.ignition(1000000)...)} */
public final class PlasmaForgeBuilder extends GenericBuilder {

    private long ignition;

    public PlasmaForgeBuilder ignition(long ignitionTemp) {
        this.ignition = ignitionTemp;
        return this;
    }

    @Override
    protected GenericRecipe create(String name) {
        return new PlasmaForgeRecipe(name).setInputEnergy(ignition);
    }
}
