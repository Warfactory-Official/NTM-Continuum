package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Liquefactor recipes, item to fluid (one item consumed): the datapack table {@code hbm:liquefaction} in
 * {@code data/hbm/recipe/liquefaction/}, {@code {"input": item id or #tag, "output": {type, amount}}}. Converted once from the
 * Java table (1.7 {@code LiquefactionRecipes.registerDefaults}, all 33 entries; ore-dict inputs are {@code c:} item tags, the
 * log key is {@code minecraft:logs}, 1.7 tallgrass:1 and :2 are short grass and fern, raw fish is {@code hbm:raw_fishes}).
 *
 * <p>Lookup as in 1.7: an exact item row first, then the first tag row (recipe-id order) that holds the item, then 1.7's
 * food fallback, which stays code: any other food gives SALIENT at nutrition x saturation modifier x 20.
 */
public final class LiquefactionRecipes {

    private LiquefactionRecipes() {}

    public record Row(Either<TagKey<Item>, Item> input, FluidStack output) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.INGREDIENT.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.FLUID_STACK.fieldOf("output").forGetter(Row::output)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("liquefaction", () -> CODEC);

    private record Index(Map<Item, FluidStack> byItem, Map<TagKey<Item>, FluidStack> byTag) {}

    private static volatile Index index;

    static {
        TABLE.changed.register(LiquefactionRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        Map<Item, FluidStack> byItem = new LinkedHashMap<>();
        Map<TagKey<Item>, FluidStack> byTag = new LinkedHashMap<>();
        for (Row row : TABLE.rows()) {
            if (row.input().left().isPresent()) byTag.putIfAbsent(row.input().left().get(), row.output());
            else byItem.putIfAbsent(row.input().right().orElseThrow(), row.output());
        }
        index = new Index(Collections.unmodifiableMap(byItem), Collections.unmodifiableMap(byTag));
    }

    private static Index index() {
        Index current = index;
        if (current == null) {
            TABLE.rows();
            if (index == null) reindex();
            current = index;
        }
        return current;
    }

    public static FluidStack getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Index current = index();
        FluidStack direct = current.byItem().get(stack.getItem());
        if (direct != null) return direct;
        for (Map.Entry<TagKey<Item>, FluidStack> e : current.byTag().entrySet()) {
            if (stack.is(e.getKey())) return e.getValue();
        }
        // 1.7 getOutput: any ItemFood gives SALIENT, food value x saturation modifier x 2 (constant) x 10 (quanta)
        FoodProperties food = ItemStacks.foodProperties(stack);
        if (food != null) {
            int salient = (int) (ItemStacks.foodNutrition(food)
                    * ItemStacks.foodSaturationModifier(food) * 20F);
            // 1.7 would return an empty 0 mB stack for a zero-saturation food; the port's tanks reject a 0 mB fill
            if (salient > 0) return new FluidStack(Fluids.SALIENT, salient);
        }
        return null;
    }

    /** Read-only views for JEI display. */
    public static Map<Item, FluidStack> getItemRecipes() { return index().byItem(); }
    public static Map<TagKey<Item>, FluidStack> getTagRecipes() { return index().byTag(); }
}
