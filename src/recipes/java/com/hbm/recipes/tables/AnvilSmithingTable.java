package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.SmithingRow;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 1.7 {@code AnvilRecipes.registerSmithing}. The smithing behaviours that are code rather than rows (the dusted-steel
 * folding chain, the casting molds, cyanide and rename: {@code AnvilRecipes.codeSmithing}) are not rows; they still take
 * their 1.7 positions, so a row's {@code order} is its 1.7 index.
 */
public final class AnvilSmithingTable extends RecipeSource<SmithingRow> {

    /** The 1.7 index of the next recipe, rows and code recipes alike. */
    private int position;
    private final Set<String> ids = new HashSet<>();

    public AnvilSmithingTable() {
        super(AnvilRecipes.SMITHING.table);
    }

    @Override
    protected void registerDefaults() {

        Block[] anvils = new Block[]{ModAnvilBlocks.anvil_iron.get(), ModAnvilBlocks.anvil_lead.get()};

        for (Block anvil : anvils) {
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_steel.get(), 1), new ComparableStack(anvil.asItem()), STEEL.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_desh.get(), 1), new ComparableStack(anvil.asItem()), DESH.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_saturnite.get(), 1), new ComparableStack(anvil.asItem()), BIGMT.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_ferrouranium.get(), 1), new ComparableStack(anvil.asItem()), new ComparableStack(ModItems.ingot_ferrouranium.get(), 10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_bismuth_bronze.get(), 1), new ComparableStack(anvil.asItem()), BBRONZE.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_arsenic_bronze.get(), 1), new ComparableStack(anvil.asItem()), ABRONZE.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_schrabidate.get(), 1), new ComparableStack(anvil.asItem()), SBD.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_dnt.get(), 1), new ComparableStack(anvil.asItem()), DNT.ingot().count(10)));
            add(new AnvilSmithingRecipe(1, new ItemStack(ModAnvilBlocks.anvil_osmiridium.get(), 1), new ComparableStack(anvil.asItem()), OSMIRIDIUM.ingot().count(10)));
        }

        // the dusted-steel folding chain: its stages are item data, so it is code (AnvilRecipes.codeSmithing)
        for (int i = 0; i < 9; i++)
            code(); // new AnvilSmithingHotRecipe(3, ingot_steel_dusted:i+1, ingot_steel_dusted:i, ingot_steel_dusted:i)

        code(); // new AnvilSmithingHotRecipe(3, ingot_chainsteel, ingot_steel_dusted:9, ingot_steel_dusted:9)

        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.ingot_meteorite_forged.get(), 1), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.blade_meteorite.get(), 1), new ComparableStack(ModAbilityToolItems.ingot_meteorite_forged.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite_forged.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.meteorite_sword_reforged.get(), 1), new ComparableStack(ModAbilityToolItems.meteorite_sword_seared.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite_forged.get())));


        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_sword.get(), 1), new ComparableStack(ModAbilityToolItems.cobalt_sword.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_pickaxe.get(), 1), new ComparableStack(ModAbilityToolItems.cobalt_pickaxe.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_axe.get(), 1), new ComparableStack(ModAbilityToolItems.cobalt_axe.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_shovel.get(), 1), new ComparableStack(ModAbilityToolItems.cobalt_shovel.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));
        add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_hoe.get(), 1), new ComparableStack(ModAbilityToolItems.cobalt_hoe.get()), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get())));

        add(new AnvilSmithingRecipe(1916169, new ItemStack(ModArmorItems.wings_murk.get(), 1), new ComparableStack(ModArmorItems.wings_limp.get()), new ComparableStack(ModItems.particle_tachyon.get())));
        add(new AnvilSmithingRecipe(4, new ItemStack(ModFinalConsumableItems.flask_infusion_shield.get(), 1), new ComparableStack(ModItems.gem_alexandrite.get()), new ComparableStack(ModConsumableTailItems.bottle_nuka.get())));

        add(new AnvilSmithingRecipe(1, new ItemStack(ModItems.ingot_gunmetal.get(), 1), CU.ingot(), AL.ingot()));

        // the casting molds (shape matching), cyanide and rename are code (AnvilRecipes.codeSmithing), after the rows
    }

    /** 1.7 {@code AnvilSmithingRecipe}. */
    private record AnvilSmithingRecipe(int tier, ItemStack output, AStack left, AStack right, boolean hot) {

        AnvilSmithingRecipe(int tier, ItemStack output, AStack left, AStack right) {
            this(tier, output, left, right, false);
        }
    }

    /** 1.7 {@code AnvilSmithingHotRecipe}: both inputs must be hot, the output takes their averaged heat. */
    private record AnvilSmithingHotRecipe(int tier, ItemStack output, AStack left, AStack right) {}

    private void add(AnvilSmithingHotRecipe recipe) {
        add(new AnvilSmithingRecipe(recipe.tier(), recipe.output(), recipe.left(), recipe.right(), true));
    }

    /**
     * 1.7 {@code smithingRecipes.add(recipe)}. The id is the output's item name; the second row of one output (the lead
     * anvil's) also names its left input.
     */
    private void add(AnvilSmithingRecipe recipe) {
        String id = BuiltInRegistries.ITEM.getKey(recipe.output().getItem()).getPath();
        if (!ids.add(id)) {
            id = id + "_" + BuiltInRegistries.ITEM.getKey(((ComparableStack) recipe.left()).item).getPath();
            ids.add(id);
        }
        register(id, new SmithingRow(recipe.tier(), ItemSpec.of(recipe.output()).getOrThrow(), recipe.left(), recipe.right(),
                recipe.hot(), false, position++));
    }

    /** A 1.7 smithing recipe that is code, not a row: it only takes its position. */
    private void code() {
        position++;
    }
}
