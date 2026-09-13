package com.hbm.blocks.machine;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockToolConvertible;
import com.hbm.blocks.machine.struct.BlockStruct;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.struct.TileEntityWatzStruct;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.blocks.network.FluidValve;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.bomb.LaunchPad;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Registry for HBM machine blocks (blocks backed by a BlockEntity). Separate from the auto-generated
 * {@link com.hbm.blocks.ModBlocks} (which holds the simple blocks) so codegen never clobbers them.
 */
public final class ModMachineBlocks {

    private ModMachineBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockMachineBattery> machine_battery = BLOCKS.registerBlock(
            "machine_battery", BlockMachineBattery::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<MachineRadGen> machine_radgen = BLOCKS.registerBlock(
            "machine_radgen", MachineRadGen::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineMiniRTG> machine_minirtg = BLOCKS.registerBlock(
            "machine_minirtg", MachineMiniRTG::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineMiniRTG> rtg_polonium = BLOCKS.registerBlock(
            "rtg_polonium", MachineMiniRTG::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineRTG> machine_rtg_grey = BLOCKS.registerBlock(
            "machine_rtg_grey", MachineRTG::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<BlockItem> machine_rtg_grey_item = ITEMS.registerItem(
            "machine_rtg_grey",
            p -> new dev.golem.core.world.CompatBlockItem(machine_rtg_grey.get(), p) {
                @Override
                public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack) {
                    return net.minecraft.network.chat.Component.literal("RT Generator");
                }
            }, p -> Compat.blockDescription(p));
    public static final RegistryObject<RadSensor> radsensor = BLOCKS.registerBlock(
            "radsensor", RadSensor::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));

    public static final RegistryObject<BlockCable> red_cable = BLOCKS.registerBlock(
            "red_cable", BlockCable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<BlockCreativeGenerator> creative_generator = BLOCKS.registerBlock(
            "creative_generator", BlockCreativeGenerator::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL));

    /** Nuclear steam-cycle machines. */
    public static final RegistryObject<MachineTurbine> machine_turbine = BLOCKS.registerBlock(
            "machine_turbine", MachineTurbine::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineLargeTurbine> machine_large_turbine = BLOCKS.registerBlock(
            "machine_large_turbine", MachineLargeTurbine::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineTurbineGas> machine_turbine_gas = BLOCKS.registerBlock(
            "machine_turbine_gas", MachineTurbineGas::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineTurbofan> machine_turbofan = BLOCKS.registerBlock(
            "machine_turbofan", MachineTurbofan::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineSteamEngine> machine_steam_engine = BLOCKS.registerBlock(
            "machine_steam_engine", MachineSteamEngine::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachinePump> machine_pump_electric = BLOCKS.registerBlock(
            "machine_pump_electric", MachinePump::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachinePump> machine_pump_steam = BLOCKS.registerBlock(
            "machine_pump_steam", p -> new MachinePump(p, true),
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineStirling> machine_stirling = BLOCKS.registerBlock(
            "machine_stirling", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineStirling> machine_stirling_steel = BLOCKS.registerBlock(
            "machine_stirling_steel", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineStirling> machine_stirling_creative = BLOCKS.registerBlock(
            "machine_stirling_creative", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineIndustrialTurbine> machine_industrial_turbine = BLOCKS.registerBlock(
            "machine_industrial_turbine", MachineIndustrialTurbine::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineCondenser> machine_condenser = BLOCKS.registerBlock(
            "machine_condenser", MachineCondenser::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineCondenserPowered> machine_condenser_powered = BLOCKS.registerBlock(
            "machine_condenser_powered", MachineCondenserPowered::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineZirnox> machine_zirnox = BLOCKS.registerBlock(
            "machine_zirnox", MachineZirnox::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    /** Accident-only replacement for a melted-down ZIRNOX; intentionally has no block item. */
    public static final RegistryObject<MachineZirnoxDestroyed> zirnox_destroyed = BLOCKS.registerBlock(
            "zirnox_destroyed", MachineZirnoxDestroyed::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineWasteDrum> machine_waste_drum = BLOCKS.registerBlock(
            "machine_waste_drum", MachineWasteDrum::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineStorageDrum> machine_storage_drum = BLOCKS.registerBlock(
            "machine_storage_drum", MachineStorageDrum::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    // --- Multiblock structure components (hand-built machines: Watz, fusion torus) ---
    // The plain forms are craftable; the upgraded forms exist only as the output of an in-world tool
    // conversion (NTMToolHandler), and the reactors' structure patterns only accept the upgraded ones.

    public static final RegistryObject<BlockToolConvertible> watz_casing = BLOCKS.registerBlock(
            "watz_casing", BlockToolConvertible::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<Block> watz_casing_tooled = BLOCKS.registerBlock(
            "watz_casing_tooled", Block::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockToolConvertible> fusion_component = BLOCKS.registerBlock(
            "fusion_component", BlockToolConvertible::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<Block> fusion_component_bscco_welded = BLOCKS.registerBlock(
            "fusion_component_bscco_welded", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<Block> fusion_component_blanket = BLOCKS.registerBlock(
            "fusion_component_blanket", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<Block> fusion_component_motor = BLOCKS.registerBlock(
            "fusion_component_motor", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /** The Watz reactor itself has no block item: it only ever assembles from a struct core. */
    public static final RegistryObject<Watz> watz = BLOCKS.registerBlock(
            "watz", Watz::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<WatzPump> watz_pump = BLOCKS.registerBlock(
            "watz_pump", WatzPump::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** The fusion torus, like the Watz, exists only by assembling its struct core. */
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionTorus> fusion_torus = BLOCKS.registerBlock(
            "fusion_torus", com.hbm.blocks.machine.fusion.MachineFusionTorus::new,
            p -> dummyable(p.strength(5.0F, 60.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- The fusion family: what feeds the torus and what consumes its plasma. ---
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionCoupler> fusion_coupler = BLOCKS.registerBlock(
            "fusion_coupler", com.hbm.blocks.machine.fusion.MachineFusionCoupler::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionCollector> fusion_collector = BLOCKS.registerBlock(
            "fusion_collector", com.hbm.blocks.machine.fusion.MachineFusionCollector::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionKlystron> fusion_klystron = BLOCKS.registerBlock(
            "fusion_klystron", com.hbm.blocks.machine.fusion.MachineFusionKlystron::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionKlystronCreative> fusion_klystron_creative = BLOCKS.registerBlock(
            "fusion_klystron_creative", com.hbm.blocks.machine.fusion.MachineFusionKlystronCreative::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionBoiler> fusion_boiler = BLOCKS.registerBlock(
            "fusion_boiler", com.hbm.blocks.machine.fusion.MachineFusionBoiler::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionBreeder> fusion_breeder = BLOCKS.registerBlock(
            "fusion_breeder", com.hbm.blocks.machine.fusion.MachineFusionBreeder::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionMHDT> fusion_mhdt = BLOCKS.registerBlock(
            "fusion_mhdt", com.hbm.blocks.machine.fusion.MachineFusionMHDT::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<com.hbm.blocks.machine.fusion.MachineFusionPlasmaForge> fusion_plasma_forge = BLOCKS.registerBlock(
            "fusion_plasma_forge", com.hbm.blocks.machine.fusion.MachineFusionPlasmaForge::new,
            p -> dummyable(p.strength(5.0F, 60.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Seed blocks for the hand-built multiblocks. */
    public static final RegistryObject<BlockStruct> struct_watz_core = BLOCKS.registerBlock(
            "struct_watz_core",
            p -> new BlockStruct(p, TileEntityWatzStruct::new, () -> ModBlockEntities.STRUCT_WATZ_CORE.get()),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).lightLevel(state -> 15));
    public static final RegistryObject<BlockStruct> struct_torus_core = BLOCKS.registerBlock(
            "struct_torus_core",
            p -> new BlockStruct(p, com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct::new,
                    () -> ModBlockEntities.STRUCT_TORUS_CORE.get()),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).lightLevel(state -> 15));

    // --- Phase C: fluid network demo (duct conductor + infinite source + receiving tank) ---

    public static final RegistryObject<BlockFluidDuct> fluid_duct_mk2 = BLOCKS.registerBlock(
            "fluid_duct_mk2", BlockFluidDuct::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockCreativeFluidSource> creative_fluid_source = BLOCKS.registerBlock(
            "creative_fluid_source", BlockCreativeFluidSource::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL));

    public static final RegistryObject<MachineFluidTank> machine_fluidtank = BLOCKS.registerBlock(
            "machine_fluidtank", MachineFluidTank::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineUF6Tank> machine_uf6_tank = BLOCKS.registerBlock(
            "machine_uf6_tank", MachineUF6Tank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachinePuF6Tank> machine_puf6_tank = BLOCKS.registerBlock(
            "machine_puf6_tank", MachinePuF6Tank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineBigAssTank> machine_bigasstank = BLOCKS.registerBlock(
            "machine_bigasstank", MachineBigAssTank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineBigAssTank9000> machine_bat9000 = BLOCKS.registerBlock(
            "machine_bat9000", MachineBigAssTank9000::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<FluidValve> fluid_valve = BLOCKS.registerBlock(
            "fluid_valve", FluidValve::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<MachineDrain> machine_drain = BLOCKS.registerBlock(
            "machine_drain", MachineDrain::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- scaffold machines: placeable, directional, BlockEntity-backed; logic deferred (§18) ---

    public static final RegistryObject<MachineElectricFurnace> machine_electric_furnace_off = BLOCKS.registerBlock(
            "machine_electric_furnace_off", MachineElectricFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<MachineFurnaceBrick> machine_furnace_brick_off = BLOCKS.registerBlock(
            "machine_furnace_brick_off", MachineFurnaceBrick::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE));
    public static final RegistryObject<MachineFurnaceIron> furnace_iron = BLOCKS.registerBlock(
            "furnace_iron", MachineFurnaceIron::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineFurnaceSteel> furnace_steel = BLOCKS.registerBlock(
            "furnace_steel", MachineFurnaceSteel::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Shredder — a plain JSON-model cube (no BER), unlike the multiblocks below. */
    public static final RegistryObject<MachineShredder> machine_shredder = BLOCKS.registerBlock(
            "machine_shredder", MachineShredder::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    // Folded multiblock machines — invisible core+cell blocks drawn by a BlockEntityRenderer.
    public static final RegistryObject<MachineEPress> machine_epress = BLOCKS.registerBlock(
            "machine_epress", MachineEPress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachinePress> machine_press = BLOCKS.registerBlock(
            "machine_press", MachinePress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineAmmoPress> machine_ammo_press = BLOCKS.registerBlock(
            "machine_ammo_press", MachineAmmoPress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineSawmill> machine_sawmill = BLOCKS.registerBlock(
            "machine_sawmill", MachineSawmill::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineAssemblyMachine> machine_assembly_machine = BLOCKS.registerBlock(
            "machine_assembly_machine", MachineAssemblyMachine::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachinePrecAss> machine_precass = BLOCKS.registerBlock(
            "machine_precass", MachinePrecAss::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineChemicalPlant> machine_chemical_plant = BLOCKS.registerBlock(
            "machine_chemical_plant", MachineChemicalPlant::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineElectrolyser> machine_electrolyser = BLOCKS.registerBlock(
            "machine_electrolyser", MachineElectrolyser::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineMixer> machine_mixer = BLOCKS.registerBlock(
            "machine_mixer", MachineMixer::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineExposureChamber> machine_exposure_chamber = BLOCKS.registerBlock(
            "machine_exposure_chamber", MachineExposureChamber::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineBlastFurnace> machine_blast_furnace = BLOCKS.registerBlock(
            "machine_blast_furnace", MachineBlastFurnace::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachinePUREX> machine_purex = BLOCKS.registerBlock(
            "machine_purex", MachinePUREX::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Diesel Generator — burns combustible fluids into HE (the first real energy producer). */
    public static final RegistryObject<MachineDiesel> machine_diesel = BLOCKS.registerBlock(
            "machine_diesel", MachineDiesel::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    /** Flare Stack — vents or burns gas for power; 12-tall 3×3 tower. */
    public static final RegistryObject<MachineGasFlare> machine_flare = BLOCKS.registerBlock(
            "machine_flare", MachineGasFlare::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Fractioning Tower — splits a hot fraction into two; stacks 3 apart via the spacer. */
    public static final RegistryObject<MachineFractionTower> machine_fraction_tower = BLOCKS.registerBlock(
            "machine_fraction_tower", MachineFractionTower::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Fractioning Tower Separator — a logic-less spacer raising the tower stack. */
    public static final RegistryObject<FractionSpacer> fraction_spacer = BLOCKS.registerBlock(
            "fraction_spacer", FractionSpacer::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Liquefactor — items → fluid. */
    public static final RegistryObject<MachineLiquefactor> machine_liquefactor = BLOCKS.registerBlock(
            "machine_liquefactor", MachineLiquefactor::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Solidifier — fluid → items. */
    public static final RegistryObject<MachineSolidifier> machine_solidifier = BLOCKS.registerBlock(
            "machine_solidifier", MachineSolidifier::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Refinery — fractionates HOT oils into 4 products; 3×3×9 tower. */
    public static final RegistryObject<MachineRefinery> machine_refinery = BLOCKS.registerBlock(
            "machine_refinery", MachineRefinery::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Heat Boiler (old id {@code heat_boiler}) — the OIL→HOTOIL / water→steam converter. */
    public static final RegistryObject<MachineHeatBoiler> machine_boiler = BLOCKS.registerBlock(
            "heat_boiler", MachineHeatBoiler::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Industrial heat boiler — the five-port, high-capacity variant. */
    public static final RegistryObject<MachineHeatBoilerIndustrial> machine_industrial_boiler = BLOCKS.registerBlock(
            "machine_industrial_boiler", MachineHeatBoilerIndustrial::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Fluid Burner — burns flammable fluids into an IHeatSource reservoir. */
    public static final RegistryObject<HeaterOilburner> heater_oilburner = BLOCKS.registerBlock(
            "heater_oilburner", HeaterOilburner::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Wood Burner — solid fuel or WOODOIL generator with a 100k HE buffer. */
    public static final RegistryObject<MachineWoodBurner> machine_wood_burner = BLOCKS.registerBlock(
            "machine_wood_burner", MachineWoodBurner::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- oil wave: extraction drills ---
    public static final RegistryObject<MachineOilWell> machine_well = BLOCKS.registerBlock(
            "machine_well", MachineOilWell::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachinePumpjack> machine_pumpjack = BLOCKS.registerBlock(
            "machine_pumpjack", MachinePumpjack::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- advanced oil wave ---

    public static final RegistryObject<MachineFrackingTower> machine_fracking_tower = BLOCKS.registerBlock(
            "machine_fracking_tower", MachineFrackingTower::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineVacuumDistill> machine_vacuum_distill = BLOCKS.registerBlock(
            "machine_vacuum_distill", MachineVacuumDistill::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineCatalyticReformer> machine_catalytic_reformer = BLOCKS.registerBlock(
            "machine_catalytic_reformer", MachineCatalyticReformer::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineCatalyticCracker> machine_catalytic_cracker = BLOCKS.registerBlock(
            "machine_catalytic_cracker", MachineCatalyticCracker::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineHydrotreater> machine_hydrotreater = BLOCKS.registerBlock(
            "machine_hydrotreater", MachineHydrotreater::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineCoker> machine_coker = BLOCKS.registerBlock(
            "machine_coker", MachineCoker::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineFurnaceCombination> machine_furnace_combination = BLOCKS.registerBlock(
            "machine_furnace_combination", MachineFurnaceCombination::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachinePyroOven> machine_pyrooven = BLOCKS.registerBlock(
            "machine_pyrooven", MachinePyroOven::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineCombustionEngine> machine_combustion_engine = BLOCKS.registerBlock(
            "machine_combustion_engine", MachineCombustionEngine::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<BlockRefueler> refueler = BLOCKS.registerBlock(
            "refueler", BlockRefueler::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    /** Compressor — raises fluid pressure; the gate the vacuum refinery (OIL @2) + hydrotreater (H @1) need. */
    public static final RegistryObject<MachineCompressor> machine_compressor = BLOCKS.registerBlock(
            "machine_compressor", MachineCompressor::new,
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineSolderingStation> machine_soldering_station = BLOCKS.registerBlock(
            "machine_soldering_station", MachineSolderingStation::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineArcWelder> machine_arc_welder = BLOCKS.registerBlock(
            "machine_arc_welder", MachineArcWelder::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineIntake> machine_intake = BLOCKS.registerBlock(
            "machine_intake", p -> new MachineIntake(p, () -> ModBlockEntities.MACHINE_INTAKE.get()),
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineFan> fan = BLOCKS.registerBlock(
            "fan", p -> new MachineFan(p, () -> ModBlockEntities.MACHINE_FAN.get()),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<MachineCrystallizer> machine_crystallizer = BLOCKS.registerBlock(
            "machine_crystallizer", MachineCrystallizer::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Standard item centrifuge — a 1×1×4 tower with four output streams. */
    public static final RegistryObject<MachineCentrifuge> machine_centrifuge = BLOCKS.registerBlock(
            "machine_centrifuge", MachineCentrifuge::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<MachineCyclotron> machine_cyclotron = BLOCKS.registerBlock(
            "machine_cyclotron", MachineCyclotron::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Gas centrifuge — a dedicated four-block cascade machine for UF6/PUF6/WATZ. */
    public static final RegistryObject<MachineGasCent> machine_gascent = BLOCKS.registerBlock(
            "machine_gascent", MachineGasCent::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineDeuteriumExtractor> machine_deuterium_extractor = BLOCKS.registerBlock(
            "machine_deuterium_extractor", MachineDeuteriumExtractor::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineDeuteriumTower> machine_deuterium_tower = BLOCKS.registerBlock(
            "machine_deuterium_tower", MachineDeuteriumTower::new,
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** SILEX — the laser isotope separation chamber. Fluid-only (peroxide); the laser supplies the energy. */
    public static final RegistryObject<MachineSILEX> machine_silex = BLOCKS.registerBlock(
            "machine_silex", MachineSILEX::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Free Electron Laser — the SILEX's power source. Energy only; no fluids, no internal recipes. */
    public static final RegistryObject<MachineFEL> machine_fel = BLOCKS.registerBlock(
            "machine_fel", MachineFEL::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- crucible / foundry ---

    public static final RegistryObject<MachineCrucible> machine_crucible = BLOCKS.registerBlock(
            "machine_crucible", MachineCrucible::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<FoundryChannel> foundry_channel = BLOCKS.registerBlock(
            "foundry_channel", FoundryChannel::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<FoundryMold> foundry_mold = BLOCKS.registerBlock(
            "foundry_mold", FoundryMold::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<FoundryBasin> foundry_basin = BLOCKS.registerBlock(
            "foundry_basin", FoundryBasin::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<FoundryOutlet> foundry_outlet = BLOCKS.registerBlock(
            "foundry_outlet", FoundryOutlet::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<FoundrySlagtap> foundry_slagtap = BLOCKS.registerBlock(
            "foundry_slagtap", FoundrySlagtap::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());

    /** Dynamic slag — the world waste pile the slagtap deposits (no block item). */
    public static final RegistryObject<com.hbm.blocks.generic.BlockDynamicSlag> slag = BLOCKS.registerBlock(
            "slag", com.hbm.blocks.generic.BlockDynamicSlag::new,
            p -> p.strength(1.0F, 2.0F).sound(SoundType.METAL).noOcclusion());

    // --- foundry heat sources ---

    public static final RegistryObject<HeaterFirebox> heater_firebox = BLOCKS.registerBlock(
            "heater_firebox", HeaterFirebox::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<HeaterOven> heater_oven = BLOCKS.registerBlock(
            "heater_oven", HeaterOven::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<HeaterElectric> heater_electric = BLOCKS.registerBlock(
            "heater_electric", HeaterElectric::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<HeaterHeatex> heater_heatex = BLOCKS.registerBlock(
            "heater_heatex", HeaterHeatex::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<LaunchPad> launch_pad = BLOCKS.registerBlock(
            "launch_pad", LaunchPad::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // --- mining laser ---

    public static final RegistryObject<MachineMiningLaser> machine_mining_laser = BLOCKS.registerBlock(
            "machine_mining_laser", MachineMiningLaser::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    /** Sandbags — the mining laser's flood dam ({@code buildDam}). Simplified full cube; the 1.12.2
     * connected/baked model is deferred. Registered here (not the generated ModBlocks) because the old
     * {@code BlockSandbags} isn't a BlockBase and the codegen doesn't parse it. */
    public static final RegistryObject<Block> sandbags = BLOCKS.registerBlock(
            "sandbags", Block::new, p -> p.strength(5.0F, 30.0F));

    static {
        tabItem("machine_battery", machine_battery);
        tabItem("machine_radgen", machine_radgen);
        tabItem("machine_minirtg", machine_minirtg);
        tabItem("rtg_polonium", rtg_polonium);
        ModTabs.addTo(HbmTab.MACHINES, machine_rtg_grey_item);
        tabItem("radsensor", radsensor);
        tabItem("red_cable", red_cable);
        tabItem("creative_generator", creative_generator);
        tabItem("machine_turbine", machine_turbine);
        tabItem("machine_large_turbine", machine_large_turbine);
        tabItem("machine_turbine_gas", machine_turbine_gas);
        tabItem("machine_turbofan", machine_turbofan);
        tabItem("machine_steam_engine", machine_steam_engine);
        tabItem("machine_pump_electric", machine_pump_electric);
        tabItem("machine_pump_steam", machine_pump_steam);
        tabItem("machine_stirling", machine_stirling);
        tabItem("machine_stirling_steel", machine_stirling_steel);
        tabItem("machine_stirling_creative", machine_stirling_creative);
         tabItem("machine_industrial_turbine", machine_industrial_turbine);
         tabItem("machine_condenser", machine_condenser);
         tabItem("machine_condenser_powered", machine_condenser_powered);
        tabItem("machine_zirnox", machine_zirnox);
        tabItem("struct_watz_core", struct_watz_core);
        tabItem("struct_torus_core", struct_torus_core);
        // Upstream puts the assembled reactors in the creative tab alongside their struct cores: the
        // struct core is the survival route (build the shape, then transmute), these two place the
        // finished multiblock in one go. Every other fusion machine was already here.
        tabItem("watz", watz);
        tabItem("fusion_torus", fusion_torus);
        tabItem("fusion_coupler", fusion_coupler);
        tabItem("fusion_collector", fusion_collector);
        tabItem("fusion_klystron", fusion_klystron);
        tabItem("fusion_klystron_creative", fusion_klystron_creative);
        tabItem("fusion_boiler", fusion_boiler);
        tabItem("fusion_breeder", fusion_breeder);
        tabItem("fusion_mhdt", fusion_mhdt);
        tabItem("fusion_plasma_forge", fusion_plasma_forge);
        tabItem("watz_pump", watz_pump);
        tabItem("watz_casing", watz_casing);
        tabItem("watz_casing_tooled", watz_casing_tooled);
        tabItem("fusion_component", fusion_component);
        tabItem("fusion_component_bscco_welded", fusion_component_bscco_welded);
        tabItem("fusion_component_blanket", fusion_component_blanket);
         tabItem("fusion_component_motor", fusion_component_motor);
         tabItem("machine_waste_drum", machine_waste_drum);
         tabItem("machine_storage_drum", machine_storage_drum);
        tabItem("fluid_duct_mk2", fluid_duct_mk2);
        tabItem("creative_fluid_source", creative_fluid_source);
         tabItem("machine_fluidtank", machine_fluidtank);
         tabItem("machine_uf6_tank", machine_uf6_tank);
         tabItem("machine_puf6_tank", machine_puf6_tank);
         tabItem("machine_bigasstank", machine_bigasstank);
         tabItem("machine_bat9000", machine_bat9000);
        tabItem("fluid_valve", fluid_valve);
        tabItem("machine_drain", machine_drain);
        tabItem("machine_electric_furnace_off", machine_electric_furnace_off);
        tabItem("machine_furnace_brick_off", machine_furnace_brick_off);
        tabItem("furnace_iron", furnace_iron);
        tabItem("furnace_steel", furnace_steel);
        tabItem("machine_shredder", machine_shredder);
         tabItem("machine_epress", machine_epress);
         tabItem("machine_press", machine_press);
         tabItem("machine_ammo_press", machine_ammo_press);
         tabItem("machine_sawmill", machine_sawmill);
        tabItem("machine_assembly_machine", machine_assembly_machine);
         tabItem("machine_precass", machine_precass);
          tabItem("machine_chemical_plant", machine_chemical_plant);
         tabItem("machine_electrolyser", machine_electrolyser);
        tabItem("machine_mixer", machine_mixer);
        tabItem("machine_exposure_chamber", machine_exposure_chamber);
        tabItem("machine_blast_furnace", machine_blast_furnace);
        tabItem("machine_purex", machine_purex);
        tabItem("machine_well", machine_well);
        tabItem("machine_pumpjack", machine_pumpjack);
        tabItem("machine_fracking_tower", machine_fracking_tower);
        tabItem("machine_vacuum_distill", machine_vacuum_distill);
        tabItem("machine_catalytic_reformer", machine_catalytic_reformer);
        tabItem("machine_catalytic_cracker", machine_catalytic_cracker);
        tabItem("machine_hydrotreater", machine_hydrotreater);
        tabItem("machine_coker", machine_coker);
        tabItem("machine_furnace_combination", machine_furnace_combination);
        tabItem("machine_pyrooven", machine_pyrooven);
        tabItem("machine_combustion_engine", machine_combustion_engine);
        tabItem("refueler", refueler);
        tabItem("machine_compressor", machine_compressor);
        tabItem("machine_diesel", machine_diesel);
        tabItem("machine_flare", machine_flare);
        tabItem("machine_fraction_tower", machine_fraction_tower);
        tabItem("fraction_spacer", fraction_spacer);
        tabItem("machine_liquefactor", machine_liquefactor);
        tabItem("machine_solidifier", machine_solidifier);
        tabItem("machine_refinery", machine_refinery);
         tabItem("heat_boiler", machine_boiler);
         tabItem("machine_industrial_boiler", machine_industrial_boiler);
        tabItem("heater_oilburner", heater_oilburner);
        tabItem("machine_soldering_station", machine_soldering_station);
        tabItem("machine_arc_welder", machine_arc_welder);
        tabItem("machine_intake", machine_intake);
        tabItem("fan", fan);
          tabItem("machine_crystallizer", machine_crystallizer);
          tabItem("machine_centrifuge", machine_centrifuge);
          tabItem("machine_cyclotron", machine_cyclotron);
          tabItem("machine_gascent", machine_gascent);
         tabItem("machine_deuterium_extractor", machine_deuterium_extractor);
         tabItem("machine_deuterium_tower", machine_deuterium_tower);
        tabItem("machine_silex", machine_silex);
        tabItem("machine_fel", machine_fel);
        tabItem("machine_crucible", machine_crucible);
          tabItem("heater_firebox", heater_firebox);
          tabItem("heater_oven", heater_oven);
           tabItem("heater_electric", heater_electric);
          tabItem("heater_heatex", heater_heatex);
         tabItem("machine_wood_burner", machine_wood_burner);
        tabItem("foundry_channel", foundry_channel);
        tabItem("foundry_mold", foundry_mold);
        tabItem("foundry_basin", foundry_basin);
        tabItem("foundry_outlet", foundry_outlet);
        tabItem("foundry_slagtap", foundry_slagtap);
        tabItem(HbmTab.MISSILES, "launch_pad", launch_pad);
        // Mining laser item carries the old addInformation tooltip and the legacy Forge renderer bridge.
        RegistryObject<BlockItem> miningLaserItem = ITEMS.registerItem("machine_mining_laser",
                p -> new dev.golem.core.world.CompatBlockItem(machine_mining_laser.get(), p) {
                    @Override
                    protected void appendCompatHoverText(net.minecraft.world.item.ItemStack stack,
                                                         dev.golem.core.world.CompatTooltipContext context,
                                                         java.util.List<net.minecraft.network.chat.Component> tooltip,
                                                         net.minecraft.world.item.TooltipFlag flag) {
                        tooltip.add(net.minecraft.network.chat.Component.translatable("desc.mininglaser1"));
                        tooltip.add(net.minecraft.network.chat.Component.translatable("desc.mininglaser2"));
                    }
                }, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.MACHINES, miningLaserItem);
        tabItem(HbmTab.BLOCKS, "sandbags", sandbags);
    }

    /**
     * Properties for folded multiblocks: their invisible full-cube cells must not count as
     * suffocating/view-blocking, or 26.1's push-out-of-block logic shoves entities that overlap them
     * (the 1.12.2 original set {@code isNormalCube=false} for the same reason).
     */
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties dummyable(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    private static void tabItem(String name, RegistryObject<? extends Block> block) {
        tabItem(HbmTab.MACHINES, name, block);
    }

    private static void tabItem(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
