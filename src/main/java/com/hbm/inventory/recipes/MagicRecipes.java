package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code MagicRecipes}, the Book of Boxcars' "extended 4-slot crafting": the filled grid slots, read in slot order
 * and compacted, must match a recipe's inputs one for one, in order. The datapack table {@code hbm:magic}, one file per
 * recipe in {@code data/hbm/recipe/magic/} ({@code {"ingredients": [item | "#tag", ...], "result": item, "order": n}};
 * the first match by {@code order} wins). Inputs are item ids (1.7's metadata stacks are items of their own:
 * {@code ingot_u238m2:1..3} = {@code hs_elements/arsenic/vault}) or {@code #tag}s. Converted once from the Java table:
 * all seven 1.7 recipes (the electronium ingot and the balefire-and-steel are registered).
 */
public final class MagicRecipes {

    private MagicRecipes() {}

    public record Row(List<Either<TagKey<Item>, Item>> ingredients, Item result, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.INGREDIENT.listOf().fieldOf("ingredients").forGetter(Row::ingredients),
            dev.golem.core.world.ItemStacks.itemCodec().fieldOf("result").forGetter(Row::result),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("magic", () -> CODEC);

    private static volatile List<MagicRecipe> recipes = List.of();

    static {
        TABLE.changed.register(MagicRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<MagicRecipe> next = new ArrayList<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            Input[] inputs = row.ingredients().stream().map(in -> in.map(tag -> tag(tag.location().toString()),
                    item -> item(String.valueOf(BuiltInRegistries.ITEM.getKey(item))))).toArray(Input[]::new);
            next.add(new MagicRecipe(String.valueOf(BuiltInRegistries.ITEM.getKey(row.result())), inputs));
        }
        recipes = List.copyOf(next);
    }

    public static ItemStack getRecipe(List<ItemStack> grid) {
        List<ItemStack> comps = new ArrayList<>();
        for (ItemStack stack : grid) if (!stack.isEmpty()) comps.add(stack);

        for (MagicRecipe recipe : getRecipes()) {
            if (recipe.matches(comps)) return recipe.getResult();
        }
        return ItemStack.EMPTY;
    }

    public static List<MagicRecipe> getRecipes() {
        if (recipes.isEmpty()) TABLE.rows();
        return recipes;
    }

    /** 1.7 {@code ComparableStack}: this item. */
    public static Input item(String id) {
        return new Input(id, false);
    }

    /** 1.7 {@code OreDictStack}. */
    public static Input tag(String id) {
        return new Input(id, true);
    }

    public record Input(String id, boolean isTag) {

        public boolean matches(ItemStack stack) {
            if (isTag) return stack.is(TagKey.create(Registries.ITEM, Ids.parse(id)));
            Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
            return item != Items.AIR && stack.is(item);
        }
    }

    public static final class MagicRecipe {

        public final List<Input> in;
        public final String out;

        public MagicRecipe(String out, Input... in) {
            this.out = out;
            this.in = Arrays.asList(in);
        }

        public boolean matches(List<ItemStack> comps) {
            if (comps.size() != in.size()) return false;
            for (int i = 0; i < in.size(); i++) {
                if (!in.get(i).matches(comps.get(i))) return false;
            }
            return !getResult().isEmpty();
        }

        public ItemStack getResult() {
            Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(out));
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
    }
}
