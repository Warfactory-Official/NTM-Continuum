package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code LemegetonRecipes}: the Lemegeton's one-slot "material upgrade conversion", each ingot (or gem) into the
 * next rung of its ladder. The datapack table {@code hbm:lemegeton}, one file per input in
 * {@code data/hbm/recipe/lemegeton/} ({@code {"input": "#c:ingots/iron" | item, "output": item, "order": n}}); the
 * first row (by {@code order}) whose input matches wins, as 1.7's insertion-ordered map did. Converted once from the
 * Java table (1.7's 37 rows: {@code #tag}s for its ore-dictionary keys, item ids for its comparable stacks).
 */
public final class LemegetonRecipes {

    private LemegetonRecipes() {}

    public record Row(Either<TagKey<Item>, Item> input, Item output, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.INGREDIENT.fieldOf("input").forGetter(Row::input),
            dev.golem.core.world.ItemStacks.itemCodec().fieldOf("output").forGetter(Row::output),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("lemegeton", () -> CODEC);

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

    /** Every conversion in match order: input ({@code #tag} or item id) to output item id. */
    public static Map<String, String> recipes() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Row row : index()) out.put(key(row.input()), String.valueOf(BuiltInRegistries.ITEM.getKey(row.output())));
        return out;
    }

    private static String key(Either<TagKey<Item>, Item> input) {
        return input.map(tag -> "#" + tag.location(), item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
    }

    /** The first recipe the ingredient matches, or empty. */
    public static ItemStack getRecipe(ItemStack ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        for (Row row : index()) {
            boolean matches = row.input().map(ingredient::is, ingredient::is);
            if (matches) return new ItemStack(row.output());
        }
        return ItemStack.EMPTY;
    }
}
