package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;

/**
 * Hydrotreater recipes: the datapack table {@code hbm:hydrotreating}, one file per input fluid in
 * {@code data/hbm/recipe/hydrotreating/} ({@code {"input": fluid, "hydrogen": {type, amount, pressure}, "outputs": [two
 * {type, amount}]}}): per 100 mB of input plus the (pressurised) hydrogen, the two products. The machine also needs a
 * {@code catalytic_converter} and 20k HE per operation. Converted once from the Java table (1.7
 * {@code HydrotreatingRecipes}, 6 rows).
 */
public final class HydrotreatingRecipes {

    public static final int FILL_PER_OP = 100;

    private HydrotreatingRecipes() {}

    /** Hydrogen requirement (pressurized) + two outputs, per 100 mB input. */
    public record HydrotreatingRecipe(FluidStack hydrogen, FluidStack out1, FluidStack out2) {}

    public record Row(FluidType input, FluidStack hydrogen, List<FluidStack> outputs) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.FLUID_STACK.fieldOf("hydrogen").forGetter(Row::hydrogen),
            FluidRecipeCodecs.stacks(2).fieldOf("outputs").forGetter(Row::outputs)
    ).apply(i, Row::new));

    public static final FluidRecipeCodecs.ByFluid<Row, HydrotreatingRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>(
            "hydrotreating", () -> CODEC, Row::input,
            row -> new HydrotreatingRecipe(row.hydrogen(), row.outputs().get(0), row.outputs().get(1)));

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static HydrotreatingRecipe getOutput(FluidType type) {
        return TABLE.get(type);
    }

    public static Map<FluidType, HydrotreatingRecipe> getAll() {
        return TABLE.all();
    }
}
