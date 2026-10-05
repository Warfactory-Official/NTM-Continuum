package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.DiFurnaceContent;
import com.hbm.foundry.FoundryExpansion;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.container.ContainerMachineChemicalFactory;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.gui.GUIFusionBreeder;
import com.hbm.inventory.gui.GUIDiFurnace;
import com.hbm.inventory.gui.GUIMachineArcFurnaceLarge;
import com.hbm.inventory.gui.GUIMachineArcWelder;
import com.hbm.inventory.container.ContainerMachineExposureChamber;
import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.container.ContainerMachineAmmoPress;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.gui.GUICentrifuge;
import com.hbm.inventory.gui.GUIMachineCyclotron;
import com.hbm.inventory.gui.GUIMachineExposureChamber;
import com.hbm.inventory.gui.GUIMixer;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import dev.golem.core.data.ModelData;

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
    private static JEIGenericRecipeHandler precisionAssembler;
    private static JEIGenericRecipeHandler chemplant;
    private static JEIGenericRecipeHandler mixer;
    private static JEIGenericRecipeHandler exposure;
    private static JEIGenericRecipeHandler purex;
    private static JEIGenericRecipeHandler fusion;
    private static JEIGenericRecipeHandler fusionBreeder;
    private static JEIGenericRecipeHandler plasmaForge;
    private static PressRecipeHandler press;
    private static AmmoPressRecipeHandler ammoPress;
    private static SolderingStationRecipeHandler soldering;
    private static CrystallizerRecipeHandler crystallizer;
    private static CentrifugeRecipeHandler centrifuge;
    private static CyclotronRecipeHandler cyclotron;
    private static ParticleAcceleratorRecipeHandler particleAccelerator;
    private static GasCentrifugeRecipeHandler gasCentrifuge;
    private static ElectrolyserFluidRecipeHandler electrolyserFluid;
    private static ElectrolyserMetalRecipeHandler electrolyserMetal;
    private static SILEXRecipeHandler silex;
    private static ShredderRecipeHandler shredder;
    private static SawmillRecipeHandler sawmill;
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
    private static StorageDrumRecipeHandler storageDrum;
    private static FuelPoolRecipeHandler fuelPool;
    private static ArcWelderRecipeHandler arcWelder;
    private static RotaryFurnaceRecipeHandler rotaryFurnace;
    private static ArcFurnaceRecipeHandler arcFurnaceSolid;
    private static ArcFurnaceRecipeHandler arcFurnaceFluid;
    private static DiFurnaceRecipeHandler diFurnace;

    /**
     * Keep component-backed fluid containers, droplets, scraps, blueprints, and waste variants distinct.
     *
     * <p>The enum-backed families are swept from the registry rather than listed: every one of them
     * selects its variant with the same component, and a family JEI is not told about collapses to a
     * single ingredient, which is how the HIMARS pods and the artillery shells went missing.
     */
    public static void registerItemSubtypes(ISubtypeRegistration reg) {
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, com.hbm.items.ModMachineItems.blueprints.get(),
                ModDataComponents.BLUEPRINT_POOL);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModFluidItems.fluid_tank.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModFluidItems.fluid_barrel.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, com.hbm.items.ModMachineItems.fluid_identifier.get(),
                ModDataComponents.FLUID);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModFluidItems.fluid_bucket.get(),
                ModDataComponents.FLUID);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModFluidItems.fluid_icon.get(),
                ModDataComponents.FLUID, ModDataComponents.FLUID_AMOUNT, ModDataComponents.FLUID_PRESSURE);
        dev.golem.core.jei.Jei.registerSubtypeInterpreter(reg, com.hbm.items.ModMachineItems.scraps.get(), stack ->
                ModDataComponents.MATERIAL.get(stack).orElse("") + "|"
                        + ModDataComponents.MAT_AMOUNT.get(stack).map(String::valueOf).orElse("") + "|"
                        + ModelData.flag(stack) + "|" + ModelData.color(stack, -1));
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_long.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_long_tiny.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_short.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_short_tiny.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_long_depleted.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_short_depleted.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, ModStorageDrumItems.nuclear_waste_short_depleted_tiny.get(), ModDataComponents.STORAGE_WASTE_CLASS);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg,
                com.hbm.blocks.material.MaterialCompletionRegistrations.CONCRETE_COLORED_ITEM.get(),
                ModDataComponents.ITEM_VARIANT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg,
                com.hbm.blocks.material.MaterialCompletionRegistrations.CONCRETE_COLORED_EXT_ITEM.get(),
                ModDataComponents.ITEM_VARIANT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg,
                com.hbm.blocks.material.MaterialCompletionRegistrations.LIGHTSTONE_ITEM.get(),
                ModDataComponents.ITEM_VARIANT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, com.hbm.items.ModGearItems.gear_large.get(),
                ModDataComponents.ITEM_VARIANT);
        dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg,
                com.hbm.blocks.ModSteelBlocks.steel_scaffold.get().asItem(), ModDataComponents.ITEM_VARIANT);
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.hbm.items.ItemEnumMulti) {
                dev.golem.core.jei.Jei.registerFromDataComponentTypes(reg, item, ModDataComponents.ITEM_VARIANT);
            }
        }
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
        precisionAssembler = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.PRECISION_ASSEMBLER,
                title("precass"), PrecAssRecipes.INSTANCE, stack(ModMachineBlocks.machine_precass.get()));
        chemplant = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.CHEMICAL_PLANT,
                title("chemical_plant"), ChemicalPlantRecipes.INSTANCE, stack(ModMachineBlocks.machine_chemical_plant.get()));
        mixer = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.MIXER,
                title("mixer"), MixerRecipes.INSTANCE, stack(ModMachineBlocks.machine_mixer.get()));
        exposure = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                title("exposure_chamber"), ExposureChamberRecipes.INSTANCE,
                stack(ModMachineBlocks.machine_exposure_chamber.get()));
        purex = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.PUREX,
                title("purex"), PUREXRecipes.INSTANCE, stack(ModMachineBlocks.machine_purex.get()));
        fusion = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.FUSION,
                title("fusion"), com.hbm.inventory.recipes.FusionRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_torus.get()));
        fusionBreeder = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.FUSION_BREEDER,
                title("fusionbreeder"), FluidBreederRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_breeder.get()));
        plasmaForge = new JEIGenericRecipeHandler(helper, HbmJeiRecipeTypes.PLASMA_FORGE,
                title("plasma_forge"), PlasmaForgeRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_plasma_forge.get()));
         press = new PressRecipeHandler(helper, HbmJeiRecipeTypes.PRESS, title("press"),
                 stack(ModMachineBlocks.machine_press.get()));
         ammoPress = new AmmoPressRecipeHandler(helper, HbmJeiRecipeTypes.AMMO_PRESS, title("ammo_press"),
                 stack(ModMachineBlocks.machine_ammo_press.get()));
        soldering = new SolderingStationRecipeHandler(helper, HbmJeiRecipeTypes.SOLDERING_STATION,
                title("soldering_station"), stack(ModMachineBlocks.machine_soldering_station.get()));
        crystallizer = new CrystallizerRecipeHandler(helper, HbmJeiRecipeTypes.CRYSTALLIZER,
                title("crystallizer"), stack(ModMachineBlocks.machine_crystallizer.get()));
        centrifuge = new CentrifugeRecipeHandler(helper, HbmJeiRecipeTypes.CENTRIFUGE,
                title("centrifuge"), stack(ModMachineBlocks.machine_centrifuge.get()));
        cyclotron = new CyclotronRecipeHandler(helper, HbmJeiRecipeTypes.CYCLOTRON,
                title("cyclotron"), stack(ModMachineBlocks.machine_cyclotron.get()));
        particleAccelerator = new ParticleAcceleratorRecipeHandler(helper, HbmJeiRecipeTypes.PARTICLE_ACCELERATOR,
                title("particle_accelerator"), stack(ModMachineBlocks.pa_detector.get()));
        gasCentrifuge = new GasCentrifugeRecipeHandler(helper, HbmJeiRecipeTypes.GAS_CENTRIFUGE,
                title("gas_centrifuge"), stack(ModMachineBlocks.machine_gascent.get()));
        electrolyserFluid = new ElectrolyserFluidRecipeHandler(helper, HbmJeiRecipeTypes.ELECTROLYSIS_FLUID,
                title("electrolysis_fluid"), stack(ModMachineBlocks.machine_electrolyser.get()));
        electrolyserMetal = new ElectrolyserMetalRecipeHandler(helper, HbmJeiRecipeTypes.ELECTROLYSIS_METAL,
                title("electrolysis_metal"), stack(ModMachineBlocks.machine_electrolyser.get()));
        silex = new SILEXRecipeHandler(helper, HbmJeiRecipeTypes.SILEX,
                title("silex"), stack(ModMachineBlocks.machine_silex.get()));
        shredder = new ShredderRecipeHandler(helper, HbmJeiRecipeTypes.SHREDDER,
                title("shredder"), stack(ModMachineBlocks.machine_shredder.get()));
        sawmill = new SawmillRecipeHandler(helper, HbmJeiRecipeTypes.SAWMILL,
                title("sawmill"), stack(ModMachineBlocks.machine_sawmill.get()));
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
        storageDrum = new StorageDrumRecipeHandler(helper, HbmJeiRecipeTypes.STORAGE_DRUM,
                Component.translatable("container.storageDrum"));
        fuelPool = new FuelPoolRecipeHandler(helper, HbmJeiRecipeTypes.FUEL_POOL,
                Component.translatable("block.hbm.machine_waste_drum"), stack(ModMachineBlocks.machine_waste_drum.get()));
        arcWelder = new ArcWelderRecipeHandler(helper, HbmJeiRecipeTypes.ARC_WELDER,
                Component.translatable("block.hbm.machine_arc_welder"),
                stack(ModMachineBlocks.machine_arc_welder.get()));
        arcFurnaceSolid = new ArcFurnaceRecipeHandler(helper, HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                Component.translatable("block.hbm.machine_arc_furnace"), false,
                stack(FoundryExpansion.ARC_FURNACE.get()));
        arcFurnaceFluid = new ArcFurnaceRecipeHandler(helper, HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                Component.translatable("block.hbm.machine_arc_furnace"), true,
                stack(FoundryExpansion.ARC_FURNACE.get()));
        diFurnace = new DiFurnaceRecipeHandler(helper, HbmJeiRecipeTypes.ALLOY,
                Component.translatable("block.hbm.machine_difurnace_off"));
        rotaryFurnace = new RotaryFurnaceRecipeHandler(helper, HbmJeiRecipeTypes.ROTARY_FURNACE,
                title("rotary_furnace"), stack(com.hbm.blocks.ModProductionBlocks.MACHINE_ROTARY_FURNACE.get()));

          reg.addRecipeCategories(assembly, precisionAssembler, chemplant, mixer, exposure, purex, fusion, fusionBreeder, plasmaForge, press, ammoPress, soldering, crystallizer, centrifuge, cyclotron, particleAccelerator, gasCentrifuge, silex, shredder, sawmill, crucible, anvilCon, anvilSmith,
                 electrolyserFluid, electrolyserMetal, refinery, fractioning, liquefaction, solidification, boiling, cooling,
                 vacuum, reforming, cracking, hydrotreating, coker, pyrolysis, compressor, storageDrum, fuelPool,
                 arcWelder, arcFurnaceSolid, arcFurnaceFluid, diFurnace, rotaryFurnace);
        RTGJeiIntegration.registerCategory(reg);
        SatelliteRecipeHandler.registerCategory(reg);
        com.hbm.handler.jei.NuclearMiscJeiIntegration.registerCategories(reg);
        com.hbm.handler.jei.CustomMachineJeiIntegration.registerCategories(reg);
    }

    public static void registerRecipes(IRecipeRegistration reg) {
         AssemblyMachineRecipes.INSTANCE.registerRecipes();
         PrecAssRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        MixerRecipes.INSTANCE.registerRecipes();
        ExposureChamberRecipes.INSTANCE.registerRecipes();
        PUREXRecipes.INSTANCE.registerRecipes();
        com.hbm.inventory.recipes.FusionRecipes.INSTANCE.registerRecipes();
        FluidBreederRecipes.INSTANCE.registerRecipes();
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
         PressRecipes.register();
         AmmoPressRecipes.register();
        SolderingRecipes.register();
        CrystallizerRecipes.register();
        CentrifugeRecipes.register();
        CyclotronRecipes.register();
        GasCentrifugeRecipes.register();
        ElectrolyserFluidRecipes.register();
        ElectrolyserMetalRecipes.register();

         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ASSEMBLY_MACHINE, assembly.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PRECISION_ASSEMBLER, precisionAssembler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CHEMICAL_PLANT, chemplant.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.MIXER, mixer.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.EXPOSURE_CHAMBER, exposure.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PUREX, purex.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.FUSION, fusion.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.FUSION_BREEDER, fusionBreeder.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PLASMA_FORGE, plasmaForge.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PRESS, PressRecipes.getRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.AMMO_PRESS, AmmoPressRecipes.getRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SOLDERING_STATION, SolderingRecipes.recipes);
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CRYSTALLIZER, CrystallizerRecipes.recipes);
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CENTRIFUGE, CentrifugeRecipes.recipes);
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.CYCLOTRON, CyclotronRecipes.recipes);
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.PARTICLE_ACCELERATOR,
                 ParticleAcceleratorRecipeHandler.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.GAS_CENTRIFUGE, GasCentrifugeRecipes.RECIPES);
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, electrolyserFluid.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ELECTROLYSIS_METAL, electrolyserMetal.buildRecipes());
        SILEXRecipes.register();
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SILEX, SILEXRecipes.recipes);
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SHREDDER, ShredderRecipeHandler.buildRecipes());
        dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.SAWMILL, SawmillRecipeHandler.buildRecipes());
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
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.STORAGE_DRUM, StorageDrumRecipeHandler.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.FUEL_POOL, FuelPoolRecipeHandler.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ARC_WELDER, ArcWelderRecipeHandler.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ROTARY_FURNACE, RotaryFurnaceRecipeHandler.buildRecipes());
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 ArcFurnaceRecipeHandler.buildRecipes(false));
         dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 ArcFurnaceRecipeHandler.buildRecipes(true));
          dev.golem.core.jei.Jei.addRecipes(reg, HbmJeiRecipeTypes.ALLOY, DiFurnaceRecipeHandler.buildRecipes());
        RTGJeiIntegration.registerRecipes(reg);
        SatelliteRecipeHandler.registerRecipes(reg);
        com.hbm.handler.jei.NuclearMiscJeiIntegration.registerRecipes(reg);
        com.hbm.handler.jei.CustomMachineJeiIntegration.registerRecipes(reg);
      }

    public static void registerGuiHandlers(IGuiHandlerRegistration reg) {
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMixer.class, 62, 36, 52, 44,
                HbmJeiRecipeTypes.MIXER);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMachineExposureChamber.class, 36, 40, 40, 8,
                HbmJeiRecipeTypes.EXPOSURE_CHAMBER);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUICentrifuge.class, 35, 9, 106, 40,
                HbmJeiRecipeTypes.CENTRIFUGE);
        // 1.7.10's NEI rect (43, 16, 34 x 34): the earlier 134 x 54 area covered all nine slots.
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMachineCyclotron.class, 43, 16, 34, 34,
                HbmJeiRecipeTypes.CYCLOTRON);
        // The arrow between the two slot rows, as in 1.12.2 (75, 35, 7 x 8): a 36 x 45 area here covered all
        // four slots, and JEI took every click on them, so nothing could be put into the source by hand.
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIPADetector.class, 75, 35, 7, 8,
                HbmJeiRecipeTypes.PARTICLE_ACCELERATOR);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIPASource.class, 75, 35, 7, 8,
                HbmJeiRecipeTypes.PARTICLE_ACCELERATOR);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIMachineGasCent.class, 70, 36, 36, 12,
                HbmJeiRecipeTypes.GAS_CENTRIFUGE);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIMachineElectrolyser.class, 62, 26, 12, 40,
                HbmJeiRecipeTypes.ELECTROLYSIS_FLUID);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIMachineElectrolyser.class, 7, 46, 22, 25,
                HbmJeiRecipeTypes.ELECTROLYSIS_METAL);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIFusionBreeder.class, 67, 49, 42, 9,
                HbmJeiRecipeTypes.FUSION_BREEDER);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMachineArcWelder.class, 72, 38, 32, 13,
                HbmJeiRecipeTypes.ARC_WELDER);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, com.hbm.inventory.gui.GUIMachineRotaryFurnace.class, 58, 19, 32, 10,
                HbmJeiRecipeTypes.ROTARY_FURNACE);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMachineArcFurnaceLarge.class, 17, 36, 7, 70,
                HbmJeiRecipeTypes.ARC_FURNACE_SOLID);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIMachineArcFurnaceLarge.class, 152, 36, 16, 70,
                HbmJeiRecipeTypes.ARC_FURNACE_FLUID);
        dev.golem.core.jei.Jei.addRecipeClickArea(reg, GUIDiFurnace.class, 102, 36, 21, 14,
                HbmJeiRecipeTypes.ALLOY);
        RTGJeiIntegration.registerGuiHandler(reg);
    }

    public static void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
         dev.golem.core.jei.Jei.addRecipeTransferHandler(reg, ContainerMachineAmmoPress.class,
                 ModMenus.MACHINE_AMMO_PRESS.get(), HbmJeiRecipeTypes.AMMO_PRESS, 0, 9, 10, 36);
         dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMixer.class,
                ModMenus.MACHINE_MIXER.get(), HbmJeiRecipeTypes.MIXER, new int[]{1}, 5, 36,
                recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
        dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachineExposureChamber.class,
                ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                new int[]{0, 2}, 7, 36, recipe -> true);
        dev.golem.core.jei.Jei.addRecipeTransferHandler(reg, ContainerCentrifuge.class,
                ModMenus.MACHINE_CENTRIFUGE.get(), HbmJeiRecipeTypes.CENTRIFUGE,
                0, 1, ContainerCentrifuge.MACHINE_SLOTS, 36);
         dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachineCyclotron.class,
                 ModMenus.MACHINE_CYCLOTRON.get(), HbmJeiRecipeTypes.CYCLOTRON,
                 new int[]{0, 3}, 12, 36, recipe -> true);
         dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachinePrecAss.class,
                 ModMenus.MACHINE_PREC_ASS.get(), HbmJeiRecipeTypes.PRECISION_ASSEMBLER,
                 new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12}, 22, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
         dev.golem.core.jei.Jei.addRecipeTransferHandler(reg, com.hbm.inventory.container.ContainerMachineElectrolyser.class,
                 ModMenus.MACHINE_ELECTROLYSER_METAL.get(), HbmJeiRecipeTypes.ELECTROLYSIS_METAL,
                 3, 1, 10, 36);
         dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachineArcWelder.class,
                 ModMenus.MACHINE_ARC_WELDER.get(), HbmJeiRecipeTypes.ARC_WELDER,
                 new int[]{0, 1, 2}, 8, 36, recipe -> recipe.fluid == null);
        dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachineAssemblyFactory.class,
                 com.hbm.assemblyfactory.AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY_MENU.get(),
                 HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                 new int[]{5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, 60, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
        dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerMachineChemicalFactory.class,
                 com.hbm.chemicalfactory.ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_MENU.get(),
                 HbmJeiRecipeTypes.CHEMICAL_PLANT, new int[]{5, 6, 7}, 32, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
         dev.golem.core.jei.Jei.addRecipeTransferHandler(reg, ContainerMachineArcFurnaceLarge.class,
                 FoundryExpansion.ARC_FURNACE_MENU.get(), HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 5, 1, 30, 36);
         dev.golem.core.jei.Jei.addRecipeTransferHandler(reg, ContainerMachineArcFurnaceLarge.class,
                 FoundryExpansion.ARC_FURNACE_MENU.get(), HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 5, 1, 30, 36);
          dev.golem.core.jei.Jei.addSelectiveRecipeTransferHandler(reg, ContainerDiFurnace.class,
                  DiFurnaceContent.DI_FURNACE_MENU.get(), HbmJeiRecipeTypes.ALLOY,
                  new int[]{0, 1}, 4, 36, recipe -> true);
        RTGJeiIntegration.registerTransfer(reg);
     }

    public static void registerCatalysts(IRecipeCatalystRegistration reg) {
        RTGJeiIntegration.registerCatalyst(reg);
        SatelliteRecipeHandler.registerCatalyst(reg);
        com.hbm.handler.jei.NuclearMiscJeiIntegration.registerCatalysts(reg);
        com.hbm.handler.jei.CustomMachineJeiIntegration.registerCatalysts(reg);
          dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ASSEMBLY_MACHINE, stack(ModMachineBlocks.machine_assembly_machine.get()));
          dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                  stack(com.hbm.assemblyfactory.AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY.get()));
          dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PRECISION_ASSEMBLER, stack(ModMachineBlocks.machine_precass.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CHEMICAL_PLANT, stack(ModMachineBlocks.machine_chemical_plant.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CHEMICAL_PLANT,
                 stack(com.hbm.chemicalfactory.ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.MIXER, stack(ModMachineBlocks.machine_mixer.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PUREX, stack(ModMachineBlocks.machine_purex.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.FUSION, stack(ModMachineBlocks.fusion_torus.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.FUSION_BREEDER,
                stack(ModMachineBlocks.fusion_breeder.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PLASMA_FORGE, stack(ModMachineBlocks.fusion_plasma_forge.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PRESS, stack(ModMachineBlocks.machine_press.get()), stack(ModMachineBlocks.machine_epress.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.AMMO_PRESS,
                 stack(ModMachineBlocks.machine_ammo_press.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SOLDERING_STATION, stack(ModMachineBlocks.machine_soldering_station.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CRYSTALLIZER, stack(ModMachineBlocks.machine_crystallizer.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CENTRIFUGE, stack(ModMachineBlocks.machine_centrifuge.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CYCLOTRON, stack(ModMachineBlocks.machine_cyclotron.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.PARTICLE_ACCELERATOR, stack(ModMachineBlocks.pa_detector.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.GAS_CENTRIFUGE, stack(ModMachineBlocks.machine_gascent.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, stack(ModMachineBlocks.machine_electrolyser.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ELECTROLYSIS_METAL, stack(ModMachineBlocks.machine_electrolyser.get()));
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                stack(ModMachineBlocks.machine_exposure_chamber.get()));
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
        dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.SAWMILL, stack(ModMachineBlocks.machine_sawmill.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.CRUCIBLE_ALLOY, stack(ModMachineBlocks.machine_crucible.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ARC_WELDER,
                 stack(ModMachineBlocks.machine_arc_welder.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ROTARY_FURNACE,
                 stack(com.hbm.blocks.ModProductionBlocks.MACHINE_ROTARY_FURNACE.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 stack(FoundryExpansion.ARC_FURNACE.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 stack(FoundryExpansion.ARC_FURNACE.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.ALLOY,
                 stack(DiFurnaceContent.MACHINE_DIFURNACE.get()));
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
                  stack(ModMachineBlocks.machine_condenser.get()), stack(ModMachineBlocks.machine_condenser_powered.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.STORAGE_DRUM,
                 stack(ModMachineBlocks.machine_storage_drum.get()));
         dev.golem.core.jei.Jei.addCraftingStation(reg, HbmJeiRecipeTypes.FUEL_POOL,
                 stack(ModMachineBlocks.machine_waste_drum.get()));

        // The electric furnace runs vanilla smelting — surface it under JEI's built-in furnace category
        // (mirrors the old JEIConfig line; no bespoke category needed).
        dev.golem.core.jei.Jei.addCraftingStation(reg, RecipeTypes.SMELTING, stack(ModMachineBlocks.machine_electric_furnace_off.get()));
    }

    /**
     * Runs once JEI's runtime exists: 1.7 {@code ArcFurnaceRecipes.registerFurnaceSmeltables}, the arc furnace's
     * furnace-smelting pass, listed in the solid arc furnace page. The furnace recipes are only readable from JEI's own
     * lookup on a 26.1 client, so the pass is added here rather than at registration (History §19ek-hooks).
     */
    public static void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        java.util.List<com.hbm.foundry.ArcFurnaceRecipes.ArcFurnaceRecipe> pass = com.hbm.foundry.ArcFurnaceRecipes
                .furnacePass(dev.golem.core.jei.Jei.smeltingRecipes(runtime));
        dev.golem.core.jei.Jei.addRecipes(runtime, HbmJeiRecipeTypes.ARC_FURNACE_SOLID, pass);
        com.hbm.Constants.LOG.info("[JEI] arc furnace furnace pass: {} recipe(s)", pass.size());
        // HBM fluids as JEI fluid ingredients (History section 19ek-fluids): JEI lists every registered source fluid
        // itself; this only reads the list back, for the log and the harness.
        java.util.List<String> fluids = hbmFluidIngredients(runtime);
        com.hbm.client.ForeignFluidHarnessQueries.jeiFluids = () -> hbmFluidIngredients(runtime);
        com.hbm.Constants.LOG.info("[JEI] {} HBM fluid(s) listed as fluid ingredients", fluids.size());
    }

    /** The display names of the JEI fluid ingredients that are HBM fluids. */
    private static java.util.List<String> hbmFluidIngredients(mezz.jei.api.runtime.IJeiRuntime runtime) {
        return hbmFluidIngredients(runtime, runtime.getJeiHelpers().getPlatformFluidHelper());
    }

    private static <T> java.util.List<String> hbmFluidIngredients(mezz.jei.api.runtime.IJeiRuntime runtime,
            mezz.jei.api.helpers.IPlatformFluidHelper<T> fluids) {
        mezz.jei.api.ingredients.IIngredientTypeWithSubtypes<net.minecraft.world.level.material.Fluid, T> type =
                fluids.getFluidIngredientType();
        mezz.jei.api.ingredients.IIngredientHelper<T> helper = runtime.getIngredientManager().getIngredientHelper(type);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (T ingredient : runtime.getIngredientManager().getAllIngredients(type)) {
            if (dev.golem.core.fluid.TokenFluids.isToken(type.getBase(ingredient))) names.add(helper.getDisplayName(ingredient));
        }
        return names;
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
