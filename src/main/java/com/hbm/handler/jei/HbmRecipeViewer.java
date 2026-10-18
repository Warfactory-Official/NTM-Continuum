package com.hbm.handler.jei;

import com.hbm.Constants;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.container.ContainerMachineChemicalFactory;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.container.ContainerMachineElectrolyser;
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
import com.hbm.inventory.gui.GUIMachineElectrolyser;
import com.hbm.inventory.gui.GUIMachineExposureChamber;
import com.hbm.inventory.gui.GUIMachineGasCent;
import com.hbm.inventory.gui.GUIMachineRotaryFurnace;
import com.hbm.inventory.gui.GUIMixer;
import com.hbm.inventory.gui.GUISILEX;
import com.hbm.inventory.gui.GUIBook;
import com.hbm.inventory.gui.GUIPADetector;
import com.hbm.inventory.gui.GUIPASource;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFluidIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import dev.golem.core.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.ModSatelliteBlocks;
import dev.golem.core.viewer.ViewerPlugin;

/**
 * HBM's recipe-viewer integration, declared once for JEI and REI: the modern equivalent of the old {@code JEIConfig}.
 * The JEI plugins ({@link HbmJeiPluginForgelike}, {@link HbmJeiPluginFabric}) hand it to Golem Core's
 * {@code JeiViewer}, the REI plugins ({@code com.hbm.handler.rei}) to Golem Core's {@code ReiClientPlugin}; the item
 * subtypes are {@link HbmViewerSubtypes}. Adding a machine is a one-liner in each of the methods below, exactly as it
 * was in the old plugin.
 */
public final class HbmRecipeViewer implements ViewerPlugin {

    public static final HbmRecipeViewer INSTANCE = new HbmRecipeViewer();

    private HbmRecipeViewer() {}

    // Categories are created in categories() and reused in recipes() (each viewer calls both, in that order).
    private static JEIGenericRecipeHandler assembly;
    private static JEIGenericRecipeHandler precisionAssembler;
    private static JEIGenericRecipeHandler chemplant;
    private static JEIGenericRecipeHandler mixer;
    private static JEIGenericRecipeHandler exposure;
    private static JEIGenericRecipeHandler purex;
    private static JEIGenericRecipeHandler rockMill;
    private static JEIGenericRecipeHandler superComputer;
    private static JEIGenericRecipeHandler fusion;
    private static JEIGenericRecipeHandler fusionBreeder;
    private static JEIGenericRecipeHandler plasmaForge;
    private static JEIGenericRecipeHandler spaceAssembler;
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
    private static FluidContainerRecipeHandler fluidContainers;
    private static ConstructionRecipeHandler construction;
    private static ToolingRecipeHandler tooling;
    private static AshpitRecipeHandler ashpit;
    private static CrucibleSmeltingHandler crucibleSmelting;
    private static CrucibleCastingHandler crucibleCasting;
    private static BookRecipeHandler book;

    /** Add a droplet icon for every registered fluid to the viewers' ingredient lists / search: the fluid‑icon item
     * lives in no creative tab, so without this only the fluids that happen to appear in a ported recipe
     * would be searchable. (Modern equivalent of the old {@code FluidIconRecipeRegistryPlugin} seam.) */
    @Override
    public void ingredients(Ingredients reg) {
        List<ItemStack> icons = new ArrayList<>();
        for (FluidType fluid : Fluids.getInNiceOrder()) { // 1.7 ItemFluidIcon.getSubItems
            if (fluid == Fluids.NONE) continue;
            ItemStack icon = ItemFluidIcon.make(fluid);
            if (!icon.isEmpty()) icons.add(icon);
        }
        reg.addItemStacks(icons);
    }

