package com.hbm.scripting.recipes;

import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

/** {@code e.crucible.add('kubejs.alloy', r => r.frequency(9).icon('hbm:ingot_steel').molten('64x iron', '8x carbon').moltenOutputs('72x steel'))}; molten specs. */
public final class CrucibleBuilder extends GenericBuilder {

    private int frequency = 1;
    private Object icon;
    private Object[] molten;
    private Object[] moltenOut;

    public CrucibleBuilder frequency(int ticks) {
        this.frequency = ticks;
        return this;
    }

    @Override
    public CrucibleBuilder icon(Spec.Item spec) {
        this.icon = spec;
        return this;
    }

    public CrucibleBuilder molten(Spec.Molten... specs) {
        this.molten = specs;
        return this;
    }

    public CrucibleBuilder moltenOutputs(Spec.Molten... specs) {
        this.moltenOut = specs;
        return this;
    }

    @Override
    GenericRecipe build(String name, GenericRecipes set) {
        return new CrucibleRecipes.CrucibleRecipe(name).crucibleSetup(frequency, ScriptSpecs.stack(icon))
                .inputs(ScriptSpecs.molten(molten)).outputs(ScriptSpecs.molten(moltenOut));
    }
}
