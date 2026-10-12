package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Coker recipes (heat-driven: the coker pulls heat from the heat source below): the datapack table {@code hbm:coker}, one
 * file per input fluid in {@code data/hbm/recipe/coker/}. Converted once from the Java table (1.7 {@code CokerRecipes},
 * 33 rows). A row is either literal:
 * <pre>{"input": fluid, "amount": mB, "output": item, "byproduct": {type, amount}}   (byproduct optional)</pre>
 * or 1.7's {@code registerAuto}/{@code registerSFAuto} rule, which stays a rule here and reads the fuel value from the
 * fluid's {@code fluid_property} file, so an addon that changes a fuel's energy changes its coker row as 1.7 does:
 * <pre>{"input": fluid, "solid_fuel_energy": TU per output item, "output": item, "byproduct_type": fluid}</pre>
 * mB = energy x 1000 / max({@code flammable}, {@code combustible}) energy per bucket, cut to two significant digits (no
 * penalty factor); the byproduct is {@code max(10, mB / 10)} of {@code byproduct_type}; a fluid with no fuel value gets no
 * row. HBM's auto rows use 820,000 TU per petroleum coke and 340,000 TU per charcoal (wood oil).
 */
public final class CokerRecipes {

    private CokerRecipes() {}

    /** mB required + output item + optional byproduct fluid (nullable). */
    public record CokerRecipe(int amount, ItemStack output, FluidStack byproduct) {}

    public record Row(FluidType input, Optional<Integer> amount, Optional<Long> solidFuelEnergy, GenericRecipeCodec.ItemOut output,
                      Optional<FluidStack> byproduct, Optional<FluidType> byproductType) {}

    public static final MapCodec<Row> CODEC = FluidRecipeCodecs.checked(RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            Codec.INT.optionalFieldOf("amount").forGetter(Row::amount),
            Codec.LONG.optionalFieldOf("solid_fuel_energy").forGetter(Row::solidFuelEnergy),
            GenericRecipeCodec.ITEM_OUT.fieldOf("output").forGetter(Row::output),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("byproduct").forGetter(Row::byproduct),
            GenericRecipeCodec.FLUID.optionalFieldOf("byproduct_type").forGetter(Row::byproductType)
    ).apply(i, Row::new)), row -> row.amount().isPresent() == row.solidFuelEnergy().isPresent()
            ? com.mojang.serialization.DataResult.error(() -> "a coker row has either \"amount\" or \"solid_fuel_energy\"")
            : com.mojang.serialization.DataResult.success(row));

    public static final FluidRecipeCodecs.ByFluid<Row, CokerRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>("coker",
            () -> CODEC, Row::input, CokerRecipes::recipe);

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static CokerRecipe recipe(Row row) {
        if (row.amount().isPresent()) {
            return new CokerRecipe(row.amount().get(), row.output().stack(), row.byproduct().orElse(null));
        }
        // 1.7 registerSFAuto: the larger of the two fuel values, penalty 1
        long flammable = FluidCombustion.getFlammableEnergy(row.input());
        FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(row.input());
        long perBucket = Math.max(flammable, combustible == null ? 0L : combustible.energyPerBucket());
        int mB = FluidRecipeCodecs.solidFuelAmount(row.solidFuelEnergy().get(), 1D, perBucket);
        if (mB < 0) return null;
        FluidStack byproduct = row.byproductType().map(type -> new FluidStack(type, Math.max(10, mB / 10))).orElse(null);
        return new CokerRecipe(mB, row.output().stack(), byproduct);
    }

    public static CokerRecipe getOutput(FluidType type) {
        return TABLE.get(type);
    }

    public static Map<FluidType, CokerRecipe> getAll() {
        return TABLE.all();
    }
}
