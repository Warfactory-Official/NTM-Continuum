package com.hbm.handler.jei;

import com.hbm.Constants;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.resources.Identifier;

/**
 * The mod's JEI recipe-category identifiers — the modern-API equivalent of the old {@code JEIConfig}'s
 * {@code public static final String} UID block (1.12.2 JEI 4.x used bare String UIDs; JEI 29.x uses a
 * typed {@link IRecipeType}). Kept in one place so a new machine category is a one-line addition, exactly
 * like the old plugin. The path strings are preserved from the old mod (e.g. {@code "assembly_machine"}).
 *
 * <p>Two machines whose recipes share a Java class ({@link GenericRecipe}) still get distinct types
 * because {@link IRecipeType} identity is {@code (uid, class)} — a different path is a different type.
 *
 * <p>Grown one entry per machine as categories are ported (see IMPLEMENTATION.md §13p).
 */
public final class HbmJeiRecipeTypes {

    private HbmJeiRecipeTypes() {}

    static <T> IRecipeType<T> type(String path, Class<T> clazz) {
        return IRecipeType.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path), clazz);
    }

    public static final IRecipeType<GenericRecipe> ASSEMBLY_MACHINE = type("assembly_machine", GenericRecipe.class);
    public static final IRecipeType<GenericRecipe> CHEMICAL_PLANT = type("chemical_plant", GenericRecipe.class);
    public static final IRecipeType<GenericRecipe> PUREX = type("purex", GenericRecipe.class);

    public static final IRecipeType<PressRecipes.PressRecipe> PRESS = type("press", PressRecipes.PressRecipe.class);
    public static final IRecipeType<SolderingRecipes.SolderingRecipe> SOLDERING_STATION = type("soldering_station", SolderingRecipes.SolderingRecipe.class);
    public static final IRecipeType<CrystallizerRecipes.CrystallizerRecipe> CRYSTALLIZER = type("crystallizer", CrystallizerRecipes.CrystallizerRecipe.class);
    public static final IRecipeType<ShredderRecipeHandler.ShredderPair> SHREDDER = type("shredder", ShredderRecipeHandler.ShredderPair.class);
    public static final IRecipeType<com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe> SILEX = type("silex", com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe.class);

    public static final IRecipeType<CrucibleRecipes.CrucibleRecipe> CRUCIBLE_ALLOY = type("crucible_alloy", CrucibleRecipes.CrucibleRecipe.class);
    public static final IRecipeType<AnvilRecipes.AnvilConstructionRecipe> ANVIL_CON = type("anvil_construction", AnvilRecipes.AnvilConstructionRecipe.class);
    public static final IRecipeType<AnvilSmithingRecipe> ANVIL_SMITH = type("anvil_smithing", AnvilSmithingRecipe.class);

    // oil wave categories
    public static final IRecipeType<RefineryRecipeHandler.Entry> REFINERY = type("refinery", RefineryRecipeHandler.Entry.class);
    public static final IRecipeType<FractioningRecipeHandler.Entry> FRACTIONING = type("fractioning", FractioningRecipeHandler.Entry.class);
    public static final IRecipeType<LiquefactionRecipeHandler.Entry> LIQUEFACTION = type("liquefaction", LiquefactionRecipeHandler.Entry.class);
    public static final IRecipeType<SolidificationRecipeHandler.Entry> SOLIDIFICATION = type("solidification", SolidificationRecipeHandler.Entry.class);
    public static final IRecipeType<BoilingRecipeHandler.Entry> BOILING = type("boiling", BoilingRecipeHandler.Entry.class);
    public static final IRecipeType<CoolingRecipeHandler.Entry> COOLING = type("cooling", CoolingRecipeHandler.Entry.class);

    // advanced oil wave categories
    public static final IRecipeType<VacuumRecipeHandler.Entry> VACUUM = type("vacuum_distiller", VacuumRecipeHandler.Entry.class);
    public static final IRecipeType<ReformingRecipeHandler.Entry> REFORMING = type("reforming", ReformingRecipeHandler.Entry.class);
    public static final IRecipeType<CrackingRecipeHandler.Entry> CRACKING = type("cracking", CrackingRecipeHandler.Entry.class);
    public static final IRecipeType<HydrotreatingRecipeHandler.Entry> HYDROTREATING = type("hydrotreating", HydrotreatingRecipeHandler.Entry.class);
    public static final IRecipeType<CokerRecipeHandler.Entry> COKER = type("coker", CokerRecipeHandler.Entry.class);
    public static final IRecipeType<PyrolysisRecipeHandler.Entry> PYROLYSIS = type("pyrolysis", PyrolysisRecipeHandler.Entry.class);
    public static final IRecipeType<CompressorRecipeHandler.Entry> COMPRESSOR = type("compressor", CompressorRecipeHandler.Entry.class);
}
