package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * The loader-agnostic body of the mod's JEI integration — the modern equivalent of the old
 * {@code JEIConfig}. Each loader's thin {@code @JeiPlugin} entrypoint delegates its
 * {@code registerCategories}/{@code registerRecipes}/{@code registerRecipeCatalysts} calls straight here,
 * mirroring how the port keeps shared logic in {@code common} and only the loader glue per-module
 * (like {@code HbmClient}/{@code HbmClientNeoForge}). Adding a machine is a one-liner in each of the three
 * methods below, exactly as it was in the old plugin.
 */
public final class HbmJeiPluginCommon {

    private HbmJeiPluginCommon() {}

    // Categories are created in registerCategories (needs the IGuiHelper) and reused in registerRecipes.
    private static JEIGenericRecipeHandler assembly;
    private static JEIGenericRecipeHandler chemplant;
    private static JEIGenericRecipeHandler purex;
    private static JEIGenericRecipeHandler fusion;
    private static PressRecipeHandler press;
    private static SolderingStationRecipeHandler soldering;
    private static CrystallizerRecipeHandler crystallizer;
    private static SILEXRecipeHandler silex;
    private static ShredderRecipeHandler shredder;
    private static CrucibleAlloyingHandler crucible;
    private static AnvilConstructionRecipeHandler anvilCon;
    private static AnvilSmithingRecipeHandler anvilSmith;
    private static RefineryRecipeHandler refinery;
    private static FractioningRecipeHandler fractioning;
    private static LiquefactionRecipeHandler liquefaction;
    private static SolidificationRecipeHandler solidification;
    private static BoilingRecipeHandler boiling;
    private static CoolingRecipeHandler cooling;
    private static VacuumRecipeHandler vacuum;
    private static ReformingRecipeHandler reforming;
    private static CrackingRecipeHandler cracking;
    private static HydrotreatingRecipeHandler hydrotreating;
    private static CokerRecipeHandler coker;
    private static PyrolysisRecipeHandler pyrolysis;
    private static CompressorRecipeHandler compressor;

