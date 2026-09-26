package com.hbm.foundry;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.container.ContainerMachineStrandCaster;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.ItemArcElectrodeBurnt;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The registration leaf for the two foundry machines. The main registries deliberately do not know
 * about this leaf; a loader integration calls {@link #init()} before its normal registry flush.
 */
public final class FoundryExpansion {

    private FoundryExpansion() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MachineStrandCaster> STRAND_CASTER = BLOCKS.registerBlock(
            "machine_strand_caster", MachineStrandCaster::new, FoundryExpansion::machineProperties);
    public static final RegistryObject<MachineArcFurnaceLarge> ARC_FURNACE = BLOCKS.registerBlock(
            "machine_arc_furnace", MachineArcFurnaceLarge::new, FoundryExpansion::machineProperties);

    public static final RegistryObject<BlockItem> STRAND_CASTER_ITEM = ITEMS.registerBlockItem(
            "machine_strand_caster", STRAND_CASTER);
    public static final RegistryObject<BlockItem> ARC_FURNACE_ITEM = ITEMS.registerBlockItem(
            "machine_arc_furnace", ARC_FURNACE);

    private static RegistryObject<ItemArcElectrode> fresh(String name, ItemArcElectrode.ElectrodeType type) {
        return ITEMS.registerItem(name, p -> new ItemArcElectrode(p, type), p -> p.stacksTo(1).durability(type.durability));
    }

    private static RegistryObject<ItemArcElectrodeBurnt> burnt(String name, ItemArcElectrode.ElectrodeType type) {
        return ITEMS.registerItem(name, p -> new ItemArcElectrodeBurnt(p, type), p -> p.stacksTo(64));
    }

    public static final RegistryObject<ItemArcElectrode> ARC_ELECTRODE =
            fresh("arc_electrode", ItemArcElectrode.ElectrodeType.GRAPHITE);
    public static final RegistryObject<ItemArcElectrode> ARC_ELECTRODE_LANTHANIUM =
            fresh("arc_electrode_lanthanium", ItemArcElectrode.ElectrodeType.LANTHANIUM);
    public static final RegistryObject<ItemArcElectrode> ARC_ELECTRODE_DESH =
            fresh("arc_electrode_desh", ItemArcElectrode.ElectrodeType.DESH);
    public static final RegistryObject<ItemArcElectrode> ARC_ELECTRODE_SATURNITE =
            fresh("arc_electrode_saturnite", ItemArcElectrode.ElectrodeType.SATURNITE);

    public static final RegistryObject<ItemArcElectrodeBurnt> ARC_ELECTRODE_BURNT =
            burnt("arc_electrode_burnt", ItemArcElectrode.ElectrodeType.GRAPHITE);
    public static final RegistryObject<ItemArcElectrodeBurnt> ARC_ELECTRODE_BURNT_LANTHANIUM =
            burnt("arc_electrode_burnt_lanthanium", ItemArcElectrode.ElectrodeType.LANTHANIUM);
    public static final RegistryObject<ItemArcElectrodeBurnt> ARC_ELECTRODE_BURNT_DESH =
            burnt("arc_electrode_burnt_desh", ItemArcElectrode.ElectrodeType.DESH);
    public static final RegistryObject<ItemArcElectrodeBurnt> ARC_ELECTRODE_BURNT_SATURNITE =
            burnt("arc_electrode_burnt_saturnite", ItemArcElectrode.ElectrodeType.SATURNITE);

    private static final Map<ItemArcElectrode.ElectrodeType, RegistryObject<ItemArcElectrode>> FRESH = new EnumMap<>(ItemArcElectrode.ElectrodeType.class);
    private static final Map<ItemArcElectrode.ElectrodeType, RegistryObject<ItemArcElectrodeBurnt>> BURNT = new EnumMap<>(ItemArcElectrode.ElectrodeType.class);

    static {
        FRESH.put(ItemArcElectrode.ElectrodeType.GRAPHITE, ARC_ELECTRODE);
        FRESH.put(ItemArcElectrode.ElectrodeType.LANTHANIUM, ARC_ELECTRODE_LANTHANIUM);
        FRESH.put(ItemArcElectrode.ElectrodeType.DESH, ARC_ELECTRODE_DESH);
        FRESH.put(ItemArcElectrode.ElectrodeType.SATURNITE, ARC_ELECTRODE_SATURNITE);
        BURNT.put(ItemArcElectrode.ElectrodeType.GRAPHITE, ARC_ELECTRODE_BURNT);
        BURNT.put(ItemArcElectrode.ElectrodeType.LANTHANIUM, ARC_ELECTRODE_BURNT_LANTHANIUM);
        BURNT.put(ItemArcElectrode.ElectrodeType.DESH, ARC_ELECTRODE_BURNT_DESH);
        BURNT.put(ItemArcElectrode.ElectrodeType.SATURNITE, ARC_ELECTRODE_BURNT_SATURNITE);

        ModTabs.addTo(HbmTab.MACHINES, STRAND_CASTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, ARC_FURNACE_ITEM);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_LANTHANIUM);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_DESH);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_SATURNITE);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_BURNT);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_BURNT_LANTHANIUM);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_BURNT_DESH);
        ModTabs.addTo(HbmTab.CONTROL, ARC_ELECTRODE_BURNT_SATURNITE);
    }

    public static final RegistryObject<BlockEntityType<TileEntityMachineStrandCaster>> STRAND_CASTER_BE =
            BLOCK_ENTITIES.register("machine_strand_caster", () ->
                    dev.golem.core.registry.BlockEntities.serverTicking(TileEntityMachineStrandCaster::new, TileEntityMachineStrandCaster::serverTick, STRAND_CASTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineArcFurnaceLarge>> ARC_FURNACE_BE =
            BLOCK_ENTITIES.register("machine_arc_furnace", () ->
                    dev.golem.core.registry.BlockEntities.ticking(TileEntityMachineArcFurnaceLarge::new, TileEntityMachineArcFurnaceLarge::clientTick, TileEntityMachineArcFurnaceLarge::serverTick, ARC_FURNACE.get()));

    public static final RegistryObject<MenuType<ContainerMachineStrandCaster>> STRAND_CASTER_MENU = MENUS.register(
            "machine_strand_caster", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineStrandCaster::new));
    public static final RegistryObject<MenuType<ContainerMachineArcFurnaceLarge>> ARC_FURNACE_MENU = MENUS.register(
            "machine_arc_furnace", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineArcFurnaceLarge::new));

    public static ItemArcElectrode fresh(ItemArcElectrode.ElectrodeType type) {
        return FRESH.get(type).get();
    }

    public static ItemArcElectrodeBurnt burnt(ItemArcElectrode.ElectrodeType type) {
        return BURNT.get(type).get();
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties machineProperties(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return properties.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .noOcclusion().isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    public static void init() {}
}
