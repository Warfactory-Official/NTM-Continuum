package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModOilItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Combination-furnace recipes ported from the 1.12.2 {@code CombinationRecipes} defaults. */
public final class CombinationRecipes {

    private CombinationRecipes() {}

    public record CombinationRecipe(AStack input, ItemStack itemOutput, FluidStack fluidOutput) {}

    private static final List<CombinationRecipe> RECIPES = new ArrayList<>();
    private static boolean initialized;

    public static CombinationRecipe getOutput(ItemStack stack) {
        if (!initialized) register();
        if (stack == null || stack.isEmpty()) return null;
        for (CombinationRecipe recipe : RECIPES) {
            if (recipe.input().matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }

    public static List<CombinationRecipe> getRecipes() {
        if (!initialized) register();
        return Collections.unmodifiableList(RECIPES);
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        add(Mats.MAT_COAL.gem(), ModOilItems.coke(EnumCokeType.COAL, 1), fluid(Fluids.COALCREOSOTE, 100));
        add(Mats.MAT_COAL.dust(), ModOilItems.coke(EnumCokeType.COAL, 1), fluid(Fluids.COALCREOSOTE, 100));
        add(Mats.MAT_LIGNITE.gem(), ModOilItems.coke(EnumCokeType.LIGNITE, 1), fluid(Fluids.COALCREOSOTE, 50));
        add(Mats.MAT_LIGNITE.dust(), ModOilItems.coke(EnumCokeType.LIGNITE, 1), fluid(Fluids.COALCREOSOTE, 50));

        add(Mats.MAT_CHLOROCALCITE.dust(), new ItemStack(ModItems.powder_calcium.get()), fluid(Fluids.CHLORINE, 250));
        add(Mats.MAT_MOLYSITE.dust(), new ItemStack(Items.IRON_INGOT), fluid(Fluids.CHLORINE, 250));
        add(Mats.MAT_CINNABAR.crystal(), new ItemStack(ModItems.sulfur.get()), fluid(Fluids.MERCURY, 100));
        add(new ComparableStack(Items.GLOWSTONE_DUST), new ItemStack(ModItems.sulfur.get()), fluid(Fluids.CHLORINE, 100));
        add(Mats.MAT_SODALITE.gem(), new ItemStack(ModItems.powder_sodium.get()), fluid(Fluids.CHLORINE, 100));
        add(new ComparableStack(ModIntermediateItems.chunk(com.hbm.items.ItemEnums.EnumChunkType.CRYOLITE)),
                new ItemStack(ModItems.powder_aluminium.get()), fluid(Fluids.LYE, 150));
        // Preserved even though the legacy canSmelt() rejects its null item output.
        add(Mats.MAT_SODIUM.dust(), null, fluid(Fluids.SODIUM, 100));
        add(new TagStack(tag("dusts/limestone")), new ItemStack(ModItems.powder_calcium.get()), fluid(Fluids.CARBONDIOXIDE, 50));

        add(new TagStack(ItemTags.LOGS), new ItemStack(Items.CHARCOAL), fluid(Fluids.WOODOIL, 250));
        add(new TagStack(ItemTags.SAPLINGS), ModIngredientItems.ash(EnumAshType.WOOD, 1), fluid(Fluids.WOODOIL, 50));

        add(new ComparableStack(ModOilItems.tar(EnumTarType.CRUDE)), ModOilItems.coke(EnumCokeType.PETROLEUM, 1), null);
        add(new ComparableStack(ModOilItems.tar(EnumTarType.CRACK)), ModOilItems.coke(EnumCokeType.PETROLEUM, 1), null);
        add(new ComparableStack(ModOilItems.tar(EnumTarType.COAL)), ModOilItems.coke(EnumCokeType.COAL, 1), null);
        add(new ComparableStack(ModOilItems.tar(EnumTarType.WOOD)), ModOilItems.coke(EnumCokeType.COAL, 1), null);

        add(new ComparableStack(Items.SUGAR_CANE), new ItemStack(Items.SUGAR, 2), fluid(Fluids.ETHANOL, 50));
        add(new ComparableStack(Blocks.CLAY.asItem()), new ItemStack(Blocks.BRICKS.asItem()), null);
        dev.golem.core.content.Replay.apply(CombinationRecipes.class);
    }

    public static void putFirst(CombinationRecipe recipe) {
        register();
        RECIPES.add(0, recipe);
    }

    public static boolean removeIf(Predicate<CombinationRecipe> filter) {
        register();
        return RECIPES.removeIf(filter);
    }

    public static synchronized void reset() {
        RECIPES.clear();
        initialized = false;
    }

    private static void add(AStack input, ItemStack itemOutput, FluidStack fluidOutput) {
        RECIPES.add(new CombinationRecipe(input, itemOutput, fluidOutput));
    }

    private static FluidStack fluid(com.hbm.api.fluid.FluidType type, int amount) {
        return new FluidStack(type, amount);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Ids.of("c", path));
    }
}
