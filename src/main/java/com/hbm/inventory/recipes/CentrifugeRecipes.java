package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.CrystallizerRecipes.ChanceOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Output;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Centrifuge recipes: the datapack table {@code hbm:centrifuge}, one file per row in {@code data/hbm/recipe/centrifuge/}:
 *
 * <pre>
 * {"type": "hbm:centrifuge", "input": {"ingredient": "#c:ores/iron"}, "outputs": ["hbm:powder_iron", ..., "minecraft:gravel"],
 *  "order": 2}
 * </pre>
 *
 * The first row (by {@code order}, then recipe id) whose input matches wins, as 1.7 iterated its list; HBM's rows keep
 * 1.7's order. The centrifuge, the mining laser's centrifuge upgrade and the centrifuge tool ability read it. The rows
 * are generated from {@code CentrifugeTable} (1.7's defaults with the ore dictionary as {@code c:} tags, and the
 * 102 rows of the bedrock ore grade chain, 1.7 {@code ItemBedrockOreNew}).
 *
 * <p>An output is an item ({@code "id"} or {@code {"id", "count"}}) or the first member of a tag ({@code {"tag",
 * "count"}}, 1.7's {@code OreDictionary.getOres(name).get(0)}), as in {@link CrystallizerRecipes#OUTPUT}. A row's
 * optional {@code "requires"} names the 1.7 config branch it sat in ({@link RecipeConditions}: 1.7's
 * {@code enableLBSM && enableLBSMSimpleCentrifuge} yields are rows with {@code "lbsm_simple_centrifuge"} beside the
 * default rows with {@code "!lbsm_simple_centrifuge"}; {@code "#tag"} is 1.7's compat row for another mod's item).
 * The rows are tested again whenever a flag changed.
 */
public final class CentrifugeRecipes {

    private CentrifugeRecipes() {}

    /**
     * One row: an ingredient (its count is ignored, one item is processed at a time), up to four outputs and the 1.7
     * config branch it sat in (empty: always).
     */
    public record Row(AStack input, List<Output> outputs, List<String> requires, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.STACK.fieldOf("input").forGetter(Row::input),
            CrystallizerRecipes.OUTPUT.listOf().fieldOf("outputs").forGetter(Row::outputs),
            TableCodecs.REQUIRES.optionalFieldOf("requires", List.of()).forGetter(Row::requires),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("centrifuge", () -> CODEC);

    private static volatile List<CentrifugeRecipe> current = List.of();
    /** Set by a reload or sync: the rows are indexed again on the next read, once tags are bound. */
    private static volatile boolean stale = true;
    /** {@link RecipeConditions#fingerprint} of the last index. */
    private static volatile String builtFlags;

    /** The current recipes in lookup order, always the current rows (read-only). */
    public static final List<CentrifugeRecipe> recipes = TableCodecs.view(() -> {
        register();
        return current;
    });

    static {
        TABLE.changed.register(() -> stale = true);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static final class CentrifugeRecipe {
        public final AStack input;
        public final ItemStack[] output;
        /** Each output's chance; below 1 only for ntm-next's raw ore rows. */
        public final float[] chance;

        CentrifugeRecipe(AStack input, ItemStack[] output, float[] chance) {
            this.input = input;
            this.output = output;
            this.chance = chance;
        }
    }

    /**
     * Ensures the rows are loaded and indexed: again after a reload, or when a {@link RecipeConditions} flag changed
     * since the last index (1.7 read the config while registering). Rows whose {@code requires} fail are left out.
     */
    public static synchronized void register() {
        List<Row> rows = TABLE.rows();
        String flags = RecipeConditions.fingerprint();
        if (!stale && flags.equals(builtFlags)) return;
        stale = false;
        builtFlags = flags;
        List<CentrifugeRecipe> next = new ArrayList<>();
        for (Row row : RecipeText.ordered(rows, Row::order)) {
            if (!TableCodecs.holds(row.requires())) continue;
            ItemStack[] output = row.outputs().stream().map(Output::stack).toArray(ItemStack[]::new);
            boolean empty = false;
            for (ItemStack stack : output) empty |= stack.isEmpty();
            if (empty) continue; // a tag output whose tag has no item
            float[] chance = new float[output.length];
            for (int i = 0; i < chance.length; i++) chance[i] = row.outputs().get(i).chance();
            next.add(new CentrifugeRecipe(row.input(), output, chance));
        }
        current = List.copyOf(next);
    }

    /** Every output of the matching recipe, each in full (what the machine needs room for), or null. */
    public static ItemStack[] getOutput(ItemStack stack) {
        CentrifugeRecipe recipe = find(stack);
        if (recipe == null) return null;
        ItemStack[] result = new ItemStack[recipe.output.length];
        for (int i = 0; i < result.length; i++) result[i] = recipe.output[i].copy();
        return result;
    }

    /** What one operation makes: {@link #getOutput(ItemStack)} with each chance rolled, a lost roll empty. */
    public static ItemStack[] getOutput(ItemStack stack, RandomSource random) {
        CentrifugeRecipe recipe = find(stack);
        if (recipe == null) return null;
        ItemStack[] result = new ItemStack[recipe.output.length];
        for (int i = 0; i < result.length; i++) {
            boolean made = recipe.chance[i] >= 1F || random.nextFloat() < recipe.chance[i];
            result[i] = made ? recipe.output[i].copy() : ItemStack.EMPTY;
        }
        return result;
    }

    private static CentrifugeRecipe find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        for (CentrifugeRecipe recipe : current) {
            if (recipe.input.matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }

    public static boolean isAnyRecipeInput(ItemStack stack) {
        return getOutput(stack) != null;
    }

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(CentrifugeRecipe recipe, int order) {
        List<Output> outputs = new ArrayList<>();
        for (int i = 0; i < recipe.output.length; i++) {
            ItemOut out = ItemOut.of(recipe.output[i]);
            outputs.add(recipe.chance[i] < 1F ? new ChanceOutput(out, recipe.chance[i]) : new ItemOutput(out));
        }
        return new Row(recipe.input, outputs, List.of(), order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(CentrifugeRecipe recipe) {
        return RecipeText.describe(recipe.input) + " -> " + RecipeText.describe(recipe.output);
    }
}
