package com.hbm.inventory.recipes;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockCap;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.block.ItemBlockDecoVariant;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * The machine recipes of the crate and deco blocks, each handed to its registry from one line in that registry's own
 * registration: 1.7 {@code AssemblyMachineRecipes} "ass.cap*" (two stacks of 64 caps into a cap block),
 * {@code AnvilRecipes} recycling of the FM radio and the filing cabinet, and the bobblehead rule of
 * {@code ShredderRecipes} (each figure shreds to its own {@code scrap_plastic_<type>}, see {@link #bobbleScrap}; the
 * tungsten crate and plain bobblehead rows are files of the shredder table, M2 R-C1).
 */
public final class DecoCrateMachineRecipes {

    private DecoCrateMachineRecipes() {}

    private static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /** 1.7 AssemblyMachineRecipes:237-248. */
    public static void assembly(GenericRecipes recipes) {
        for (BlockCap.EnumCapBlock type : BlockCap.EnumCapBlock.values()) {
            String name = type.name().toLowerCase(Locale.US);
            ItemStack cap = stack("cap_" + name, 1);
            if (cap.isEmpty() || recipes.getRecipe("ass.cap" + name) != null) continue;
            recipes.registerGenerated(new GenericRecipe("ass.cap" + name).setup(10, 100)
                    .outputItems(new ItemStack(ModDecoCrateBlocks.CAP_BLOCKS.get(type).get(), 1))
                    .inputItems(new ComparableStack(cap.getItem(), 64), new ComparableStack(cap.getItem(), 64)));
        }
    }

    /** 1.7 ShredderRecipes:338-341: a bobblehead gives the plastic scrap of its {@code BobbleType.scrap}; else null. */
    public static ItemStack bobbleScrap(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlockDecoVariant item)
                || item.getBlock() != ModDecoCrateBlocks.bobblehead.get()) return null;
        BlockBobble.BobbleType type =
                BlockBobble.BobbleType.of(item.variant(stack));
        ItemStack scrap = stack("scrap_plastic_" + type.scrap.toLowerCase(Locale.US), 1);
        return scrap.isEmpty() ? null : scrap;
    }
}
