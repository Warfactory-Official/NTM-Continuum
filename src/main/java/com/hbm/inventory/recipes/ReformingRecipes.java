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
 * Catalytic reformer recipes: the datapack table {@code hbm:reforming}, one file per input fluid in
 * {@code data/hbm/recipe/reforming/} ({@code {"input": fluid, "outputs": [three {type, amount}]}}): per 100 mB of input the
 * three products. The machine also needs a {@code catalytic_converter} and 20k HE per operation. Converted once from the
 * Java table (1.7 {@code ReformingRecipes}, 9 rows).
 */
public final class ReformingRecipes {

    private ReformingRecipes() {}

    /** Three fluid outputs per 100 mB input. */
    public record ReformingRecipe(FluidStack out1, FluidStack out2, FluidStack out3) {}

    public record Row(FluidType input, List<FluidStack> outputs) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            FluidRecipeCodecs.stacks(3).fieldOf("outputs").forGetter(Row::outputs)
    ).apply(i, Row::new));

    public static final FluidRecipeCodecs.ByFluid<Row, ReformingRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>("reforming",
            () -> CODEC, Row::input, row -> new ReformingRecipe(row.outputs().get(0), row.outputs().get(1), row.outputs().get(2)));

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static ReformingRecipe getOutput(FluidType type) {
        return TABLE.get(type);
    }

    public static Map<FluidType, ReformingRecipe> getAll() {
        return TABLE.all();
    }
}
