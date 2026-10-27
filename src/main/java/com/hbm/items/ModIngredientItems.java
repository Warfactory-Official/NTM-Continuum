package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumIngotMetal;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemPlateFuel.FunctionEnum;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Hand-registered item families that need variant behavior or escaped generated-item ownership. */
public final class ModIngredientItems {

    private ModIngredientItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);
    private static final Map<EnumPartType, RegistryObject<Item>> PARTS = new EnumMap<>(EnumPartType.class);
    private static final Map<EnumAshType, RegistryObject<Item>> ASHES = new EnumMap<>(EnumAshType.class);
    private static final Map<PlateFuelType, RegistryObject<ItemPlateFuel>> PLATE_FUELS =
            new EnumMap<>(PlateFuelType.class);

    /** 1.7 {@code ItemAmatExtractor}: an empty item, stack size 1, no creative tab. */
    public static final RegistryObject<Item> BISMUTH_TOOL = REG.simpleItem("bismuth_tool", p -> p.stacksTo(1))
            .transform(Presets.sprite());
    /** 1.7 {@code memory}: a battery holding {@code Long.MAX_VALUE / 100}, no creative tab. */
    public static final RegistryObject<ItemBattery> MEMORY = REG.item("memory",
                    (Item.Properties p) -> new ItemBattery(p, Long.MAX_VALUE / 100L, 100000000000000L, 100000000000000L),
                    p -> p.stacksTo(1))
            .transform(Presets.sprite("hbm:items/mo8_anim"));

    static {
        for (EnumPartType type : EnumPartType.VALUES) {
            String name = type.name().toLowerCase(Locale.ROOT);
            // the 1.7 metadata sprites keep their dotted names: part_generic.<type>
            RegistryObject<Item> item = REG.item("part_generic_" + name)
                    .transform(Presets.sprite("hbm:items/part_generic." + name));
            PARTS.put(type, item);
            ModTabs.addTo(HbmTab.MATERIALS, item);
        }
        for (EnumAshType type : EnumAshType.VALUES) {
            String name = type.name().toLowerCase(Locale.ROOT);
            RegistryObject<Item> item = REG.item("powder_ash_" + name)
                    .transform(Presets.sprite("hbm:items/powder_ash." + name));
            ASHES.put(type, item);
            ModTabs.addTo(HbmTab.MATERIALS, item);
        }
        for (EnumIngotMetal type : EnumIngotMetal.VALUES) {
            String name = type.name().toLowerCase(Locale.ROOT);
            REG.item("ingot_metal_" + name).transform(Presets.sprite("hbm:items/ingot_metal." + name));
        }
        for (PlateFuelType type : PlateFuelType.values()) {
            RegistryObject<ItemPlateFuel> item = REG.item(type.id,
                    (Item.Properties p) -> new ItemPlateFuel(p.stacksTo(1), type.lifeTime, type.function, type.reactivity))
                    .transform(Presets.sprite());
            PLATE_FUELS.put(type, item);
            ModTabs.addTo(HbmTab.CONTROL, item);
        }
    }

    public static Item part(EnumPartType type) {
        return PARTS.get(type).get();
    }

    public static ItemStack part(EnumPartType type, int count) {
        return new ItemStack(part(type), count);
    }

    public static Item ash(EnumAshType type) {
        return ASHES.get(type).get();
    }

    public static ItemStack ash(EnumAshType type, int count) {
        return new ItemStack(ash(type), count);
    }

    public static ItemPlateFuel plateFuel(PlateFuelType type) {
        return PLATE_FUELS.get(type).get();
    }

    public enum PlateFuelType {
        U233("plate_fuel_u233", 2_200_000, FunctionEnum.SQUARE_ROOT, 50),
        U235("plate_fuel_u235", 2_200_000, FunctionEnum.SQUARE_ROOT, 40),
        MOX("plate_fuel_mox", 2_400_000, FunctionEnum.LOGARITHM, 50),
        PU239("plate_fuel_pu239", 2_000_000, FunctionEnum.NEGATIVE_QUADRATIC, 50),
        SA326("plate_fuel_sa326", 2_000_000, FunctionEnum.LINEAR, 80),
        RA226BE("plate_fuel_ra226be", 1_300_000, FunctionEnum.PASSIVE, 30),
        PU238BE("plate_fuel_pu238be", 1_000_000, FunctionEnum.PASSIVE, 50);

        public final String id;
        public final int lifeTime;
        public final FunctionEnum function;
        public final int reactivity;

        PlateFuelType(String id, int lifeTime, FunctionEnum function, int reactivity) {
            this.id = id;
            this.lifeTime = lifeTime;
            this.function = function;
            this.reactivity = reactivity;
        }
    }

    public static void init() {}
}
