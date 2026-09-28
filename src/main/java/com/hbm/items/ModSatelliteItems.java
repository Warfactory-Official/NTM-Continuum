package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.tool.ItemSatDesignator;
import com.hbm.items.tool.ItemSatInterface;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.Arrays;

/**
 * The satellite items, as 1.7 {@code ModItems} declared them: all stack to one. The {@code satellite} variants,
 * Gerald, the ID chip, both designators and the relay chip sit on the missile tab; the seven "(LEGACY)" chips on
 * no tab at all.
 */
public final class ModSatelliteItems {

    private ModSatelliteItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemSatellite> satellite =
            ITEMS.registerItem("satellite", ItemSatellite::new, p -> p.stacksTo(1));

    @Deprecated public static final RegistryObject<ItemSatChip> sat_mapper = chip("sat_mapper");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_scanner = chip("sat_scanner");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_radar = chip("sat_radar");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_laser = chip("sat_laser");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_foeq = chip("sat_foeq");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_resonator = chip("sat_resonator");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_miner = chip("sat_miner");
    @Deprecated public static final RegistryObject<ItemSatChip> sat_lunar_miner = chip("sat_lunar_miner");
    public static final RegistryObject<ItemSatChip> sat_gerald = chip("sat_gerald");
    public static final RegistryObject<ItemSatChip> sat_chip = chip("sat_chip");
    public static final RegistryObject<ItemSatInterface> sat_coord =
            ITEMS.registerItem("sat_coord", ItemSatInterface::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemSatDesignator> sat_designator =
            ITEMS.registerItem("sat_designator", ItemSatDesignator::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemSatChip> sat_relay = chip("sat_relay");

    private static RegistryObject<ItemSatChip> chip(String name) {
        return ITEMS.registerItem(name, ItemSatChip::new, p -> p.stacksTo(1));
    }

    static {
        ModTabs.addStacksTo(HbmTab.MISSILES, () -> Arrays.stream(EnumSatType.VALUES)
                .map(type -> satellite.get().of(type)).toList());
        for (RegistryObject<? extends Item> item : java.util.List.of(sat_gerald, sat_chip, sat_coord, sat_designator,
                sat_relay)) {
            ModTabs.addTo(HbmTab.MISSILES, item);
        }
    }

    /** Class-load hook so the register is populated during bootstrap. */
    public static void init() {}
}
