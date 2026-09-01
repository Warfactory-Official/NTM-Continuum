package com.hbm.foundry;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** Feasible normal-mode assembly recipes for the two machines, kept out of the central recipe table. */
public final class FoundryExpansionAssemblyRecipes extends GenericRecipes {

    public static final FoundryExpansionAssemblyRecipes INSTANCE = new FoundryExpansionAssemblyRecipes();
    private static final TagKey<net.minecraft.world.item.Item> CONCRETE = TagKey.create(
            Registries.ITEM, dev.golem.core.Ids.parse("c:concretes"));

    private FoundryExpansionAssemblyRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        register(new GenericRecipe("ass.arcfurnace").setup(200, 100)
                .inputItems(new TagStack(CONCRETE, 12), com.hbm.inventory.OreDictManager.ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16), Mats.MAT_STEEL.plateCast().count(8),
                        new ComparableStack(com.hbm.blocks.ModBlocks.machine_transformer.get().asItem()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG)))
                .outputItems(new ItemStack(FoundryExpansion.ARC_FURNACE.get())));

        register(new GenericRecipe("ass.strandcaster").setup(200, 100)
                .inputItems(new ComparableStack(ModItems.ingot_firebrick.get(), 16), Mats.MAT_STEEL.plateCast().count(6),
                        Mats.MAT_COPPER.plateWelded().count(2), Mats.MAT_STEEL.shell().count(2),
                        new TagStack(CONCRETE, 8))
                .outputItems(new ItemStack(FoundryExpansion.STRAND_CASTER.get())));
    }

    public static void register() { INSTANCE.registerRecipes(); }
}
