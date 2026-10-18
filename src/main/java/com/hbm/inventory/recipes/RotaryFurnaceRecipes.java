package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Rotary furnace recipes: the datapack table {@code hbm:rotary_furnace}, one file per row in
 * {@code data/hbm/recipe/rotary_furnace/} (1.7 had this as a {@code SerializableRecipe} too):
 *
 * <pre>
 * {"type": "hbm:rotary_furnace", "inputs": [{"ingredient": "#c:ingots/iron"}, {"ingredient": "#c:gems/coal"}],
 *  "fluid": {"type": "hbm:lightoil", "amount": 100}, "output": {"material": "steel", "amount": 72},
 *  "duration": 100, "steam": 100, "order": 0}
 * </pre>
 *
 * Up to three unordered item inputs (counts required), an optional fluid, a molten-metal output, a duration in ticks
 * and a steam draw per tick. The first row (by {@code order}, then recipe id) that matches wins, as 1.7 iterated its
 * list. The 12 rows were converted once from the former Java table (1.7's defaults) and are the source now.
 */
public final class RotaryFurnaceRecipes {

    private RotaryFurnaceRecipes() {}

    /** One row. */
    public record Row(List<AStack> inputs, Optional<FluidStack> fluid, MaterialStack output, int duration, int steam,
                      int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.STACK.listOf().optionalFieldOf("inputs", List.of()).forGetter(Row::inputs),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("fluid").forGetter(Row::fluid),
            MaterialCodecs.STACK.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("duration").forGetter(Row::duration),
            Codec.INT.fieldOf("steam").forGetter(Row::steam),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new)).flatXmap(RotaryFurnaceRecipes::checkRow, RotaryFurnaceRecipes::checkRow);

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("rotary_furnace", () -> CODEC);

    /** The current recipes in lookup order (rebuilt after every reload or sync). */
    public static volatile List<RotaryFurnaceRecipe> recipes = List.of();

    static {
        TABLE.changed.register(RotaryFurnaceRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<RotaryFurnaceRecipe> next = new ArrayList<>();
        for (Row row : RecipeText.ordered(TABLE.rows(), Row::order)) {
            next.add(new RotaryFurnaceRecipe(row.output(), row.duration(), row.steam(), row.fluid().orElse(null),
                    row.inputs().toArray(AStack[]::new)));
        }
        recipes = List.copyOf(next);
    }

    /** Ensures the rows are loaded (kept for the existing callers; the table fills itself from the datapack). */
    public static void register() {
        TABLE.rows();
    }

    /**
     * The original matcher, verbatim in shape: every non-empty input must be consumed by exactly one
     * ingredient (counts included) and every ingredient must be claimed, in any slot order.
     */
    public static @Nullable RotaryFurnaceRecipe getRecipe(FluidType fluid, ItemStack... inputs) {
        register();

        outer:
        for (RotaryFurnaceRecipe recipe : recipes) {
            // never try to match if a fluid is required and our tank doesn't match it
            if (recipe.fluid != null && recipe.fluid.type != fluid) continue;

            List<AStack> recipeList = new ArrayList<>(List.of(recipe.ingredients));

            for (ItemStack inputStack : inputs) {
                if (inputStack == null || inputStack.isEmpty()) continue;

                boolean hasMatch = false;
                Iterator<AStack> iterator = recipeList.iterator();
                while (iterator.hasNext()) {
                    AStack recipeStack = iterator.next();
                    if (recipeStack.matchesRecipe(inputStack, true) && inputStack.getCount() >= recipeStack.stacksize) {
                        hasMatch = true;
                        iterator.remove();
                        break;
                    }
                }
                if (!hasMatch) continue outer;
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    public static final class RotaryFurnaceRecipe {
        public final AStack[] ingredients;
        public final @Nullable FluidStack fluid;
        public final MaterialStack output;
        public final int duration;
        public final int steam;

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, @Nullable FluidStack fluid, AStack... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.steam = steam;
        }
    }

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(RotaryFurnaceRecipe recipe, int order) {
        return new Row(List.of(recipe.ingredients), Optional.ofNullable(recipe.fluid), recipe.output, recipe.duration,
                recipe.steam, order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(RotaryFurnaceRecipe recipe) {
        StringBuilder out = new StringBuilder("in=");
        for (AStack stack : recipe.ingredients) out.append(RecipeText.describe(stack)).append(',');
        out.append(" fluid=").append(recipe.fluid == null ? "-" : recipe.fluid.type.getKey() + "x" + recipe.fluid.fill
                + "@" + recipe.fluid.pressure);
        return out.append(" -> ").append(MaterialCodecs.describe(recipe.output)).append(" t=").append(recipe.duration)
                .append(" steam=").append(recipe.steam).toString();
    }

    /** Rejects a row the machine cannot run (MapCodec.validate is missing from 1.20.1's DataFixerUpper). */
    private static DataResult<Row> checkRow(Row row) {
        return row.inputs().size() > 3 ? DataResult.error(() -> "a rotary furnace row has at most three inputs, not " + row.inputs().size()) : DataResult.success(row);
    }
}
