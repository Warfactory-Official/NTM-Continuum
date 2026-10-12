package com.hbm.handler.jei;

import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.viewer.ViewerPlugin;
import net.minecraft.world.item.ItemStack;

/** One-call hooks that keep the radiolysis and breeder recipe-viewer registrations out of the shared plugin tables. */
public final class NuclearMiscJeiIntegration {
    private NuclearMiscJeiIntegration() {}

    public static void registerCategories(ViewerPlugin.Categories registration) {
        registration.add(new RadiolysisRecipeHandler(), new BreederRecipeHandler());
    }

    public static void registerRecipes(ViewerPlugin.Recipes registration) {
        registration.add(RadiolysisRecipeHandler.TYPE, RadiolysisRecipeHandler.buildRecipes());
        registration.add(BreederRecipeHandler.TYPE, BreederRecipeHandler.buildRecipes());
    }

    public static void registerCatalysts(ViewerPlugin.Stations registration) {
        registration.add(RadiolysisRecipeHandler.TYPE, new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get()));
        registration.add(BreederRecipeHandler.TYPE, new ItemStack(ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get()));
    }
}
