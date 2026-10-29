package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The sawmill's inputs and outputs: the datapack table {@code hbm:sawmill}, one file per input in
 * {@code data/hbm/recipe/sawmill/}:
 * <pre>
 * {"input": {"ingredient": "#minecraft:planks"}, "output": {"id": "minecraft:stick", "count": 6},
 *  "sawdust_chance": 0.1, "order": 3}
 * {"input": {"ingredient": "#minecraft:logs"}, "output": {"id": "minecraft:oak_planks", "count": 6},
 *  "crafting_result": {"require": "#minecraft:planks", "multiply": 6, "divide": 4}, "sawdust_chance": 0.5, "order": 2}
 * </pre>
 * The first row (by {@code order}) whose input matches decides the output; {@code sawdust_chance} is the chance of one
 * sawdust in the side slot. {@code crafting_result} makes the output whatever the input crafts into on the crafting
 * table (only if it is in {@code require}), scaled by {@code multiply / divide}: 1.7 sawed a log into the planks its own
 * crafting recipe gives, four planks becoming six, so modded logs make their own planks; {@code output} is then only
 * what the recipe viewer shows. {@code hidden} rows are left out of the viewer.
 *
 * <p>1.7's {@code TileEntitySawmill.getOutput} was code, not a table: {@code stickWood} to sawdust, {@code logWood} to
 * its crafted planks x 6/4, {@code plankWood} to six sticks, {@code treeSapling} to one stick, in that order, with
 * {@code sawdustChance} 0 for sawdust, 0.1 for sticks and 0.5 otherwise. These are five rows here (the stick has
 * its own hidden row besides {@code c:rods/wooden}, as the port matches it separately), so an addon adds a sawing
 * (NTM: Space's vinyl and PVC woods) with a file.
 */
public final class SawmillRecipes {

    private SawmillRecipes() {}

    /** The modern equivalent of the old {@code stickWood} ore-dictionary entry. */
    public static final TagKey<Item> WOODEN_RODS = TagKey.create(Registries.ITEM, Ids.of("c", "rods/wooden"));

    /** The recipe viewer's effective inputs, primary output and unrounded sawdust probability. */
    public record SawmillRecipe(List<ItemStack> input, ItemStack output, float sawdustChance) {}

    /** The input's crafting-table result, if it is in {@code require}, scaled by {@code multiply / divide}. */
    public record CraftingResult(TagKey<Item> require, int multiply, int divide) {

        static final Codec<CraftingResult> CODEC = RecordCodecBuilder.create(i -> i.group(
                TagKey.hashedCodec(Registries.ITEM).fieldOf("require").forGetter(CraftingResult::require),
                Codec.INT.optionalFieldOf("multiply", 1).forGetter(CraftingResult::multiply),
                Codec.INT.optionalFieldOf("divide", 1).forGetter(CraftingResult::divide)
        ).apply(i, CraftingResult::new));
    }

    public record Row(AStack input, ItemSpec output, Optional<CraftingResult> craftingResult, float sawdustChance,
                      boolean hidden, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.INGREDIENT.fieldOf("input").forGetter(Row::input),
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            CraftingResult.CODEC.optionalFieldOf("crafting_result").forGetter(Row::craftingResult),
            Codec.FLOAT.optionalFieldOf("sawdust_chance", 0F).forGetter(Row::sawdustChance),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Row::hidden),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("sawmill", () -> CODEC);

    private static volatile List<Row> rows = List.of();

    static {
        TABLE.changed.register(() -> rows = TableCodecs.ordered(TABLE.rows(), Row::order));
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static List<Row> index() {
        if (rows.isEmpty()) TABLE.rows();
        return rows;
    }

    /** The row that saws {@code input} (the first match by order), or null. */
    public static @Nullable Row match(ItemStack input) {
        if (input.isEmpty()) return null;
        return firstMatch(index(), input);
    }

    private static @Nullable Row firstMatch(List<Row> candidates, ItemStack input) {
        for (var row : candidates) if (row.input().matchesRecipe(input, true)) return row;
        return null;
    }

    /** Visible rows contain only inputs that actually select that row, including addon overrides. */
    public static List<SawmillRecipe> getRecipes() {
        var out = new ArrayList<SawmillRecipe>();
        // Use one snapshot: a table reload must not compare old rows against a newer match index.
        var snapshot = index();
        for (var row : snapshot) {
            if (row.hidden()) continue;
            var input = row.input().getDisplayStacks().stream()
                    .filter(stack -> !stack.isEmpty() && firstMatch(snapshot, stack) == row)
                    .map(ItemStack::copy).toList();
            if (input.isEmpty()) continue;
            out.add(new SawmillRecipe(input, row.output().stack(), row.sawdustChance()));
        }
        return out;
    }

    /** Resolves a sawmill input on the server (a crafting-result row reads the level's crafting recipes). */
    public static ItemStack getOutput(Level level, ItemStack input) {
        Row row = match(input);
        if (row == null) return ItemStack.EMPTY;
        if (row.craftingResult().isEmpty()) return row.output().stack();
        CraftingResult crafting = row.craftingResult().get();
        return Recipes.craftingResult(level, input)
                .filter(result -> result.is(crafting.require()))
                .map(result -> {
                    result.setCount(result.getCount() * crafting.multiply() / crafting.divide());
                    return result;
                })
                .orElse(ItemStack.EMPTY);
    }

    /** The chance of a sawdust beside the output of sawing {@code input} (1.7 {@code sawdustChance}). */
    public static float sawdustChance(ItemStack input) {
        Row row = match(input);
        return row == null ? 0F : row.sawdustChance();
    }

    /** Whether the output of {@code input} depends on the server's crafting recipes (a client cannot check it). */
    public static boolean needsCraftingResult(ItemStack input) {
        Row row = match(input);
        return row != null && row.craftingResult().isPresent();
    }

    public static boolean isValidInput(ItemStack input) {
        return match(input) != null;
    }
}
