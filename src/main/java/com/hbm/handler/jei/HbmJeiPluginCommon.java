package com.hbm.handler.jei;

//? if jei {

import dev.golem.core.jei.JeiViewer;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;

/**
 * The loader-agnostic body of the mod's JEI plugin. Each loader's thin {@code @JeiPlugin} entrypoint delegates its
 * phases here, and each phase hands HBM's recipe-viewer declarations ({@link HbmRecipeViewer},
 * {@link HbmViewerSubtypes}) to Golem Core's {@link JeiViewer}; the REI plugins ({@code com.hbm.handler.rei}) feed the
 * same declarations to REI. Only the JEI fluid-ingredient read-back below is JEI's alone.
 */
public final class HbmJeiPluginCommon {

    private HbmJeiPluginCommon() {}

    public static void registerItemSubtypes(ISubtypeRegistration reg) {
        JeiViewer.subtypes(reg, HbmViewerSubtypes::register);
    }

    public static void registerExtraIngredients(IExtraIngredientRegistration reg) {
        JeiViewer.ingredients(reg, HbmRecipeViewer.INSTANCE);
    }

    public static void registerCategories(IRecipeCategoryRegistration reg) {
        JeiViewer.categories(reg, HbmRecipeViewer.INSTANCE);
    }

    public static void registerRecipes(IRecipeRegistration reg) {
        JeiViewer.recipes(reg, HbmRecipeViewer.INSTANCE);
    }

    public static void registerGuiHandlers(IGuiHandlerRegistration reg) {
        JeiViewer.clickAreas(reg, HbmRecipeViewer.INSTANCE);
    }

    public static void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        JeiViewer.transfers(reg, HbmRecipeViewer.INSTANCE);
    }

    public static void registerCatalysts(IRecipeCatalystRegistration reg) {
        JeiViewer.stations(reg, HbmRecipeViewer.INSTANCE);
    }

    /**
     * Runs once JEI's runtime exists: HBM's runtime categories (the arc furnace's furnace pass, {@link HbmRecipeViewer#runtime}),
     * then the JEI-only read-back of HBM fluids as JEI fluid ingredients (History section 19ek-fluids): JEI lists every
     * registered source fluid itself; this only reads the list back, for the log and the harness.
     */
    public static void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        JeiViewer.runtime(runtime, HbmRecipeViewer.INSTANCE);
        java.util.List<String> fluids = hbmFluidIngredients(runtime);
        com.hbm.client.ForeignFluidHarnessQueries.jeiFluids = () -> hbmFluidIngredients(runtime);
        com.hbm.Constants.LOG.info("[JEI] {} HBM fluid(s) listed as fluid ingredients", fluids.size());
    }

    /** The display names of the JEI fluid ingredients that are HBM fluids. */
    private static java.util.List<String> hbmFluidIngredients(mezz.jei.api.runtime.IJeiRuntime runtime) {
        return hbmFluidIngredients(runtime, runtime.getJeiHelpers().getPlatformFluidHelper());
    }

    private static <T> java.util.List<String> hbmFluidIngredients(mezz.jei.api.runtime.IJeiRuntime runtime,
            mezz.jei.api.helpers.IPlatformFluidHelper<T> fluids) {
        mezz.jei.api.ingredients.IIngredientTypeWithSubtypes<net.minecraft.world.level.material.Fluid, T> type =
                fluids.getFluidIngredientType();
        mezz.jei.api.ingredients.IIngredientHelper<T> helper = runtime.getIngredientManager().getIngredientHelper(type);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (T ingredient : runtime.getIngredientManager().getAllIngredients(type)) {
            if (dev.golem.core.fluid.TokenFluids.isToken(type.getBase(ingredient))) names.add(helper.getDisplayName(ingredient));
        }
        return names;
    }
}
//?}
