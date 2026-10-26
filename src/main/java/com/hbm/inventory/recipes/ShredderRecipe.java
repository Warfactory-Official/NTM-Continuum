package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One resolved input and its whole-stack output probability, shared by lookup and processing. */
public final class ShredderRecipe extends GenericRecipe {
    public ShredderRecipe(String name, Item input, ItemStack output, float chance) {
        super(name);
        inputItems(new ComparableStack(input, 1));
        outputItems(output);
        outputChance = new float[] {chance};
        setup(TileEntityMachineShredder.PROCESSING_SPEED, TileEntityMachineShredder.CONSUMPTION);
    }

    public ItemStack output() { return outputItem[0]; }

    public ItemStack roll(RandomSource random) {
        var probability = outputChance == null ? 1F : outputChance[0];
        return probability >= 1F || random.nextFloat() < probability ? output().copy() : ItemStack.EMPTY;
    }
}