    @Override
    public void categories(Categories reg) {
        assembly = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                title("assembly_machine"), AssemblyMachineRecipes.INSTANCE, stack(ModMachineBlocks.machine_assembly_machine.get()));
        precisionAssembler = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.PRECISION_ASSEMBLER,
                title("precass"), PrecAssRecipes.INSTANCE, stack(ModMachineBlocks.machine_precass.get()));
        chemplant = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.CHEMICAL_PLANT,
                title("chemical_plant"), ChemicalPlantRecipes.INSTANCE, stack(ModMachineBlocks.machine_chemical_plant.get()));
        mixer = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.MIXER,
                title("mixer"), MixerRecipes.INSTANCE, stack(ModMachineBlocks.machine_mixer.get()));
        exposure = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                title("exposure_chamber"), ExposureChamberRecipes.INSTANCE,
                stack(ModMachineBlocks.machine_exposure_chamber.get()));
        purex = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.PUREX,
                title("purex"), PUREXRecipes.INSTANCE, stack(ModMachineBlocks.machine_purex.get()));
        rockMill = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.ROCK_MILL,
                Component.translatable("block.hbm.machine_rockmill"), RockMillRecipes.INSTANCE, stack(ModMachineBlocks.machine_rockmill.get()));
        superComputer = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.SUPERCOMPUTER,
                Component.translatable("block.hbm.machine_supercomputer"), SuperComputerRecipes.INSTANCE, stack(ModMachineBlocks.machine_supercomputer.get()));
        fusion = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.FUSION,
                title("fusion"), FusionRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_torus.get()));
        fusionBreeder = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.FUSION_BREEDER,
                title("fusionbreeder"), FluidBreederRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_breeder.get()));
        ItemStack zeroG = ModSatelliteItems.satellite.stack(EnumSatType.SCIENCE_ASSEMBLER);
        spaceAssembler = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.SPACE_ASSEMBLER,
                zeroG.getHoverName(), SpaceAssemblerRecipes.INSTANCE, zeroG);
        plasmaForge = new JEIGenericRecipeHandler(HbmJeiRecipeTypes.PLASMA_FORGE,
                title("plasma_forge"), PlasmaForgeRecipes.INSTANCE,
                stack(ModMachineBlocks.fusion_plasma_forge.get()));
         press = new PressRecipeHandler(HbmJeiRecipeTypes.PRESS, title("press"),
                 stack(ModMachineBlocks.machine_press.get()));
         ammoPress = new AmmoPressRecipeHandler(HbmJeiRecipeTypes.AMMO_PRESS, title("ammo_press"),
                 stack(ModMachineBlocks.machine_ammo_press.get()));
        soldering = new SolderingStationRecipeHandler(HbmJeiRecipeTypes.SOLDERING_STATION,
                title("soldering_station"), stack(ModMachineBlocks.machine_soldering_station.get()));
        crystallizer = new CrystallizerRecipeHandler(HbmJeiRecipeTypes.CRYSTALLIZER,
                title("crystallizer"), stack(ModMachineBlocks.machine_crystallizer.get()));
        centrifuge = new CentrifugeRecipeHandler(HbmJeiRecipeTypes.CENTRIFUGE,
                title("centrifuge"), stack(ModMachineBlocks.machine_centrifuge.get()));
        cyclotron = new CyclotronRecipeHandler(HbmJeiRecipeTypes.CYCLOTRON,
                title("cyclotron"), stack(ModMachineBlocks.machine_cyclotron.get()));
        particleAccelerator = new ParticleAcceleratorRecipeHandler(HbmJeiRecipeTypes.PARTICLE_ACCELERATOR,
                title("particle_accelerator"), stack(ModMachineBlocks.pa_detector.get()));
        gasCentrifuge = new GasCentrifugeRecipeHandler(HbmJeiRecipeTypes.GAS_CENTRIFUGE,
                title("gas_centrifuge"), stack(ModMachineBlocks.machine_gascent.get()));
        electrolyserFluid = new ElectrolyserFluidRecipeHandler(HbmJeiRecipeTypes.ELECTROLYSIS_FLUID,
                title("electrolysis_fluid"), stack(ModMachineBlocks.machine_electrolyser.get()));
        electrolyserMetal = new ElectrolyserMetalRecipeHandler(HbmJeiRecipeTypes.ELECTROLYSIS_METAL,
                title("electrolysis_metal"), stack(ModMachineBlocks.machine_electrolyser.get()));
        silex = new SILEXRecipeHandler(HbmJeiRecipeTypes.SILEX,
                title("silex"), stack(ModMachineBlocks.machine_silex.get()));
        fluidContainers = new FluidContainerRecipeHandler(HbmJeiRecipeTypes.FLUID_CONTAINERS, title("fluid_containers"));
        construction = new ConstructionRecipeHandler(HbmJeiRecipeTypes.CONSTRUCTION, title("construction"),
                ConstructionRecipeHandler.stations());
        tooling = new ToolingRecipeHandler(HbmJeiRecipeTypes.TOOLING, title("tooling"), ToolingRecipeHandler.stations());
        ashpit = new AshpitRecipeHandler(HbmJeiRecipeTypes.ASHPIT, Component.translatable("block.hbm.machine_ashpit"),
                stack(ModMachineBlocks.machine_ashpit.get()));
        crucibleSmelting = new CrucibleSmeltingHandler(HbmJeiRecipeTypes.CRUCIBLE_SMELTING, title("crucible_smelting"),
                stack(ModMachineBlocks.machine_crucible.get()));
        crucibleCasting = new CrucibleCastingHandler(HbmJeiRecipeTypes.CRUCIBLE_CASTING, title("crucible_casting"),
                CrucibleCastingHandler.stations()[0]);
        book = new BookRecipeHandler(HbmJeiRecipeTypes.BOOK, title("book_of_boxcars"));
        shredder = new ShredderRecipeHandler(HbmJeiRecipeTypes.SHREDDER,
                title("shredder"), stack(ModMachineBlocks.machine_shredder.get()));
        sawmill = new SawmillRecipeHandler(HbmJeiRecipeTypes.SAWMILL,
                title("sawmill"), stack(ModMachineBlocks.machine_sawmill.get()));
        crucible = new CrucibleAlloyingHandler(HbmJeiRecipeTypes.CRUCIBLE_ALLOY,
                title("crucible_alloy"), stack(ModMachineBlocks.machine_crucible.get()));
        anvilCon = new AnvilConstructionRecipeHandler(HbmJeiRecipeTypes.ANVIL_CON,
                title("anvil_construction"), anvilIcon());
        anvilSmith = new AnvilSmithingRecipeHandler(HbmJeiRecipeTypes.ANVIL_SMITH,
                title("anvil_smithing"), anvilIcon());

        refinery = new RefineryRecipeHandler(HbmJeiRecipeTypes.REFINERY,
                title("refinery"), stack(ModMachineBlocks.machine_refinery.get()));
        fractioning = new FractioningRecipeHandler(HbmJeiRecipeTypes.FRACTIONING,
                title("fractioning"), stack(ModMachineBlocks.machine_fraction_tower.get()));
        liquefaction = new LiquefactionRecipeHandler(HbmJeiRecipeTypes.LIQUEFACTION,
                title("liquefaction"), stack(ModMachineBlocks.machine_liquefactor.get()));
        solidification = new SolidificationRecipeHandler(HbmJeiRecipeTypes.SOLIDIFICATION,
                title("solidification"), stack(ModMachineBlocks.machine_solidifier.get()));
        boiling = new BoilingRecipeHandler(HbmJeiRecipeTypes.BOILING,
                title("boiling"), stack(ModMachineBlocks.machine_boiler.get()));
        cooling = new CoolingRecipeHandler(HbmJeiRecipeTypes.COOLING,
                title("cooling"), stack(ModMachineBlocks.machine_large_turbine.get()));

        vacuum = new VacuumRecipeHandler(HbmJeiRecipeTypes.VACUUM,
                title("vacuum_distiller"), stack(ModMachineBlocks.machine_vacuum_distill.get()));
        reforming = new ReformingRecipeHandler(HbmJeiRecipeTypes.REFORMING,
                title("reforming"), stack(ModMachineBlocks.machine_catalytic_reformer.get()));
        cracking = new CrackingRecipeHandler(HbmJeiRecipeTypes.CRACKING,
                title("cracking"), stack(ModMachineBlocks.machine_catalytic_cracker.get()));
        hydrotreating = new HydrotreatingRecipeHandler(HbmJeiRecipeTypes.HYDROTREATING,
                title("hydrotreating"), stack(ModMachineBlocks.machine_hydrotreater.get()));
        coker = new CokerRecipeHandler(HbmJeiRecipeTypes.COKER,
                title("coker"), stack(ModMachineBlocks.machine_coker.get()));
        pyrolysis = new PyrolysisRecipeHandler(HbmJeiRecipeTypes.PYROLYSIS,
                title("pyrolysis"), stack(ModMachineBlocks.machine_pyrooven.get()));
        compressor = new CompressorRecipeHandler(HbmJeiRecipeTypes.COMPRESSOR,
                title("compressor"), stack(ModMachineBlocks.machine_compressor.get()));
        storageDrum = new StorageDrumRecipeHandler(HbmJeiRecipeTypes.STORAGE_DRUM,
                Component.translatable("container.storageDrum"));
        fuelPool = new FuelPoolRecipeHandler(HbmJeiRecipeTypes.FUEL_POOL,
                Component.translatable("block.hbm.machine_waste_drum"), stack(ModMachineBlocks.machine_waste_drum.get()));
        arcWelder = new ArcWelderRecipeHandler(HbmJeiRecipeTypes.ARC_WELDER,
                Component.translatable("block.hbm.machine_arc_welder"),
                stack(ModMachineBlocks.machine_arc_welder.get()));
        arcFurnaceSolid = new ArcFurnaceRecipeHandler(HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                Component.translatable("block.hbm.machine_arc_furnace"), false,
                stack(ModMachineBlocks.machine_arc_furnace.get()));
        arcFurnaceFluid = new ArcFurnaceRecipeHandler(HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                Component.translatable("block.hbm.machine_arc_furnace"), true,
                stack(ModMachineBlocks.machine_arc_furnace.get()));
        diFurnace = new DiFurnaceRecipeHandler(HbmJeiRecipeTypes.ALLOY,
                Component.translatable("block.hbm.machine_difurnace_off"));
        rotaryFurnace = new RotaryFurnaceRecipeHandler(HbmJeiRecipeTypes.ROTARY_FURNACE,
                title("rotary_furnace"), stack(ModProductionBlocks.MACHINE_ROTARY_FURNACE.get()));

          reg.add(assembly, precisionAssembler, chemplant, mixer, exposure, purex, rockMill, superComputer, fusion, fusionBreeder, plasmaForge, spaceAssembler, press, ammoPress, soldering, crystallizer, centrifuge, cyclotron, particleAccelerator, gasCentrifuge, silex, shredder, sawmill, crucible, anvilCon, anvilSmith,
                 electrolyserFluid, electrolyserMetal, refinery, fractioning, liquefaction, solidification, boiling, cooling,
                 vacuum, reforming, cracking, hydrotreating, coker, pyrolysis, compressor, storageDrum, fuelPool,
                 arcWelder, arcFurnaceSolid, arcFurnaceFluid, diFurnace, rotaryFurnace,
                 fluidContainers, construction, tooling, ashpit, crucibleSmelting, crucibleCasting, book);
        RTGJeiIntegration.registerCategory(reg);
        reg.add(new SatelliteRecipeHandler());
        NuclearMiscJeiIntegration.registerCategories(reg);
        CustomMachineJeiIntegration.registerCategories(reg);
    }

    @Override
    public void recipes(Recipes reg) {
         AssemblyMachineRecipes.INSTANCE.registerRecipes();
         PrecAssRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        MixerRecipes.INSTANCE.registerRecipes();
        ExposureChamberRecipes.INSTANCE.registerRecipes();
        PUREXRecipes.INSTANCE.registerRecipes();
        RockMillRecipes.INSTANCE.registerRecipes();
        SuperComputerRecipes.INSTANCE.registerRecipes();
        FusionRecipes.INSTANCE.registerRecipes();
        FluidBreederRecipes.INSTANCE.registerRecipes();
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        SpaceAssemblerRecipes.INSTANCE.registerRecipes();
         PressRecipes.register();
         AmmoPressRecipes.register();
        SolderingRecipes.register();
        CrystallizerRecipes.register();
        CentrifugeRecipes.register();
        CyclotronRecipes.register();
        GasCentrifugeRecipes.register();
        ElectrolyserFluidRecipes.register();
        ElectrolyserMetalRecipes.register();

         reg.add(HbmJeiRecipeTypes.ASSEMBLY_MACHINE, assembly.buildRecipes());
         reg.add(HbmJeiRecipeTypes.PRECISION_ASSEMBLER, precisionAssembler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CHEMICAL_PLANT, chemplant.buildRecipes());
        reg.add(HbmJeiRecipeTypes.MIXER, mixer.buildRecipes());
        reg.add(HbmJeiRecipeTypes.EXPOSURE_CHAMBER, exposure.buildRecipes());
        reg.add(HbmJeiRecipeTypes.PUREX, purex.buildRecipes());
        reg.add(HbmJeiRecipeTypes.ROCK_MILL, rockMill.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SUPERCOMPUTER, superComputer.buildRecipes());
        reg.add(HbmJeiRecipeTypes.FUSION, fusion.buildRecipes());
        reg.add(HbmJeiRecipeTypes.FUSION_BREEDER, fusionBreeder.buildRecipes());
        reg.add(HbmJeiRecipeTypes.PLASMA_FORGE, plasmaForge.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SPACE_ASSEMBLER, spaceAssembler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.PRESS, PressRecipes.getRecipes());
         reg.add(HbmJeiRecipeTypes.AMMO_PRESS, AmmoPressRecipes.getRecipes());
        reg.add(HbmJeiRecipeTypes.SOLDERING_STATION, SolderingRecipes.recipes);
         reg.add(HbmJeiRecipeTypes.CRYSTALLIZER, CrystallizerRecipes.recipes);
         reg.add(HbmJeiRecipeTypes.CENTRIFUGE, CentrifugeRecipes.recipes);
         reg.add(HbmJeiRecipeTypes.CYCLOTRON, CyclotronRecipes.recipes);
         reg.add(HbmJeiRecipeTypes.PARTICLE_ACCELERATOR,
                 ParticleAcceleratorRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.GAS_CENTRIFUGE, GasCentrifugeRecipes.RECIPES);
        reg.add(HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, ElectrolyserFluidRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.ELECTROLYSIS_METAL, ElectrolyserMetalRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SILEX, SILEXRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.FLUID_CONTAINERS, FluidContainerRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CONSTRUCTION, ConstructionRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.TOOLING, ToolingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.ASHPIT, AshpitRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CRUCIBLE_SMELTING, CrucibleSmeltingHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CRUCIBLE_CASTING, CrucibleCastingHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.BOOK, BookRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SHREDDER, ShredderRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SAWMILL, SawmillRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CRUCIBLE_ALLOY, CrucibleAlloyingHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.ANVIL_CON, AnvilConstructionRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.ANVIL_SMITH, AnvilSmithingRecipeHandler.buildRecipes());

        reg.add(HbmJeiRecipeTypes.REFINERY, RefineryRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.FRACTIONING, FractioningRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.LIQUEFACTION, LiquefactionRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.SOLIDIFICATION, SolidificationRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.BOILING, BoilingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.COOLING, CoolingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.VACUUM, VacuumRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.REFORMING, ReformingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.CRACKING, CrackingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.HYDROTREATING, HydrotreatingRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.COKER, CokerRecipeHandler.buildRecipes());
        reg.add(HbmJeiRecipeTypes.PYROLYSIS, PyrolysisRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.COMPRESSOR, CompressorRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.STORAGE_DRUM, StorageDrumRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.FUEL_POOL, FuelPoolRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.ARC_WELDER, ArcWelderRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.ROTARY_FURNACE, RotaryFurnaceRecipeHandler.buildRecipes());
         reg.add(HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 ArcFurnaceRecipeHandler.buildRecipes(false));
         reg.add(HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 ArcFurnaceRecipeHandler.buildRecipes(true));
          reg.add(HbmJeiRecipeTypes.ALLOY, DiFurnaceRecipeHandler.buildRecipes());
        RTGJeiIntegration.registerRecipes(reg);
        reg.add(SatelliteRecipeHandler.TYPE, SatelliteRecipeHandler.buildRecipes());
        NuclearMiscJeiIntegration.registerRecipes(reg);
        CustomMachineJeiIntegration.registerRecipes(reg);
      }

    @Override
    public void clickAreas(ClickAreas reg) {
        reg.add(GUIMixer.class, 62, 36, 52, 44,
                HbmJeiRecipeTypes.MIXER);
        reg.add(GUIMachineExposureChamber.class, 36, 40, 40, 8,
                HbmJeiRecipeTypes.EXPOSURE_CHAMBER);
        reg.add(GUICentrifuge.class, 35, 9, 106, 40,
                HbmJeiRecipeTypes.CENTRIFUGE);
        // 1.7.10's NEI rect (43, 16, 34 x 34): the earlier 134 x 54 area covered all nine slots.
        reg.add(GUIMachineCyclotron.class, 43, 16, 34, 34,
                HbmJeiRecipeTypes.CYCLOTRON);
        // The arrow between the two slot rows, as in 1.12.2 (75, 35, 7 x 8): a 36 x 45 area here covered all
        // four slots, and JEI took every click on them, so nothing could be put into the source by hand.
        reg.add(GUIPADetector.class, 75, 35, 7, 8,
                HbmJeiRecipeTypes.PARTICLE_ACCELERATOR);
        reg.add(GUIPASource.class, 75, 35, 7, 8,
                HbmJeiRecipeTypes.PARTICLE_ACCELERATOR);
        reg.add(GUIMachineGasCent.class, 70, 36, 36, 12,
                HbmJeiRecipeTypes.GAS_CENTRIFUGE);
        reg.add(GUIMachineElectrolyser.class, 62, 26, 12, 40,
                HbmJeiRecipeTypes.ELECTROLYSIS_FLUID);
        reg.add(GUIMachineElectrolyser.class, 7, 46, 22, 25,
                HbmJeiRecipeTypes.ELECTROLYSIS_METAL);
        reg.add(GUIFusionBreeder.class, 67, 49, 42, 9,
                HbmJeiRecipeTypes.FUSION_BREEDER);
        reg.add(GUIMachineArcWelder.class, 72, 38, 32, 13,
                HbmJeiRecipeTypes.ARC_WELDER);
        reg.add(GUIMachineRotaryFurnace.class, 58, 19, 32, 10,
                HbmJeiRecipeTypes.ROTARY_FURNACE);
        reg.add(GUIMachineArcFurnaceLarge.class, 17, 36, 7, 70,
                HbmJeiRecipeTypes.ARC_FURNACE_SOLID);
        reg.add(GUIMachineArcFurnaceLarge.class, 152, 36, 16, 70,
                HbmJeiRecipeTypes.ARC_FURNACE_FLUID);
        reg.add(GUIDiFurnace.class, 102, 36, 21, 14,
                HbmJeiRecipeTypes.ALLOY);
        // 1.7 NEI rects, offset by NEI's (5, 11) recipe origin
        reg.add(GUISILEX.class, 44, 71, 60, 50, HbmJeiRecipeTypes.SILEX);
        reg.add(GUIBook.class, 89, 34, 24, 18, HbmJeiRecipeTypes.BOOK);
        RTGJeiIntegration.registerGuiHandler(reg);
    }

    @Override
    public void transfers(Transfers reg) {
         reg.add(ContainerMachineAmmoPress.class,
                 ModMenus.MACHINE_AMMO_PRESS.get(), HbmJeiRecipeTypes.AMMO_PRESS, 0, 9, 10, 36);
         reg.addSelective(ContainerMixer.class,
                ModMenus.MACHINE_MIXER.get(), HbmJeiRecipeTypes.MIXER, new int[]{1}, 5, 36,
                recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
        reg.addSelective(ContainerMachineExposureChamber.class,
                ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                new int[]{0, 2}, 7, 36, recipe -> true);
        reg.add(ContainerCentrifuge.class,
                ModMenus.MACHINE_CENTRIFUGE.get(), HbmJeiRecipeTypes.CENTRIFUGE,
                0, 1, ContainerCentrifuge.MACHINE_SLOTS, 36);
         reg.addSelective(ContainerMachineCyclotron.class,
                 ModMenus.MACHINE_CYCLOTRON.get(), HbmJeiRecipeTypes.CYCLOTRON,
                 new int[]{0, 3}, 12, 36, recipe -> true);
         reg.addSelective(ContainerMachinePrecAss.class,
                 ModMenus.MACHINE_PREC_ASS.get(), HbmJeiRecipeTypes.PRECISION_ASSEMBLER,
                 new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12}, 22, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
         reg.add(ContainerMachineElectrolyser.class,
                 ModMenus.MACHINE_ELECTROLYSER_METAL.get(), HbmJeiRecipeTypes.ELECTROLYSIS_METAL,
                 3, 1, 10, 36);
         reg.addSelective(ContainerMachineArcWelder.class,
                 ModMenus.MACHINE_ARC_WELDER.get(), HbmJeiRecipeTypes.ARC_WELDER,
                 new int[]{0, 1, 2}, 8, 36, recipe -> recipe.fluid == null);
        reg.addSelective(ContainerMachineAssemblyFactory.class,
                 ModMenus.MACHINE_ASSEMBLY_FACTORY.get(),
                 HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                 new int[]{5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, 60, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
        reg.addSelective(ContainerMachineChemicalFactory.class,
                 ModMenus.MACHINE_CHEMICAL_FACTORY.get(),
                 HbmJeiRecipeTypes.CHEMICAL_PLANT, new int[]{5, 6, 7}, 32, 36,
                 recipe -> recipe.inputFluid == null || recipe.inputFluid.length == 0);
         reg.add(ContainerMachineArcFurnaceLarge.class,
                 ModMenus.MACHINE_ARC_FURNACE.get(), HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 5, 1, 30, 36);
         reg.add(ContainerMachineArcFurnaceLarge.class,
                 ModMenus.MACHINE_ARC_FURNACE.get(), HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 5, 1, 30, 36);
          reg.addSelective(ContainerDiFurnace.class,
                  ModMenus.MACHINE_DIFURNACE.get(), HbmJeiRecipeTypes.ALLOY,
                  new int[]{0, 1}, 4, 36, recipe -> true);
        RTGJeiIntegration.registerTransfer(reg);
     }

    @Override
    public void stations(Stations reg) {
        RTGJeiIntegration.registerCatalyst(reg);
        reg.add(SatelliteRecipeHandler.TYPE, new ItemStack(ModSatelliteBlocks.sat_dock.get()));
        reg.add(HbmJeiRecipeTypes.SPACE_ASSEMBLER, ModSatelliteItems.satellite.stack(EnumSatType.SCIENCE_ASSEMBLER));
        NuclearMiscJeiIntegration.registerCatalysts(reg);
        CustomMachineJeiIntegration.registerCatalysts(reg);
          reg.add(HbmJeiRecipeTypes.ASSEMBLY_MACHINE, stack(ModMachineBlocks.machine_assembly_machine.get()));
          reg.add(HbmJeiRecipeTypes.ASSEMBLY_MACHINE,
                  stack(ModMachineBlocks.machine_assembly_factory.get()));
          reg.add(HbmJeiRecipeTypes.PRECISION_ASSEMBLER, stack(ModMachineBlocks.machine_precass.get()));
         reg.add(HbmJeiRecipeTypes.CHEMICAL_PLANT, stack(ModMachineBlocks.machine_chemical_plant.get()));
         reg.add(HbmJeiRecipeTypes.CHEMICAL_PLANT,
                 stack(ModMachineBlocks.machine_chemical_factory.get()));
        reg.add(HbmJeiRecipeTypes.MIXER, stack(ModMachineBlocks.machine_mixer.get()));
        reg.add(HbmJeiRecipeTypes.PUREX, stack(ModMachineBlocks.machine_purex.get()));
        reg.add(HbmJeiRecipeTypes.ROCK_MILL, stack(ModMachineBlocks.machine_rockmill.get()));
        reg.add(HbmJeiRecipeTypes.SUPERCOMPUTER, stack(ModMachineBlocks.machine_supercomputer.get()));
        reg.add(HbmJeiRecipeTypes.FUSION, stack(ModMachineBlocks.fusion_torus.get()));
        reg.add(HbmJeiRecipeTypes.FUSION_BREEDER,
                stack(ModMachineBlocks.fusion_breeder.get()));
        reg.add(HbmJeiRecipeTypes.PLASMA_FORGE, stack(ModMachineBlocks.fusion_plasma_forge.get()));
         reg.add(HbmJeiRecipeTypes.PRESS, stack(ModMachineBlocks.machine_press.get()), stack(ModMachineBlocks.machine_epress.get()));
         reg.add(HbmJeiRecipeTypes.AMMO_PRESS,
                 stack(ModMachineBlocks.machine_ammo_press.get()));
        reg.add(HbmJeiRecipeTypes.SOLDERING_STATION, stack(ModMachineBlocks.machine_soldering_station.get()));
         reg.add(HbmJeiRecipeTypes.CRYSTALLIZER, stack(ModMachineBlocks.machine_crystallizer.get()));
         reg.add(HbmJeiRecipeTypes.CENTRIFUGE, stack(ModMachineBlocks.machine_centrifuge.get()));
         reg.add(HbmJeiRecipeTypes.CYCLOTRON, stack(ModMachineBlocks.machine_cyclotron.get()));
         reg.add(HbmJeiRecipeTypes.PARTICLE_ACCELERATOR, stack(ModMachineBlocks.pa_detector.get()));
         reg.add(HbmJeiRecipeTypes.GAS_CENTRIFUGE, stack(ModMachineBlocks.machine_gascent.get()));
        reg.add(HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, stack(ModMachineBlocks.machine_electrolyser.get()));
        reg.add(HbmJeiRecipeTypes.ELECTROLYSIS_METAL, stack(ModMachineBlocks.machine_electrolyser.get()));
        reg.add(HbmJeiRecipeTypes.EXPOSURE_CHAMBER,
                stack(ModMachineBlocks.machine_exposure_chamber.get()));
        reg.add(HbmJeiRecipeTypes.SILEX, stack(ModMachineBlocks.machine_silex.get()));
        reg.add(HbmJeiRecipeTypes.FLUID_CONTAINERS, FluidContainerRecipeHandler.machines().toArray(ItemStack[]::new));
        reg.add(HbmJeiRecipeTypes.CONSTRUCTION, ConstructionRecipeHandler.stations());
        reg.add(HbmJeiRecipeTypes.TOOLING, ToolingRecipeHandler.stations());
        reg.add(HbmJeiRecipeTypes.ASHPIT, stack(ModMachineBlocks.machine_ashpit.get()));
        reg.add(HbmJeiRecipeTypes.CRUCIBLE_SMELTING, stack(ModMachineBlocks.machine_crucible.get()));
        reg.add(HbmJeiRecipeTypes.CRUCIBLE_CASTING, CrucibleCastingHandler.stations());
        reg.add(HbmJeiRecipeTypes.BOOK, BookRecipeHandler.book());
        reg.add(HbmJeiRecipeTypes.SHREDDER, stack(ModMachineBlocks.machine_shredder.get()));
        reg.add(HbmJeiRecipeTypes.SAWMILL, stack(ModMachineBlocks.machine_sawmill.get()));
         reg.add(HbmJeiRecipeTypes.CRUCIBLE_ALLOY, stack(ModMachineBlocks.machine_crucible.get()));
         reg.add(HbmJeiRecipeTypes.ARC_WELDER,
                 stack(ModMachineBlocks.machine_arc_welder.get()));
         reg.add(HbmJeiRecipeTypes.ROTARY_FURNACE,
                 stack(ModProductionBlocks.MACHINE_ROTARY_FURNACE.get()));
         reg.add(HbmJeiRecipeTypes.ARC_FURNACE_SOLID,
                 stack(ModMachineBlocks.machine_arc_furnace.get()));
         reg.add(HbmJeiRecipeTypes.ARC_FURNACE_FLUID,
                 stack(ModMachineBlocks.machine_arc_furnace.get()));
         reg.add(HbmJeiRecipeTypes.ALLOY,
                 stack(ModMachineBlocks.machine_difurnace_off.get()));
        reg.add(HbmJeiRecipeTypes.ANVIL_CON, anvilIcon());
        reg.add(HbmJeiRecipeTypes.ANVIL_SMITH, anvilIcon());

        reg.add(HbmJeiRecipeTypes.REFINERY, stack(ModMachineBlocks.machine_refinery.get()));
        reg.add(HbmJeiRecipeTypes.FRACTIONING, stack(ModMachineBlocks.machine_fraction_tower.get()));
        reg.add(HbmJeiRecipeTypes.LIQUEFACTION, stack(ModMachineBlocks.machine_liquefactor.get()));
        reg.add(HbmJeiRecipeTypes.SOLIDIFICATION, stack(ModMachineBlocks.machine_solidifier.get()));
        // Boiling: the boiler + all three heaters (firebox / electric / oilburner) can drive it.
        reg.add(HbmJeiRecipeTypes.VACUUM, stack(ModMachineBlocks.machine_vacuum_distill.get()));
        reg.add(HbmJeiRecipeTypes.REFORMING, stack(ModMachineBlocks.machine_catalytic_reformer.get()));
        reg.add(HbmJeiRecipeTypes.CRACKING, stack(ModMachineBlocks.machine_catalytic_cracker.get()));
        reg.add(HbmJeiRecipeTypes.HYDROTREATING, stack(ModMachineBlocks.machine_hydrotreater.get()));
        reg.add(HbmJeiRecipeTypes.COKER, stack(ModMachineBlocks.machine_coker.get()));
        reg.add(HbmJeiRecipeTypes.PYROLYSIS, stack(ModMachineBlocks.machine_pyrooven.get()));
        reg.add(HbmJeiRecipeTypes.COMPRESSOR, stack(ModMachineBlocks.machine_compressor.get()));
        reg.add(HbmJeiRecipeTypes.BOILING, stack(ModMachineBlocks.machine_boiler.get()),
                stack(ModMachineBlocks.heater_firebox.get()), stack(ModMachineBlocks.heater_electric.get()),
                stack(ModMachineBlocks.heater_oilburner.get()));
         reg.add(HbmJeiRecipeTypes.COOLING, stack(ModMachineBlocks.machine_large_turbine.get()),
                  stack(ModMachineBlocks.machine_condenser.get()), stack(ModMachineBlocks.machine_condenser_powered.get()));
         reg.add(HbmJeiRecipeTypes.STORAGE_DRUM,
                 stack(ModMachineBlocks.machine_storage_drum.get()));
         reg.add(HbmJeiRecipeTypes.FUEL_POOL,
                 stack(ModMachineBlocks.machine_waste_drum.get()));

        // The electric furnace runs vanilla smelting, so list it under the viewer's built-in furnace category
        // (mirrors the old JEIConfig line; no bespoke category needed).
        reg.addSmelting(stack(ModMachineBlocks.machine_electric_furnace_off.get()));
    }

    /**
     * Runs once the viewer has its recipes: 1.7 {@code ArcFurnaceRecipes.registerFurnaceSmeltables}, the arc furnace's
     * furnace-smelting pass, listed in the solid arc furnace page. The furnace recipes are only readable from the viewer's
     * own lookup on a 26.1 client, so the pass is added here rather than at registration: JEI runs it
     * at {@code onRuntimeAvailable}, REI the first time it is asked for the page.
     */
    @Override
    public void runtime(Runtime runtime) {
        List<ArcFurnaceRecipes.ArcFurnaceRecipe> pass = ArcFurnaceRecipes
                .furnacePass(runtime.smeltingRecipes());
        runtime.addRecipes(HbmJeiRecipeTypes.ARC_FURNACE_SOLID, pass);
        Constants.LOG.info("[{}] arc furnace furnace pass: {} recipe(s)", runtime.viewer(), pass.size());
    }

    /** The category title, from the {@code hbm.<path>.name} lang key (the old title-key convention). */
    static Component title(String path) {
        return Component.translatable("hbm." + path + ".name");
    }

    static ItemStack stack(Block block) {
        return new ItemStack(block);
    }

    /** Icon/catalyst for the anvil categories: the tier-1 iron anvil (the anvil has no processing TE). */
    static ItemStack anvilIcon() {
        return new ItemStack(ModAnvilBlocks.anvil_iron.get());
    }
}
