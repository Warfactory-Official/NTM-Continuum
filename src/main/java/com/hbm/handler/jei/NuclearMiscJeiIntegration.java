package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.jei.Jei;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.ItemStack;

/** One-call hooks that keep the radiolysis and breeder JEI registrations out of the shared plugin tables. */
public final class NuclearMiscJeiIntegration {
    private NuclearMiscJeiIntegration() {}

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new RadiolysisRecipeHandler(registration.getJeiHelpers().getGuiHelper()),
                new BreederRecipeHandler(registration.getJeiHelpers().getGuiHelper()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        Jei.addRecipes(registration, RadiolysisRecipeHandler.TYPE, RadiolysisRecipeHandler.buildRecipes());
        Jei.addRecipes(registration, BreederRecipeHandler.TYPE, BreederRecipeHandler.buildRecipes());
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        Jei.addCraftingStation(registration, RadiolysisRecipeHandler.TYPE, new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get()));
        Jei.addCraftingStation(registration, BreederRecipeHandler.TYPE, new ItemStack(ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get()));
    }
}
//?}
