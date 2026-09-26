package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

/**
 * One {@code GenericRecipe}, as a script states it. Specs stay raw until the machine's registry builds.
 *
 * <pre>
 * r => r.duration(100).power(500)
 *       .inputs('4x #c:plates/steel', 'hbm:bolt_steel').fluidInputs('1000x water')
 *       .outputs('hbm:motor', '2x minecraft:iron_nugget').chances(1, 0.5).fluidOutputs('500x steam')
 *       .pools('alt.plates').group('plates').icon('hbm:motor').nameWrapper('purex.recycle')
 * </pre>
 */
public class GenericBuilder {

    private int duration = 100;
    private long power;
    private Object[] inputs;
    private Object[] fluidInputs;
    private Object[] outputs;
    private Object[] fluidOutputs;
    private float[] chances;
    private String[] pools;
    private String group;
    private Object icon;
    private String nameWrapper;

    public GenericBuilder duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    public GenericBuilder power(long perTick) {
        this.power = perTick;
        return this;
    }

    public GenericBuilder inputs(Spec.Ingredient... specs) {
        this.inputs = specs;
        return this;
    }

    public GenericBuilder fluidInputs(Spec.Fluid... specs) {
        this.fluidInputs = specs;
        return this;
    }

    public GenericBuilder outputs(Spec.Item... specs) {
        this.outputs = specs;
        return this;
    }

    public GenericBuilder fluidOutputs(Spec.Fluid... specs) {
        this.fluidOutputs = specs;
        return this;
    }

    public GenericBuilder chances(float... chances) {
        this.chances = chances;
        return this;
    }

    public GenericBuilder pools(String... pools) {
        this.pools = pools;
        return this;
    }

    public GenericBuilder group(String group) {
        this.group = group;
        return this;
    }

    public GenericBuilder icon(Spec.Item spec) {
        this.icon = spec;
        return this;
    }

    public GenericBuilder nameWrapper(String key) {
        this.nameWrapper = key;
        return this;
    }

    protected GenericRecipe create(String name) {
        return new GenericRecipe(name);
    }

    GenericRecipe build(String name, GenericRecipes set) {
        GenericRecipe recipe = create(name).setup(duration, power);
        if (inputs != null) recipe.inputItems(ScriptSpecs.ingredients(inputs));
        if (fluidInputs != null) recipe.inputFluids(ScriptSpecs.fluids(fluidInputs));
        if (outputs != null) recipe.outputItems(ScriptSpecs.stacks(outputs));
        if (fluidOutputs != null) recipe.outputFluids(ScriptSpecs.fluids(fluidOutputs));
        if (chances != null) recipe.outputChances(chances);
        if (pools != null) recipe.setPools(pools);
        if (group != null) recipe.setGroup(group, set);
        if (icon != null) recipe.setIcon(ScriptSpecs.stack(icon));
        if (nameWrapper != null) recipe.setNameWrapper(nameWrapper);
        return recipe;
    }
}
