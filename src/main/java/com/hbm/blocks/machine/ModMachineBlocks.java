package com.hbm.blocks.machine;

import com.hbm.blocks.generic.BlockDynamicSlag;
import com.hbm.blocks.machine.fusion.MachineFusionBoiler;
import com.hbm.blocks.machine.fusion.MachineFusionBreeder;
import com.hbm.blocks.machine.fusion.MachineFusionCollector;
import com.hbm.blocks.machine.fusion.MachineFusionCoupler;
import com.hbm.blocks.machine.fusion.MachineFusionKlystron;
import com.hbm.blocks.machine.fusion.MachineFusionKlystronCreative;
import com.hbm.blocks.machine.fusion.MachineFusionMHDT;
import com.hbm.blocks.machine.fusion.MachineFusionPlasmaForge;
import com.hbm.blocks.machine.fusion.MachineFusionTorus;
import com.hbm.items.block.CarriedTankInfo;
import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockToolConvertible;
import com.hbm.blocks.machine.albion.BlockPABeamline;
import com.hbm.blocks.machine.albion.BlockPADetector;
import com.hbm.blocks.machine.albion.BlockPADipole;
import com.hbm.blocks.machine.albion.BlockPAQuadrupole;
import com.hbm.blocks.machine.albion.BlockPARFC;
import com.hbm.blocks.machine.albion.BlockPASource;
import com.hbm.blocks.machine.struct.BlockStruct;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.struct.TileEntityWatzStruct;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.FluidValve;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.bomb.LaunchPad;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.CarriedDrops;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.network.RadioAUTOCAL;
import com.hbm.blocks.network.RadioTelex;
import com.hbm.blocks.network.RadioTorchController;
import com.hbm.blocks.network.RadioTorchCounter;
import com.hbm.blocks.network.RadioTorchLogic;
import com.hbm.blocks.network.RadioTorchReader;
import com.hbm.blocks.network.RadioTorchReceiver;
import com.hbm.blocks.network.RadioTorchSender;
import dev.golem.core.dejson.Shape;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registry for HBM machine blocks (blocks backed by a BlockEntity). Separate from the auto-generated
 * {@link com.hbm.blocks.ModBlocks} (which holds the simple blocks) so codegen never clobbers them.
 *
 * <p>Each block's model, blockstate, loot and tool tags (pickaxe included) are stated on its registration
 * (Reg; emitted by Dejson at build time). The block items register in the trailing static block, in
 * creative-tab order.
 */
public final class ModMachineBlocks {

