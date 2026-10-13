package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Electrolyser (metal side) recipes: the datapack table {@code hbm:electrolyser_metal}, one file per input item in
 * {@code data/hbm/recipe/electrolyser_metal/}:
 *
 * <pre>
 * {"type": "hbm:electrolyser_metal", "input": "hbm:crystal_iron",
 *  "molten": [{"material": "iron", "amount": 432}, {"material": "titanium", "amount": 144}],
 *  "byproducts": [{"id": "hbm:powder_lithium_tiny", "count": 3}], "duration": 600, "order": 0}
 * </pre>
 *
 * One or two molten outputs, any number of item byproducts, {@code duration} in ticks (default 600, 1.7's first
 * constructor). The lookup is by input item; a second row for the same item replaces the first (1.7 kept a map). The
 * rows were converted once from the former Java table (1.7 {@code ElectrolyserMetalRecipes}, plus the 18 rows of the
 * bedrock ore grade chain, 20 ticks each) and are the source now.
 */
public final class ElectrolyserMetalRecipes {

    private ElectrolyserMetalRecipes() {}

    /** One row. */
    public record Row(Item input, List<Mats.MaterialStack> molten, List<ItemOut> byproducts, int duration, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            ItemStacks.itemCodec().fieldOf("input").forGetter(Row::input),
            MaterialCodecs.STACK.listOf().fieldOf("molten").forGetter(Row::molten),
            GenericRecipeCodec.ITEM_OUT.listOf().optionalFieldOf("byproducts", List.of()).forGetter(Row::byproducts),
            Codec.INT.optionalFieldOf("duration", 600).forGetter(Row::duration),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new)).flatXmap(ElectrolyserMetalRecipes::checkRow, ElectrolyserMetalRecipes::checkRow);

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("electrolyser_metal", () -> CODEC);

    /** The current recipes in display order (rebuilt after every reload or sync). */
    public static volatile List<ElectrolysisMetalRecipe> RECIPES = List.of();
    private static volatile Map<Item, ElectrolysisMetalRecipe> byItem = Map.of();

    static {
        TABLE.changed.register(ElectrolyserMetalRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<ElectrolysisMetalRecipe> next = new ArrayList<>();
        Map<Item, ElectrolysisMetalRecipe> index = new LinkedHashMap<>();
        for (Row row : RecipeText.ordered(TABLE.rows(), Row::order)) {
            ElectrolysisMetalRecipe recipe = new ElectrolysisMetalRecipe(new ComparableStack(row.input()), row.molten().get(0),
                    row.molten().size() > 1 ? row.molten().get(1) : null, row.duration(),
                    row.byproducts().stream().map(ItemOut::stack).toArray(ItemStack[]::new));
            next.add(recipe);
            index.put(row.input(), recipe); // last wins, as 1.7's map
        }
        RECIPES = List.copyOf(next);
        byItem = Map.copyOf(index);
    }

    /** Ensures the rows are loaded (kept for the existing callers; the table fills itself from the datapack). */
    public static void register() {
        TABLE.rows();
    }

    public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
        register();
        return stack.isEmpty() ? null : byItem.get(stack.getItem());
    }

    public static final class ElectrolysisMetalRecipe {
        public final AStack input;
        public final Mats.MaterialStack output1;
        public final Mats.MaterialStack output2;
        public final ItemStack[] byproduct;
        public final int duration;

        ElectrolysisMetalRecipe(AStack input, Mats.MaterialStack output1, Mats.MaterialStack output2,
                                int duration, ItemStack[] byproduct) {
            this.duration = duration;
            this.input = input;
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
        }
    }

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(ElectrolysisMetalRecipe recipe, int order) {
        List<Mats.MaterialStack> molten = new ArrayList<>();
        molten.add(recipe.output1);
        if (recipe.output2 != null) molten.add(recipe.output2);
        List<ItemOut> byproducts = new ArrayList<>();
        for (ItemStack stack : recipe.byproduct) byproducts.add(ItemOut.of(stack));
        return new Row(((ComparableStack) recipe.input).item, molten, byproducts, recipe.duration, order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(ElectrolysisMetalRecipe recipe) {
        return RecipeText.describe(recipe.input) + " -> " + MaterialCodecs.describe(recipe.output1) + " + "
                + MaterialCodecs.describe(recipe.output2) + " + " + RecipeText.describe(recipe.byproduct) + " t=" + recipe.duration;
    }

    /** Rejects a row the machine cannot run (MapCodec.validate is missing from 1.20.1's DataFixerUpper). */
    private static DataResult<Row> checkRow(Row row) {
        return row.molten().isEmpty() || row.molten().size() > 2 ? DataResult.error(() -> "an electrolyser row has one or two molten outputs, not " + row.molten().size()) : DataResult.success(row);
    }
}
