package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/** {@code e.fusion.add(name, r => r.ignition(1000).outputEnergy(5000).flux(2.5).color(1, 0.2, 0.6)...)} */
public final class FusionBuilder extends GenericBuilder {

    private long ignition;
    private long outputEnergy;
    private double flux;
    private float[] color;

    public FusionBuilder ignition(long ignitionTemp) {
        this.ignition = ignitionTemp;
        return this;
    }

    public FusionBuilder outputEnergy(long outputTemp) {
        this.outputEnergy = outputTemp;
        return this;
    }

    public FusionBuilder flux(double neutronFlux) {
        this.flux = neutronFlux;
        return this;
    }

    public FusionBuilder color(float r, float g, float b) {
        this.color = new float[] {r, g, b};
        return this;
    }

    @Override
    protected GenericRecipe create(String name) {
        FusionRecipe recipe = new FusionRecipe(name).setInputEnergy(ignition).setOutputEnergy(outputEnergy).setOutputFlux(flux);
        if (color != null) recipe.setRGB(color[0], color[1], color[2]);
        return recipe;
    }
}
