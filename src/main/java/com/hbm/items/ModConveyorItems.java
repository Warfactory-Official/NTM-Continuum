package com.hbm.items;

import com.hbm.Constants;
import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;
import com.hbm.items.tool.ItemConveyorWand;

/**
 * The conveyor family's items: the four wand subtypes (1.7 {@code conveyor_wand} metadata 0-3, flattened to
 * {@code conveyor_wand_<type>}) and the two crane upgrade ladders (1.7 {@code ItemMetaUpgrade(3)}
 * {@code upgrade_ejector} / {@code upgrade_stack}, flattened to {@code _1.._3}).
 */
public final class ModConveyorItems {

    private ModConveyorItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemConveyorWand> CONVEYOR_WAND_REGULAR = wand(ConveyorType.REGULAR);
    public static final RegistryObject<ItemConveyorWand> CONVEYOR_WAND_EXPRESS = wand(ConveyorType.EXPRESS);
    public static final RegistryObject<ItemConveyorWand> CONVEYOR_WAND_DOUBLE = wand(ConveyorType.DOUBLE);
    public static final RegistryObject<ItemConveyorWand> CONVEYOR_WAND_TRIPLE = wand(ConveyorType.TRIPLE);

    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_EJECTOR_1 = upgrade("upgrade_ejector_1", 1);
    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_EJECTOR_2 = upgrade("upgrade_ejector_2", 2);
    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_EJECTOR_3 = upgrade("upgrade_ejector_3", 3);
    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_STACK_1 = upgrade("upgrade_stack_1", 1);
    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_STACK_2 = upgrade("upgrade_stack_2", 2);
    public static final RegistryObject<ItemMachineUpgrade> UPGRADE_STACK_3 = upgrade("upgrade_stack_3", 3);

    private static RegistryObject<ItemConveyorWand> wand(ConveyorType type) {
        RegistryObject<ItemConveyorWand> ro = ITEMS.registerItem("conveyor_wand_" + type.id, p -> new ItemConveyorWand(p, type));
        ModTabs.addTo(HbmTab.MACHINES, ro);
        return ro;
    }

    /** 1.7 {@code ItemMetaUpgrade}: an {@code ItemMachineUpgrade()} with no type, i.e. SPECIAL; the tier is the old metadata + 1. */
    private static RegistryObject<ItemMachineUpgrade> upgrade(String name, int tier) {
        RegistryObject<ItemMachineUpgrade> ro = ITEMS.registerItem(name, p -> new ItemMachineUpgrade(UpgradeType.SPECIAL, tier, p), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    public static ItemStack wand(ConveyorType type, int count) {
        return switch (type) {
            case EXPRESS -> new ItemStack(CONVEYOR_WAND_EXPRESS.get(), count);
            case DOUBLE -> new ItemStack(CONVEYOR_WAND_DOUBLE.get(), count);
            case TRIPLE -> new ItemStack(CONVEYOR_WAND_TRIPLE.get(), count);
            default -> new ItemStack(CONVEYOR_WAND_REGULAR.get(), count);
        };
    }

    /** The old {@code upgrade_ejector} metadata + 1, or 0 when the stack is not one. */
    public static int ejectorTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == UPGRADE_EJECTOR_1.get()) return 1;
        if (stack.getItem() == UPGRADE_EJECTOR_2.get()) return 2;
        if (stack.getItem() == UPGRADE_EJECTOR_3.get()) return 3;
        return 0;
    }

    /** The old {@code upgrade_stack} metadata + 1, or 0 when the stack is not one. */
    public static int stackTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == UPGRADE_STACK_1.get()) return 1;
        if (stack.getItem() == UPGRADE_STACK_2.get()) return 2;
        if (stack.getItem() == UPGRADE_STACK_3.get()) return 3;
        return 0;
    }

    /** 1.7 crane ejection delay: 20 ticks, or 10 / 5 / 2 with an ejector upgrade Mk.I / II / III. */
    public static int ejectorDelay(ItemStack upgrade) {
        return switch (ejectorTier(upgrade)) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };
    }

    /** 1.7 crane stack size: 1 item, or 4 / 16 / 64 with a stack upgrade Mk.I / II / III. */
    public static int stackAmount(ItemStack upgrade) {
        return switch (stackTier(upgrade)) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    public static void init() {}
}
