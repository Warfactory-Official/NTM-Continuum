package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockMachineBattery;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
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
import com.hbm.blocks.generic.HEVBattery;
import com.hbm.blocks.machine.MachineBatteryTier;
import com.hbm.blocks.machine.MachineCapacitor;
import com.hbm.blocks.machine.MachineCapacitorBus;
import com.hbm.blocks.machine.MachineTesla;
import com.hbm.blocks.machine.PowerDetector;
import com.hbm.blocks.network.BlockCableClassic;
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

    // --- pylons and wires ---

    public static final RegistryObject<PylonRedWire> red_pylon = BLOCKS.registerBlock("red_pylon", PylonRedWire::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<PylonRedWire> red_pylon_steel = BLOCKS.registerBlock("red_pylon_steel", PylonRedWire::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<PylonMedium> red_pylon_medium_wood = BLOCKS.registerBlock("red_pylon_medium_wood",
            p -> new PylonMedium(p, false, false), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<PylonMedium> red_pylon_medium_wood_transformer = BLOCKS.registerBlock("red_pylon_medium_wood_transformer",
            p -> new PylonMedium(p, false, true), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<PylonMedium> red_pylon_medium_steel = BLOCKS.registerBlock("red_pylon_medium_steel",
            p -> new PylonMedium(p, true, false), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<PylonMedium> red_pylon_medium_steel_transformer = BLOCKS.registerBlock("red_pylon_medium_steel_transformer",
            p -> new PylonMedium(p, true, true), p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<PylonLarge> red_pylon_large = BLOCKS.registerBlock("red_pylon_large", PylonLarge::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Substation> substation = BLOCKS.registerBlock("substation", Substation::new,
            p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<ConnectorRedWire> red_connector = BLOCKS.registerBlock("red_connector", ConnectorRedWire::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<ConnectorRedWireSuper> red_connector_super = BLOCKS.registerBlock("red_connector_super", ConnectorRedWireSuper::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    // --- cable variants ---

    public static final RegistryObject<BlockCableClassic> red_cable_classic = BLOCKS.registerBlock("red_cable_classic", BlockCableClassic::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<BlockCableGauge> red_cable_gauge = BLOCKS.registerBlock("red_cable_gauge", BlockCableGauge::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<BlockCablePaintable> red_cable_paintable = BLOCKS.registerBlock("red_cable_paintable", BlockCablePaintable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<CableSwitch> cable_switch = BLOCKS.registerBlock("cable_switch", CableSwitch::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<CableDetector> cable_detector = BLOCKS.registerBlock("cable_detector", CableDetector::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<CableDiode> cable_diode = BLOCKS.registerBlock("cable_diode", CableDiode::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<PowerDetector> machine_detector = BLOCKS.registerBlock("machine_detector", PowerDetector::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));

    // --- storage and conversion ---

    public static final RegistryObject<MachineCapacitorBus> capacitor_bus = BLOCKS.registerBlock("capacitor_bus", MachineCapacitorBus::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    // 1.7 capacitor_copper/gold/niobium; the block ids carry a machine_ prefix because the bare names are the
    // battery-capacitor items in this port. Only copper sits on a tab, as upstream.
    public static final RegistryObject<MachineCapacitor> machine_capacitor_copper = BLOCKS.registerBlock("machine_capacitor_copper",
            p -> new MachineCapacitor(p, 1_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineCapacitor> machine_capacitor_gold = BLOCKS.registerBlock("machine_capacitor_gold",
            p -> new MachineCapacitor(p, 5_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineCapacitor> machine_capacitor_niobium = BLOCKS.registerBlock("machine_capacitor_niobium",
            p -> new MachineCapacitor(p, 25_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineCapacitor> capacitor_tantalium = BLOCKS.registerBlock("capacitor_tantalium",
            p -> new MachineCapacitor(p, 150_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineCapacitor> capacitor_schrabidate = BLOCKS.registerBlock("capacitor_schrabidate",
            p -> new MachineCapacitor(p, 50_000_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<MachineBatteryTier> machine_battery_potato = BLOCKS.registerBlock("machine_battery_potato",
            p -> new MachineBatteryTier(p, 10_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineBatteryTier> machine_lithium_battery = BLOCKS.registerBlock("machine_lithium_battery",
            p -> new MachineBatteryTier(p, 50_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineBatteryTier> machine_schrabidium_battery = BLOCKS.registerBlock("machine_schrabidium_battery",
            p -> new MachineBatteryTier(p, 25_000_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineBatteryTier> machine_dineutronium_battery = BLOCKS.registerBlock("machine_dineutronium_battery",
            p -> new MachineBatteryTier(p, 1_000_000_000_000L), p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<HEVBattery> hev_battery = BLOCKS.registerBlock("hev_battery", HEVBattery::new,
            p -> p.strength(0.5F, 0.25F).sound(SoundType.METAL).lightLevel(s -> 10).noOcclusion());
    public static final RegistryObject<MachineTesla> tesla = BLOCKS.registerBlock("tesla", MachineTesla::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<ItemWiring> wiring_red_copper = ITEMS.registerItem("wiring_red_copper",
            ItemWiring::new, p -> p.stacksTo(1));

    static {
        // 1.7 addInformation lines, verbatim.
        item(HbmTab.MACHINES, "red_pylon", red_pylon, 0, EnergyNetBlockItem.pylon("Single", "25m"));
        item(HbmTab.MACHINES, "red_pylon_steel", red_pylon_steel, 0, EnergyNetBlockItem.pylon("Single", "25m"));
        item(HbmTab.MACHINES, "red_pylon_medium_wood", red_pylon_medium_wood, 0, EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_wood_transformer", red_pylon_medium_wood_transformer, 0, EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_steel", red_pylon_medium_steel, 0, EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_medium_steel_transformer", red_pylon_medium_steel_transformer, 0, EnergyNetBlockItem.pylon("Triple", "45m"));
        item(HbmTab.MACHINES, "red_pylon_large", red_pylon_large, 0, stack -> List.of(
                EnergyNetBlockItem.pair("Connection Type: ", "Quadruple"),
                EnergyNetBlockItem.pair("Connection Range: ", "100m"),
                EnergyNetBlockItem.gold("This pylon requires a substation!")));
        item(HbmTab.MACHINES, "substation", substation, 0, EnergyNetBlockItem.pylon("Quadruple", "20m"));
        item(HbmTab.MACHINES, "red_connector", red_connector, 0, EnergyNetBlockItem.pylon("Single", "10m"));
        item(HbmTab.MACHINES, "red_connector_super", red_connector_super, 0, EnergyNetBlockItem.pylon("Single", "100m"));

        item(HbmTab.MACHINES, "red_cable_classic", red_cable_classic, 0);
        item(HbmTab.MACHINES, "red_cable_gauge", red_cable_gauge, 4);
        item(HbmTab.MACHINES, "red_cable_paintable", red_cable_paintable, 4);
        item(HbmTab.MACHINES, "cable_switch", cable_switch, 0);
        item(HbmTab.MACHINES, "cable_detector", cable_detector, 0);
        item(HbmTab.MACHINES, "cable_diode", cable_diode, 0,
                stack -> List.of(EnergyNetBlockItem.gold("Limits throughput and restricts flow direction")));
        item(HbmTab.MACHINES, "machine_detector", machine_detector, 0);

        // 1.7 registered the legacy storage tiers with no creative tab; they stay reachable by id and pick-block.
        item(null, "capacitor_bus", capacitor_bus, 2);
        item(HbmTab.MACHINES, "machine_capacitor_copper", machine_capacitor_copper, 0, capacitorInfo(machine_capacitor_copper));
        item(null, "machine_capacitor_gold", machine_capacitor_gold, 0, capacitorInfo(machine_capacitor_gold));
        item(null, "machine_capacitor_niobium", machine_capacitor_niobium, 0, capacitorInfo(machine_capacitor_niobium));
        item(null, "capacitor_tantalium", capacitor_tantalium, 0, capacitorInfo(capacitor_tantalium));
        item(null, "capacitor_schrabidate", capacitor_schrabidate, 0, capacitorInfo(capacitor_schrabidate));
        item(null, "machine_battery_potato", machine_battery_potato, 0, batteryInfo(machine_battery_potato));
        item(null, "machine_lithium_battery", machine_lithium_battery, 0, batteryInfo(machine_lithium_battery));
        item(null, "machine_schrabidium_battery", machine_schrabidium_battery, 0, batteryInfo(machine_schrabidium_battery));
        item(null, "machine_dineutronium_battery", machine_dineutronium_battery, 0, batteryInfo(machine_dineutronium_battery));
        item(HbmTab.MACHINES, "hev_battery", hev_battery, 0);
        item(HbmTab.BLOCKS, "tesla", tesla, 0);

        ModTabs.addTo(HbmTab.MATERIALS, wiring_red_copper);
    }

    /** {@code tile.capacitor.desc} (shared by every capacitor) plus the persistent-charge lines. */
    private static Function<ItemStack, List<Component>> capacitorInfo(RegistryObject<MachineCapacitor> block) {
        return stack -> {
            List<Component> list = new ArrayList<>();
            list.add(Component.translatable("desc.capacitor.0").withStyle(ChatFormatting.YELLOW));
            list.add(Component.translatable("desc.capacitor.1").withStyle(ChatFormatting.YELLOW));
            list.addAll(block.get().persistentInfo(stack));
            return list;
        };
    }

    /** 1.7 {@code MachineBattery.addInformation}: capacity, charge/discharge speed and the persistent charge. */
    private static Function<ItemStack, List<Component>> batteryInfo(RegistryObject<? extends BlockMachineBattery> block) {
        return stack -> block.get().persistentInfo(stack);
    }

    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block, int descLines) {
        item(tab, name, block, descLines, stack -> List.of());
    }

    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block, int descLines,
                             Function<ItemStack, List<Component>> lines) {
        RegistryObject<BlockItem> item = ITEMS.registerItem(name,
                p -> new EnergyNetBlockItem(block.get(), p, descLines, lines), Compat::blockDescription);
        if (tab != null) ModTabs.addTo(tab, item);
    }

    /** Folded multiblock properties: invisible full-cube cells must not suffocate or block the view. */
    private static BlockBehaviour.Properties dummyable(BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    public static void init() {}
}
