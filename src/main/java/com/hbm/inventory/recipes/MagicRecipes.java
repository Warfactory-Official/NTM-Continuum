package com.hbm.inventory.recipes;

import com.hbm.items.ModDataComponents;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1.7 {@code MagicRecipes}, the Book of Boxcars' "extended 4-slot crafting": the filled grid slots, read in slot
 * order and compacted, must match a recipe's inputs one for one, in order. Inputs are item ids (with the 1.7
 * metadata as {@code item_variant}) or {@code #tag}s; a recipe naming an item the port does not register never
 * matches and yields nothing (1.7's electronium and balefire-and-steel entries, for now).
 */
public final class MagicRecipes {

    private MagicRecipes() {}

    private static final List<MagicRecipe> recipes = new ArrayList<>();

    public static ItemStack getRecipe(List<ItemStack> grid) {
        List<ItemStack> comps = new ArrayList<>();
        for (ItemStack stack : grid) if (!stack.isEmpty()) comps.add(stack);

        for (MagicRecipe recipe : getRecipes()) {
            if (recipe.matches(comps)) return recipe.getResult();
        }
        return ItemStack.EMPTY;
    }

    public static List<MagicRecipe> getRecipes() {
        if (recipes.isEmpty()) register();
        return recipes;
    }

    private static void register() {
        recipes.add(new MagicRecipe("hbm:ingot_u238m2", item("hbm:ingot_u238m2", 1), item("hbm:ingot_u238m2", 2),
                item("hbm:ingot_u238m2", 3)));
        recipes.add(new MagicRecipe("hbm:rod_of_discord", item("minecraft:ender_pearl"), item("minecraft:blaze_rod"),
                item("hbm:nugget_euphemium")));
        recipes.add(new MagicRecipe("hbm:balefire_and_steel", tag("c:ingots/steel"), item("hbm:egg_balefire_shard")));
        recipes.add(new MagicRecipe("hbm:mysteryshovel", item("minecraft:iron_shovel"), item("minecraft:bone"),
                item("hbm:ingot_starmetal"), item("hbm:ducttape")));
        recipes.add(new MagicRecipe("hbm:ingot_electronium", item("hbm:pellet_charged"), item("hbm:pellet_charged"),
                item("hbm:ingot_dineutronium"), item("hbm:ingot_dineutronium")));
        recipes.add(new MagicRecipe("hbm:diamond_gavel", item("hbm:gravel_diamond"), item("hbm:gravel_diamond"),
                item("hbm:gravel_diamond"), item("hbm:lead_gavel")));
        recipes.add(new MagicRecipe("hbm:mese_gavel", item("hbm:shimmer_handle"), item("hbm:powder_dineutronium"),
                item("hbm:blades_desh"), item("hbm:diamond_gavel")));
    }

    /** 1.7 {@code ComparableStack}: this item and (singular) metadata. */
    public static Input item(String id) {
        return item(id, 0);
    }

    public static Input item(String id, int variant) {
        return new Input(id, variant, false);
    }

    /** 1.7 {@code OreDictStack}. */
    public static Input tag(String id) {
        return new Input(id, 0, true);
    }

    public record Input(String id, int variant, boolean isTag) {

        public boolean matches(ItemStack stack) {
            if (isTag) return stack.is(TagKey.create(Registries.ITEM, Ids.parse(id)));
            Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
            return item != Items.AIR && stack.is(item) && ModDataComponents.ITEM_VARIANT.getOr(stack, 0) == variant;
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
