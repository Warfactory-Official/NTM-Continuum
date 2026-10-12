package com.hbm.handler.jei;

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import dev.golem.core.viewer.ViewerPlugin;

/** One-call hooks for 1.7 {@code NEIConfig}'s loop over {@code CustomMachineConfigJSON.niceList}. */
public final class CustomMachineJeiIntegration {
    private CustomMachineJeiIntegration() {}

    public static void registerCategories(ViewerPlugin.Categories registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.add(new CustomMachineRecipeHandler(conf));
        }
    }

    public static void registerRecipes(ViewerPlugin.Recipes registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.add(CustomMachineRecipeHandler.type(conf), CustomMachineRecipeHandler.buildRecipes(conf));
        }
    }

    public static void registerCatalysts(ViewerPlugin.Stations registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.add(CustomMachineRecipeHandler.type(conf), CustomMachineRecipeHandler.machine(conf));
        }
    }
}
