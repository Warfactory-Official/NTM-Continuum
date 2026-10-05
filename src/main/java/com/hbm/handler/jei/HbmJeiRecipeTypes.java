package com.hbm.handler.jei;

//? if jei {

import com.hbm.Constants;
import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.resources.Identifier;

/**
 * The mod's JEI recipe-category identifiers — the modern-API equivalent of the old {@code JEIConfig}'s
 * {@code public static final String} UID block (1.12.2 JEI 4.x used bare String UIDs; JEI 29.x uses a
 * typed {@link JeiRecipeType}). Kept in one place so a new machine category is a one-line addition, exactly
 * like the old plugin. The path strings are preserved from the old mod (e.g. {@code "assembly_machine"}).
 *
 * <p>Two machines whose recipes share a Java class ({@link GenericRecipe}) still get distinct types
 * because {@link JeiRecipeType} identity is {@code (uid, class)} — a different path is a different type.
 *
 * <p>Grown one entry per machine as categories are ported (see IMPLEMENTATION.md §13p).
 */
public final class HbmJeiRecipeTypes {

    private HbmJeiRecipeTypes() {}

    static <T> JeiRecipeType<T> type(String path, Class<T> clazz) {
        return JeiRecipeType.create(Constants.MOD_ID, path, clazz);
    }

