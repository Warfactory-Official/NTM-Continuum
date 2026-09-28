package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModPwrBlocks;

/**
 * The 1.7 {@code AssemblyMachineRecipes} "PWR" block, kept with the reactor feature. The old
 * {@code inputItemsEx} expensive-mode lists are carried and applied by {@code GenericRecipe} in expensive mode, as in the
 * rest of the curated assembler catalogue. {@code ANY_CONCRETE.any()} is the {@code c:concretes} tag.
 */
public final class PwrAssemblyRecipes {

    private PwrAssemblyRecipes() {}

    private static TagStack concretes(int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "concretes")), count);
    }

    public static void registerAll(GenericRecipes recipes) {
        recipes.registerGenerated(new GenericRecipe("ass.pwrfuel").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_FUEL.get(), 4))
                .inputItems(PB.plateCast().count(4), ZR.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING)), ZR.plateWelded().count(2)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrcontrol").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CONTROL.get(), 4))
                .inputItems(STEEL.plateCast().count(2), B.ingot().count(4), new ComparableStack(ModItems.motor.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING)), B.ingot().count(4), new ComparableStack(ModItems.motor.get())));
        recipes.registerGenerated(new GenericRecipe("ass.pwrchannel").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CHANNEL.get(), 4))
                .inputItems(STEEL.pipe().count(4), CU.plate().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), STEEL.pipe().count(4)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrheatex").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_HEATEX.get(), 4))
                .inputItems(CU.plateCast().count(4), new ComparableStack(ModItems.motor.get(), 1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2), CU.plateCast().count(4))
                .inputFluidsEx(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrheatsink").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_HEATSINK.get(), 4))
                .inputItems(BIGMT.plateCast().count(4), CU.plateCast().count(4))
                .inputItemsEx(BIGMT.plateCast().count(4), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING))));
        recipes.registerGenerated(new GenericRecipe("ass.pwrreflector").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_REFLECTOR.get(), 4))
                .inputItems(STEEL.plateCast().count(2), new ComparableStack(ModItems.neutron_reflector.get(), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING)), new ComparableStack(ModItems.neutron_reflector.get(), 16)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrreflectoralt").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_REFLECTOR.get(), 4))
                .inputItems(STEEL.plateCast().count(2), WEAPONSTEEL.plate().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING)), WEAPONSTEEL.plate().count(32)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrcasing").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CASING.get(), 4))
                .inputItems(PB.plate().count(4), concretes(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING)), concretes(4)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrcontroller").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_CONTROLLER.get(), 1))
                .inputItems(PB.plateCast().count(4), RUBBER.ingot().count(4),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(com.hbm.items.ItemEnums.EnumCircuitType.BASIC), 4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING)), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 3)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrport").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_PORT.get(), 4))
                .inputItems(PB.plate().count(4), concretes(4), STEEL.pipe().count(4))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING)), concretes(4), STEEL.pipe().count(4)));
        recipes.registerGenerated(new GenericRecipe("ass.pwrneutronsource").setup(200, 500).outputItems(new ItemStack(ModPwrBlocks.PWR_NEUTRON_SOURCE.get(), 1))
                .inputItems(ZR.plateWelded().count(1), new ComparableStack(ModItems.billet_ra226be.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING)), ZR.plateWelded(), new ComparableStack(ModItems.billet_ra226be.get(), 3)));
    }
}
