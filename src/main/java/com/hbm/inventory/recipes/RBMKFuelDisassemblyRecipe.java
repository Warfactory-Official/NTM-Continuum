package com.hbm.inventory.recipes;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKPellet;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Dynamic RBMK rod recycling. The central recipe registrar must register {@link #SERIALIZER} as
 * {@code hbm:rbmk_fuel_disassembly} before the generated data can load.
 */
public final class RBMKFuelDisassemblyRecipe extends CompatCustomRecipe {
    public static final RBMKFuelDisassemblyRecipe INSTANCE = new RBMKFuelDisassemblyRecipe();
    public static final RecipeSerializer<RBMKFuelDisassemblyRecipe> SERIALIZER =
            CustomRecipes.serializer(() -> INSTANCE);

    private RBMKFuelDisassemblyRecipe() {
        super("hbm", "rbmk_fuel_disassembly");
    }

    private static ItemStack rod(List<ItemStack> input) {
        ItemStack rod = ItemStack.EMPTY;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            if (!rod.isEmpty()) return ItemStack.EMPTY;
            rod = stack;
        }
        return rod;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        ItemStack stack = rod(input);
        return stack.getItem() instanceof ItemRBMKRod fuel && fuel.pelletItem() != null
                && ItemRBMKRod.getHullHeat(stack) < 50 && ItemRBMKRod.getCoreHeat(stack) < 50;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        ItemStack stack = rod(input);
        if (!(stack.getItem() instanceof ItemRBMKRod fuel) || fuel.pelletItem() == null
                || ItemRBMKRod.getEnrichment(stack) > 0.99D
                || ItemRBMKRod.getHullHeat(stack) >= 50 || ItemRBMKRod.getCoreHeat(stack) >= 50) return ItemStack.EMPTY;

        int depletion = 4 - Mth.clamp((int) Math.ceil(ItemRBMKRod.getEnrichment(stack) * 5 - 1), 0, 4);
        boolean xenon = ItemRBMKRod.getPoisonLevel(stack) >= 0.5D;
        ItemStack pellets = new ItemStack(fuel.pelletItem(), 8);
        ItemRBMKPellet.Stage stage = new ItemRBMKPellet.Stage(depletion, xenon);
        if (!stage.equals(ItemRBMKPellet.Stage.FRESH)) ModDataComponents.RBMK_PELLET.set(pellets, stage);
        return pellets;
    }

    @Override
    protected RecipeSerializer<RBMKFuelDisassemblyRecipe> serializer() {
        return SERIALIZER;
    }
}
