package com.hbm.blocks.bomb;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModBombItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.AL;
import static com.hbm.inventory.OreDictManager.DURA;
import static com.hbm.inventory.OreDictManager.PB;
import static com.hbm.inventory.OreDictManager.TI;
import static com.hbm.inventory.OreDictManager.U238;
import static com.hbm.inventory.OreDictManager.WEAPONSTEEL;

/** Dependency-complete Ivy Mike assembly-machine recipes, kept with the bomb feature. */
public final class BombAssemblyRecipes {

    private BombAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        recipes.registerGenerated(new GenericRecipe("ass.ivymike").setup(600, 100)
                .inputItems(new ComparableStack(ModItems.sphere_steel.get()), AL.shell().count(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8),
                        new ComparableStack(Items.LIGHT_GRAY_DYE, 16))
                .outputItems(new ItemStack(ModBombBlocks.nuke_mike.get())));
        recipes.registerGenerated(new GenericRecipe("ass.mikecore").setup(1_200, 100)
                .inputItems(U238.nugget().count(24), PB.plate().count(6))
                .outputItems(new ItemStack(ModBombItems.mike_core.get())));
        recipes.registerGenerated(new GenericRecipe("ass.mikedeut").setup(600, 100)
                .inputItems(WEAPONSTEEL.plate().count(16), TI.plate().count(16))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10_000))
                .outputItems(new ItemStack(ModBombItems.mike_deut.get())));
        recipes.registerGenerated(new GenericRecipe("ass.mikecooler").setup(300, 100)
                .inputItems(DURA.plate().count(8), new ComparableStack(ModItems.coil_copper.get(), 5),
                        new ComparableStack(ModItems.coil_tungsten.get(), 5),
                        new ComparableStack(ModItems.motor.get(), 2))
                .outputItems(new ItemStack(ModBombItems.mike_cooling_unit.get())));
    }
}
