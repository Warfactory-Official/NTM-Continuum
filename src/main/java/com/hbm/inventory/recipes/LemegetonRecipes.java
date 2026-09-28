package com.hbm.inventory.recipes;

import dev.golem.core.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code LemegetonRecipes}: the Lemegeton's one-slot "material upgrade conversion", each ingot (or gem)
 * into the next rung of its ladder. Keys are {@code #tag}s for 1.7's ore-dictionary stacks and item ids for its
 * comparable stacks; outputs are item ids resolved when a recipe is read.
 */
public final class LemegetonRecipes {

    public static final Map<String, String> recipes = new LinkedHashMap<>();

    static {
        register();
    }

    private LemegetonRecipes() {}

    private static void put(String input, String output) {
        recipes.put(input, output);
    }

    public static void register() {
        put("#c:ingots/iron", "hbm:ingot_steel");
        put("#c:ingots/steel", "hbm:ingot_dura_steel");
        put("#c:ingots/dura_steel", "hbm:ingot_tcalloy");
        put("#c:ingots/tcalloy", "hbm:ingot_combine_steel");
        put("#c:ingots/combine_steel", "hbm:ingot_dineutronium");

        put("#c:ingots/titanium", "hbm:ingot_saturnite");
        put("#c:ingots/saturnite", "hbm:ingot_starmetal");

        put("#c:ingots/copper", "hbm:ingot_red_copper");
        put("#c:ingots/red_copper", "hbm:ingot_desh");
        put("#c:ingots/desh", "hbm:ingot_bscco");

        put("#c:ingots/lead", "minecraft:gold_ingot");
        put("#c:ingots/gold", "hbm:ingot_bismuth");
        put("#c:ingots/bismuth", "hbm:ingot_osmiridium");

        put("#c:ingots/th232", "hbm:ingot_uranium");
        put("#c:ingots/uranium", "hbm:ingot_u238");
        put("#c:ingots/u238", "hbm:ingot_u235");
        put("#c:ingots/u235", "hbm:ingot_plutonium");
        put("#c:ingots/plutonium", "hbm:ingot_pu238");
        put("#c:ingots/pu238", "hbm:ingot_pu239");
        put("#c:ingots/pu239", "hbm:ingot_pu240");
        put("#c:ingots/pu240", "hbm:ingot_pu241");
        put("#c:ingots/pu241", "hbm:ingot_am241");
        put("#c:ingots/am241", "hbm:ingot_am242");

        put("#c:ingots/ra226", "hbm:ingot_polonium");
        put("#c:ingots/polonium", "hbm:ingot_technetium");

        put("#c:ingots/polymer", "hbm:ingot_pc");
        put("#c:ingots/bakelite", "hbm:ingot_pvc");
        put("#c:ingots/latex", "hbm:ingot_rubber");

        put("#c:gems/coal", "hbm:ingot_graphite");
        put("#c:ingots/graphite", "minecraft:diamond");
        put("#c:gems/diamond", "hbm:ingot_cft");

        put("#c:dusts/fluorite", "hbm:gem_sodalite");
        put("#c:gems/sodalite", "hbm:gem_volcanic");
        put("#c:gems/volcanic", "hbm:gem_rad");
        put("hbm:gem_rad", "hbm:gem_alexandrite");

        put("#minecraft:sand", "hbm:ingot_fiberglass");
        put("#c:ingots/fiberglass", "hbm:ingot_asbestos");
    }

    /** The first recipe the ingredient matches, or empty. */
    public static ItemStack getRecipe(ItemStack ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            String key = entry.getKey();
            boolean matches = key.startsWith("#")
                    ? ingredient.is(TagKey.create(Registries.ITEM, Ids.parse(key.substring(1))))
                    : BuiltInRegistries.ITEM.getKey(ingredient.getItem()).toString().equals(key);
            if (matches) {
                Item out = BuiltInRegistries.ITEM.getValue(Ids.parse(entry.getValue()));
                return out == null ? ItemStack.EMPTY : new ItemStack(out);
            }
        }
        return ItemStack.EMPTY;
    }
}
