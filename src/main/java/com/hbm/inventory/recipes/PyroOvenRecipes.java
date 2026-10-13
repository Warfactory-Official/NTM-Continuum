package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pyrolysis oven recipes: the datapack table {@code hbm:pyrolysis} in {@code data/hbm/recipe/pyrolysis/}, an optional fluid
 * and an optional item in, an optional fluid and an optional item out, over {@code duration} ticks:
 * <pre>{"input_fluid": {type, amount}, "input_item": {"ingredient": id or #tag, "count"}, "output_fluid": {type, amount},
 *  "output_item": item, "duration": ticks}</pre>
 * or 1.7's {@code registerSFAuto} "solid fuel" rule, which stays a rule reading the fuel's {@code flammable} energy from its
 * {@code fluid_property} file: {@code {"solid_fuel": fluid, "solid_fuel_energy": TU per item, "output_item": item,
 * "duration": 60}} takes energy x 1000 x 0.5 / energy per bucket mB of the fluid (cut to two significant digits, at least 1;
 * a fluid that is not flammable gets no row). The rows are 1.7's {@code PyroOvenRecipes}: 27 solid-fuel rows and
 * 14 literal ones.
 *
 * <p>The machine takes the <b>first</b> recipe that matches, so the order is 1.7's and load-bearing: solid-fuel rows first
 * (1.7 registers them first; the syngas one is why syngas plus tungsten dust makes solid fuel), then the bedrock-ore roasting
 * rows {@link BedrockOreChainRecipes} still adds in code (R-10), then the literal rows; inside each group, recipe-id order.
 * {@code ANY_COKE} / {@code ANY_TAR} are the {@code c:cokes} / {@code c:tars} tags.
 */
public final class PyroOvenRecipes {

    private PyroOvenRecipes() {}

    /** The coke aggregate (old OreDict ANY_COKE) - the 3 coke items, hand-tagged in data/c/tags/item/cokes.json. */
    public static final TagKey<Item> COKES = TagKey.create(Registries.ITEM, Ids.of("c", "cokes"));
    /** The tar aggregate (old OreDict ANY_TAR), hand-tagged in data/c/tags/item/tars.json. */
    public static final TagKey<Item> TARS = TagKey.create(Registries.ITEM, Ids.of("c", "tars"));

    public static final class PyroOvenRecipe {
        public FluidStack inputFluid;
        public AStack inputItem;
        public FluidStack outputFluid;
        public ItemStack outputItem;
        public final int duration;

        public PyroOvenRecipe(int duration) { this.duration = duration; }
        public PyroOvenRecipe in(FluidStack stack) { this.inputFluid = stack; return this; }
        public PyroOvenRecipe in(AStack stack) { this.inputItem = stack; return this; }
        public PyroOvenRecipe out(FluidStack stack) { this.outputFluid = stack; return this; }
        public PyroOvenRecipe out(ItemStack stack) { this.outputItem = stack; return this; }
    }

    public record Row(Optional<FluidStack> inputFluid, Optional<AStack> inputItem, Optional<FluidStack> outputFluid,
                      Optional<GenericRecipeCodec.ItemOut> outputItem, int duration, Optional<FluidType> solidFuel,
                      Optional<Long> solidFuelEnergy) {}

    public static final MapCodec<Row> CODEC = FluidRecipeCodecs.checked(RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("input_fluid").forGetter(Row::inputFluid),
            GenericRecipeCodec.STACK.optionalFieldOf("input_item").forGetter(Row::inputItem),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("output_fluid").forGetter(Row::outputFluid),
            GenericRecipeCodec.ITEM_OUT.optionalFieldOf("output_item").forGetter(Row::outputItem),
            Codec.INT.fieldOf("duration").forGetter(Row::duration),
            GenericRecipeCodec.FLUID.optionalFieldOf("solid_fuel").forGetter(Row::solidFuel),
            Codec.LONG.optionalFieldOf("solid_fuel_energy").forGetter(Row::solidFuelEnergy)
    ).apply(i, Row::new)), row -> row.solidFuel().isPresent() != row.solidFuelEnergy().isPresent()
            || (row.solidFuel().isPresent() && row.inputFluid().isPresent())
            ? DataResult.error(() -> "a pyrolysis row has \"input_fluid\", or \"solid_fuel\" with \"solid_fuel_energy\"")
            : DataResult.success(row));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("pyrolysis", () -> CODEC);

    private static volatile List<PyroOvenRecipe> recipes;

    static {
        TABLE.changed.register(PyroOvenRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<PyroOvenRecipe> solidFuel = new ArrayList<>();
        List<PyroOvenRecipe> literal = new ArrayList<>();
        for (Row row : TABLE.rows()) {
            PyroOvenRecipe recipe = new PyroOvenRecipe(row.duration());
            if (row.solidFuel().isPresent()) {
                // 1.7 registerSFAuto: x0.5 bonus, at least 1 mB
                int mB = FluidRecipeCodecs.solidFuelAmount(row.solidFuelEnergy().get(), 0.5D,
                        FluidCombustion.getFlammableEnergy(row.solidFuel().get()));
                if (mB < 0) continue;
                recipe.in(new FluidStack(row.solidFuel().get(), Math.max(mB, 1)));
            }
            row.inputFluid().ifPresent(recipe::in);
            row.inputItem().ifPresent(recipe::in);
            row.outputFluid().ifPresent(recipe::out);
            row.outputItem().ifPresent(out -> recipe.out(out.stack()));
            (row.solidFuel().isPresent() ? solidFuel : literal).add(recipe);
        }
        List<PyroOvenRecipe> next = new ArrayList<>(solidFuel);
        BedrockOreChainRecipes.pyroOven(next); // bedrock-ore roast loops, still code (R-10)
        next.addAll(literal);
        recipes = Collections.unmodifiableList(next);
    }

    /** Every recipe in the order the machine tries them. */
    public static List<PyroOvenRecipe> getAll() {
        List<PyroOvenRecipe> current = recipes;
        if (current == null) {
            TABLE.rows();
            if (recipes == null) reindex();
            current = recipes;
        }
        return current;
    }
}
