package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.STEEL;
import com.hbm.blocks.ModExplosivesBlocks;

/** 1.7 {@code AssemblyMachineRecipes} entries for this family (the naval mine). */
public final class ExplosivesAssemblyRecipes {

    private ExplosivesAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        recipes.registerGenerated(new GenericRecipe("ass.minenaval").setup(300, 100)
                .inputItems(new ComparableStack(ModItems.sphere_steel.get()), STEEL.pipe().count(3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED)),
                        tag("c", "ingots/any_plasticexplosive", 24))
                .outputItems(new ItemStack(ModExplosivesBlocks.mine_naval.get())));
    }

    private static AStack tag(String ns, String path, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of(ns, path)), count);
    }
}
