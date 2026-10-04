package com.hbm.inventory.recipes;

import com.hbm.items.ModPwrItems;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemZirnoxWaste;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code FuelPoolRecipes}: what the spent-fuel pool drum ({@code TileEntityWasteDrum}) cools, hot stack to cold stack.
 * The 1.7 table holds the 9 hot ZIRNOX wastes, the 7 hot research-reactor plate wastes and every hot PWR rod. In the port
 * the first two keep their item and carry a hot flag ({@code fuel_waste_hot}); hot PWR rods are separate items. 1.7's
 * RBMK entries are display-only: the drum cools any RBMK rod through its own heat branch, not through this table.
 */
public final class FuelPoolRecipes {

    private FuelPoolRecipes() {}

    /** True for a 1.7 FuelPool key: an item the drum turns into its cooled form. */
    public static boolean isHot(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemZirnoxWaste) return ItemZirnoxWaste.isHot(stack);
        if (stack.getItem() instanceof ItemDepletedFuel) return ItemDepletedFuel.isHot(stack);
        return hotPwr(stack) != null;
    }

    /** The cooled form of a hot stack (count kept), or {@link ItemStack#EMPTY} if the stack is not a key. */
    public static ItemStack cool(ItemStack stack) {
        if (!isHot(stack)) return ItemStack.EMPTY;
        if (stack.getItem() instanceof ItemZirnoxWaste) {
            ItemStack out = stack.copy();
            ItemZirnoxWaste.setHot(out, false);
            return out;
        }
        if (stack.getItem() instanceof ItemDepletedFuel) {
            ItemStack out = stack.copy();
            ItemDepletedFuel.setHot(out, false);
            return out;
        }
        EnumPWRFuel fuel = hotPwr(stack);
        return ItemStacks.copyWithCount(new ItemStack(ModPwrItems.depleted(fuel)), stack.getCount());
    }

    private static EnumPWRFuel hotPwr(ItemStack stack) {
        for (EnumPWRFuel fuel : EnumPWRFuel.values()) {
            if (stack.getItem() == ModPwrItems.hot(fuel)) return fuel;
        }
        return null;
    }
}
