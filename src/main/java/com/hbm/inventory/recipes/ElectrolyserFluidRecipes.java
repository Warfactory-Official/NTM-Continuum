package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Electrolyser fluid recipes: the datapack table {@code hbm:electrolyser_fluid}, one file per input fluid in
 * {@code data/hbm/recipe/electrolyser_fluid/} ({@code {"input": fluid, "amount": mB, "output1"/"output2": {type, amount}
 * (either may be left out), "duration": ticks (default 20), "byproducts": [items]}}). Converted once from the Java table
 * (1.7 {@code ElectrolyserFluidRecipes}, 8 rows). 1.7's {@code ModItems.ingot_mercury} is the item registered as
 * {@code nugget_mercury}, so one 1.7 mercury "ingot" is one {@code hbm:nugget_mercury}.
 */
public final class ElectrolyserFluidRecipes {

    private ElectrolyserFluidRecipes() {}

    public record Row(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration,
                      List<GenericRecipeCodec.ItemOut> byproducts) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            Codec.INT.fieldOf("amount").forGetter(Row::amount),
            FluidRecipeCodecs.optionalStack("output1").forGetter(Row::output1),
            FluidRecipeCodecs.optionalStack("output2").forGetter(Row::output2),
            Codec.INT.optionalFieldOf("duration", 20).forGetter(Row::duration),
            GenericRecipeCodec.ITEM_OUT.listOf().optionalFieldOf("byproducts", List.of()).forGetter(Row::byproducts)
    ).apply(i, Row::new));

    public static final FluidRecipeCodecs.ByFluid<Row, ElectrolysisRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>(
            "electrolyser_fluid", () -> CODEC, Row::input, row -> new ElectrolysisRecipe(row.input(), row.amount(),
            row.output1(), row.output2(), row.duration(),
            row.byproducts().stream().map(GenericRecipeCodec.ItemOut::stack).toArray(ItemStack[]::new)));

    /** The current rows, by input fluid in recipe-id order (a live read-only view of the table). */
    public static final Map<FluidType, ElectrolysisRecipe> RECIPES = new AbstractMap<>() {
        @Override
        public Set<Entry<FluidType, ElectrolysisRecipe>> entrySet() {
            return TABLE.all().entrySet();
        }

        @Override
        public ElectrolysisRecipe get(Object key) {
            return TABLE.all().get(key);
        }
    };

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** Loads the rows if nothing has yet (they load by themselves; kept for callers of the former Java table). */
    public static void register() {
        TABLE.all();
    }

    public static ElectrolysisRecipe getRecipe(FluidType input) {
        return TABLE.get(input);
    }

    public static final class ElectrolysisRecipe {
        public final FluidType input;
        public final int amount;
        public final FluidStack output1;
        public final FluidStack output2;
        public final int duration;
        public final ItemStack[] byproduct;

        private ElectrolysisRecipe(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration,
                                   ItemStack[] byproduct) {
            this.input = input;
            this.amount = amount;
            this.output1 = output1;
            this.output2 = output2;
            this.duration = duration;
            this.byproduct = byproduct;
        }

        public List<ItemStack> displayOutputs() {
            List<ItemStack> outputs = new ArrayList<>();
            if (output1.type != Fluids.NONE) outputs.add(com.hbm.items.machine.ItemFluidIcon.make(output1));
            if (output2.type != Fluids.NONE) outputs.add(com.hbm.items.machine.ItemFluidIcon.make(output2));
            for (ItemStack stack : byproduct) outputs.add(stack.copy());
            return outputs;
        }
    }
}
