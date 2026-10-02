package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.tool.ItemSatDesignator;
import com.hbm.items.tool.ItemSatInterface;
import dev.golem.core.dejson.Models;
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
    private static final Reg REG = Reg.of(ITEMS);

    /** One sprite per {@link EnumSatType}, selected by {@code hbm:item_variant}. */
    public static final RegistryObject<ItemSatellite> satellite =
            REG.item("satellite", ItemSatellite::new, p -> p.stacksTo(1))
                    .icon(d -> d.models(Models.group()
                                    .and("", Models.generated("hbm:items/satellite_spy"))
                                    .and("_scanner", Models.generated("hbm:items/satellite_scanner"))
                                    .and("_radar", Models.generated("hbm:items/satellite_radar"))
                                    .and("_miner_astro", Models.generated("hbm:items/satellite_miner_astro"))
                                    .and("_miner_lunar", Models.generated("hbm:items/satellite_miner_lunar"))
                                    .and("_precision_laser", Models.generated("hbm:items/satellite_precision_laser"))
                                    .and("_death_ray", Models.generated("hbm:items/satellite_death_ray"))
                                    .and("_xenium_resonator", Models.generated("hbm:items/satellite_xenium_resonator"))
                                    .and("_relay", Models.generated("hbm:items/satellite_relay"))
                                    .and("_detector", Models.generated("hbm:items/satellite_detector"))
                                    .and("_ray_scan", Models.generated("hbm:items/satellite_ray_scan")))
                            .selects(Constants.MOD_ID + ":item_variant"));

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
            REG.item("sat_coord", ItemSatInterface::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite());
    public static final RegistryObject<ItemSatDesignator> sat_designator =
            REG.item("sat_designator", ItemSatDesignator::new, p -> p.stacksTo(1))
                    .transform(Presets.handheld()); // 1.7 setFull3D
    public static final RegistryObject<ItemSatChip> sat_relay = chip("sat_relay");

    private static RegistryObject<ItemSatChip> chip(String name) {
        return REG.item(name, ItemSatChip::new, p -> p.stacksTo(1))
                .transform(Presets.sprite());
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
