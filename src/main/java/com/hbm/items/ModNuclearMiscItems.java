package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.items.special.ItemDemonCore;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.armor.ItemModQuartz;
import com.hbm.items.machine.ItemPileRod;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.items.special.ItemUnstable;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Feature-local items: breeding rods, legacy pile rods, spent plate fuel and the odd nuclear specials. */
public final class ModNuclearMiscItems {
    private ModNuclearMiscItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final Map<RodSize, Map<BreedingRodType, RegistryObject<ItemBreedingRod>>> RODS = new EnumMap<>(RodSize.class);

    static {
        // 1.7 ItemBreedingRod (ItemEnumMulti): rod / rod_dual / rod_quad x BreedingRodType, container = the empty rod.
        for (RodSize size : RodSize.values()) {
            RODS.put(size, new HashMap<>());
            for (BreedingRodType type : BreedingRodType.values()) registerRod(ITEMS, size, type);
        }
    }

    /** Single, dual and quad rods of {@code type}, each on the creative tab. */
    public static void registerRods(DeferredRegister.Items register, BreedingRodType type) {
        for (RodSize size : RodSize.values()) registerRod(register, size, type);
    }

    private static void registerRod(DeferredRegister.Items register, RodSize size, BreedingRodType type) {
        RegistryObject<Item> empty = switch (size) {
            case SINGLE -> ModItems.rod_empty;
            case DUAL -> ModItems.rod_dual_empty;
            case QUAD -> ModItems.rod_quad_empty;
        };
        RegistryObject<ItemBreedingRod> rod = register.registerItem(size.base + "_" + type.id,
                p -> new ItemBreedingRod(p, size, type), p -> p.craftRemainder(empty.get()));
        RODS.get(size).put(type, rod);
        ModTabs.addTo(HbmTab.CONTROL, rod);
    }

    // Legacy Chicago pile rods (1.7: creative tab null).
    public static final RegistryObject<ItemPileRod> PILE_ROD_URANIUM = ITEMS.registerItem("pile_rod_uranium", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_PU239 = ITEMS.registerItem("pile_rod_pu239", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_PLUTONIUM = ITEMS.registerItem("pile_rod_plutonium", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_SOURCE = ITEMS.registerItem("pile_rod_source", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_BORON = ITEMS.registerItem("pile_rod_boron", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_LITHIUM = ITEMS.registerItem("pile_rod_lithium", ItemPileRod::new);
    public static final RegistryObject<ItemPileRod> PILE_ROD_DETECTOR = ITEMS.registerItem("pile_rod_detector", ItemPileRod::new);

    // Spent research-reactor plate fuel (1.7 ItemDepletedFuel, partsTab).
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_U233 = waste("waste_plate_u233");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_U235 = waste("waste_plate_u235");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_MOX = waste("waste_plate_mox");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_PU239 = waste("waste_plate_pu239");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_SA326 = waste("waste_plate_sa326");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_RA226BE = waste("waste_plate_ra226be");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_PU238BE = waste("waste_plate_pu238be");

    /** 1.7: {@code new ItemUnstable(350, 200)}, no creative tab. */
    public static final RegistryObject<ItemUnstable> INGOT_U238M2 = ITEMS.registerItem("ingot_u238m2",
            p -> new ItemUnstable(p, 350, 200));
    public static final RegistryObject<ItemDemonCore> DEMON_CORE_OPEN = ITEMS.registerItem("demon_core_open", ItemDemonCore::new);
    /** 1.7: no creative tab. */
    public static final RegistryObject<ItemModQuartz> QUARTZ_PLUTONIUM = ITEMS.registerItem("quartz_plutonium", ItemModQuartz::new);
    public static final RegistryObject<ItemReactorSensor> REACTOR_SENSOR = ITEMS.registerItem("reactor_sensor",
            ItemReactorSensor::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.BOMBS, DEMON_CORE_OPEN);
        ModTabs.addTo(HbmTab.CONSUMABLES, REACTOR_SENSOR);
    }

    private static RegistryObject<ItemDepletedFuel> waste(String id) {
        RegistryObject<ItemDepletedFuel> item = ITEMS.registerItem(id, ItemDepletedFuel::new);
        ModTabs.addTo(HbmTab.MATERIALS, item);
        return item;
    }

    public static ItemBreedingRod rod(RodSize size, BreedingRodType type) {
        return RODS.get(size).get(type).get();
    }

    public static RegistryObject<ItemBreedingRod> rodObject(RodSize size, BreedingRodType type) {
        return RODS.get(size).get(type);
    }

    public static void init() {}
}
