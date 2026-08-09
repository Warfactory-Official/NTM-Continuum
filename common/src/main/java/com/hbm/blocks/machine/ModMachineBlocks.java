package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
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

    public static final RegistryObject<BlockCable> red_cable = BLOCKS.registerBlock(
            "red_cable", BlockCable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<BlockCreativeGenerator> creative_generator = BLOCKS.registerBlock(
            "creative_generator", BlockCreativeGenerator::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL));

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

    // --- scaffold machines: placeable, directional, BlockEntity-backed; logic deferred (§18) ---

    public static final RegistryObject<MachineElectricFurnace> machine_electric_furnace_off = BLOCKS.registerBlock(
            "machine_electric_furnace_off", MachineElectricFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /** Shredder — a plain JSON-model cube (no BER), unlike the multiblocks below. */
    public static final RegistryObject<MachineShredder> machine_shredder = BLOCKS.registerBlock(
            "machine_shredder", MachineShredder::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    // Multiblock (BlockDummyable) machines — invisible core+dummy blocks drawn by a BlockEntityRenderer.
    public static final RegistryObject<MachineEPress> machine_epress = BLOCKS.registerBlock(
            "machine_epress", MachineEPress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachinePress> machine_press = BLOCKS.registerBlock(
            "machine_press", MachinePress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineAssemblyMachine> machine_assembly_machine = BLOCKS.registerBlock(
            "machine_assembly_machine", MachineAssemblyMachine::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<MachineChemicalPlant> machine_chemical_plant = BLOCKS.registerBlock(
            "machine_chemical_plant", MachineChemicalPlant::new,
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

    /** Fluid Burner — burns flammable fluids into an IHeatSource reservoir. */
    public static final RegistryObject<HeaterOilburner> heater_oilburner = BLOCKS.registerBlock(
            "heater_oilburner", HeaterOilburner::new,
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

    public static final RegistryObject<MachineCrystallizer> machine_crystallizer = BLOCKS.registerBlock(
            "machine_crystallizer", MachineCrystallizer::new,
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

    public static final RegistryObject<HeaterElectric> heater_electric = BLOCKS.registerBlock(
            "heater_electric", HeaterElectric::new,
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
        tabItem("red_cable", red_cable);
        tabItem("creative_generator", creative_generator);
        tabItem("fluid_duct_mk2", fluid_duct_mk2);
        tabItem("creative_fluid_source", creative_fluid_source);
        tabItem("machine_fluidtank", machine_fluidtank);
        tabItem("machine_electric_furnace_off", machine_electric_furnace_off);
        tabItem("machine_shredder", machine_shredder);
        tabItem("machine_epress", machine_epress);
        tabItem("machine_press", machine_press);
        tabItem("machine_assembly_machine", machine_assembly_machine);
        tabItem("machine_chemical_plant", machine_chemical_plant);
        tabItem("machine_well", machine_well);
        tabItem("machine_pumpjack", machine_pumpjack);
        tabItem("machine_fracking_tower", machine_fracking_tower);
        tabItem("machine_vacuum_distill", machine_vacuum_distill);
        tabItem("machine_catalytic_reformer", machine_catalytic_reformer);
        tabItem("machine_catalytic_cracker", machine_catalytic_cracker);
        tabItem("machine_hydrotreater", machine_hydrotreater);
        tabItem("machine_coker", machine_coker);
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
        tabItem("heater_oilburner", heater_oilburner);
        tabItem("machine_soldering_station", machine_soldering_station);
        tabItem("machine_crystallizer", machine_crystallizer);
        tabItem("machine_crucible", machine_crucible);
        tabItem("heater_firebox", heater_firebox);
        tabItem("heater_electric", heater_electric);
        tabItem("foundry_channel", foundry_channel);
        tabItem("foundry_mold", foundry_mold);
        tabItem("foundry_basin", foundry_basin);
        tabItem("foundry_outlet", foundry_outlet);
        tabItem("foundry_slagtap", foundry_slagtap);
        tabItem(HbmTab.MISSILES, "launch_pad", launch_pad);
        // Mining laser item carries the old addInformation tooltip (26.1 blocks have no tooltip hook).
        RegistryObject<BlockItem> miningLaserItem = ITEMS.registerItem("machine_mining_laser",
                p -> new BlockItem(machine_mining_laser.get(), p) {
                    @Override
                    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                                net.minecraft.world.item.Item.TooltipContext context,
                                                net.minecraft.world.item.component.TooltipDisplay display,
                                                java.util.function.Consumer<net.minecraft.network.chat.Component> adder,
                                                net.minecraft.world.item.TooltipFlag flag) {
                        adder.accept(net.minecraft.network.chat.Component.translatable("desc.mininglaser1"));
                        adder.accept(net.minecraft.network.chat.Component.translatable("desc.mininglaser2"));
                    }
                }, p -> p.useBlockDescriptionPrefix());
        ModTabs.addTo(HbmTab.MACHINES, miningLaserItem);
        tabItem(HbmTab.BLOCKS, "sandbags", sandbags);
    }

    /**
     * Properties for BlockDummyable multiblocks: their invisible full-cube cells must not count as
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