    /** Make each fluid-icon variant a distinct JEI ingredient, keyed on its {@code FLUID_ID} component
     * (so the droplet icons don't all collapse to one "fluid_icon" in the item list). */
    public static void registerItemSubtypes(ISubtypeRegistration reg) {
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModFluidItems.fluid_icon.get(), ModDataComponents.FLUID_ID);
    }

    /** Add a droplet icon for every registered fluid to JEI's ingredient list / search — the fluid‑icon item
     * lives in no creative tab, so without this only the fluids that happen to appear in a ported recipe
     * would be searchable. (Modern equivalent of the old {@code FluidIconRecipeRegistryPlugin} seam.) */
    public static void registerExtraIngredients(IExtraIngredientRegistration reg) {
        List<ItemStack> icons = new ArrayList<>();
        for (FluidType fluid : Fluids.getAll()) {
            if (fluid == Fluids.NONE) continue;
            ItemStack icon = ItemFluidIcon.make(fluid);
            if (!icon.isEmpty()) icons.add(icon);
        }
        reg.addExtraItemStacks(icons);
    }

    public static void registerCategories(IRecipeCategoryRegistration reg) {
        IGuiHelper helper = reg.getJeiHelpers().getGuiHelper();

        assembly = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                title("assembly_machine"), AssemblyMachineRecipes.INSTANCE, stack(ModMachineBlocks.machine_assembly_machine.get()));
        chemplant = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.CHEMICAL_PLANT,
                title("chemical_plant"), ChemicalPlantRecipes.INSTANCE, stack(ModMachineBlocks.machine_chemical_plant.get()));
        purex = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.PUREX,
                title("purex"), PUREXRecipes.INSTANCE, stack(ModMachineBlocks.machine_purex.get()));
        fusion = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.FUSION,
                title("fusion"), com.hbm.inventory.recipes.FusionRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_torus.get()));
        press = new PressRecipeHandler(helper, HbmJeiRecipeTypes.PRESS, title("press"),
                stack(ModMachineBlocks.machine_press.get()));
        soldering = new SolderingStationRecipeHandler(helper, HbmJeiRecipeTypes.SOLDERING_STATION,
                title("soldering_station"), stack(ModMachineBlocks.machine_soldering_station.get()));
        crystallizer = new CrystallizerRecipeHandler(helper, HbmJeiRecipeTypes.CRYSTALLIZER,
                title("crystallizer"), stack(ModMachineBlocks.machine_crystallizer.get()));
        silex = new SILEXRecipeHandler(helper, HbmJeiRecipeTypes.SILEX,
                title("silex"), stack(ModMachineBlocks.machine_silex.get()));
        shredder = new ShredderRecipeHandler(helper, HbmJeiRecipeTypes.SHREDDER,
                title("shredder"), stack(ModMachineBlocks.machine_shredder.get()));
        crucible = new CrucibleAlloyingHandler(helper, HbmJeiRecipeTypes.CRUCIBLE_ALLOY,
                title("crucible_alloy"), stack(ModMachineBlocks.machine_crucible.get()));
        anvilCon = new AnvilConstructionRecipeHandler(helper, HbmJeiRecipeTypes.ANVIL_CON,
                title("anvil_construction"), anvilIcon());
        anvilSmith = new AnvilSmithingRecipeHandler(helper, HbmJeiRecipeTypes.ANVIL_SMITH,
                title("anvil_smithing"), anvilIcon());

        refinery = new RefineryRecipeHandler(helper, HbmJeiRecipeTypes.REFINERY,
                title("refinery"), stack(ModMachineBlocks.machine_refinery.get()));
        fractioning = new FractioningRecipeHandler(helper, HbmJeiRecipeTypes.FRACTIONING,
                title("fractioning"), stack(ModMachineBlocks.machine_fraction_tower.get()));
        liquefaction = new LiquefactionRecipeHandler(helper, HbmJeiRecipeTypes.LIQUEFACTION,
                title("liquefaction"), stack(ModMachineBlocks.machine_liquefactor.get()));
        solidification = new SolidificationRecipeHandler(helper, HbmJeiRecipeTypes.SOLIDIFICATION,
                title("solidification"), stack(ModMachineBlocks.machine_solidifier.get()));
        boiling = new BoilingRecipeHandler(helper, HbmJeiRecipeTypes.BOILING,
                title("boiling"), stack(ModMachineBlocks.machine_boiler.get()));
        cooling = new CoolingRecipeHandler(helper, HbmJeiRecipeTypes.COOLING,
                title("cooling"), stack(ModMachineBlocks.machine_turbine.get()));

        vacuum = new VacuumRecipeHandler(helper, HbmJeiRecipeTypes.VACUUM,
                title("vacuum_distiller"), stack(ModMachineBlocks.machine_vacuum_distill.get()));
        reforming = new ReformingRecipeHandler(helper, HbmJeiRecipeTypes.REFORMING,
                title("reforming"), stack(ModMachineBlocks.machine_catalytic_reformer.get()));
        cracking = new CrackingRecipeHandler(helper, HbmJeiRecipeTypes.CRACKING,
                title("cracking"), stack(ModMachineBlocks.machine_catalytic_cracker.get()));
        hydrotreating = new HydrotreatingRecipeHandler(helper, HbmJeiRecipeTypes.HYDROTREATING,
                title("hydrotreating"), stack(ModMachineBlocks.machine_hydrotreater.get()));
        coker = new CokerRecipeHandler(helper, HbmJeiRecipeTypes.COKER,
                title("coker"), stack(ModMachineBlocks.machine_coker.get()));
        pyrolysis = new PyrolysisRecipeHandler(helper, HbmJeiRecipeTypes.PYROLYSIS,
                title("pyrolysis"), stack(ModMachineBlocks.machine_pyrooven.get()));
        compressor = new CompressorRecipeHandler(helper, HbmJeiRecipeTypes.COMPRESSOR,
                title("compressor"), stack(ModMachineBlocks.machine_compressor.get()));

        reg.addRecipeCategories(assembly, chemplant, purex, fusion, press, soldering, crystallizer, silex, shredder, crucible, anvilCon, anvilSmith,
                refinery, fractioning, liquefaction, solidification, boiling, cooling,
                vacuum, reforming, cracking, hydrotreating, coker, pyrolysis, compressor);
    }

    public static void registerRecipes(IRecipeRegistration reg) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        PUREXRecipes.INSTANCE.registerRecipes();
        com.hbm.inventory.recipes.FusionRecipes.INSTANCE.registerRecipes();
        PressRecipes.register();
        SolderingRecipes.register();
        CrystallizerRecipes.register();

        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ASSEMBLY_MACHINE, assembly.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CHEMICAL_PLANT, chemplant.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PUREX, purex.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.FUSION, fusion.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PRESS, PressRecipes.getRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SOLDERING_STATION, SolderingRecipes.recipes);
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CRYSTALLIZER, CrystallizerRecipes.recipes);
        SILEXRecipes.register();
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SILEX, SILEXRecipes.recipes);
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SHREDDER, ShredderRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CRUCIBLE_ALLOY, CrucibleAlloyingHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ANVIL_CON, AnvilConstructionRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ANVIL_SMITH, AnvilSmithingRecipeHandler.buildRecipes());

        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.REFINERY, RefineryRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.FRACTIONING, FractioningRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.LIQUEFACTION, LiquefactionRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SOLIDIFICATION, SolidificationRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.BOILING, BoilingRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.COOLING, CoolingRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.VACUUM, VacuumRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.REFORMING, ReformingRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CRACKING, CrackingRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.HYDROTREATING, HydrotreatingRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.COKER, CokerRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PYROLYSIS, PyrolysisRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.COMPRESSOR, CompressorRecipeHandler.buildRecipes());
    }

    public static void registerCatalysts(IRecipeCatalystRegistration reg) {
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ASSEMBLY_MACHINE, stack(ModMachineBlocks.machine_assembly_machine.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CHEMICAL_PLANT, stack(ModMachineBlocks.machine_chemical_plant.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PUREX, stack(ModMachineBlocks.machine_purex.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.FUSION, stack(ModMachineBlocks.fusion_torus.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PRESS, stack(ModMachineBlocks.machine_press.get()), stack(ModMachineBlocks.machine_epress.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SOLDERING_STATION, stack(ModMachineBlocks.machine_soldering_station.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CRYSTALLIZER, stack(ModMachineBlocks.machine_crystallizer.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SILEX, stack(ModMachineBlocks.machine_silex.get()));
        // Each laser crystal is also a catalyst: clicking one shows what the SILEX can do with it
        // loaded (the old plugin's per-wavelength categories, folded into one).
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SILEX,
                new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.laser_crystal_co2.get()),
                new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.laser_crystal_bismuth.get()),
                new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.laser_crystal_cmb.get()),
                new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.laser_crystal_bale.get()),
                new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.laser_crystal_digamma.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SHREDDER, stack(ModMachineBlocks.machine_shredder.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CRUCIBLE_ALLOY, stack(ModMachineBlocks.machine_crucible.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ANVIL_CON, anvilIcon());
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ANVIL_SMITH, anvilIcon());

        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.REFINERY, stack(ModMachineBlocks.machine_refinery.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.FRACTIONING, stack(ModMachineBlocks.machine_fraction_tower.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.LIQUEFACTION, stack(ModMachineBlocks.machine_liquefactor.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SOLIDIFICATION, stack(ModMachineBlocks.machine_solidifier.get()));
        // Boiling: the boiler + all three heaters (firebox / electric / oilburner) can drive it.
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.VACUUM, stack(ModMachineBlocks.machine_vacuum_distill.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.REFORMING, stack(ModMachineBlocks.machine_catalytic_reformer.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CRACKING, stack(ModMachineBlocks.machine_catalytic_cracker.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.HYDROTREATING, stack(ModMachineBlocks.machine_hydrotreater.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.COKER, stack(ModMachineBlocks.machine_coker.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PYROLYSIS, stack(ModMachineBlocks.machine_pyrooven.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.COMPRESSOR, stack(ModMachineBlocks.machine_compressor.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.BOILING, stack(ModMachineBlocks.machine_boiler.get()),
                stack(ModMachineBlocks.heater_firebox.get()), stack(ModMachineBlocks.heater_electric.get()),
                stack(ModMachineBlocks.heater_oilburner.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.COOLING, stack(ModMachineBlocks.machine_turbine.get()),
                stack(ModMachineBlocks.machine_condenser.get()));

        // The electric furnace runs vanilla smelting — surface it under JEI's built-in furnace category
        // (mirrors the old JEIConfig line; no bespoke category needed).
        dev.golem.core.jei.Jei.addCraftingStation(reg, RecipeTypes.SMELTING, stack(ModMachineBlocks.machine_electric_furnace_off.get()));
    }

    /** The category title, from the {@code hbm.<path>.name} lang key (the old title-key convention). */
    static Component title(String path) {
        return Component.translatable("hbm." + path + ".name");
    }

    static ItemStack stack(Block block) {
        return new ItemStack(block);
    }

    /** Icon/catalyst for the anvil categories — the tier-1 iron anvil (the anvil has no processing TE). */
    static ItemStack anvilIcon() {
        return new ItemStack(ModAnvilBlocks.anvil_iron.get());
    }
}
//?}
