package com.hbm.inventory.recipes;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockCap;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * The machine recipes of the crate/deco wave, each handed to its registry from one line in that registry's own
 * registration: 1.7 {@code AssemblyMachineRecipes} "ass.cap*" (two stacks of 64 caps into a cap block),
 * {@code AnvilRecipes} recycling of the FM radio and the filing cabinet, and {@code ShredderRecipes} for the
 * tungsten crate and the bobbleheads (1.7 gave each bobblehead its own {@code scrap_plastic} subtype; the port's
 * plastic scrap has none, so every figure shreds to the one item).
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

    /** 1.7 AnvilRecipes: the FM radio (tier 2) and the filing cabinet (tier 1) recycling. */
    public static void anvil(List<AnvilConstructionRecipe> recipes) {
        recipes.add(new AnvilConstructionRecipe(new ComparableStack(ModDecoCrateBlocks.radiorec.get().asItem()),
                new AnvilOutput[]{
                        new AnvilOutput(stack("plate_steel", 4)),
                        new AnvilOutput(stack("wire_fine_copper", 1)),
                        new AnvilOutput(stack("circuit_vacuum_tube", 1), 0.5F),
                        new AnvilOutput(stack("ingot_polymer", 1), 0.25F),
                }).setTier(2));
        recipes.add(new AnvilConstructionRecipe(new ComparableStack(ModDecoCrateBlocks.filing_cabinet.get().asItem()),
                new AnvilOutput[]{
                        new AnvilOutput(stack("plate_steel", 2)),
                        new AnvilOutput(stack("plate_steel", 2), 0.5F),
                        new AnvilOutput(stack("plate_polymer", 2), 0.25F),
                        new AnvilOutput(stack("scrap", 1)),
                }).setTier(1)); // 1.7 AnvilRecipes: the filing cabinet is tier 1 (the radio tier 2)
    }

    /** 1.7 ShredderRecipes:281 and 337-341. */
    public static void shredder(BiConsumer<Item, ItemStack> set) {
        set.accept(ModDecoCrateBlocks.crate_tungsten.get().asItem(), stack("powder_tungsten", 36));
        set.accept(ModDecoCrateBlocks.bobblehead.get().asItem(), stack("scrap_plastic", 1));
    }
}
