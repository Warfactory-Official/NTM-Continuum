package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockMachineBattery;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.Variants;
import dev.golem.core.dejson.When;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.hbm.blocks.generic.HEVBattery;
import com.hbm.blocks.machine.MachineBatteryTier;
import com.hbm.blocks.machine.MachineCapacitor;
import com.hbm.blocks.machine.MachineCapacitorBus;
import com.hbm.blocks.machine.MachineTesla;
import com.hbm.blocks.machine.PowerDetector;
import com.hbm.blocks.network.BlockCableClassic;
import com.hbm.blocks.network.BlockNetworkSeparator;
import com.hbm.blocks.network.BlockCableGauge;
import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.blocks.network.CableDetector;
import com.hbm.blocks.network.CableDiode;
import com.hbm.blocks.network.CableSwitch;
import com.hbm.blocks.network.ConnectorRedWire;
import com.hbm.blocks.network.ConnectorRedWireSuper;
import com.hbm.blocks.network.PylonLarge;
import com.hbm.blocks.network.PylonMedium;
import com.hbm.blocks.network.PylonRedWire;
import com.hbm.blocks.network.Substation;
import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.items.tool.ItemWiring;

/** The energy-network blocks 1.7 kept in {@code ModBlocks} after {@code red_cable}: pylons, cable variants, storage. */
public final class ModEnergyNetBlocks {

