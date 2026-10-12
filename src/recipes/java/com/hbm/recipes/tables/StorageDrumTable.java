package com.hbm.recipes.tables;

import com.hbm.items.special.ItemWasteStorage.LongWasteClass;
import com.hbm.items.special.ItemWasteStorage.ShortWasteClass;
import com.hbm.items.ModItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.inventory.recipes.StorageDrumRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code TileEntityStorageDrum.updateEntity}, the decay rules it hard-codes, one row per input item (1.7 metadata is
 * one item per waste class). {@code decay} names the config chance ({@code VersatileConfig.getLongDecayChance} or
 * {@code getShortDecayChance}) and {@code divisor} what 1.7 divided it by. Short waste is looked up with
 * {@code ItemWasteLong.rectify} (meta % 5, the long family's size) as in 1.7, so Pu-241, Am-242 and Sa-326 short waste
 * release U-235's, U-233's and Np-237's liquid and gas. 1.7's {@code ingot_mercury} is the item registered as
 * {@code nugget_mercury}, its {@code nugget_mercury} {@code nugget_mercury_tiny}.
 */
public final class StorageDrumTable extends RecipeSource<Row> {

    public StorageDrumTable() {
        super(StorageDrumRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        for (int meta = 0; meta < ModStorageDrumItems.nuclear_waste_long.size(); meta++) {
            LongWasteClass wasteClass = LongWasteClass.values()[rectify(meta)];
            put(ModStorageDrumItems.nuclear_waste_long.stack(meta), ModStorageDrumItems.nuclear_waste_long_depleted.stack(meta),
                    "long", 1, wasteClass.liquid, wasteClass.gas);
        }

        for (int meta = 0; meta < ModStorageDrumItems.nuclear_waste_long_tiny.size(); meta++) {
            LongWasteClass wasteClass = LongWasteClass.values()[rectify(meta)];
            put(ModStorageDrumItems.nuclear_waste_long_tiny.stack(meta), ModStorageDrumItems.nuclear_waste_long_depleted_tiny.stack(meta),
                    "long", 10, wasteClass.liquid / 10, wasteClass.gas / 10);
        }

        for (int meta = 0; meta < ModStorageDrumItems.nuclear_waste_short.size(); meta++) {
            ShortWasteClass wasteClass = ShortWasteClass.values()[rectify(meta)];
            put(ModStorageDrumItems.nuclear_waste_short.stack(meta), ModStorageDrumItems.nuclear_waste_short_depleted.stack(meta),
                    "short", 1, wasteClass.liquid, wasteClass.gas);
        }

        for (int meta = 0; meta < ModStorageDrumItems.nuclear_waste_short_tiny.size(); meta++) {
            ShortWasteClass wasteClass = ShortWasteClass.values()[rectify(meta)];
            put(ModStorageDrumItems.nuclear_waste_short_tiny.stack(meta), ModStorageDrumItems.nuclear_waste_short_depleted_tiny.stack(meta),
                    "short", 10, wasteClass.liquid / 10, wasteClass.gas / 10);
        }

        put(new ItemStack(ModItems.ingot_au198.get()), new ItemStack(ModItems.nugget_mercury.get()), "short", 20, 0, 0);
        put(new ItemStack(ModItems.nugget_au198.get()), new ItemStack(ModItems.nugget_mercury_tiny.get()), "short", 100, 0, 0);

        put(new ItemStack(ModItems.ingot_pb209.get()), new ItemStack(ModItems.ingot_bismuth.get()), "short", 10, 0, 0);
        put(new ItemStack(ModItems.nugget_pb209.get()), new ItemStack(ModItems.nugget_bismuth.get()), "short", 50, 0, 0);

        put(new ItemStack(ModItems.powder_sr90.get()), new ItemStack(ModItems.powder_zirconium.get()), "short", 10, 0, 0);
        put(new ItemStack(ModItems.nugget_sr90.get()), new ItemStack(ModItems.nugget_zirconium.get()), "short", 50, 0, 0);
    }

    /** 1.7 {@code ItemWasteLong.rectify}: {@code abs(meta) % } the long waste class count, used for both families. */
    private static int rectify(int meta) {
        return Math.abs(meta) % LongWasteClass.values().length;
    }

    /** One decay rule; the id is the input item. */
    private void put(ItemStack input, ItemStack output, String decay, int divisor, int liquid, int gas) {
        register(BuiltInRegistries.ITEM.getKey(input.getItem()).getPath(),
                new Row(ItemOut.of(input), ItemOut.of(output), decay, divisor, liquid, gas));
    }
}
