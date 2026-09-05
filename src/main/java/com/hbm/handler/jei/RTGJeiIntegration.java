package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModRtgMenus;
import com.hbm.inventory.container.ContainerMachineRTG;
import com.hbm.inventory.gui.GUIMachineRTG;
import com.hbm.inventory.gui.RtgGuiLayout;
import dev.golem.core.jei.Jei;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.item.ItemStack;

/** One-call hooks that keep every RTG-specific JEI registration out of the shared plugin tables. */
public final class RTGJeiIntegration {
    private RTGJeiIntegration() {}

    public static void registerCategory(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RTGRecipeHandler(registration.getJeiHelpers().getGuiHelper()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        Jei.addRecipes(registration, RTGRecipeHandler.TYPE, RTGRecipeHandler.buildRecipes());
    }

    public static void registerCatalyst(IRecipeCatalystRegistration registration) {
        Jei.addCraftingStation(registration, RTGRecipeHandler.TYPE,
                new ItemStack(ModMachineBlocks.machine_rtg_grey.get()));
    }

    public static void registerGuiHandler(IGuiHandlerRegistration registration) {
        // The old registration covered the heat gauge. Keep that gauge's legacy tooltip intact and use
        // the empty separator beside it for JEI's "Show Recipes" affordance instead.
        Jei.addRecipeClickArea(registration, GUIMachineRTG.class,
                RtgGuiLayout.RECIPE_CLICK_X, RtgGuiLayout.RECIPE_CLICK_Y,
                RtgGuiLayout.RECIPE_CLICK_WIDTH, RtgGuiLayout.RECIPE_CLICK_HEIGHT,
                RTGRecipeHandler.TYPE);
    }

    public static void registerTransfer(IRecipeTransferRegistration registration) {
        Jei.addRecipeTransferHandler(registration, ContainerMachineRTG.class, ModRtgMenus.MACHINE_RTG.get(),
                RTGRecipeHandler.TYPE, 0, 15, 15, 36);
    }
}
//?}
