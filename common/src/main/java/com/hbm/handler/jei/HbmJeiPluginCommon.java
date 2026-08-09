package com.hbm.handler.jei;

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.PressRecipes;
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
    private static PressRecipeHandler press;
    private static SolderingStationRecipeHandler soldering;
    private static CrystallizerRecipeHandler crystallizer;
    private static ShredderRecipeHandler shredder;
    private static CrucibleAlloyingHandler crucible;
    private static AnvilConstructionRecipeHandler anvilCon;
    private static AnvilSmithingRecipeHandler anvilSmith;
    private static RefineryRecipeHandler refinery;
    private static FractioningRecipeHandler fractioning;
    private static LiquefactionRecipeHandler liquefaction;
    private static SolidificationRecipeHandler solidification;
    private static BoilingRecipeHandler boiling;
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
        reg.registerFromDataComponentTypes(ModFluidItems.fluid_icon.get(), ModDataComponents.FLUID_ID.get());
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
        press = new PressRecipeHandler(helper, HbmJeiRecipeTypes.PRESS, title("press"),
                stack(ModMachineBlocks.machine_press.get()));
        soldering = new SolderingStationRecipeHandler(helper, HbmJeiRecipeTypes.SOLDERING_STATION,
                title("soldering_station"), stack(ModMachineBlocks.machine_soldering_station.get()));
        crystallizer = new CrystallizerRecipeHandler(helper, HbmJeiRecipeTypes.CRYSTALLIZER,
                title("crystallizer"), stack(ModMachineBlocks.machine_crystallizer.get()));
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

        reg.addRecipeCategories(assembly, chemplant, press, soldering, crystallizer, shredder, crucible, anvilCon, anvilSmith,
                refinery, fractioning, liquefaction, solidification, boiling,
                vacuum, reforming, cracking, hydrotreating, coker, pyrolysis, compressor);
    }

    public static void registerRecipes(IRecipeRegistration reg) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        PressRecipes.register();
        SolderingRecipes.register();
        CrystallizerRecipes.register();

        reg.addRecipes(HbmJeiRecipeTypes.ASSEMBLY_MACHINE, assembly.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.CHEMICAL_PLANT, chemplant.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.PRESS, PressRecipes.getRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.SOLDERING_STATION, SolderingRecipes.recipes);
        reg.addRecipes(HbmJeiRecipeTypes.CRYSTALLIZER, CrystallizerRecipes.recipes);
        reg.addRecipes(HbmJeiRecipeTypes.SHREDDER, ShredderRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.CRUCIBLE_ALLOY, CrucibleAlloyingHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.ANVIL_CON, AnvilConstructionRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.ANVIL_SMITH, AnvilSmithingRecipeHandler.buildRecipes());

        reg.addRecipes(HbmJeiRecipeTypes.REFINERY, RefineryRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.FRACTIONING, FractioningRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.LIQUEFACTION, LiquefactionRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.SOLIDIFICATION, SolidificationRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.BOILING, BoilingRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.VACUUM, VacuumRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.REFORMING, ReformingRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.CRACKING, CrackingRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.HYDROTREATING, HydrotreatingRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.COKER, CokerRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.PYROLYSIS, PyrolysisRecipeHandler.buildRecipes());
        reg.addRecipes(HbmJeiRecipeTypes.COMPRESSOR, CompressorRecipeHandler.buildRecipes());
    }

    public static void registerCatalysts(IRecipeCatalystRegistration reg) {
        reg.addCraftingStation(HbmJeiRecipeTypes.ASSEMBLY_MACHINE, stack(ModMachineBlocks.machine_assembly_machine.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.CHEMICAL_PLANT, stack(ModMachineBlocks.machine_chemical_plant.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.PRESS, stack(ModMachineBlocks.machine_press.get()), stack(ModMachineBlocks.machine_epress.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.SOLDERING_STATION, stack(ModMachineBlocks.machine_soldering_station.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.CRYSTALLIZER, stack(ModMachineBlocks.machine_crystallizer.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.SHREDDER, stack(ModMachineBlocks.machine_shredder.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.CRUCIBLE_ALLOY, stack(ModMachineBlocks.machine_crucible.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.ANVIL_CON, anvilIcon());
        reg.addCraftingStation(HbmJeiRecipeTypes.ANVIL_SMITH, anvilIcon());

        reg.addCraftingStation(HbmJeiRecipeTypes.REFINERY, stack(ModMachineBlocks.machine_refinery.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.FRACTIONING, stack(ModMachineBlocks.machine_fraction_tower.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.LIQUEFACTION, stack(ModMachineBlocks.machine_liquefactor.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.SOLIDIFICATION, stack(ModMachineBlocks.machine_solidifier.get()));
        // Boiling: the boiler + all three heaters (firebox / electric / oilburner) can drive it.
        reg.addCraftingStation(HbmJeiRecipeTypes.VACUUM, stack(ModMachineBlocks.machine_vacuum_distill.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.REFORMING, stack(ModMachineBlocks.machine_catalytic_reformer.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.CRACKING, stack(ModMachineBlocks.machine_catalytic_cracker.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.HYDROTREATING, stack(ModMachineBlocks.machine_hydrotreater.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.COKER, stack(ModMachineBlocks.machine_coker.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.PYROLYSIS, stack(ModMachineBlocks.machine_pyrooven.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.COMPRESSOR, stack(ModMachineBlocks.machine_compressor.get()));
        reg.addCraftingStation(HbmJeiRecipeTypes.BOILING, stack(ModMachineBlocks.machine_boiler.get()),
                stack(ModMachineBlocks.heater_firebox.get()), stack(ModMachineBlocks.heater_electric.get()),
                stack(ModMachineBlocks.heater_oilburner.get()));

        // The electric furnace runs vanilla smelting — surface it under JEI's built-in furnace category
        // (mirrors the old JEIConfig line; no bespoke category needed).
        reg.addCraftingStation(RecipeTypes.SMELTING, stack(ModMachineBlocks.machine_electric_furnace_off.get()));
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
