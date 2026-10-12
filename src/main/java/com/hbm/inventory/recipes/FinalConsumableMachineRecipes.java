package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.DiFurnaceRecipes.FuelEntry;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemStamp.StampType;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The machine recipes of the final consumables wave, each handed to its registry from one line in that
 * registry's own registration (the pattern of {@link AbilityToolMachineRecipes}).
 */
public final class FinalConsumableMachineRecipes {

    private FinalConsumableMachineRecipes() {}

    private static ComparableStack item(Item item) {
        return new ComparableStack(item);
    }

    /** 1.7 TileEntityDiFurnace.getItemPower: every briquette burns for 200. */
    public static void diFurnaceFuels(List<FuelEntry> entries) {
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_coal.get()), 200));
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_lignite.get()), 200));
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_wood.get()), 200));
    }
}
