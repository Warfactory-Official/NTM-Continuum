package com.hbm.inventory.recipes;

import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;


/**
 * Every HBM machine recipe table that is a datapack table, declared at mod construction (HbmEntry.declare) so their
 * recipe types and serializers register with HBM's content. A table converted to data adds its line here.
 */
public final class HbmRecipeTables {

    private HbmRecipeTables() {}

    public static void declare() {
        ChemicalPlantRecipes.declare();
        FractionRecipes.declare();
        // R-B: fluid-processing tables
        CokerRecipes.declare();
        CompressorRecipes.declare();
        CrackingRecipes.declare();
        ElectrolyserFluidRecipes.declare();
        GasCentrifugeRecipes.declare();
        HydrotreatingRecipes.declare();
        LiquefactionRecipes.declare();
        PyroOvenRecipes.declare();
        RadiolysisRecipes.declare();
        RefineryRecipes.declare();
        ReformingRecipes.declare();
        SolidificationRecipes.declare();
        StorageDrumRecipes.declare();
        NTMToolHandler.declare();
        CrucibleRecipes.declare(); // R-C1: item-processing tables
        MaterialDistribution.declare();
        ArcFurnaceRecipes.declare();
        CentrifugeRecipes.declare();
        CrystallizerRecipes.declare();
        CyclotronRecipes.declare();
        ElectrolyserMetalRecipes.declare();
        RotaryFurnaceRecipes.declare();
        SILEXRecipes.declare();
        ShredderRecipes.declare();
        // R-C2: item tables
        AmmoPressRecipes.declare();
        AnnihilatorRecipes.declare();
        ArcWelderRecipes.declare();
        BreederRecipes.declare();
        CombinationRecipes.declare();
        LemegetonRecipes.declare();
        MagicRecipes.declare();
        ParticleAcceleratorRecipes.declare();
        PedestalRecipes.declare();
        PressRecipes.declare();
        SawmillRecipes.declare();
        SolderingRecipes.declare();
        AnvilRecipes.declare();
        // R-A: GenericRecipe tables
        AssemblyMachineRecipes.declare();
        BlastFurnaceRecipesNT.declare();
        DiFurnaceRecipes.declare();
        ExposureChamberRecipes.declare();
        FluidBreederRecipes.declare();
        FusionRecipes.declare();
        MixerRecipes.declare();
        OutgasserRecipes.declare();
        PUREXRecipes.declare();
        PlasmaForgeRecipes.declare();
        PrecAssRecipes.declare();
        // script fields of the record-shaped tables (the generic ones state theirs in DataGenericRecipes)
        HbmRecipeFields.declare();
    }
}
