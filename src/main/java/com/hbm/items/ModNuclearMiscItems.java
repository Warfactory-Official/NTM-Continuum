package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tints;
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
import java.util.Map;

/** Feature-local items: breeding rods, legacy pile rods, spent plate fuel and the odd nuclear specials. */
public final class ModNuclearMiscItems {
    private ModNuclearMiscItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static final Map<RodSize, Map<BreedingRodType, RegistryObject<ItemBreedingRod>>> RODS = new EnumMap<>(RodSize.class);

    static {
        // 1.7 ItemBreedingRod (ItemEnumMulti): rod / rod_dual / rod_quad x BreedingRodType, container = the empty rod.
        for (RodSize size : RodSize.values()) {
            Map<BreedingRodType, RegistryObject<ItemBreedingRod>> bySize = new EnumMap<>(BreedingRodType.class);
            for (BreedingRodType type : BreedingRodType.values()) {
                // the empty rod is read inside the properties lambda: naming ModItems here would initialise it
                RegistryObject<ItemBreedingRod> rod = REG.item(size.base + "_" + type.id,
                                p -> new ItemBreedingRod(p, size, type), p -> p.craftRemainder(empty(size).get()))
                        .transform(Presets.sprite())
                        .tab(HbmTab.CONTROL);
                bySize.put(type, rod);
            }
            RODS.put(size, bySize);
        }
    }

    // Legacy Chicago pile rods (1.7: creative tab null).
    public static final RegistryObject<ItemPileRod> PILE_ROD_URANIUM = REG.item("pile_rod_uranium", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_PU239 = REG.item("pile_rod_pu239", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_PLUTONIUM = REG.item("pile_rod_plutonium", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_SOURCE = REG.item("pile_rod_source", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_BORON = REG.item("pile_rod_boron", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_LITHIUM = REG.item("pile_rod_lithium", ItemPileRod::new).transform(Presets.sprite());
    public static final RegistryObject<ItemPileRod> PILE_ROD_DETECTOR = REG.item("pile_rod_detector", ItemPileRod::new).transform(Presets.sprite());

    // Spent research-reactor plate fuel (1.7 ItemDepletedFuel, partsTab).
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_U233 = waste("waste_plate_u233", "waste_plate_uranium");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_U235 = waste("waste_plate_u235", "waste_plate_uranium");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_MOX = waste("waste_plate_mox", "waste_plate_mox");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_PU239 = waste("waste_plate_pu239", "waste_plate_mox");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_SA326 = waste("waste_plate_sa326", "waste_plate_sa326");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_RA226BE = waste("waste_plate_ra226be", "waste_plate_ra226be");
    public static final RegistryObject<ItemDepletedFuel> WASTE_PLATE_PU238BE = waste("waste_plate_pu238be", "waste_plate_pu238be");

    /** 1.7: {@code new ItemUnstable(350, 200)}, no creative tab. */
    // 1.7 ItemUnstable metadata 1..3 (ELEMENTS / ARSENIC / VAULT), the Brittle Spade's inert easter eggs.
    public static final RegistryObject<ItemUnstable> INGOT_U238M2 = REG.item("ingot_u238m2",
                    (Item.Properties p) -> new ItemUnstable(p, 350, 200))
            .icon(d -> d.models(Models.group()
                            .and("", Models.generated("hbm:items/ingot_u238m2"))
                            .and("_elements", Models.generated("hbm:items/hs-elements"))
                            .and("_arsenic", Models.generated("hbm:items/hs-arsenic"))
                            .and("_vault", Models.generated("hbm:items/hs-vault")))
                    .selects(Constants.MOD_ID + ":item_variant"));
    public static final RegistryObject<ItemDemonCore> DEMON_CORE_OPEN = REG.item("demon_core_open", ItemDemonCore::new)
            .transform(Presets.sprite())
            .tab(HbmTab.BOMBS);
    /** 1.7: no creative tab. */
    public static final RegistryObject<ItemModQuartz> QUARTZ_PLUTONIUM = REG.item("quartz_plutonium", ItemModQuartz::new)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemReactorSensor> REACTOR_SENSOR = REG.item("reactor_sensor",
                    ItemReactorSensor::new, p -> p.stacksTo(1))
            .transform(Presets.sprite())
            .tab(HbmTab.CONSUMABLES);

    /** The empty rod a breeding rod of this size leaves in the crafting grid. */
    private static RegistryObject<Item> empty(RodSize size) {
        return switch (size) {
            case SINGLE -> ModItems.rod_empty;
            case DUAL -> ModItems.rod_dual_empty;
            case QUAD -> ModItems.rod_quad_empty;
        };
    }

    /** 1.7 ItemDepletedFuel: subtype 1 (hot) tinted 0xFFBFA5, carried as custom model data here. */
    private static RegistryObject<ItemDepletedFuel> waste(String id, String texture) {
        return REG.item(id, ItemDepletedFuel::new)
                .transform(Presets.sprite("hbm:items/" + texture).tints(Tints.customModelData(0, 0xFFFFFF)))
                .tab(HbmTab.MATERIALS);
    }

    public static ItemBreedingRod rod(RodSize size, BreedingRodType type) {
        return RODS.get(size).get(type).get();
    }

    public static RegistryObject<ItemBreedingRod> rodObject(RodSize size, BreedingRodType type) {
        return RODS.get(size).get(type);
    }

    public static void init() {}
}
