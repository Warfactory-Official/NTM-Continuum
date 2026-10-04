package com.hbm.inventory.recipes;

import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code PlasmaForgeRecipes}' "ICF" block: every ICF block is made in the plasma forge, nothing is crafted.
 *
 * <p>Two established port substitutions apply, as in the other plasma-forge and assembly recipes: the
 * {@code ANY_BISMOIDBRONZE} dict group (bismuth or arsenic bronze) is its bismuth-bronze member, since the group is
 * not ported. The expensive-mode alternatives ({@code inputItemsEx}) and the 528 "chlorophyte" blueprint pool are
 * carried and apply only in those modes, as in 1.7. Every other input, count, energy, duration and fluid is 1.7's.
 */
public final class ICFRecipes {

    private ICFRecipes() {}

    public static void registerPlasmaForge(PlasmaForgeRecipes recipes) {
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfcell").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_cell.get()))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 2), ANY_BISMOIDBRONZE.plateCast().count(4),
                        new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 2), new ComparableStack(ModItems.ingot_cft.get(), 8), new ComparableStack(ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfemitter").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_emitter.get()))
                .inputItems(W.plateWelded().count(4), MAGTUNG.wireDense().count(16))
                .inputFluids(new FluidStack(Fluids.XENON, 16_000))
                .inputItemsEx(W.plateWelded().count(8), MAGTUNG.wireDense().count(16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfcapacitor").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_capacitor.get()))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(1), ND.wireDense().count(16), SBD.wireDense().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 3), ND.wireDense().count(16), SBD.wireDense().count(2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfturbo").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_turbo.get()))
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(2), DNT.wireDense().count(4), SBD.wireDense().count(4))
                .inputItemsEx(ANY_RESISTANTALLOY.plateWelded().count(8), DNT.wireDense().count(8), SBD.wireDense().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfcasing").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_casing.get()))
                .inputItems(ANY_BISMOIDBRONZE.plateCast().count(4), BIGMT.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 4), BIGMT.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfport").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_laser_component_port.get()))
                .inputItems(ANY_BISMOIDBRONZE.plateCast().count(4), ANY_HARDPLASTIC.ingot().count(16), ND.wireDense().count(16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 4), ANY_HARDPLASTIC.ingot().count(16), ND.wireDense().count(16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfcontroller").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_controller.get()))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 16), ANY_BISMOIDBRONZE.plateCast().count(4),
                        ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 4), new ComparableStack(ModItems.ingot_cft.get(), 16), ANY_HARDPLASTIC.ingot().count(16), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfscaffold").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_component.get()))
                .inputItems(STEEL.plateWelded().count(4), TI.plateWelded().count(2))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfvessel").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_component_vessel.get()))
                .inputItems(new ComparableStack(ModItems.ingot_cft.get(), 1), CMB.plateCast().count(1), W.plateWelded().count(2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfstructural").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.icf_component_structure.get()))
                .inputItems(STEEL.plateWelded().count(2), CU.plateWelded().count(2), ANY_BISMOIDBRONZE.plateCast().count(1))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 1), STEEL.plateWelded().count(8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfcore").setInputEnergy(1_000_000).setup(3_000, 10_000_000)
                .outputItems(stack(ModIcfBlocks.struct_icf_core.get()))
                .inputItems(CMB.plateWelded().count(16), ANY_RESISTANTALLOY.plateWelded().count(16),
                        ANY_BISMOIDBRONZE.plateCast().count(16), SBD.wireDense().count(32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 16))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.BRONZE_TUBES), 16), CMB.plateWelded().count(16), SBD.wireDense().count(32), new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 32), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.COMPUTER), 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        recipes.registerGenerated(new PlasmaForgeRecipe("plsm.icfpress").setInputEnergy(1_000_000).setup(800, 10_000_000)
                .outputItems(stack(ModIcfBlocks.machine_icf_press.get()))
                .inputItems(GOLD.plateCast().count(8), new ComparableStack(ModItems.motor.get(), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
    }

    private static ItemStack stack(Block block) {
        return new ItemStack(block.asItem());
    }
}
