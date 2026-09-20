package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** Feasible 1.12.2 plasma-forge recipes ported to the modern item/material set. */
public final class PlasmaForgeRecipes extends GenericRecipes {

    public static final PlasmaForgeRecipes INSTANCE = new PlasmaForgeRecipes();
    private static final String GROUP_PLATES = "autoswitch.weldPlates";

    private PlasmaForgeRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        register(new PlasmaForgeRecipe("plsm.plateeuphemium").setInputEnergy(1_000_000).setup(600, 10_000_000)
                .outputItems(new ItemStack(ModItems.plate_euphemium.get(), 4))
                .inputItems(new ComparableStack(ModItems.ingot_euphemium.get(), 4), new ComparableStack(ModItems.powder_astatine.get(), 3), BI.dust(),
                        new ComparableStack(ModItems.gem_volcanic.get()), OSMIRIDIUM.ingot()));
        register(new PlasmaForgeRecipe("plsm.platednt").setInputEnergy(1_000_000).setup(600, 10_000_000)
                .outputItems(new ItemStack(ModItems.plate_dineutronium.get(), 4))
                .inputItems(DNT.ingot().count(4), new ComparableStack(ModItems.powder_spark_mix.get(), 2), DESH.ingot()));
        register(new PlasmaForgeRecipe("plsm.hde").setInputEnergy(10_000_000).setup(600, 25_000_000L)
                .outputItems(ModIngredientItems.part(EnumPartType.HDE, 1))
                .inputItems(BBRONZE.plateCast().count(2), CMB.plateWelded(), new ComparableStack(ModItems.ingot_cft.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));

        weld("iron", Mats.MAT_IRON, 50, 100, null);
        weld("steel", Mats.MAT_STEEL, 50, 500, null);
        weld("copper", Mats.MAT_COPPER, 50, 1_000, null);
        weld("titanium", Mats.MAT_TITANIUM, 300, 50_000, null);
        weld("zirconium", Mats.MAT_ZIRCONIUM, 300, 10_000, null);
        weld("aluminium", Mats.MAT_ALUMINIUM, 150, 10_000, null);
        weld("tcalloy", Mats.MAT_TCALLOY, 600, 1_000_000, new FluidStack(Fluids.OXYGEN, 1_000));
        weld("cdalloy", Mats.MAT_CDALLOY, 600, 1_000_000, new FluidStack(Fluids.OXYGEN, 1_000));
        weld("tungsten", Mats.MAT_TUNGSTEN, 600, 250_000, new FluidStack(Fluids.OXYGEN, 1_000));
        weld("cmb", Mats.MAT_CMB, 600, 10_000_000, new FluidStack(Fluids.REFORMGAS, 1_000));
        weld("osmiridium", Mats.MAT_OSMIRIDIUM, 3_000, 50_000_000, new FluidStack(Fluids.REFORMGAS, 16_000));

        register(new PlasmaForgeRecipe("plsm.fusionvessel").setInputEnergy(3_000_000).setup(1_200, 2_000_000)
                .outputItems(new ItemStack(ModMachineBlocks.fusion_torus.get()))
                .inputItems(
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_bscco_welded.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_bscco_welded.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_bscco_welded.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_blanket.get().asItem(), 64),
                        new ComparableStack(ModMachineBlocks.fusion_component_blanket.get().asItem(), 64),
                         new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4)));

        // --- dark fusion core ---
        register(new PlasmaForgeRecipe("plsm.dfccore").setInputEnergy(50_000_000).setup(12_000, 100_000_000)
                .outputItems(new ItemStack(ModDFCBlocks.dfc_core.get()))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), DNT.wireDense().count(16),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 12),
                        new ComparableStack(com.hbm.items.ModSingularityItems.singularity_spark.get()),
                        new ComparableStack(ModItems.powder_chlorophyte.get(), 64))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 12_000)));
        register(new PlasmaForgeRecipe("plsm.dfcemitter").setInputEnergy(50_000_000).setup(1_200, 10_000_000)
                .outputItems(new ItemStack(ModDFCBlocks.dfc_emitter.get()))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), STAR.wireDense().count(16),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));
        register(new PlasmaForgeRecipe("plsm.dfcreceiver").setInputEnergy(50_000_000).setup(1_200, 10_000_000)
                .outputItems(new ItemStack(ModDFCBlocks.dfc_receiver.get()))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), STAR.plateCast().count(16),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));
        register(new PlasmaForgeRecipe("plsm.dfcinjector").setInputEnergy(50_000_000).setup(1_200, 10_000_000)
                .outputItems(new ItemStack(ModDFCBlocks.dfc_injector.get()))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), BIGMT.plateCast().count(16),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 4))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));
        register(new PlasmaForgeRecipe("plsm.dfcstabilizer").setInputEnergy(50_000_000).setup(1_200, 10_000_000)
                .outputItems(new ItemStack(ModDFCBlocks.dfc_stabilizer.get()))
                .inputItems(OSMIRIDIUM.plateWelded().count(16), SBD.wireDense().count(16),
                        new ComparableStack(com.hbm.items.ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 8))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 4_000)));

        // ass.fensusan is intentionally deferred: its exact old inputs require the unregistered
        // ingot_electronium and ingot_u238m2. Do not replace either with a similar material.
    }

    private void weld(String name, com.hbm.inventory.material.NTMMaterial material,
                      int duration, long power, FluidStack fluid) {
        var output = ModAutogenItems.get(MaterialShapes.WELDEDPLATE, material);
        if (output == null) return;
        PlasmaForgeRecipe recipe = new PlasmaForgeRecipe("plsm.weld" + name);
        recipe.setInputEnergy(500_000).setup(duration, power)
                .outputItems(new ItemStack(output))
                .inputItems(material.plateCast().count(2))
                .setGroup(GROUP_PLATES, this);
        if (fluid != null) recipe.inputFluids(fluid);
        register(recipe);
    }
}
