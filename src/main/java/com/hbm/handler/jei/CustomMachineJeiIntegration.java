package com.hbm.handler.jei;

//? if jei {

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import dev.golem.core.jei.Jei;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

/** One-call hooks for 1.7 {@code NEIConfig}'s loop over {@code CustomMachineConfigJSON.niceList}. */
public final class CustomMachineJeiIntegration {
    private CustomMachineJeiIntegration() {}

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.addRecipeCategories(new CustomMachineRecipeHandler(registration.getJeiHelpers().getGuiHelper(), conf));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            Jei.addRecipes(registration, CustomMachineRecipeHandler.type(conf), CustomMachineRecipeHandler.buildRecipes(conf));
        }
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            Jei.addCraftingStation(registration, CustomMachineRecipeHandler.type(conf), CustomMachineRecipeHandler.machine(conf));
        }
    }
}
//?}
