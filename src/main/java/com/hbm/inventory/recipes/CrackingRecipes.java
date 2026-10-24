package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

/**
 * Catalytic cracker recipes: the datapack table {@code hbm:cracking}, one file per input fluid in
 * {@code data/hbm/recipe/cracking/} ({@code {"input": fluid, "left": {type, amount}, "right": {type, amount}}}; "right" may
 * be left out, as for kerosene, which only makes petroleum). Per 100 mB of input (+200 mB steam, 2 mB spent steam, fixed in
 * the machine). Converted once from the Java table (1.7 {@code CrackingRecipes}, 12 rows). {@link RadiolysisRecipes} copies
 * every row, as 1.7 does.
 */
public final class CrackingRecipes {

    public static final int FILL_PER_OP = 100;

    private CrackingRecipes() {}

    // 1.7's public percentage constants, kept for code that names them
    public static final int oil_crack_oil = 80;
    public static final int oil_crack_petro = 20;
    public static final int bitumen_crack_oil = 80;
    public static final int bitumen_crack_aroma = 20;
    public static final int smear_crack_napht = 60;
    public static final int smear_crack_petro = 40;
    public static final int gas_crack_petro = 30;
    public static final int gas_crack_unsat = 20;
    public static final int diesel_crack_kero = 40;
    public static final int diesel_crack_petro = 30;
    public static final int kero_crack_petro = 60;
    public static final int wood_crack_aroma = 10;
    public static final int wood_crack_heat = 40;
    public static final int xyl_crack_aroma = 80;
    public static final int xyl_crack_petro = 20;

    /** Two outputs per 100 mB input; {@code right} is NONE 0 when the file has none. */
    public record CrackingRecipe(FluidStack left, FluidStack right) {}

    /** One row: the input fluid and its two products. */
    public record Row(FluidType input, FluidStack left, FluidStack right) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.FLUID_STACK.fieldOf("left").forGetter(Row::left),
            FluidRecipeCodecs.optionalStack("right").forGetter(Row::right)
    ).apply(i, Row::new));

    public static final FluidRecipeCodecs.ByFluid<Row, CrackingRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>("cracking",
            () -> CODEC, Row::input, row -> new CrackingRecipe(row.left(), row.right()));

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static CrackingRecipe getCracking(FluidType oil) {
        return TABLE.get(oil);
    }

    /** Read-only view (JEI, radiolysis). */
    public static Map<FluidType, CrackingRecipe> getAll() {
        return TABLE.all();
    }
}