    public static final JeiRecipeType<GenericRecipe> ASSEMBLY_MACHINE = type("assembly_machine", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> ALLOY = type("alloy", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> PRECISION_ASSEMBLER = type("precass", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> CHEMICAL_PLANT = type("chemical_plant", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> PUREX = type("purex", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> FUSION = type("fusion", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> FUSION_BREEDER = type("fusionbreeder", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> PLASMA_FORGE = type("plasma_forge", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> MIXER = type("mixer", GenericRecipe.class);
    public static final JeiRecipeType<GenericRecipe> EXPOSURE_CHAMBER = type("exposure_chamber", GenericRecipe.class);
    public static final JeiRecipeType<ArcFurnaceRecipes.ArcFurnaceRecipe> ARC_FURNACE_SOLID =
            type("arc_furnace_solid", ArcFurnaceRecipes.ArcFurnaceRecipe.class);
    public static final JeiRecipeType<ArcFurnaceRecipes.ArcFurnaceRecipe> ARC_FURNACE_FLUID =
            type("arc_furnace_fluid", ArcFurnaceRecipes.ArcFurnaceRecipe.class);
    public static final JeiRecipeType<com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe> ARC_WELDER =
            type("arc_welder", com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe.class);
    public static final JeiRecipeType<com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe> ROTARY_FURNACE =
            type("rotary_furnace", com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe.class);
    public static final JeiRecipeType<StorageDrumRecipeHandler.Entry> STORAGE_DRUM =
            type("storage_drum", StorageDrumRecipeHandler.Entry.class);
    /** 1.7 {@code FuelPoolHandler} ("ntmSpentDrum"): the spent fuel pool drum. */
    public static final JeiRecipeType<com.hbm.inventory.recipes.FuelPoolRecipes.Entry> FUEL_POOL =
            type("fuel_pool", com.hbm.inventory.recipes.FuelPoolRecipes.Entry.class);

    public static final JeiRecipeType<PressRecipes.PressRecipe> PRESS = type("press", PressRecipes.PressRecipe.class);
    public static final JeiRecipeType<AmmoPressRecipes.AmmoPressRecipe> AMMO_PRESS =
            type("ammo_press", AmmoPressRecipes.AmmoPressRecipe.class);
    public static final JeiRecipeType<SolderingRecipes.SolderingRecipe> SOLDERING_STATION = type("soldering_station", SolderingRecipes.SolderingRecipe.class);
    public static final JeiRecipeType<CrystallizerRecipes.CrystallizerRecipe> CRYSTALLIZER = type("crystallizer", CrystallizerRecipes.CrystallizerRecipe.class);
    public static final JeiRecipeType<CentrifugeRecipes.CentrifugeRecipe> CENTRIFUGE = type("centrifuge", CentrifugeRecipes.CentrifugeRecipe.class);
    public static final JeiRecipeType<CyclotronRecipes.CyclotronRecipe> CYCLOTRON =
            type("cyclotron", CyclotronRecipes.CyclotronRecipe.class);
    public static final JeiRecipeType<com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe>
            PARTICLE_ACCELERATOR = type("particle_accelerator",
            com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe.class);
    public static final JeiRecipeType<GasCentrifugeRecipes.GasCentrifugeRecipe> GAS_CENTRIFUGE =
            type("gas_centrifuge", GasCentrifugeRecipes.GasCentrifugeRecipe.class);
    public static final JeiRecipeType<ElectrolyserFluidRecipes.ElectrolysisRecipe> ELECTROLYSIS_FLUID =
            type("electrolysis_fluid", ElectrolyserFluidRecipes.ElectrolysisRecipe.class);
    public static final JeiRecipeType<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> ELECTROLYSIS_METAL =
            type("electrolysis_metal", ElectrolyserMetalRecipes.ElectrolysisMetalRecipe.class);
    public static final JeiRecipeType<ShredderRecipeHandler.ShredderPair> SHREDDER = type("shredder", ShredderRecipeHandler.ShredderPair.class);
    public static final JeiRecipeType<com.hbm.inventory.recipes.SawmillRecipes.SawmillRecipe> SAWMILL =
            type("sawmill", com.hbm.inventory.recipes.SawmillRecipes.SawmillRecipe.class);
    public static final JeiRecipeType<com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe> SILEX = type("silex", com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe.class);

    public static final JeiRecipeType<CrucibleRecipes.CrucibleRecipe> CRUCIBLE_ALLOY = type("crucible_alloy", CrucibleRecipes.CrucibleRecipe.class);
    public static final JeiRecipeType<AnvilRecipes.AnvilConstructionRecipe> ANVIL_CON = type("anvil_construction", AnvilRecipes.AnvilConstructionRecipe.class);
    public static final JeiRecipeType<AnvilSmithingRecipe> ANVIL_SMITH = type("anvil_smithing", AnvilSmithingRecipe.class);

    // oil wave categories
    public static final JeiRecipeType<RefineryRecipeHandler.Entry> REFINERY = type("refinery", RefineryRecipeHandler.Entry.class);
    public static final JeiRecipeType<FractioningRecipeHandler.Entry> FRACTIONING = type("fractioning", FractioningRecipeHandler.Entry.class);
    public static final JeiRecipeType<LiquefactionRecipeHandler.Entry> LIQUEFACTION = type("liquefaction", LiquefactionRecipeHandler.Entry.class);
    public static final JeiRecipeType<SolidificationRecipeHandler.Entry> SOLIDIFICATION = type("solidification", SolidificationRecipeHandler.Entry.class);
    public static final JeiRecipeType<BoilingRecipeHandler.Entry> BOILING = type("boiling", BoilingRecipeHandler.Entry.class);
    public static final JeiRecipeType<CoolingRecipeHandler.Entry> COOLING = type("cooling", CoolingRecipeHandler.Entry.class);

    // advanced oil wave categories
    public static final JeiRecipeType<VacuumRecipeHandler.Entry> VACUUM = type("vacuum_distiller", VacuumRecipeHandler.Entry.class);
    public static final JeiRecipeType<ReformingRecipeHandler.Entry> REFORMING = type("reforming", ReformingRecipeHandler.Entry.class);
    public static final JeiRecipeType<CrackingRecipeHandler.Entry> CRACKING = type("cracking", CrackingRecipeHandler.Entry.class);
    public static final JeiRecipeType<HydrotreatingRecipeHandler.Entry> HYDROTREATING = type("hydrotreating", HydrotreatingRecipeHandler.Entry.class);
    public static final JeiRecipeType<CokerRecipeHandler.Entry> COKER = type("coker", CokerRecipeHandler.Entry.class);
    public static final JeiRecipeType<PyrolysisRecipeHandler.Entry> PYROLYSIS = type("pyrolysis", PyrolysisRecipeHandler.Entry.class);
    public static final JeiRecipeType<CompressorRecipeHandler.Entry> COMPRESSOR = type("compressor", CompressorRecipeHandler.Entry.class);
}
//?}
