package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fraction tower recipes: the datapack table {@code hbm:fraction}, one file per input fluid in
 * {@code data/hbm/recipe/fraction/} ({@code {"input": fluid, "left": {type, amount}, "right": {type, amount}}}; 100 mB of
 * input per operation splits into the two outputs). Converted once from the Java table (1.7's 19 rows).
 */
public final class FractionRecipes {

    public static final int FILL_PER_OP = 100;

    private FractionRecipes() {}

    public record FractionPair(FluidStack left, FluidStack right) {}

    /** One row: the input fluid and its two fractions. */
    public record Row(FluidType input, FluidStack left, FluidStack right) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.FLUID_STACK.fieldOf("left").forGetter(Row::left),
            GenericRecipeCodec.FLUID_STACK.fieldOf("right").forGetter(Row::right)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("fraction", () -> CODEC);

    private static volatile Map<FluidType, FractionPair> fractions = Map.of();

    static {
        TABLE.changed.register(FractionRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        Map<FluidType, FractionPair> next = new LinkedHashMap<>();
        for (Row row : TABLE.rows()) next.put(row.input(), new FractionPair(row.left(), row.right()));
        fractions = next;
    }

    private static Map<FluidType, FractionPair> index() {
        if (fractions.isEmpty()) TABLE.rows();
        return fractions;
    }

    public static FractionPair getFractions(FluidType input) {
        return index().get(input);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, FractionPair> getAll() {
        return Collections.unmodifiableMap(index());
    }
}
