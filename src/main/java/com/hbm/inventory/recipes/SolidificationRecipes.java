package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Solidifier recipes, fluid to item: the datapack table {@code hbm:solidification}, one file per input fluid in
 * {@code data/hbm/recipe/solidification/}. Converted once from the Java table (1.7 {@code SolidificationRecipes}, 48
 * registrations on 47 fluids). A row is literal, {@code {"input": fluid, "amount": mB, "output": item}}, or 1.7's
 * {@code registerSFAuto} "solid fuel" rule, {@code {"input": fluid, "solid_fuel_energy": TU per item, "output": item}}: mB =
 * energy x 1000 x 1.25 / the fluid's {@code flammable} energy per bucket (its {@code fluid_property} file), cut to two
 * significant digits, at least 1; a fluid that is not flammable gets no row. The rule stays a rule so a changed fuel value
 * changes the row, as in 1.7. HBM's solid fuel holds 1,440,000 TU (3200 burn ticks x 1.5 bonus x 300 TU/t), balefire solid
 * fuel 24,000,000.
 *
 * <p>1.7 registers BALEFIRE twice, literally (250 mB) and then by the rule, and the rule wins (a map put): the file is
 * the rule. 1.7's {@code ModItems.ingot_mercury} is registered under the name {@code nugget_mercury}, so MERCURY
 * solidifies into {@code hbm:nugget_mercury}.
 */
public final class SolidificationRecipes {

    private SolidificationRecipes() {}

    // 1.7's public amount constants, kept for code that names them
    public static final int SF_OIL = 200;
    public static final int SF_CRACK = 200;
    public static final int SF_HEAVY = 150;
    public static final int SF_BITUMEN = 100;
    public static final int SF_COALOIL = 200;
    public static final int SF_CREOSOTE = 200;
    public static final int SF_WOOD = 1000;
    public static final int SF_LUBE = 100;

    public record SolidRecipe(int amount, ItemStack output) {}

    public record Row(FluidType input, Optional<Integer> amount, Optional<Long> solidFuelEnergy, GenericRecipeCodec.ItemOut output) {}

    public static final MapCodec<Row> CODEC = FluidRecipeCodecs.checked(RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            Codec.INT.optionalFieldOf("amount").forGetter(Row::amount),
            Codec.LONG.optionalFieldOf("solid_fuel_energy").forGetter(Row::solidFuelEnergy),
            GenericRecipeCodec.ITEM_OUT.fieldOf("output").forGetter(Row::output)
    ).apply(i, Row::new)), row -> row.amount().isPresent() == row.solidFuelEnergy().isPresent()
            ? DataResult.error(() -> "a solidification row has either \"amount\" or \"solid_fuel_energy\"")
            : DataResult.success(row));

    public static final FluidRecipeCodecs.ByFluid<Row, SolidRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>("solidification",
            () -> CODEC, Row::input, SolidificationRecipes::recipe);

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static SolidRecipe recipe(Row row) {
        if (row.amount().isPresent()) return new SolidRecipe(row.amount().get(), row.output().stack());
        int mB = FluidRecipeCodecs.solidFuelAmount(row.solidFuelEnergy().get(), 1.25D,
                FluidCombustion.getFlammableEnergy(row.input()));
        return mB < 0 ? null : new SolidRecipe(Math.max(mB, 1), row.output().stack());
    }

    public static SolidRecipe getOutput(FluidType type) {
        return TABLE.get(type);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, SolidRecipe> getAll() {
        return TABLE.all();
    }
}
