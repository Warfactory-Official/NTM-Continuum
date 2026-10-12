package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compressor recipes: the datapack table {@code hbm:compressor}, one file per input fluid and pressure in
 * {@code data/hbm/recipe/compressor/} ({@code {"input": fluid, "pressure": the pressure it is compressed from (default 0),
 * "amount": mB in, "output": {type, amount, pressure}, "duration": ticks}}). Converted once from the Java table (1.7
 * {@code CompressorRecipes}, 5 rows).
 *
 * <p>Most fluids have <b>no</b> row: the machine then takes 1.7's generic path, which stays code in
 * {@code TileEntityMachineCompressorBase}: 1000 mB in, 1000 mB of the <i>same</i> fluid out at {@code pressure + 1}. That
 * path is what pressurises OIL for the vacuum refinery (0, 1, 2: two compressor stages) and HYDROGEN for the hydrotreater
 * (0 to 1). The rows are the fluids that <i>change</i> when compressed.
 */
public final class CompressorRecipes {

    private CompressorRecipes() {}

    /** Input fluid + the pressure it's compressed FROM. */
    public record Key(FluidType type, int pressure) {}

    public record CompressorRecipe(int inputAmount, FluidStack output, int duration) {}

    public record Row(FluidType input, int pressure, int amount, FluidStack output, int duration) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            Codec.INT.optionalFieldOf("pressure", 0).forGetter(Row::pressure),
            Codec.INT.fieldOf("amount").forGetter(Row::amount),
            GenericRecipeCodec.FLUID_STACK.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("duration").forGetter(Row::duration)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("compressor", () -> CODEC);

    private static volatile Map<Key, CompressorRecipe> recipes;

    static {
        TABLE.changed.register(CompressorRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        Map<Key, CompressorRecipe> next = new LinkedHashMap<>();
        for (Row row : TABLE.rows()) {
            next.put(new Key(row.input(), row.pressure()), new CompressorRecipe(row.amount(), row.output(), row.duration()));
        }
        recipes = Collections.unmodifiableMap(next);
    }

    public static CompressorRecipe getRecipe(FluidType type, int pressure) {
        return getAll().get(new Key(type, pressure));
    }

    public static Map<Key, CompressorRecipe> getAll() {
        Map<Key, CompressorRecipe> current = recipes;
        if (current == null) {
            TABLE.rows();
            if (recipes == null) reindex();
            current = recipes;
        }
        return current;
    }
}
