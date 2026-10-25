package com.hbm.handler.jei;

import com.hbm.Constants;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.resources.Identifier;

/**
 * The mod's recipe-viewer category identifiers, the modern-API equivalent of the old {@code JEIConfig}'s
 * {@code public static final String} UID block (1.12.2 JEI 4.x used bare String UIDs; here a Golem Core
 * {@link ViewerType}, which JEI's adapter turns into a JEI recipe type and REI's into a category id).
 * Kept in one place so a new machine category is a one-line addition. The path
 * strings are the old mod's (e.g. {@code "assembly_machine"}).
 *
 * <p>Two machines whose recipes share a Java class ({@link GenericRecipe}) still get distinct types
 * because {@link ViewerType} identity is {@code (uid, class)}, so a different path is a different type.
 *
 * <p>Grown one entry per machine as categories are ported.
 */
public final class HbmJeiRecipeTypes {

    private HbmJeiRecipeTypes() {}

    static <T> ViewerType<T> type(String path, Class<T> clazz) {
        return ViewerType.create(Constants.MOD_ID, path, clazz);
    }

    public static final ViewerType<GenericRecipe> ASSEMBLY_MACHINE = type("assembly_machine", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> ALLOY = type("alloy", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> PRECISION_ASSEMBLER = type("precass", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> CHEMICAL_PLANT = type("chemical_plant", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> PUREX = type("purex", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> ROCK_MILL = type("rock_mill", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> SUPERCOMPUTER = type("supercomputer", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> FUSION = type("fusion", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> FUSION_BREEDER = type("fusionbreeder", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> PLASMA_FORGE = type("plasma_forge", GenericRecipe.class);
    /** 1.7 {@code SpaceAssemblerHandler} ("ntmSpaceAssembler"). */
    public static final ViewerType<GenericRecipe> SPACE_ASSEMBLER = type("space_assembler", GenericRecipe.class);
    /** 1.7 {@code BlastFurnaceHandler} ("ntmBlastFurnace"). */
    public static final ViewerType<GenericRecipe> BLAST_FURNACE = type("blast_furnace", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> MIXER = type("mixer", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> EXPOSURE_CHAMBER = type("exposure_chamber", GenericRecipe.class);
    public static final ViewerType<ArcFurnaceRecipes.ArcFurnaceRecipe> ARC_FURNACE_SOLID =
            type("arc_furnace_solid", ArcFurnaceRecipes.ArcFurnaceRecipe.class);
    public static final ViewerType<ArcFurnaceRecipes.ArcFurnaceRecipe> ARC_FURNACE_FLUID =
            type("arc_furnace_fluid", ArcFurnaceRecipes.ArcFurnaceRecipe.class);
    public static final ViewerType<ArcWelderRecipes.ArcWelderRecipe> ARC_WELDER =
            type("arc_welder", ArcWelderRecipes.ArcWelderRecipe.class);
    public static final ViewerType<RotaryFurnaceRecipes.RotaryFurnaceRecipe> ROTARY_FURNACE =
            type("rotary_furnace", RotaryFurnaceRecipes.RotaryFurnaceRecipe.class);
    public static final ViewerType<StorageDrumRecipeHandler.Entry> STORAGE_DRUM =
            type("storage_drum", StorageDrumRecipeHandler.Entry.class);
    /** 1.7 {@code FuelPoolHandler} ("ntmSpentDrum"): the spent fuel pool drum. */
    public static final ViewerType<FuelPoolRecipes.Entry> FUEL_POOL =
            type("fuel_pool", FuelPoolRecipes.Entry.class);

    public static final ViewerType<PressRecipes.PressRecipe> PRESS = type("press", PressRecipes.PressRecipe.class);
    public static final ViewerType<AmmoPressRecipes.AmmoPressRecipe> AMMO_PRESS =
            type("ammo_press", AmmoPressRecipes.AmmoPressRecipe.class);
    public static final ViewerType<SolderingRecipes.SolderingRecipe> SOLDERING_STATION = type("soldering_station", SolderingRecipes.SolderingRecipe.class);
    public static final ViewerType<CrystallizerRecipes.CrystallizerRecipe> CRYSTALLIZER = type("crystallizer", CrystallizerRecipes.CrystallizerRecipe.class);
    public static final ViewerType<CentrifugeRecipes.CentrifugeRecipe> CENTRIFUGE = type("centrifuge", CentrifugeRecipes.CentrifugeRecipe.class);
    public static final ViewerType<CyclotronRecipes.CyclotronRecipe> CYCLOTRON =
            type("cyclotron", CyclotronRecipes.CyclotronRecipe.class);
    public static final ViewerType<ParticleAcceleratorRecipes.ParticleAcceleratorRecipe>
            PARTICLE_ACCELERATOR = type("particle_accelerator",
            ParticleAcceleratorRecipes.ParticleAcceleratorRecipe.class);
    public static final ViewerType<GasCentrifugeRecipes.GasCentrifugeRecipe> GAS_CENTRIFUGE =
            type("gas_centrifuge", GasCentrifugeRecipes.GasCentrifugeRecipe.class);
    public static final ViewerType<ElectrolyserFluidRecipes.ElectrolysisRecipe> ELECTROLYSIS_FLUID =
            type("electrolysis_fluid", ElectrolyserFluidRecipes.ElectrolysisRecipe.class);
    public static final ViewerType<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> ELECTROLYSIS_METAL =
            type("electrolysis_metal", ElectrolyserMetalRecipes.ElectrolysisMetalRecipe.class);
    public static final ViewerType<ShredderRecipeHandler.ShredderPair> SHREDDER = type("shredder", ShredderRecipeHandler.ShredderPair.class);
    public static final ViewerType<SawmillRecipes.SawmillRecipe> SAWMILL =
            type("sawmill", SawmillRecipes.SawmillRecipe.class);
    public static final ViewerType<SILEXRecipes.SILEXRecipe> SILEX = type("silex", SILEXRecipes.SILEXRecipe.class);
    public static final ViewerType<FluidContainerRegistry.Entry> FLUID_CONTAINERS =
            type("fluid_containers", FluidContainerRegistry.Entry.class);
    public static final ViewerType<ConstructionRecipeHandler.Row> CONSTRUCTION =
            type("construction", ConstructionRecipeHandler.Row.class);
    public static final ViewerType<ConstructionRecipeHandler.Row> TOOLING = type("tooling", ConstructionRecipeHandler.Row.class);
    public static final ViewerType<ConstructionRecipeHandler.Row> ASHPIT = type("ashpit", ConstructionRecipeHandler.Row.class);
    public static final ViewerType<CrucibleSmeltingHandler.Smelting> CRUCIBLE_SMELTING =
            type("crucible_smelting", CrucibleSmeltingHandler.Smelting.class);
    public static final ViewerType<CrucibleCastingHandler.Casting> CRUCIBLE_CASTING =
            type("crucible_casting", CrucibleCastingHandler.Casting.class);
    public static final ViewerType<MagicRecipes.MagicRecipe> BOOK = type("book_of_boxcars", MagicRecipes.MagicRecipe.class);

    public static final ViewerType<CrucibleRecipes.CrucibleRecipe> CRUCIBLE_ALLOY = type("crucible_alloy", CrucibleRecipes.CrucibleRecipe.class);
    public static final ViewerType<AnvilRecipes.AnvilConstructionRecipe> ANVIL_CON = type("anvil_construction", AnvilRecipes.AnvilConstructionRecipe.class);
    public static final ViewerType<AnvilSmithingRecipe> ANVIL_SMITH = type("anvil_smithing", AnvilSmithingRecipe.class);

    // Oil categories
    public static final ViewerType<RefineryRecipeHandler.Entry> REFINERY = type("refinery", RefineryRecipeHandler.Entry.class);
    public static final ViewerType<FractioningRecipeHandler.Entry> FRACTIONING = type("fractioning", FractioningRecipeHandler.Entry.class);
    public static final ViewerType<LiquefactionRecipeHandler.Entry> LIQUEFACTION = type("liquefaction", LiquefactionRecipeHandler.Entry.class);
    public static final ViewerType<SolidificationRecipeHandler.Entry> SOLIDIFICATION = type("solidification", SolidificationRecipeHandler.Entry.class);
    public static final ViewerType<BoilingRecipeHandler.Entry> BOILING = type("boiling", BoilingRecipeHandler.Entry.class);
    public static final ViewerType<CoolingRecipeHandler.Entry> COOLING = type("cooling", CoolingRecipeHandler.Entry.class);

    // Advanced oil categories
    public static final ViewerType<VacuumRecipeHandler.Entry> VACUUM = type("vacuum_distiller", VacuumRecipeHandler.Entry.class);
    public static final ViewerType<ReformingRecipeHandler.Entry> REFORMING = type("reforming", ReformingRecipeHandler.Entry.class);
    public static final ViewerType<CrackingRecipeHandler.Entry> CRACKING = type("cracking", CrackingRecipeHandler.Entry.class);
    public static final ViewerType<HydrotreatingRecipeHandler.Entry> HYDROTREATING = type("hydrotreating", HydrotreatingRecipeHandler.Entry.class);
    public static final ViewerType<CokerRecipeHandler.Entry> COKER = type("coker", CokerRecipeHandler.Entry.class);
    public static final ViewerType<PyrolysisRecipeHandler.Entry> PYROLYSIS = type("pyrolysis", PyrolysisRecipeHandler.Entry.class);
    public static final ViewerType<CombinationRecipes.CombinationRecipe> COMBINATION =
            type("combination", CombinationRecipes.CombinationRecipe.class);
    public static final ViewerType<CompressorRecipeHandler.Entry> COMPRESSOR = type("compressor", CompressorRecipeHandler.Entry.class);
}
