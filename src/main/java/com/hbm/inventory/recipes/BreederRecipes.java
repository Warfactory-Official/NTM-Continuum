package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Breeding reactor recipes: the datapack table {@code hbm:breeder}, one file per input in {@code data/hbm/recipe/breeder/}
 * ({@code {"input": item, "output": item, "flux": n, "order": n}}; the reactor breeds the input into the output once
 * it has absorbed {@code flux}). Converted once from 1.7's {@code BreederRecipes.registerDefaults}: each rod type for
 * the single, dual (flux x2) and quad (flux x3) rods, and the etched meteorite sword (flux 1000).
 */
public final class BreederRecipes {

    private BreederRecipes() {}

    /** nicer than opaque object arrays */
    public record BreederRecipe(ItemStack output, int flux) {}

    /** One row; {@code order} keeps 1.7's registration order for the recipe display. */
    public record Row(Item input, ItemSpec output, int flux, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStacks.itemCodec().fieldOf("input").forGetter(Row::input),
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("flux").forGetter(Row::flux),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("breeder", () -> CODEC);

    private static volatile Map<Item, BreederRecipe> recipes = Map.of();

    static {
        TABLE.changed.register(BreederRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        Map<Item, BreederRecipe> next = new LinkedHashMap<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            next.put(row.input(), new BreederRecipe(row.output().stack(), row.flux()));
        }
        recipes = next;
    }

    private static Map<Item, BreederRecipe> index() {
        if (recipes.isEmpty()) TABLE.rows();
        return recipes;
    }

    public static Map<Item, BreederRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(index());
    }

    public static BreederRecipe getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return index().get(stack.getItem());
    }
}
