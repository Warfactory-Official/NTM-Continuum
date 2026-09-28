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

    /** 1.7 PressRecipes:70-72 (briquettes, flat stamp) and 101-108 (the eight printing plates). */
    public static void press() {
        PressRecipes.makeRecipe(StampType.FLAT, Mats.MAT_COAL.dust(), new ItemStack(ModFinalConsumableItems.briquette_coal.get()));
        PressRecipes.makeRecipe(StampType.FLAT, Mats.MAT_LIGNITE.dust(), new ItemStack(ModFinalConsumableItems.briquette_lignite.get()));
        PressRecipes.makeRecipe(StampType.FLAT, item(ModItems.powder_sawdust.get()), new ItemStack(ModFinalConsumableItems.briquette_wood.get()));
        for (int i = 1; i <= 8; i++) {
            PressRecipes.makeRecipe(StampType.valueOf("PRINTING" + i), item(Items.PAPER),
                    new ItemStack(ModFinalConsumableItems.flat("page_of_page" + i)));
        }
    }

    public interface CombinationSink {
        void add(AStack input, ItemStack itemOutput, FluidStack fluidOutput);
    }

    /** 1.7 CombinationRecipes:43/47/60, the three briquettes. */
    public static void combination(CombinationSink sink) {
        sink.add(item(ModFinalConsumableItems.briquette_coal.get()), ModOilItems.coke(EnumCokeType.COAL, 1),
                new FluidStack(Fluids.COALCREOSOTE, 150));
        sink.add(item(ModFinalConsumableItems.briquette_lignite.get()), ModOilItems.coke(EnumCokeType.LIGNITE, 1),
                new FluidStack(Fluids.COALCREOSOTE, 100));
        sink.add(item(ModFinalConsumableItems.briquette_wood.get()), new ItemStack(Items.CHARCOAL),
                new FluidStack(Fluids.WOODOIL, 500));
    }

    /** 1.7 AnvilRecipes:94, tier 4: alexandrite + Nuka-Cola = shield infusion. */
    public static void smithing(List<AnvilSmithingRecipe> recipes) {
        recipes.add(new AnvilSmithingRecipe(4, new ItemStack(ModFinalConsumableItems.flask_infusion_shield.get()),
                item(ModItems.gem_alexandrite.get()), item(ModConsumableTailItems.bottle_nuka.get())));
    }

    /** 1.7 TileEntityDiFurnace.getItemPower: every briquette burns for 200. */
    public static void diFurnaceFuels(List<FuelEntry> entries) {
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_coal.get()), 200));
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_lignite.get()), 200));
        entries.add(new FuelEntry(item(ModFinalConsumableItems.briquette_wood.get()), 200));
    }
}
