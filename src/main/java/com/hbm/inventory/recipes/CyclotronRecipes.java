package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Cyclotron recipes: the datapack table {@code hbm:cyclotron}, one file per row in {@code data/hbm/recipe/cyclotron/}:
 *
 * <pre>
 * {"type": "hbm:cyclotron", "particle": "hbm:part_lithium", "input": {"ingredient": "#c:dusts/lithium"},
 *  "output": "hbm:powder_beryllium", "antimatter": 50, "order": 0}
 * </pre>
 *
 * The first row (by {@code order}, then recipe id) whose particle and input match wins, as 1.7 iterated its list. The
 * 42 rows were converted once from the former Java table (1.7 {@code CyclotronRecipes}; {@code #c:dusts/phosphorus}
 * matches only another mod's phosphorus dust, as 1.7's {@code dustPhosphorus}) and are the source now.
 */
public final class CyclotronRecipes {

    private CyclotronRecipes() {}

    /** One row: particle item, target ingredient, product and the antimatter (mB) one conversion yields. */
    public record Row(Item particle, AStack input, ItemOut output, int antimatter, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStacks.itemCodec().fieldOf("particle").forGetter(Row::particle),
            GenericRecipeCodec.STACK.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.ITEM_OUT.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("antimatter").forGetter(Row::antimatter),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("cyclotron", () -> CODEC);

    /** The current recipes in lookup order (rebuilt after every reload or sync). */
    public static volatile List<CyclotronRecipe> recipes = List.of();

    static {
        TABLE.changed.register(CyclotronRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static final class CyclotronRecipe {
        public final ComparableStack particle;
        public final AStack input;
        public final ItemStack output;
        public final int antimatter;

        CyclotronRecipe(ComparableStack particle, AStack input, ItemStack output, int antimatter) {
            this.particle = particle;
            this.input = input;
            this.output = output;
            this.antimatter = antimatter;
        }
    }

    private static void reindex() {
        List<CyclotronRecipe> next = new ArrayList<>();
        for (Row row : RecipeText.ordered(TABLE.rows(), Row::order)) {
            next.add(new CyclotronRecipe(new ComparableStack(row.particle()), row.input(), row.output().stack(), row.antimatter()));
        }
        recipes = List.copyOf(next);
    }

    /** Ensures the rows are loaded (kept for the existing callers; the table fills itself from the datapack). */
    public static void register() {
        TABLE.rows();
    }

    public static CyclotronRecipe getRecipe(ItemStack particle, ItemStack input) {
        if (particle == null || particle.isEmpty() || input == null || input.isEmpty()) return null;
        register();
        for (CyclotronRecipe recipe : recipes) {
            if (recipe.particle.matchesRecipe(particle, true) && recipe.input.matchesRecipe(input, true)) return recipe;
        }
        return null;
    }

    public static Object[] getOutput(ItemStack input, ItemStack particle) {
        CyclotronRecipe recipe = getRecipe(particle, input);
        return recipe == null ? null : new Object[]{recipe.output.copy(), recipe.antimatter};
    }

    public static boolean isAnyParticle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        register();
        for (CyclotronRecipe recipe : recipes) if (recipe.particle.matchesRecipe(stack, true)) return true;
        return false;
    }

    public static boolean isAnyInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        register();
        for (CyclotronRecipe recipe : recipes) if (recipe.input.matchesRecipe(stack, true)) return true;
        return false;
    }

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(CyclotronRecipe recipe, int order) {
        return new Row(recipe.particle.item, recipe.input, ItemOut.of(recipe.output), recipe.antimatter, order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(CyclotronRecipe recipe) {
        return RecipeText.describe(recipe.particle) + " + " + RecipeText.describe(recipe.input) + " -> "
                + RecipeText.describe(recipe.output) + " am=" + recipe.antimatter;
    }
}