    private ModMachineBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * Folded multiblocks: the core and its cells are one block told apart by {@code cell}, and the renderer on
     * the core draws both, so every variant is the shared empty placeholder (no item model of its own).
     */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));
    /** A renderer-drawn block turned four ways. */
    private static final Preset RENDERED_FACING = Presets.rendered(
            Variants.uniform("facing", "north", "east", "south", "west"));
    /** A renderer-drawn block with one state. */
    private static final Preset RENDERED_SIMPLE = Presets.rendered(Variants.simple());
    /** Particle-accelerator parts: a block_steel cube (no item model of its own) that drops itself. */
    private static final Preset PA_PART = Presets.cubeAll("hbm:blocks/block_steel").noItem()
            .and(handle -> handle.loot(Loot.self()));

    public static final RegistryObject<MachineSatLink> machine_satlink = REG.<MachineSatLink>block(
            "machine_satlink", MachineSatLink::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.cubeAll("hbm:blocks/machine_satlinker_side")).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockMachineBattery> machine_battery = REG.<BlockMachineBattery>block(
            "machine_battery", BlockMachineBattery::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeAll("hbm:blocks/battery_side_alt")).states(Variants.simple()).blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineRadGen> machine_radgen = REG.<MachineRadGen>block(
            "machine_radgen", MachineRadGen::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineMiniRTG> machine_minirtg = REG.<MachineMiniRTG>block(
            "machine_minirtg", MachineMiniRTG::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineMiniRTG> rtg_polonium = REG.<MachineMiniRTG>block(
            "rtg_polonium", MachineMiniRTG::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineRTG> machine_rtg_grey = REG.<MachineRTG>block(
            "machine_rtg_grey", MachineRTG::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> machine_rtg_grey_item = REG.<MachineRTG, BlockItem>blockItem(
            machine_rtg_grey,
            (block, p) -> new CompatBlockItem(block, p) {
                @Override
                public Component getName(ItemStack stack) {
                    return Component.literal("RT Generator");
                }
            }, p -> Compat.blockDescription(p));
    public static final RegistryObject<RadSensor> radsensor = REG.<RadSensor>block(
            "radsensor", RadSensor::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/radsensor", "hbm:blocks/radsensor_side", "hbm:blocks/radsensor"))
                    .states(Variants.simple())
                    .blockItemDirect())
            .loot(Loot.self()).tag(BlockTag.PICKAXE);







    public static final RegistryObject<BlockCargoElevator> cargo_elevator = REG.<BlockCargoElevator>block(
            "cargo_elevator", BlockCargoElevator::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().dynamicShape())
            .model(d -> d.models(Models.parented("minecraft:block/block", Slots.of("particle", "hbm:blocks/block_steel")))
                    .states(Variants.simple())).tag(BlockTag.PICKAXE);







    public static final RegistryObject<GeigerCounter> geiger = REG.<GeigerCounter>block(
            "geiger", GeigerCounter::new,
            // 1.7 GeigerCounter(Material.iron): no drop override, so it drops itself, pickaxe-harvested.
            p -> p.strength(15.0F, 0.25F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);



    public static final RegistryObject<BlockCable> red_cable = REG.<BlockCable>block(
            "red_cable", BlockCable::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .loot(Loot.self())
            .andThen(h -> States.multipart(h.namespace(), h.path())
                    .part(new Variant("hbm:block/machine_dummy_empty"))
                    .register())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockCreativeGenerator> creative_generator = REG.<BlockCreativeGenerator>block(
            "creative_generator", BlockCreativeGenerator::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL))
            .model(d -> d.models(Models.cubeAll("hbm:blocks/machine_generator")).states(Variants.simple()).blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineLargeTurbine> machine_large_turbine = REG.<MachineLargeTurbine>block(
            "machine_large_turbine", MachineLargeTurbine::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY)).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineTurbineGas> machine_turbine_gas = REG.<MachineTurbineGas>block(
            "machine_turbine_gas", MachineTurbineGas::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY)).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineTurbofan> machine_turbofan = REG.<MachineTurbofan>block(
            "machine_turbofan", MachineTurbofan::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY)).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineSteamEngine> machine_steam_engine = REG.<MachineSteamEngine>block(
            "machine_steam_engine", MachineSteamEngine::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY)).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachinePump> machine_pump_electric = REG.<MachinePump>block(
            "machine_pump_electric", MachinePump::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachinePump> machine_pump_steam = REG.<MachinePump>block(
            "machine_pump_steam", p -> new MachinePump(p, true),
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineStirling> machine_stirling = REG.<MachineStirling>block(
            "machine_stirling", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineStirling> machine_stirling_steel = REG.<MachineStirling>block(
            "machine_stirling_steel", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineStirling> machine_stirling_creative = REG.<MachineStirling>block(
            "machine_stirling_creative", MachineStirling::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineIndustrialTurbine> machine_industrial_turbine = REG.<MachineIndustrialTurbine>block(
            "machine_industrial_turbine", MachineIndustrialTurbine::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY)).states(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCondenser> machine_condenser = REG.<MachineCondenser>block(
            "machine_condenser", MachineCondenser::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeAll("hbm:blocks/condenser")).states(Variants.simple()).blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineCondenserPowered> machine_condenser_powered = REG.<MachineCondenserPowered>block(
            "machine_condenser_powered", MachineCondenserPowered::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineZirnox> machine_zirnox = REG.<MachineZirnox>block(
            "machine_zirnox", MachineZirnox::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    /** Accident-only replacement for a melted-down ZIRNOX; has no block item. */
    public static final RegistryObject<MachineZirnoxDestroyed> zirnox_destroyed = REG.<MachineZirnoxDestroyed>block(
            "zirnox_destroyed", MachineZirnoxDestroyed::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .loot(Loot.stacks("hbm:concrete_smooth", 6, "hbm:pipe_steel", 4, "hbm:steel_grate", 2, "hbm:debris_metal", 6, "hbm:debris_graphite", 2, "hbm:falloutitem", 4))
            .tag(BlockTag.PICKAXE)
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("facing=north,cell=false", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=north,cell=true", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=east,cell=false", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=east,cell=true", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=south,cell=false", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=south,cell=true", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=west,cell=false", new Variant("hbm:block/zirnox_destroyed"))
                    .variant("facing=west,cell=true", new Variant("hbm:block/zirnox_destroyed"))
                    .register());
    public static final RegistryObject<MachineWasteDrum> machine_waste_drum = REG.<MachineWasteDrum>block(
            "machine_waste_drum", MachineWasteDrum::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/waste_drum", "hbm:blocks/waste_drum_side", "hbm:blocks/waste_drum")).states(Variants.simple()).blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineStorageDrum> machine_storage_drum = REG.<MachineStorageDrum>block(
            "machine_storage_drum", MachineStorageDrum::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false))
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Multiblock structure components (hand-built machines: Watz, fusion torus)
    // The plain forms are craftable; the upgraded forms exist only as the output of an in-world tool
    // conversion (NTMToolHandler), and the reactors' structure patterns only accept the upgraded ones.

    public static final RegistryObject<BlockToolConvertible> watz_casing = REG.<BlockToolConvertible>block(
            "watz_casing", BlockToolConvertible::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<Block> watz_casing_tooled = REG.<Block>block(
            "watz_casing_tooled", Block::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<BlockToolConvertible> fusion_component = REG.<BlockToolConvertible>block(
            "fusion_component", BlockToolConvertible::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<Block> fusion_component_bscco_welded = REG.<Block>block(
            "fusion_component_bscco_welded", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<Block> fusion_component_blanket = REG.<Block>block(
            "fusion_component_blanket", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<Block> fusion_component_motor = REG.<Block>block(
            "fusion_component_motor", Block::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    /** The Watz reactor itself has no block item: it only ever assembles from a struct core. */
    public static final RegistryObject<Watz> watz = REG.<Watz>block(
            "watz", Watz::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<WatzPump> watz_pump = REG.<WatzPump>block(
            "watz_pump", WatzPump::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    /**
     * The fusion torus. 1.7 registers it with a creative tab and {@code MachineFusionTorus} has no drop override,
     * so the {@code BlockDummyable} harvest drops the block itself (it is not a Watz-style "struct core only" block).
     */
    public static final RegistryObject<MachineFusionTorus> fusion_torus = REG.<MachineFusionTorus>block(
            "fusion_torus", MachineFusionTorus::new,
            p -> dummyable(p.strength(5.0F, 60.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    // The fusion family: what feeds the torus and what consumes its plasma.
    public static final RegistryObject<MachineFusionCoupler> fusion_coupler = REG.<MachineFusionCoupler>block(
            "fusion_coupler", MachineFusionCoupler::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionCollector> fusion_collector = REG.<MachineFusionCollector>block(
            "fusion_collector", MachineFusionCollector::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionKlystron> fusion_klystron = REG.<MachineFusionKlystron>block(
            "fusion_klystron", MachineFusionKlystron::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionKlystronCreative> fusion_klystron_creative = REG.<MachineFusionKlystronCreative>block(
            "fusion_klystron_creative", MachineFusionKlystronCreative::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionBoiler> fusion_boiler = REG.<MachineFusionBoiler>block(
            "fusion_boiler", MachineFusionBoiler::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionBreeder> fusion_breeder = REG.<MachineFusionBreeder>block(
            "fusion_breeder", MachineFusionBreeder::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineFusionMHDT> fusion_mhdt = REG.<MachineFusionMHDT>block(
            "fusion_mhdt", MachineFusionMHDT::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<MachineFusionPlasmaForge> fusion_plasma_forge = REG.<MachineFusionPlasmaForge>block(
            "fusion_plasma_forge", MachineFusionPlasmaForge::new,
            p -> dummyable(p.strength(5.0F, 60.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Seed blocks for the hand-built multiblocks. */
    public static final RegistryObject<BlockStruct> struct_watz_core = REG.<BlockStruct>block(
            "struct_watz_core",
            p -> new BlockStruct(p, TileEntityWatzStruct::new, () -> ModBlockEntities.STRUCT_WATZ_CORE.get()),
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).lightLevel(state -> 15))
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockStruct> struct_torus_core = REG.<BlockStruct>block(
            "struct_torus_core",
            p -> new BlockStruct(p, TileEntityFusionTorusStruct::new,
                    () -> ModBlockEntities.STRUCT_TORUS_CORE.get()),
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).lightLevel(state -> 15))
            .transform(Presets.cubeAll())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    // Fluid network blocks (duct conductor + infinite source + receiving tank)

    public static final RegistryObject<BlockCreativeFluidSource> creative_fluid_source = REG.<BlockCreativeFluidSource>block(
            "creative_fluid_source", BlockCreativeFluidSource::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL))
            .model(d -> d.models(Models.cubeAll("hbm:blocks/foundry_tank_side")).states(Variants.simple()).blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineFluidTank> machine_fluidtank = REG.<MachineFluidTank>block(
            "machine_fluidtank", MachineFluidTank::new, // resistance: 1.7 setResistance(20) = 60/5 = 12 against explosions
            p -> dummyable(p.strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(CarriedDrops.SURVIVING)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineUF6Tank> machine_uf6_tank = REG.<MachineUF6Tank>block(
            "machine_uf6_tank", MachineUF6Tank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.nothing()) // 1.7 MachineUF6Tank.getItemDropped returns null
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachinePuF6Tank> machine_puf6_tank = REG.<MachinePuF6Tank>block(
            "machine_puf6_tank", MachinePuF6Tank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.nothing()) // 1.7 MachinePuF6Tank.getItemDropped returns null
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineBigAssTank> machine_bigasstank = REG.<MachineBigAssTank>block(
            "machine_bigasstank", MachineBigAssTank::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineBigAssTank9000> machine_bat9000 = REG.<MachineBigAssTank9000>block(
            "machine_bat9000", MachineBigAssTank9000::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FluidValve> fluid_valve = REG.<FluidValve>block(
            "fluid_valve", FluidValve::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_off", Models.cubeAll("hbm:blocks/fluid_valve_off"))
                    .and("_on", Models.cubeAll("hbm:blocks/fluid_valve_on")))
                    .states(Variants.select("open", "false", "_off", "true", "_on")))
            .loot(Loot.self())
            .icon(d -> d.shares("hbm:block/fluid_valve_off"))
            .tag(BlockTag.PICKAXE);


    public static final RegistryObject<MachineDrain> machine_drain = REG.<MachineDrain>block(
            "machine_drain", MachineDrain::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Placeable, directional, BlockEntity-backed machines

    public static final RegistryObject<MachineElectricFurnace> machine_electric_furnace_off = REG.<MachineElectricFurnace>block(
            "machine_electric_furnace_off", MachineElectricFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .loot(Loot.self())
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("facing=north,lit=false", new Variant("hbm:block/machine_electric_furnace_off"))
                    .variant("facing=east,lit=false", new Variant("hbm:block/machine_electric_furnace_off", 0, 90, false))
                    .variant("facing=south,lit=false", new Variant("hbm:block/machine_electric_furnace_off", 0, 180, false))
                    .variant("facing=west,lit=false", new Variant("hbm:block/machine_electric_furnace_off", 0, 270, false))
                    .variant("facing=north,lit=true", new Variant("hbm:block/machine_electric_furnace_on"))
                    .variant("facing=east,lit=true", new Variant("hbm:block/machine_electric_furnace_on", 0, 90, false))
                    .variant("facing=south,lit=true", new Variant("hbm:block/machine_electric_furnace_on", 0, 180, false))
                    .variant("facing=west,lit=true", new Variant("hbm:block/machine_electric_furnace_on", 0, 270, false))
                    .register())
            .icon(d -> d.shares("hbm:block/machine_electric_furnace_off"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineFurnaceBrick> machine_furnace_brick_off = REG.<MachineFurnaceBrick>block(
            "machine_furnace_brick_off", MachineFurnaceBrick::new,
            // 1.7 MachineBrickFurnace(Material.iron) has no drop override: it drops itself (the lit 1.7 block
            // dropped the lit item; the port folds both into one block with a lit state, so it drops this one).
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("facing=north,lit=false", new Variant("hbm:block/machine_furnace_brick_off"))
                    .variant("facing=east,lit=false", new Variant("hbm:block/machine_furnace_brick_off", 0, 90, false))
                    .variant("facing=south,lit=false", new Variant("hbm:block/machine_furnace_brick_off", 0, 180, false))
                    .variant("facing=west,lit=false", new Variant("hbm:block/machine_furnace_brick_off", 0, 270, false))
                    .variant("facing=north,lit=true", new Variant("hbm:block/machine_furnace_brick_on"))
                    .variant("facing=east,lit=true", new Variant("hbm:block/machine_furnace_brick_on", 0, 90, false))
                    .variant("facing=south,lit=true", new Variant("hbm:block/machine_furnace_brick_on", 0, 180, false))
                    .variant("facing=west,lit=true", new Variant("hbm:block/machine_furnace_brick_on", 0, 270, false))
                    .register())
            .icon(d -> d.shares("hbm:block/machine_furnace_brick_off"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineFurnaceIron> furnace_iron = REG.<MachineFurnaceIron>block(
            "furnace_iron", MachineFurnaceIron::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineFurnaceSteel> furnace_steel = REG.<MachineFurnaceSteel>block(
            "furnace_steel", MachineFurnaceSteel::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Shredder: a plain JSON-model cube (no BER), unlike the multiblocks below. */
    public static final RegistryObject<MachineShredder> machine_shredder = REG.<MachineShredder>block(
            "machine_shredder", MachineShredder::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.parented("minecraft:block/cube", Slots.of("down", "hbm:blocks/machine_shredder_bottom_alt")
                    .and("up", "hbm:blocks/machine_shredder_top_alt")
                    .and("north", "hbm:blocks/machine_shredder_front_alt")
                    .and("south", "hbm:blocks/machine_shredder_front_alt")
                    .and("west", "hbm:blocks/machine_shredder_side_alt")
                    .and("east", "hbm:blocks/machine_shredder_side_alt")
                    .and("particle", "hbm:blocks/machine_shredder_front_alt")))
                    .states(Variants.facing())
                    .blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Folded multiblock machines: invisible core+cell blocks drawn by a BlockEntityRenderer.
    public static final RegistryObject<MachineEPress> machine_epress = REG.<MachineEPress>block(
            "machine_epress", MachineEPress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachinePress> machine_press = REG.<MachinePress>block(
            "machine_press", MachinePress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineAmmoPress> machine_ammo_press = REG.<MachineAmmoPress>block(
            "machine_ammo_press", MachineAmmoPress::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineSawmill> machine_sawmill = REG.<MachineSawmill>block(
            "machine_sawmill", MachineSawmill::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineAssemblyMachine> machine_assembly_machine = REG.<MachineAssemblyMachine>block(
            "machine_assembly_machine", MachineAssemblyMachine::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineAssemblyFactory> machine_assembly_factory = REG.<MachineAssemblyFactory>block(
            "machine_assembly_factory", MachineAssemblyFactory::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachinePrecAss> machine_precass = REG.<MachinePrecAss>block(
            "machine_precass", MachinePrecAss::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineChemicalPlant> machine_chemical_plant = REG.<MachineChemicalPlant>block(
            "machine_chemical_plant", MachineChemicalPlant::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineChemicalFactory> machine_chemical_factory = REG.<MachineChemicalFactory>block(
            "machine_chemical_factory", MachineChemicalFactory::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineElectrolyser> machine_electrolyser = REG.<MachineElectrolyser>block(
            "machine_electrolyser", MachineElectrolyser::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineMixer> machine_mixer = REG.<MachineMixer>block(
            "machine_mixer", MachineMixer::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineExposureChamber> machine_exposure_chamber = REG.<MachineExposureChamber>block(
            "machine_exposure_chamber", MachineExposureChamber::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineBlastFurnace> machine_blast_furnace = REG.<MachineBlastFurnace>block(
            "machine_blast_furnace", MachineBlastFurnace::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachinePUREX> machine_purex = REG.<MachinePUREX>block(
            "machine_purex", MachinePUREX::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineRockMill> machine_rockmill = REG.<MachineRockMill>block(
            "machine_rockmill", MachineRockMill::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineSuperComputer> machine_supercomputer = REG.<MachineSuperComputer>block(
            "machine_supercomputer", MachineSuperComputer::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineTapeDrive> machine_tape_drive = REG.<MachineTapeDrive>block(
            "machine_tape_drive", MachineTapeDrive::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Diesel Generator: burns combustible fluids into HE. */
    public static final RegistryObject<MachineDiesel> machine_diesel = REG.<MachineDiesel>block(
            "machine_diesel", MachineDiesel::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Flare Stack: vents or burns gas for power; 12-tall 3×3 tower. */
    public static final RegistryObject<MachineGasFlare> machine_flare = REG.<MachineGasFlare>block(
            "machine_flare", MachineGasFlare::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Brick and industrial chimneys: take the smoke ducted to them and vent it high up. */
    public static final RegistryObject<MachineChimneyBrick> chimney_brick = REG.<MachineChimneyBrick>block(
            "chimney_brick", MachineChimneyBrick::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineChimneyIndustrial> chimney_industrial = REG.<MachineChimneyIndustrial>block(
            "chimney_industrial", MachineChimneyIndustrial::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Fractioning Tower: splits a hot fraction into two; stacks 3 apart via the spacer. */
    public static final RegistryObject<MachineFractionTower> machine_fraction_tower = REG.<MachineFractionTower>block(
            "machine_fraction_tower", MachineFractionTower::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Fractioning Tower Separator: a logic-less spacer raising the tower stack. */
    public static final RegistryObject<FractionSpacer> fraction_spacer = REG.<FractionSpacer>block(
            "fraction_spacer", FractionSpacer::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Liquefactor: items to fluid. */
    public static final RegistryObject<MachineLiquefactor> machine_liquefactor = REG.<MachineLiquefactor>block(
            "machine_liquefactor", MachineLiquefactor::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Solidifier: fluid to items. */
    public static final RegistryObject<MachineSolidifier> machine_solidifier = REG.<MachineSolidifier>block(
            "machine_solidifier", MachineSolidifier::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Refinery: fractionates HOT oils into 4 products; 3×3×9 tower. */
    public static final RegistryObject<MachineRefinery> machine_refinery = REG.<MachineRefinery>block(
            "machine_refinery", MachineRefinery::new, // resistance: 1.7 setResistance(20) = 60/5 = 12 against explosions
            p -> dummyable(p.strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(CarriedDrops.SURVIVING)
            .tag(BlockTag.PICKAXE);

    /** Heat Boiler (old id {@code heat_boiler}): the OIL to HOTOIL and water to steam converter. */
    public static final RegistryObject<MachineHeatBoiler> machine_boiler = REG.<MachineHeatBoiler>block(
            "heat_boiler", MachineHeatBoiler::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Industrial heat boiler: the five-port, high-capacity variant. */
    public static final RegistryObject<MachineHeatBoilerIndustrial> machine_industrial_boiler = REG.<MachineHeatBoilerIndustrial>block(
            "machine_industrial_boiler", MachineHeatBoilerIndustrial::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Fluid Burner: burns flammable fluids into an IHeatSource reservoir. */
    public static final RegistryObject<HeaterOilburner> heater_oilburner = REG.<HeaterOilburner>block(
            "heater_oilburner", HeaterOilburner::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Wood Burner: solid fuel or WOODOIL generator with a 100k HE buffer. */
    public static final RegistryObject<MachineWoodBurner> machine_wood_burner = REG.<MachineWoodBurner>block(
            "machine_wood_burner", MachineWoodBurner::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Extraction drills
    public static final RegistryObject<MachineOilWell> machine_well = REG.<MachineOilWell>block(
            "machine_well", MachineOilWell::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(CarriedDrops.SURVIVING)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachinePumpjack> machine_pumpjack = REG.<MachinePumpjack>block(
            "machine_pumpjack", MachinePumpjack::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(CarriedDrops.SURVIVING)
            .tag(BlockTag.PICKAXE);

    // Advanced oil machines

    public static final RegistryObject<MachineFrackingTower> machine_fracking_tower = REG.<MachineFrackingTower>block(
            "machine_fracking_tower", MachineFrackingTower::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(CarriedDrops.SURVIVING)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineVacuumDistill> machine_vacuum_distill = REG.<MachineVacuumDistill>block(
            "machine_vacuum_distill", MachineVacuumDistill::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineCatalyticReformer> machine_catalytic_reformer = REG.<MachineCatalyticReformer>block(
            "machine_catalytic_reformer", MachineCatalyticReformer::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineCatalyticCracker> machine_catalytic_cracker = REG.<MachineCatalyticCracker>block(
            "machine_catalytic_cracker", MachineCatalyticCracker::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineHydrotreater> machine_hydrotreater = REG.<MachineHydrotreater>block(
            "machine_hydrotreater", MachineHydrotreater::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineCoker> machine_coker = REG.<MachineCoker>block(
            "machine_coker", MachineCoker::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineFurnaceCombination> machine_furnace_combination = REG.<MachineFurnaceCombination>block(
            "machine_furnace_combination", MachineFurnaceCombination::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachinePyroOven> machine_pyrooven = REG.<MachinePyroOven>block(
            "machine_pyrooven", MachinePyroOven::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineAutosaw> machine_autosaw = REG.<MachineAutosaw>block(
            "machine_autosaw", MachineAutosaw::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineCombustionEngine> machine_combustion_engine = REG.<MachineCombustionEngine>block(
            "machine_combustion_engine", MachineCombustionEngine::new,
            p -> dummyable(p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockRefueler> refueler = REG.<BlockRefueler>block(
            "refueler", BlockRefueler::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Compressor: raises fluid pressure; the gate the vacuum refinery (OIL @2) + hydrotreater (H @1) need. */
    public static final RegistryObject<MachineCompressor> machine_compressor = REG.<MachineCompressor>block(
            "machine_compressor", MachineCompressor::new,
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineTowerSmall> machine_tower_small = REG.<MachineTowerSmall>block(
            "machine_tower_small", MachineTowerSmall::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 20.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineTowerLarge> machine_tower_large = REG.<MachineTowerLarge>block(
            "machine_tower_large", MachineTowerLarge::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 20.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDecon> decon = REG.<BlockDecon>block(
            "decon", BlockDecon::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 20.0F).sound(SoundType.METAL).noOcclusion())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/decon_side", "hbm:blocks/decon_side",
                            "hbm:blocks/decon_top"))
                    .states(Variants.simple())
                    .blockItem())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Solar boiler and the mirrors that heat it. */
    public static final RegistryObject<MachineSolarBoiler> machine_solar_boiler = REG.<MachineSolarBoiler>block(
            "machine_solar_boiler", MachineSolarBoiler::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<SolarMirror> solar_mirror = REG.<SolarMirror>block(
            "solar_mirror", SolarMirror::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Compact pressure compressor; same processing/menu path, with the old six-port ring footprint. */
    public static final RegistryObject<MachineCompressorCompact> machine_compressor_compact = REG.<MachineCompressorCompact>block(
            "machine_compressor_compact", MachineCompressorCompact::new,
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineSolderingStation> machine_soldering_station = REG.<MachineSolderingStation>block(
            "machine_soldering_station", MachineSolderingStation::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Large arc furnace. */
    public static final RegistryObject<MachineArcFurnaceLarge> machine_arc_furnace = REG.<MachineArcFurnaceLarge>block(
            "machine_arc_furnace", MachineArcFurnaceLarge::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineArcWelder> machine_arc_welder = REG.<MachineArcWelder>block(
            "machine_arc_welder", MachineArcWelder::new,
            p -> dummyable(p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineIntake> machine_intake = REG.<MachineIntake>block(
            "machine_intake", p -> new MachineIntake(p, () -> ModBlockEntities.MACHINE_INTAKE.get()),
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY))
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"), Variants.uniform("cell", "false", "true"))))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineFan> fan = REG.<MachineFan>block(
            "fan", p -> new MachineFan(p, () -> ModBlockEntities.MACHINE_FAN.get()),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.EMPTY))
                    .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineCrystallizer> machine_crystallizer = REG.<MachineCrystallizer>block(
            "machine_crystallizer", MachineCrystallizer::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Standard item centrifuge: a 1×1×4 tower with four output streams. */
    public static final RegistryObject<MachineCentrifuge> machine_centrifuge = REG.<MachineCentrifuge>block(
            "machine_centrifuge", MachineCentrifuge::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCyclotron> machine_cyclotron = REG.<MachineCyclotron>block(
            "machine_cyclotron", MachineCyclotron::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Gas centrifuge: a dedicated four-block cascade machine for UF6/PUF6/WATZ. */
    public static final RegistryObject<MachineGasCent> machine_gascent = REG.<MachineGasCent>block(
            "machine_gascent", MachineGasCent::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineDeuteriumExtractor> machine_deuterium_extractor = REG.<MachineDeuteriumExtractor>block(
            "machine_deuterium_extractor", MachineDeuteriumExtractor::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/machine_deuterium_extractor_bottom", "hbm:blocks/machine_deuterium_extractor_side", "hbm:blocks/machine_deuterium_extractor_top"))
                    .states(Variants.simple())
                    .blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDeuteriumTower> machine_deuterium_tower = REG.<MachineDeuteriumTower>block(
            "machine_deuterium_tower", MachineDeuteriumTower::new,
            p -> dummyable(p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** SILEX: the laser isotope separation chamber. Fluid-only (peroxide); the laser supplies the energy. */
    public static final RegistryObject<MachineSILEX> machine_silex = REG.<MachineSILEX>block(
            "machine_silex", MachineSILEX::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Free Electron Laser: the SILEX's power source. Energy only; no fluids, no internal recipes. */
    public static final RegistryObject<MachineFEL> machine_fel = REG.<MachineFEL>block(
            "machine_fel", MachineFEL::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Crucible / foundry

    public static final RegistryObject<MachineCrucible> machine_crucible = REG.<MachineCrucible>block(
            "machine_crucible", MachineCrucible::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FoundryChannel> foundry_channel = REG.<FoundryChannel>block(
            "foundry_channel", FoundryChannel::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())
            .loot(Loot.self())
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("meta=0", new Variant("hbm:block/foundry_channel_center"))
                    .variant("meta=1", new Variant("hbm:block/foundry_channel_side", 0, 90, false))
                    .variant("meta=2", new Variant("hbm:block/foundry_channel_side", 0, 270, false))
                    .variant("meta=3", new Variant("hbm:block/foundry_channel_straight"))
                    .variant("meta=4", new Variant("hbm:block/foundry_channel_side", 0, 180, false))
                    .variant("meta=5", new Variant("hbm:block/foundry_channel_corner", 0, 90, false))
                    .variant("meta=6", new Variant("hbm:block/foundry_channel_corner", 0, 180, false))
                    .variant("meta=7", new Variant("hbm:block/foundry_channel_t"))
                    .variant("meta=8", new Variant("hbm:block/foundry_channel_side"))
                    .variant("meta=9", new Variant("hbm:block/foundry_channel_corner"))
                    .variant("meta=10", new Variant("hbm:block/foundry_channel_corner", 0, 270, false))
                    .variant("meta=11", new Variant("hbm:block/foundry_channel_t", 0, 180, false))
                    .variant("meta=12", new Variant("hbm:block/foundry_channel_straight", 0, 90, false))
                    .variant("meta=13", new Variant("hbm:block/foundry_channel_t", 0, 270, false))
                    .variant("meta=14", new Variant("hbm:block/foundry_channel_t", 0, 90, false))
                    .variant("meta=15", new Variant("hbm:block/foundry_channel_cross"))
                    .register())
            .icon(d -> d.shares("hbm:block/foundry_channel_cross"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FoundryMold> foundry_mold = REG.<FoundryMold>block(
            "foundry_mold", FoundryMold::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.statesOnly(Variants.simple()))
            .loot(Loot.self())
            .icon(d -> d.shares("hbm:block/foundry_mold"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FoundryBasin> foundry_basin = REG.<FoundryBasin>block(
            "foundry_basin", FoundryBasin::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FoundryOutlet> foundry_outlet = REG.<FoundryOutlet>block(
            "foundry_outlet", FoundryOutlet::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<FoundrySlagtap> foundry_slagtap = REG.<FoundrySlagtap>block(
            "foundry_slagtap", FoundrySlagtap::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_FACING)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineStrandCaster> machine_strand_caster = REG.<MachineStrandCaster>block(
            "machine_strand_caster", MachineStrandCaster::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** The fuel-fired DiFurnace: one registered block; LIT replaces the old _off/_on block swap. */
    public static final RegistryObject<MachineDiFurnace> machine_difurnace_off = REG.<MachineDiFurnace>block(
            "machine_difurnace_off", MachineDiFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineDiFurnace.LIT) ? 1 : 0))
            .loot(Loot.self())
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("facing=north,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off"))
                    .variant("facing=east,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 90, false))
                    .variant("facing=south,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 180, false))
                    .variant("facing=west,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 270, false))
                    .variant("facing=north,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on"))
                    .variant("facing=east,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 90, false))
                    .variant("facing=south,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 180, false))
                    .variant("facing=west,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 270, false))
                    .variant("facing=north,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall"))
                    .variant("facing=east,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 90, false))
                    .variant("facing=south,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 180, false))
                    .variant("facing=west,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 270, false))
                    .variant("facing=north,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall"))
                    .variant("facing=east,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 90, false))
                    .variant("facing=south,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 180, false))
                    .variant("facing=west,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 270, false))
                    .register())
            .icon(d -> d.shares("hbm:block/machine_difurnace_off"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDiFurnaceExtension> machine_difurnace_ext = REG.<MachineDiFurnaceExtension>block(
            "machine_difurnace_ext", MachineDiFurnaceExtension::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion()))
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Dynamic slag: the world waste pile the slagtap deposits (no block item). */
    public static final RegistryObject<BlockDynamicSlag> slag = REG.<BlockDynamicSlag>block(
            "slag", BlockDynamicSlag::new,
            p -> p.requiresCorrectToolForDrops().strength(1.0F, 2.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE).tag(BlockTag.PICKAXE);

    // Foundry heat sources

    public static final RegistryObject<HeaterFirebox> heater_firebox = REG.<HeaterFirebox>block(
            "heater_firebox", HeaterFirebox::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Collects the ash of the heater above it. */
    public static final RegistryObject<MachineAshpit> machine_ashpit = REG.<MachineAshpit>block(
            "machine_ashpit", MachineAshpit::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<HeaterOven> heater_oven = REG.<HeaterOven>block(
            "heater_oven", HeaterOven::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<HeaterElectric> heater_electric = REG.<HeaterElectric>block(
            "heater_electric", HeaterElectric::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<HeaterHeatex> heater_heatex = REG.<HeaterHeatex>block(
            "heater_heatex", HeaterHeatex::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<LaunchPad> launch_pad = REG.<LaunchPad>block(
            "launch_pad", LaunchPad::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<LaunchpadSoyuz> launchpad_soyuz = REG.<LaunchpadSoyuz>block(
            "launchpad_soyuz", LaunchpadSoyuz::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<LaunchpadLambda> launchpad_lambda = REG.<LaunchpadLambda>block(
            "launchpad_lambda", LaunchpadLambda::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Mining laser

    public static final RegistryObject<MachineMiningLaser> machine_mining_laser = REG.<MachineMiningLaser>block(
            "machine_mining_laser", MachineMiningLaser::new,
            p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Sandbags: the mining laser's flood dam ({@code buildDam}). A simplified full cube, not the 1.12.2
     * connected/baked model. Registered here, not in the generated ModBlocks, because {@code BlockSandbags}
     * isn't a BlockBase and the codegen doesn't parse it. */
    public static final RegistryObject<Block> sandbags = REG.<Block>block(
            "sandbags", Block::new, p -> p.strength(5.0F, 30.0F))
            .transform(Presets.cubeAll().noItem())
            // the 1.7 inventory icon: BlockSandbags' 0.75-wide box in the 3D block-item pose
            .transform(Presets.shares("hbm:item/sandbags_inventory"))
            .loot(Loot.self());

    public static final RegistryObject<BlockPASource> pa_source = REG.<BlockPASource>block(
            "pa_source", BlockPASource::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockPABeamline> pa_beamline = REG.<BlockPABeamline>block(
            "pa_beamline", BlockPABeamline::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockPARFC> pa_rfc = REG.<BlockPARFC>block(
            "pa_rfc", BlockPARFC::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockPAQuadrupole> pa_quadrupole = REG.<BlockPAQuadrupole>block(
            "pa_quadrupole", BlockPAQuadrupole::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockPADipole> pa_dipole = REG.<BlockPADipole>block(
            "pa_dipole", BlockPADipole::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockPADetector> pa_detector = REG.<BlockPADetector>block(
            "pa_detector", BlockPADetector::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(PA_PART).tag(BlockTag.PICKAXE);

    static {
        // 1.7 MachineBattery.addInformation: capacity, charge/discharge speed and the item's persistent charge.
        REG.<BlockMachineBattery, BlockItem>blockItem(machine_battery,
                        (block, p) -> new EnergyNetBlockItem(block, p,
                                stack -> machine_battery.get().persistentInfo(stack)),
                        p -> Compat.blockDescription(p))
                .tab(HbmTab.MACHINES);
        tabItem(machine_radgen);
        tabItem(machine_minirtg);
        tabItem(rtg_polonium);
        ModTabs.addTo(HbmTab.MACHINES, machine_rtg_grey_item);
        tabItem(radsensor);
        tabItem(machine_satlink);
        tabItem(geiger);
        tabItem(cargo_elevator);
        tabItem(pa_source);
        tabItem(pa_beamline);
        tabItem(pa_rfc);
        tabItem(pa_quadrupole);
        tabItem(pa_dipole);
        tabItem(pa_detector);
        tabItem(red_cable);
        tabItem(creative_generator);
        tabItem(machine_large_turbine);
        tabItem(machine_turbine_gas);
        tabItem(machine_turbofan);
        tabItem(machine_steam_engine);
        tabItem(machine_pump_electric);
        tabItem(machine_pump_steam);
        tabItem(machine_stirling);
        tabItem(machine_stirling_steel);
        tabItem(machine_stirling_creative);
         tabItem(machine_industrial_turbine);
         tabItem(machine_condenser);
         tabItem(machine_condenser_powered);
        tabItem(machine_zirnox);
        tabItem(struct_watz_core);
        tabItem(struct_torus_core);
        // Upstream puts the assembled reactors in the creative tab alongside their struct cores: the
        // struct core is the survival route (build the shape, then transmute), these two place the
        // finished multiblock in one go. Every other fusion machine was already here.
        tabItem(watz);
        tabItem(fusion_torus);
        tabItem(fusion_coupler);
        tabItem(fusion_collector);
        tabItem(fusion_klystron);
        tabItem(fusion_klystron_creative);
        tabItem(fusion_boiler);
        tabItem(fusion_breeder);
        tabItem(fusion_mhdt);
        tabItem(fusion_plasma_forge);
        tabItem(watz_pump);
        tabItem(watz_casing);
        tabItem(watz_casing_tooled);
        tabItem(fusion_component);
        tabItem(fusion_component_bscco_welded);
        tabItem(fusion_component_blanket);
         tabItem(fusion_component_motor);
         tabItem(machine_waste_drum);
         tabItem(machine_storage_drum);
        tabItem(creative_fluid_source);
        REG.<MachineFluidTank, BlockItem>blockItem(machine_fluidtank,
                        (block, p) -> new EnergyNetBlockItem(block, p, stack -> CarriedTankInfo.lines(stack, "tank")),
                        p -> Compat.blockDescription(p))
                .tab(HbmTab.MACHINES);
         tabItem(machine_uf6_tank);
         tabItem(machine_puf6_tank);
         tabItem(machine_bigasstank);
         tabItem(machine_bat9000);
        tabItem(fluid_valve);
        tabItem(machine_drain);
        tabItem(machine_electric_furnace_off);
        tabItem(machine_furnace_brick_off);
        tabItem(furnace_iron);
        tabItem(furnace_steel);
        tabItem(machine_shredder);
         tabItem(machine_epress);
         tabItem(machine_press);
         tabItem(machine_ammo_press);
         tabItem(machine_sawmill);
        tabItem(machine_assembly_machine);
        tabItem(machine_assembly_factory);
         tabItem(machine_precass);
          tabItem(machine_chemical_plant);
        tabItem(machine_chemical_factory);
         tabItem(machine_electrolyser);
        tabItem(machine_mixer);
        tabItem(machine_exposure_chamber);
        tabItem(machine_blast_furnace);
        tabItem(machine_purex);
        tabItem(machine_rockmill);
        tabItem(machine_supercomputer);
        tabItem(machine_well);
        tabItem(machine_pumpjack);
        tabItem(machine_fracking_tower);
        tabItem(machine_vacuum_distill);
        tabItem(machine_catalytic_reformer);
        tabItem(machine_catalytic_cracker);
        tabItem(machine_hydrotreater);
        tabItem(machine_coker);
        tabItem(machine_furnace_combination);
        tabItem(machine_pyrooven);
        tabItem(machine_autosaw);
        tabItem(machine_combustion_engine);
        tabItem(refueler);
         tabItem(machine_compressor);
         tabItem(machine_compressor_compact);
         tabItem(machine_tower_small);
         tabItem(machine_tower_large);
         tabItem(decon);
        tabItem(machine_solar_boiler);
        tabItem(solar_mirror);
        tabItem(machine_diesel);
        tabItem(machine_flare);
        tabItem(chimney_brick);
        tabItem(chimney_industrial);
        tabItem(machine_fraction_tower);
        tabItem(fraction_spacer);
        tabItem(machine_liquefactor);
        tabItem(machine_solidifier);
        REG.<MachineRefinery, BlockItem>blockItem(machine_refinery,
                        (block, p) -> new EnergyNetBlockItem(block, p,
                                stack -> CarriedTankInfo.lines(stack, "tank0", "tank1", "tank2", "tank3", "tank4")),
                        p -> Compat.blockDescription(p))
                .tab(HbmTab.MACHINES);
         tabItem(machine_boiler);
         tabItem(machine_industrial_boiler);
        tabItem(heater_oilburner);
        tabItem(machine_soldering_station);
        tabItem(machine_arc_furnace);
        tabItem(machine_arc_welder);
        tabItem(machine_intake);
        tabItem(fan);
          tabItem(machine_crystallizer);
          tabItem(machine_centrifuge);
          tabItem(machine_cyclotron);
          tabItem(machine_gascent);
         tabItem(machine_deuterium_extractor);
         tabItem(machine_deuterium_tower);
        tabItem(machine_silex);
        tabItem(machine_fel);
        tabItem(machine_crucible);
          tabItem(heater_firebox);
        tabItem(machine_ashpit);
          tabItem(heater_oven);
           tabItem(heater_electric);
          tabItem(heater_heatex);
         tabItem(machine_wood_burner);
        tabItem(machine_strand_caster);
        tabItem(foundry_channel);
        tabItem(foundry_mold);
        tabItem(foundry_basin);
        tabItem(foundry_outlet);
        tabItem(foundry_slagtap);
        tabItem(machine_difurnace_off);
        tabItem(machine_difurnace_ext);
        tabItem(HbmTab.MISSILES, launch_pad);
        tabItem(HbmTab.MISSILES, launchpad_soyuz);
        tabItem(HbmTab.MISSILES, launchpad_lambda);
        tabItem(HbmTab.MISSILES, machine_tape_drive);
        // Mining laser item carries the old addInformation tooltip and the legacy Forge renderer bridge.
        REG.<MachineMiningLaser, BlockItem>blockItem(machine_mining_laser,
                (block, p) -> new CompatBlockItem(block, p) {
                    @Override
                    protected void appendCompatHoverText(ItemStack stack,
                                                         CompatTooltipContext context,
                                                         List<Component> tooltip,
                                                         TooltipFlag flag) {
                        tooltip.add(Component.translatable("desc.mininglaser1"));
                        tooltip.add(Component.translatable("desc.mininglaser2"));
                    }
                }, p -> Compat.blockDescription(p))
                .tab(HbmTab.MACHINES);
        tabItem(HbmTab.BLOCKS, sandbags);
    }

    // Redstone over Radio. The torches ride the radio torch's element model over their 1.7 rtty_* sprite and
    // their items are that flat sprite; TELEX and AUTOCAL are renderer-drawn (their OBJ item definitions are typed
    // data in ObjItemDeclarations). Declared after the item list above, so their blocks and items follow it.

    public static final RegistryObject<RadioTorchSender> radio_torch_sender = REG.block(
            "radio_torch_sender", RadioTorchSender::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSwitching("rtty_sender_off", "rtty_sender_on"));
    public static final RegistryObject<RadioTorchReceiver> radio_torch_receiver = REG.block(
            "radio_torch_receiver", RadioTorchReceiver::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSwitching("rtty_rec_off", "rtty_rec_on"));
    public static final RegistryObject<RadioTorchReader> radio_torch_reader = REG.block(
            "radio_torch_reader", RadioTorchReader::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSteady("rtty_reader"));
    public static final RegistryObject<RadioTorchLogic> radio_torch_logic = REG.block(
            "radio_torch_logic", RadioTorchLogic::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSwitching("rtty_logic_off", "rtty_logic_on"));
    public static final RegistryObject<RadioTorchCounter> radio_torch_counter = REG.block(
            "radio_torch_counter", RadioTorchCounter::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSteady("rtty_counter"));
    public static final RegistryObject<RadioTorchController> radio_torch_controller = REG.block(
            "radio_torch_controller", RadioTorchController::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(radioSteady("rtty_controller"));
    public static final RegistryObject<RadioTelex> radio_telex = REG.block(
            "radio_telex", RadioTelex::new,
            properties -> properties.strength(3.0F, 10.0F).sound(SoundType.WOOD).noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self()).tag(BlockTag.AXE);
    public static final RegistryObject<RadioAUTOCAL> radio_autocal = REG.block(
            "radio_autocal", RadioAUTOCAL::new,
            properties -> properties.requiresCorrectToolForDrops().strength(3.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self()).tag(BlockTag.PICKAXE);

    static {
        radioItem("radio_torch_sender", radio_torch_sender, "rtty_sender_on");
        radioItem("radio_torch_receiver", radio_torch_receiver, "rtty_rec_on");
        radioItem("radio_torch_reader", radio_torch_reader, "rtty_reader");
        radioItem("radio_torch_logic", radio_torch_logic, "rtty_logic_on");
        radioItem("radio_torch_counter", radio_torch_counter, "rtty_counter");
        radioItem("radio_torch_controller", radio_torch_controller, "rtty_controller");
        radioItem("radio_telex", radio_telex, null);
        radioItem("radio_autocal", radio_autocal, null);
    }

    /** A radio block's item; a torch's item is its flat sprite. */
    private static void radioItem(String name, RegistryObject<? extends Block> block, String sprite) {
        Reg.Handle<CompatBlockItem> item = REG.item(name,
                (Item.Properties properties) -> new CompatBlockItem(block.get(), Compat.blockDescription(properties)));
        if (sprite != null) item.transform(Presets.sprite("hbm:blocks/" + sprite));
        item.tab(HbmTab.MACHINES);
    }

    /** A radio torch that shows whether it is lit: two torch models over the off/on sprites. */
    private static Preset radioSwitching(String off, String on) {
        return Presets.shape(Models.group().and("_off", radioTorch(off)).and("_on", radioTorch(on)))
                .states(Variants.product(Variants.facingUp(), Variants.select("lit", "false", "_off", "true", "_on")))
                .noItem()
                .and(handle -> handle.loot(Loot.self()));
    }

    /** A radio torch with one sprite in either state. */
    private static Preset radioSteady(String sprite) {
        return Presets.shape(radioTorch(sprite))
                .states(Variants.product(Variants.facingUp(), Variants.uniform("lit", "false", "true")))
                .noItem()
                .and(handle -> handle.loot(Loot.self()));
    }

    private static Shape radioTorch(String sprite) {
        return Models.renderType("minecraft:cutout",
                Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/" + sprite)));
    }

    /**
     * Properties for folded multiblocks: their invisible full-cube cells must not count as
     * suffocating/view-blocking, or 26.1's push-out-of-block logic shoves entities that overlap them
     * (the 1.12.2 original set {@code isNormalCube=false} for the same reason).
     */
    private static BlockBehaviour.Properties dummyable(
            BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    /** The block's plain item (registered under the block's own id), shown on the machine tab. */
    private static void tabItem(RegistryObject<? extends Block> block) {
        tabItem(HbmTab.MACHINES, block);
    }

    private static void tabItem(HbmTab tab, RegistryObject<? extends Block> block) {
        REG.blockItem(block).tab(tab);
    }

    public static void init() {}
}