    private ModEnergyNetBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    /** Renderer-drawn folded blocks: the shared empty placeholder; the item is an OBJ definition (ObjItemDeclarations). */
    private static final Preset FOLDED = Presets.rendered(EMPTY, Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true")))
            .and(handle -> handle.loot(Loot.self()));
    private static final Preset CONNECTOR = Presets.rendered(EMPTY,
                    Variants.uniform("facing", "down", "up", "north", "south", "west", "east"))
            .and(handle -> handle.loot(Loot.self()));
    /** Hand-built element models over an up-facing blockstate; the item shows the block model. */
    private static final Preset CAPACITOR = Presets.statesOnly(Variants.facingUp())
            .and(handle -> handle.icon(d -> d.shares("hbm:block/" + handle.path())).loot(Loot.self()));

    /** A cube that swaps its icon while {@code on}. */
    private static Preset onOff(String off, String on) {
        return Presets.shape(Models.group().and("", Models.cubeAll(off)).and("_on", Models.cubeAll(on)))
                .states(Variants.flag("on", "_on"))
                .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));
    }

    /** 1.7 battery icons: front on the facing, side, top. */
    private static Preset battery(String texture) {
        return Presets.orientable("hbm:blocks/" + texture + "_front", "hbm:blocks/" + texture + "_side",
                        "hbm:blocks/" + texture + "_top").states(Variants.facing())
                .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));
    }

    // --- pylons and wires ---

    public static final RegistryObject<PylonRedWire> red_pylon = REG.block("red_pylon", PylonRedWire::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<PylonRedWire> red_pylon_steel = REG.block("red_pylon_steel", PylonRedWire::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<PylonMedium> red_pylon_medium_wood = REG.block("red_pylon_medium_wood",
            p -> new PylonMedium(p, false, false), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.AXE);
    public static final RegistryObject<PylonMedium> red_pylon_medium_wood_transformer = REG.block("red_pylon_medium_wood_transformer",
            p -> new PylonMedium(p, false, true), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.AXE);
    public static final RegistryObject<PylonMedium> red_pylon_medium_steel = REG.block("red_pylon_medium_steel",
            p -> new PylonMedium(p, true, false), p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<PylonMedium> red_pylon_medium_steel_transformer = REG.block("red_pylon_medium_steel_transformer",
            p -> new PylonMedium(p, true, true), p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<PylonLarge> red_pylon_large = REG.block("red_pylon_large", PylonLarge::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Substation> substation = REG.block("substation", Substation::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<ConnectorRedWire> red_connector = REG.block("red_connector", ConnectorRedWire::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CONNECTOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<ConnectorRedWireSuper> red_connector_super = REG.block("red_connector_super", ConnectorRedWireSuper::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CONNECTOR).tag(BlockTag.PICKAXE);

    // --- cable variants ---

    public static final RegistryObject<BlockCableClassic> red_cable_classic = REG.block("red_cable_classic", BlockCableClassic::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .andThen(ModEnergyNetBlocks::classicCableStates)
            .icon(d -> d.shares("hbm:block/red_cable_classic_item"))
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCableGauge> red_cable_gauge = REG.block("red_cable_gauge", BlockCableGauge::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(Presets.statesOnly(Variants.facingAll()))
            .transform(Presets.shares("hbm:block/red_cable_gauge"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCablePaintable> red_cable_paintable = REG.block("red_cable_paintable", BlockCablePaintable::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .andThen(ModEnergyNetBlocks::paintableCableStates)
            .icon(d -> d.shares("hbm:block/red_cable_paintable"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<CableSwitch> cable_switch = REG.block("cable_switch", CableSwitch::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(onOff("hbm:blocks/cable_switch_off", "hbm:blocks/cable_switch_on"));
    public static final RegistryObject<CableDetector> cable_detector = REG.block("cable_detector", CableDetector::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(onOff("hbm:blocks/cable_detector_off", "hbm:blocks/cable_detector_on"));
    public static final RegistryObject<CableDiode> cable_diode = REG.block("cable_diode", CableDiode::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(Presets.statesOnly(Variants.facingAll()))
            .transform(Presets.shares("hbm:block/cable_diode"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNetworkSeparator> network_separator = REG.block("network_separator", BlockNetworkSeparator::new,
            p -> dummyable(p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(FOLDED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<PowerDetector> machine_detector = REG.block("machine_detector", PowerDetector::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(onOff("hbm:blocks/machine_detector_off", "hbm:blocks/machine_detector"));

    // --- storage and conversion ---

    public static final RegistryObject<MachineCapacitorBus> capacitor_bus = REG.block("capacitor_bus", MachineCapacitorBus::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(Presets.cubeBottomTop("hbm:blocks/capacitor_bus_side", "hbm:blocks/capacitor_bus_side", "hbm:blocks/capacitor_bus_out")
                    .states(Variants.facingUp()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    // 1.7 capacitor_copper/gold/niobium; the block ids carry a machine_ prefix because the bare names are the
    // battery-capacitor items in this port. Only copper sits on a tab, as upstream.
    public static final RegistryObject<MachineCapacitor> machine_capacitor_copper = REG.block("machine_capacitor_copper",
            MachineCapacitor::new, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CAPACITOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCapacitor> machine_capacitor_gold = REG.block("machine_capacitor_gold",
            MachineCapacitor::new, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CAPACITOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCapacitor> machine_capacitor_niobium = REG.block("machine_capacitor_niobium",
            MachineCapacitor::new, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CAPACITOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCapacitor> capacitor_tantalium = REG.block("capacitor_tantalium",
            MachineCapacitor::new, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CAPACITOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineCapacitor> capacitor_schrabidate = REG.block("capacitor_schrabidate",
            MachineCapacitor::new, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(CAPACITOR).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineBatteryTier> machine_battery_potato = REG.block("machine_battery_potato",
            p -> new MachineBatteryTier(p, 10_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(battery("battery_potato"));
    public static final RegistryObject<MachineBatteryTier> machine_lithium_battery = REG.block("machine_lithium_battery",
            p -> new MachineBatteryTier(p, 50_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(battery("battery_lithium"));
    public static final RegistryObject<MachineBatteryTier> machine_schrabidium_battery = REG.block("machine_schrabidium_battery",
            p -> new MachineBatteryTier(p, 25_000_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(battery("battery_schrabidium"));
    public static final RegistryObject<MachineBatteryTier> machine_dineutronium_battery = REG.block("machine_dineutronium_battery",
            p -> new MachineBatteryTier(p, 1_000_000_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(battery("battery_dineutronium"));
    public static final RegistryObject<HEVBattery> hev_battery = REG.block("hev_battery", HEVBattery::new,
            p -> p.requiresCorrectToolForDrops().strength(0.5F, 0.25F).sound(SoundType.METAL).lightLevel(s -> 10).noOcclusion())
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.only("hbm:hev_battery_block")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineTesla> tesla = REG.block("tesla", MachineTesla::new,
            p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<ItemWiring> wiring_red_copper = REG.item("wiring_red_copper",
            ItemWiring::new, p -> p.stacksTo(1)).transform(Presets.sprite());

    static {
        // 1.7 addInformation lines, verbatim.
        item(HbmTab.MACHINES, "red_pylon", red_pylon, () -> EnergyNetBlockItem.pylon("Single", "25m"));
        item(HbmTab.MACHINES, "red_pylon_steel", red_pylon_steel, () -> EnergyNetBlockItem.pylon("Single", "25m"));
        item(HbmTab.MACHINES, "red_pylon_medium_wood", red_pylon_medium_wood, () -> EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_wood_transformer", red_pylon_medium_wood_transformer, () -> EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_steel", red_pylon_medium_steel, () -> EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_steel_transformer", red_pylon_medium_steel_transformer, () -> EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_large", red_pylon_large, () -> stack -> List.of(
                EnergyNetBlockItem.pair("Connection Type: ", "Quadruple"),
                EnergyNetBlockItem.pair("Connection Range: ", "100m"),
                EnergyNetBlockItem.gold("This pylon requires a substation!")));
        item(HbmTab.MACHINES, "substation", substation, () -> EnergyNetBlockItem.pylon("Quadruple", "20m"));
        item(HbmTab.MACHINES, "red_connector", red_connector, () -> EnergyNetBlockItem.pylon("Single", "10m"));
        item(HbmTab.MACHINES, "red_connector_super", red_connector_super, () -> EnergyNetBlockItem.pylon("Single", "100m"));

        item(HbmTab.MACHINES, "red_cable_classic", red_cable_classic);
        item(HbmTab.MACHINES, "red_cable_gauge", red_cable_gauge);
        item(HbmTab.MACHINES, "red_cable_paintable", red_cable_paintable);
        item(HbmTab.MACHINES, "cable_switch", cable_switch);
        item(HbmTab.MACHINES, "cable_detector", cable_detector);
        item(null, "cable_diode", cable_diode,
                () -> stack -> List.of(EnergyNetBlockItem.gold("Limits throughput and restricts flow direction")));
        item(HbmTab.MACHINES, "network_separator", network_separator);
        item(HbmTab.MACHINES, "machine_detector", machine_detector);

        // 1.7 registered the legacy storage tiers with no creative tab; they stay reachable by id and pick-block.
        item(null, "capacitor_bus", capacitor_bus);
        item(HbmTab.MACHINES, "machine_capacitor_copper", machine_capacitor_copper);
        item(null, "machine_capacitor_gold", machine_capacitor_gold);
        item(null, "machine_capacitor_niobium", machine_capacitor_niobium);
        item(null, "capacitor_tantalium", capacitor_tantalium);
        item(null, "capacitor_schrabidate", capacitor_schrabidate);
        item(null, "machine_battery_potato", machine_battery_potato, () -> batteryInfo(machine_battery_potato));
        item(null, "machine_lithium_battery", machine_lithium_battery, () -> batteryInfo(machine_lithium_battery));
        item(null, "machine_schrabidium_battery", machine_schrabidium_battery, () -> batteryInfo(machine_schrabidium_battery));
        item(null, "machine_dineutronium_battery", machine_dineutronium_battery, () -> batteryInfo(machine_dineutronium_battery));
        // The item id is hev_battery_block: hbm:hev_battery is the 1.7 ModItems.hev_battery suit-battery item (ItemFusionCore),
        // and 1.7 kept the two apart as item. and tile.
        item(HbmTab.MACHINES, "hev_battery_block", hev_battery);
        item(HbmTab.BLOCKS, "tesla", tesla);

        ModTabs.addTo(HbmTab.MATERIALS, wiring_red_copper);
    }

    /** The classic cable: a core and one arm per connected side (multipart). */
    private static void classicCableStates(Reg.Handle<?> handle) {
        States.multipart(Constants.MOD_ID, handle.path())
                .part(new Variant("hbm:block/red_cable_classic_core"))
                .part(When.of("north", "true"), new Variant("hbm:block/red_cable_classic_arm"))
                .part(When.of("east", "true"), new Variant("hbm:block/red_cable_classic_arm", 0, 90, false))
                .part(When.of("south", "true"), new Variant("hbm:block/red_cable_classic_arm", 0, 180, false))
                .part(When.of("west", "true"), new Variant("hbm:block/red_cable_classic_arm", 0, 270, false))
                .part(When.of("up", "true"), new Variant("hbm:block/red_cable_classic_arm", 270, 0, false))
                .part(When.of("down", "true"), new Variant("hbm:block/red_cable_classic_arm", 90, 0, false))
                .register();
    }

    /** The paintable cable: its own model with or without the overlay; painted, the renderer draws it. */
    private static void paintableCableStates(Reg.Handle<?> handle) {
        States.variants(Constants.MOD_ID, handle.path())
                .variant("overlay=true,painted=false", new Variant("hbm:block/red_cable_paintable"))
                .variant("overlay=false,painted=false", new Variant("hbm:block/red_cable_paintable_plain"))
                .variant("overlay=true,painted=true", new Variant(EMPTY))
                .variant("overlay=false,painted=true", new Variant(EMPTY))
                .register();
    }

    /** 1.7 {@code MachineBattery.addInformation}: capacity, charge/discharge speed and the persistent charge. */
    private static Function<ItemStack, List<Component>> batteryInfo(RegistryObject<? extends BlockMachineBattery> block) {
        return stack -> block.get().persistentInfo(stack);
    }

    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        item(tab, name, block, () -> stack -> List.of());
    }

    /** The tooltip lines are made with the item: EnergyNetBlockItem cannot load before the game does. */
    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block,
                             Supplier<Function<ItemStack, List<Component>>> lines) {
        Reg.Handle<BlockItem> item = REG.<BlockItem>item(name,
                p -> new EnergyNetBlockItem(block.get(), p, lines.get()), Compat::blockDescription);
        if (tab != null) item.tab(tab);
    }

    /** Folded multiblock properties: invisible full-cube cells must not suffocate or block the view. */
    private static BlockBehaviour.Properties dummyable(BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    public static void init() {}
}
