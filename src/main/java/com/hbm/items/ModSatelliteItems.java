package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemOrbitalAssembly;
import com.hbm.items.machine.ItemOrbitalAssembly.EnumOrbitalAssembly;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.tool.ItemSatDesignator;
import com.hbm.items.tool.ItemSatInterface;
import dev.golem.core.dejson.Models;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import java.util.Locale;
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

    /**
     * 1.7 {@code satellite}, one item per {@link EnumSatType} as in ntm-next ({@code satellite_spy} ...
     * {@code satellite_ray_scan}), each drawn with its 1.7 sprite.
     */
    public static final ItemFamily<EnumSatType, ItemSatellite> satellite = ItemFamily.of(EnumSatType.class, type -> {
        String id = "satellite_" + type.name().toLowerCase(Locale.ROOT);
        return REG.item(id, p -> new ItemSatellite(p, type), p -> p.stacksTo(1))
                .transform(Presets.sprite("hbm:items/" + id));
    });

    /** 1.7 {@code orbital_assembly}, one item per type; every type draws the single 1.7 sprite. */
    public static final ItemFamily<EnumOrbitalAssembly, ItemOrbitalAssembly> orbital_assembly =
            ItemFamily.of(EnumOrbitalAssembly.class, type -> {
                String id = "orbital_assembly_" + type.name().toLowerCase(Locale.ROOT);
                return REG.item(id, p -> new ItemOrbitalAssembly(p, type), p -> p.stacksTo(1))
                        .transform(Presets.sprite("hbm:items/orbital_assembly"));
            });

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
        ModTabs.addStacksTo(HbmTab.MISSILES, satellite::stacks);
        ModTabs.addStacksTo(HbmTab.MATERIALS, orbital_assembly::stacks);
        for (RegistryObject<? extends Item> item : List.of(sat_gerald, sat_chip, sat_coord, sat_designator,
                sat_relay)) {
            ModTabs.addTo(HbmTab.MISSILES, item);
        }
    }

    /** Class-load hook so the register is populated during bootstrap. */
    public static void init() {}
}
